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
package org.jboss.qa.jcontainer.fuse.strategy.command;

import org.jboss.qa.jcontainer.fuse.FuseClient;
import org.jboss.qa.jcontainer.fuse.FuseConfiguration;
import org.jboss.qa.jcontainer.fuse.strategy.command.impl.Fuse6CommandStrategy;
import org.jboss.qa.jcontainer.fuse.strategy.command.impl.Fuse7CommandStrategy;
import org.jboss.qa.jcontainer.karaf.KarafConfiguration;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FuseCommandFactory {
	@Getter
	private FuseCommandStrategy strategy;

	private static final String FUSE_7_KARAF_VERSION_PREFIX = "4";

	public FuseCommandFactory(final FuseClient client) {
		initStrategy(client);
	}

	private void initStrategy(final FuseClient<FuseConfiguration> client) {
		int version = client.getConfiguration().getVersion();
		if (version == KarafConfiguration.DEFAULT_FUSE_VERSION) {
			try {
				client.execute("version");
				final String karafVersionResult = client.getCommandResult();
				version = karafVersionResult.startsWith(FUSE_7_KARAF_VERSION_PREFIX) ? 7 : 6;
				log.debug("Detected version of Fuse: {}", version);
			} catch (Exception e) {
				// we will use the default
			}
		}
		switch (version) {
			case 6:
				strategy = new Fuse6CommandStrategy();
				break;
			case 7:
				strategy = new Fuse7CommandStrategy();
				break;
			default:
				strategy = new Fuse6CommandStrategy();
		}
	}
}
