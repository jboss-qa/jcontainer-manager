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
package org.jboss.qa.jcontainer.fuse.test;

import org.jboss.qa.jcontainer.fuse.FuseClient;
import org.jboss.qa.jcontainer.fuse.FuseConfiguration;
import org.jboss.qa.jcontainer.fuse.FuseContainer;
import org.jboss.qa.jcontainer.fuse.FuseUser;
import org.jboss.qa.jcontainer.fuse.strategy.command.FuseCommandFactory;
import org.jboss.qa.jcontainer.fuse.strategy.command.FuseCommandStrategy;
import org.jboss.qa.jcontainer.fuse.strategy.command.impl.Fuse6CommandStrategy;
import org.jboss.qa.jcontainer.fuse.strategy.command.impl.Fuse7CommandStrategy;
import org.jboss.qa.jcontainer.test.ContainerTest;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RunWith(JUnit4.class)
public class FuseCommandsTest extends ContainerTest {

	public static final String FUSE_HOME = getProperty("fuse.home");
	private static FuseContainer<FuseConfiguration, FuseClient<FuseConfiguration>, FuseUser> container;

	public static void setup(final FuseConfiguration conf) throws Exception {
		container = new FuseContainer<>(conf);
		final FuseUser user = new FuseUser();
		user.setUsername(conf.getUsername());
		user.setPassword(conf.getPassword());
		user.addRoles("admin", "SuperUser");
		container.addUser(user);
		container.start();
	}

	@After
	public void tearDown() throws Exception {
		container.stop();
	}

	@Test
	public void commandFuse7StrategyTest() throws Exception {
		setup(FuseConfiguration.builder().directory(FUSE_HOME).version(7).build());
		commandFuseStrategyTest(new Fuse7CommandStrategy());
	}

	@Test
	public void commandFuse6StrategyTest() throws Exception {
		setup(FuseConfiguration.builder().directory(FUSE_HOME).version(6).build());
		commandFuseStrategyTest(new Fuse6CommandStrategy());
	}

	@Test
	public void dynamicallyResolveStrategyTest() throws Exception {
		setup(FuseConfiguration.builder().directory(FUSE_HOME).build());
		try (FuseClient client = container.getClient()) {
			client.execute("version");
			final FuseCommandStrategy expectedStrategy = client.getCommandResult().startsWith("4") ? new Fuse7CommandStrategy() : new Fuse6CommandStrategy();
			commandFuseStrategyTest(expectedStrategy);
		}
	}

	public void commandFuseStrategyTest(final FuseCommandStrategy expectedStrategy) throws Exception {
		try (FuseClient client = container.getClient()) {
			final FuseCommandStrategy strategy = new FuseCommandFactory(client).getStrategy();
			Assert.assertEquals(expectedStrategy.systemProperty(), strategy.systemProperty());
		}
	}
}
