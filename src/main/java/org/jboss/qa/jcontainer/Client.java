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
package org.jboss.qa.jcontainer;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class Client<T extends Configuration> implements Closeable {

	protected T configuration;

	public Client(T configuration) {
		this.configuration = configuration;
	}

	public T getConfiguration() {
		return configuration;
	}

	public abstract boolean isConnected();

	protected abstract void connectInternal() throws Exception;

	public void connect() throws Exception {
		if (!isConnected()) {
			connectInternal();
		}
		log.info("Client was connected");
	}

	protected abstract void executeInternal(String command) throws Exception;

	protected abstract void executeInternal(List<String> commands) throws Exception;

	public void execute(String command) throws Exception {
		log.info("Execute command: {}", command);
		if (!isConnected()) {
			connect();
		}
		executeInternal(command);
	}

	public void execute(List<String> commands) throws Exception {
		log.info("Execute commands:");
		int i = 1;
		for (String cmd : commands) {
			log.info("#{}\t{}", i++, cmd);
		}
		if (!isConnected()) {
			connect();
		}
		executeInternal(commands);
	}

	protected abstract void closeInternal() throws IOException;

	@Override
	public void close() throws IOException {
		if (isConnected()) {
			closeInternal();
		}
		log.info("Client was disconnected");
	}
}
