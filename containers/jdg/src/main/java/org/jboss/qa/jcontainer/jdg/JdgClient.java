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
package org.jboss.qa.jcontainer.jdg;

import org.jboss.qa.jcontainer.Client;

import java.io.IOException;
import java.util.List;

public class JdgClient<T extends JdgConfiguration> extends Client<T> {
	private static final String ERROR_MSG = "JDG container does not support client";

	public JdgClient(T configuration) {
		super(configuration);
	}

	@Override
	public boolean isConnected() {
		return false;
	}

	@Override
	protected void connectInternal() throws Exception {
		throw new UnsupportedOperationException(ERROR_MSG);
	}

	@Override
	protected void executeInternal(String command) throws Exception {
		throw new UnsupportedOperationException(ERROR_MSG);
	}

	@Override
	protected void executeInternal(List<String> commands) throws Exception {
		throw new UnsupportedOperationException(ERROR_MSG);
	}

	@Override
	protected void closeInternal() throws IOException {
		throw new UnsupportedOperationException(ERROR_MSG);
	}
}
