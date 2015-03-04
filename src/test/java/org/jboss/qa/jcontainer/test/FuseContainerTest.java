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
package org.jboss.qa.jcontainer.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.jboss.qa.jcontainer.Container;
import org.jboss.qa.jcontainer.fuse.FuseClient;
import org.jboss.qa.jcontainer.fuse.FuseConfiguration;
import org.jboss.qa.jcontainer.fuse.FuseContainer;
import org.jboss.qa.jcontainer.fuse.FuseUser;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class FuseContainerTest extends ContainerTest {

	protected static Container container;

	@BeforeClass
	public static void before() throws Exception {
		final FuseConfiguration conf = FuseConfiguration.builder().directory(FUSE_HOME).xmx("2g").build();
		container = new FuseContainer<>(conf);
		final FuseUser user = new FuseUser();
		user.setUsername(conf.getUsername());
		user.setPassword(conf.getPassword());
		user.addRoles("admin", "SuperUser");
		container.addUser(user);
		container.start();
	}

	@AfterClass
	public static void after() throws Exception {
		if (container != null) {
			container.stop();
		}
	}

	@Test
	public void successCmdTest() throws Exception {
		assertTrue(container.getClient().execute("osgi:version"));
		assertNotNull(((FuseClient) container.getClient()).getCommandResult());
	}

	@Test
	public void failCmdTest() throws Exception {
		assertFalse(container.getClient().execute("osgi:install xxx"));
		assertTrue(((FuseClient) container.getClient()).getCommandResult().contains("Error"));
	}

	@Test(expected = Exception.class)
	public void exceptionCmdTest() throws Exception {
		container.getClient().execute("osgi:xxx");
	}

	@Test
	public void standaloneClientTest() throws Exception {
		try (FuseClient client = new FuseClient<>(FuseConfiguration.builder().build())) {
			assertTrue(client.execute("osgi:version"));
			assertNotNull(client.getCommandResult());
		}
	}
}
