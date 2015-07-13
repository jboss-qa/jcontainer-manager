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
package org.jboss.qa.jcontainer.wildfly;

import org.jboss.as.cli.CliInitializationException;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.cli.impl.ExtendedCommandContextImpl;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;
import org.jboss.qa.jcontainer.Client;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.RealmCallback;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WildflyClient<T extends WildflyConfiguration> extends Client<T> {

	protected CommandContext context;

	public WildflyClient(T configuration) {
		super(configuration);
	}

	@Override
	public boolean isConnected() {
		return context != null;
	}

	@Override
	protected void connectInternal() throws Exception {
		final ModelControllerClient client = createClient(InetAddress.getByName(configuration.getHost()),
				configuration.getManagementPort(), configuration.getUsername(), configuration.getPassword());
		context = createContext(client);
	}

	@Override
	protected void executeInternal(String command) throws Exception {
		try {
			context.handle(command);
		} catch (CommandLineException e) {
			if (context.getBatchManager().isBatchActive()) {
				context.getBatchManager().discardActiveBatch();
			}
			throw new IllegalArgumentException(String.format("Command execution failed for command '%s'. %s", command,
					e.getLocalizedMessage()), e);
		}
	}

	@Override
	protected void executeInternal(List<String> commands) throws Exception {
		for (String cmd : commands) {
			executeInternal(cmd);
		}
		Thread.sleep(500); // NOTE: Result of the operation is not visible immediately
	}

	@Override
	protected void closeInternal() throws IOException {
		context.terminateSession();
		context = null;
	}

	public ModelNode getCommandResult() {
		if (isConnected()) {
			return ((ExtendedCommandContextImpl) context).getLastResult();
		}
		return null;
	}

	protected ModelControllerClient createClient(final InetAddress host, final int port,
												 final String username, final String password) {
		final CallbackHandler callbackHandler = new CallbackHandler() {
			public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
				for (Callback current : callbacks) {
					if (current instanceof NameCallback) {
						final NameCallback ncb = (NameCallback) current;
						ncb.setName(username);
					} else if (current instanceof PasswordCallback) {
						final PasswordCallback pcb = (PasswordCallback) current;
						pcb.setPassword(password.toCharArray());
					} else if (current instanceof RealmCallback) {
						final RealmCallback rcb = (RealmCallback) current;
						rcb.setText(rcb.getDefaultText());
					} else {
						throw new UnsupportedCallbackException(current);
					}
				}
			}
		};
		return ModelControllerClient.Factory.create(host, port, callbackHandler);
	}

	protected CommandContext createContext(final ModelControllerClient client) {
		final CommandContext commandContext;
		try {
			commandContext = new ExtendedCommandContextImpl();
			commandContext.bindClient(client);
		} catch (CliInitializationException e) {
			throw new IllegalStateException("Failed to initialize CLI context", e);
		}
		return commandContext;
	}
}
