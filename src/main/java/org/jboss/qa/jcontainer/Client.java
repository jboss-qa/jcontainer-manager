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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;

public abstract class Client<T extends Configuration> implements Closeable {

	private static final Logger logger = LoggerFactory.getLogger(Client.class);
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
		logger.info("Client was connected");
	}

	protected abstract boolean executeInternal(String command) throws Exception;

	public boolean execute(String command) throws Exception {
		logger.info("Execute command: {}", command);
		if (!isConnected()) {
			connect();
		}
		return executeInternal(command);
	}

	protected abstract void closeInternal() throws IOException;

	@Override
	public void close() throws IOException {
		if (isConnected()) {
			closeInternal();
		}
		logger.info("Client was disconnected");
	}
}
