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

import org.apache.sshd.agent.SshAgent;
import org.apache.sshd.agent.local.AgentImpl;
import org.apache.sshd.agent.local.LocalAgentFactory;
import org.apache.sshd.client.ClientBuilder;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ClientChannel;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.RuntimeSshException;
import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.apache.sshd.core.CoreModuleProperties;

import org.jboss.qa.jcontainer.Client;

import org.fusesource.jansi.AnsiConsole;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KarafClient<T extends KarafConfiguration> extends Client<T> {

	protected ClientSession session;
	protected SshClient client;
	private String commandResult;

	public KarafClient(T configuration) {
		super(configuration);
	}
	@Override
	protected void closeInternal() throws IOException {
		if (session != null) {
			session.close(true);
		}
		session = null;
		if (client != null) {
			client.stop();
		}
		client = null;
	}

	@Override
	public boolean isConnected() {
		return (session != null && !session.isClosed());
	}

	@Override
	protected void connectInternal() throws Exception {
		log.info("Connecting to server {}:{}", configuration.getHost(), configuration.getSshPort());
		client = ClientBuilder.builder().build();
		setupAgent(configuration.getUsername(), configuration.getKeyFile() != null ? configuration.getKeyFile().getAbsolutePath().toString() : null,
				client, (session, resourceKey, retryIndex) -> configuration.getPassword());
		CoreModuleProperties.HEARTBEAT_INTERVAL.set(client, Duration.ofMillis(60000));
		CoreModuleProperties.IDLE_TIMEOUT.set(client, Duration.ofMillis(1800000L));
		CoreModuleProperties.NIO2_READ_TIMEOUT.set(client, Duration.ofMillis(1800000L));
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
		final ClientChannel channel = session.createChannel("exec", command.concat(System.getProperty("line.separator")));
		try (
				InputStream in = new ByteArrayInputStream(new byte[0]);
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				ByteArrayOutputStream err = new ByteArrayOutputStream()
		) {
			channel.setIn(in);
			AnsiConsole.systemInstall();
			channel.setOut(out);
			channel.setErr(err);

			channel.open();
			channel.waitFor(Arrays.asList(ClientChannelEvent.STDOUT_DATA, ClientChannelEvent.STDERR_DATA), 10000L);

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

	private void setupAgent(String user, String keyFile, SshClient client, FilePasswordProvider passwordProvider) {
		final SshAgent agent = startAgent(user, keyFile, passwordProvider);
		client.setAgentFactory(new LocalAgentFactory(agent));
		client.getProperties().put(SshAgent.SSH_AUTHSOCKET_ENV_NAME, "local");
	}

	private SshAgent startAgent(String user, String keyFile, FilePasswordProvider passwordProvider) {
		try {
			final SshAgent agent = new AgentImpl();
			if (keyFile != null) {
				final FileKeyPairProvider fileKeyPairProvider = new FileKeyPairProvider(Paths.get(keyFile));
				fileKeyPairProvider.setPasswordFinder(passwordProvider);
				for (KeyPair key : fileKeyPairProvider.loadKeys(null)) {
					agent.addIdentity(key, user);
				}
			}
			return agent;
		} catch (Throwable e) {
			log.error("Error starting ssh agent for: " + e.getMessage());
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

	public String getCommandResult() {
		return commandResult;
	}
}
