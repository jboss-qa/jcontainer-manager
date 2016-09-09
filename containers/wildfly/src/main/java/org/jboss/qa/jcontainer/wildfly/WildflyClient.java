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

import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.qa.jcontainer.Client;

import org.wildfly.extras.creaper.commands.foundation.online.CliFile;
import org.wildfly.extras.creaper.core.ManagementClient;
import org.wildfly.extras.creaper.core.online.CliException;
import org.wildfly.extras.creaper.core.online.ManagementProtocol;
import org.wildfly.extras.creaper.core.online.ModelNodeResult;
import org.wildfly.extras.creaper.core.online.OnlineManagementClient;
import org.wildfly.extras.creaper.core.online.OnlineOptions;

import java.io.File;
import java.io.IOException;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WildflyClient<T extends WildflyConfiguration> extends Client<T> {

	protected ManagementProtocol protocol;
	protected OnlineManagementClient client;
	protected ModelNodeResult lastResult;

	public WildflyClient(T configuration) {
		super(configuration);
		protocol = ManagementProtocol.HTTP_REMOTING;
	}

	@Override
	public boolean isConnected() {
		return client != null;
	}

	@Override
	protected void connectInternal() throws Exception {
		client = ManagementClient.online(OnlineOptions
				.standalone()
				.hostAndPort(configuration.getHost(), configuration.getManagementPort())
				.protocol(protocol)
				.build()
		);
	}

	@Override
	protected void executeInternal(String command) throws Exception {
		try {
			lastResult = client.execute(command);
		} catch (CliException e) {
			log.trace(e.getMessage(), e);
			if (e.getCause().getClass().isAssignableFrom(OperationFormatException.class)) {
				// Workaround for unsupported commands by Wildfly Creaper project
				client.executeCli(command);
			}
		}
	}

	@Override
	protected void executeInternal(List<String> commands) throws Exception {
		// List of commands may contain commands like "if", "batch". Use rather "executeCli" then "execute".
		for (String cmd : commands) {
			client.executeCli(cmd);
		}
	}

	@Override
	public void execute(File file) throws Exception {
		log.info("Execute commands from file: {}", file.getAbsoluteFile());
		client.apply(new CliFile(file));
	}

	@Override
	protected void closeInternal() throws IOException {
		client.close();
		client = null;
		lastResult = null;
	}

	public ModelNodeResult getCommandResult() {
		return lastResult;
	}

	public OnlineManagementClient getInternalClient() {
		return client;
	}
}
