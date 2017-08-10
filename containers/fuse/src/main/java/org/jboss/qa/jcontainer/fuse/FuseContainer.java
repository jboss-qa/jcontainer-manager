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
package org.jboss.qa.jcontainer.fuse;

import org.jboss.qa.jcontainer.fuse.strategy.command.FuseCommandFactory;
import org.jboss.qa.jcontainer.fuse.strategy.command.FuseCommandStrategy;
import org.jboss.qa.jcontainer.karaf.KarafContainer;
import org.jboss.qa.jcontainer.karaf.utils.CoreUtils;

import java.io.File;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FuseContainer<T extends FuseConfiguration, U extends FuseClient<T>, V extends FuseUser>
		extends KarafContainer<T, U, V> {

	private FuseCommandFactory commandFactory;

	public FuseContainer(T configuration) {
		super(configuration);
	}

	@Override
	protected File getLogDirInternal() {
		return new File(CoreUtils.getSystemProperty(client, "karaf.data", getCommandStrategy().systemProperty()), "log");
	}

	private FuseCommandStrategy getCommandStrategy() {
		if (commandFactory == null) {
			commandFactory = new FuseCommandFactory(client);
		}
		return commandFactory.getStrategy();
	}
}

