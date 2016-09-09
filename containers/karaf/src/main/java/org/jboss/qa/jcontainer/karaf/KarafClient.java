/*
 * Copyright 2015 Red Hat Inc. and/or its affiliates and other contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.qa.jcontainer.karaf;

import org.apache.sshd.ClientChannel;
import org.apache.sshd.ClientSession;
import org.apache.sshd.SshClient;
import org.apache.sshd.agent.SshAgent;
import org.apache.sshd.agent.local.AgentImpl;
import org.apache.sshd.agent.local.LocalAgentFactory;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.common.RuntimeSshException;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;

import org.jboss.qa.jcontainer.Client;

import org.fusesource.jansi.AnsiConsole;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.security.KeyPair;
import java.util.List;
import java.util.concurrent.TimeUnit;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KarafClient<T extends KarafConfiguration> extends Client<T> {

	/**
	 * Error message for failed command.
	 * See: https://goo.gl/cZQSFy
	 */
	private static final String COMMAND_FAIL_MSG = "Error executing command";
	private static final String NEW_LINE = System.getProperty("line.separator");

	@Getter
	private String commandResult;

	protected SshClient client;
	protected ClientSession session;

	public KarafClient(T configuration) {
		super(configuration);
	}

	@Override
	protected void closeInternal() throws IOException {
		session.close(true);
		session = null;
		client.stop();
		client = null;
	}

	@Override
	public boolean isConnected() {
		return (session != null && !session.isClosed());
	}

	@Override
	protected void connectInternal() throws Exception {
		log.info("Connecting to server {}:{}", configuration.getHost(), configuration.getSshPort());
		client = SshClient.setUpDefaultClient();
		setupAgent(configuration.getUsername(), configuration.getKeyFile(), client);
		client.start();
		connect(client);
		if (configuration.getPassword() != null) {
			session.addPasswordIdentity(configuration.getPassword());
		}
		session.auth().verify();
	}

	@Override
	protected void executeInternal(String command) throws Exception {
		commandResult = null; // executing new command, reset previous result
		final ClientChannel channel = session.createChannel("exec", command.concat(NEW_LINE));
		try (
				InputStream in = new ByteArrayInputStream(new byte[0]);
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				ByteArrayOutputStream err = new ByteArrayOutputStream()
		) {
			channel.setIn(in);
			channel.setOut(AnsiConsole.wrapOutputStream(out));
			channel.setErr(AnsiConsole.wrapOutputStream(err));

			channel.open();
			channel.waitFor(ClientChannel.CLOSED, 0);

			out.writeTo(System.out);
			err.writeTo(System.err);

			commandResult = out.toString();
			final boolean isError = (channel.getExitStatus() != null && channel.getExitStatus() != 0);

			if (isError) {
				log.error(commandResult);
				throw new IllegalArgumentException(String.format("Operation '%s' failed", command));
			}
		} finally {
			channel.close(true);
		}
	}

	@Override
	protected void executeInternal(List<String> commands) throws Exception {
		final StringWriter sw = new StringWriter();
		final PrintWriter pw = new PrintWriter(sw, true);
		for (String cmd : commands) {
			cmd = cmd.trim();
			if (cmd.startsWith("#")) {
				continue;
			}
			pw.println(cmd);
		}
		executeInternal(sw.toString());
	}

	protected void setupAgent(String user, File keyFile, SshClient client) {
		final URL builtInPrivateKey = KarafClient.class.getClassLoader().getResource("karaf.key");
		final SshAgent agent = startAgent(user, builtInPrivateKey, keyFile);
		client.setAgentFactory(new LocalAgentFactory(agent));
		client.getProperties().put(SshAgent.SSH_AUTHSOCKET_ENV_NAME, "local");
	}

	protected SshAgent startAgent(String user, URL privateKeyUrl, File keyFile) {
		try (InputStream is = privateKeyUrl.openStream()) {
			final SshAgent agent = new AgentImpl();
			final ObjectInputStream r = new ObjectInputStream(is);
			final KeyPair keyPair = (KeyPair) r.readObject();
			is.close();
			agent.addIdentity(keyPair, user);
			if (keyFile != null) {
				final String[] keyFiles = new String[] {keyFile.getAbsolutePath()};
				final FileKeyPairProvider fileKeyPairProvider = new FileKeyPairProvider(keyFiles);
				for (KeyPair key : fileKeyPairProvider.loadKeys()) {
					agent.addIdentity(key, user);
				}
			}
			return agent;
		} catch (Exception e) {
			log.error("Error starting ssh agent for: " + e.getMessage(), e);
			return null;
		}
	}

	protected void connect(SshClient client) throws IOException, InterruptedException {
		int attempts = 10;
		do {
			final ConnectFuture future = client.connect(configuration.getUsername(), configuration.getHost(),
					configuration.getSshPort());
			future.await();
			try {
				session = future.getSession();
			} catch (RuntimeSshException ex) {
				if (--attempts > 0) {
					Thread.sleep(TimeUnit.SECONDS.toMillis(2));
					log.info("Waiting for SSH connection...");
				} else {
					throw ex;
				}
			}
		} while (session == null);
	}
}
