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
import static org.junit.Assert.assertTrue;

import org.jboss.qa.jcontainer.Container;
import org.jboss.qa.jcontainer.eap.EapClient;
import org.jboss.qa.jcontainer.eap.EapConfiguration;
import org.jboss.qa.jcontainer.eap.EapContainer;
import org.jboss.qa.jcontainer.eap.EapUser;
import org.jboss.qa.jcontainer.jboss.JBossUser;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class EapContainerTest extends ContainerTest {

	private static final String SUCCESS_OP = "\"outcome\" => \"success\"";
	private static final String FAILED_OP = "\"outcome\" => \"failed\"";

	protected static Container container;

	@BeforeClass
	public static void before() throws Exception {
		final EapConfiguration conf = EapConfiguration.builder().directory(EAP_HOME).xmx("2g").build();
		container = new EapContainer<>(conf);
		final EapUser user = new EapUser();
		user.setUsername(conf.getUsername());
		user.setPassword(conf.getPassword());
		user.setRealm(JBossUser.Realm.MANAGEMENT_REALM);
		user.addRoles("role1", "role2");
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
		assertTrue(container.getClient().execute(":whoami"));
		assertTrue(((EapClient) container.getClient()).getCommandResult().asString().contains(SUCCESS_OP));
	}

	@Test
	public void failCmdTest() throws Exception {
		assertFalse(container.getClient().execute(":bad-operation"));
		assertTrue(((EapClient) container.getClient()).getCommandResult().asString().contains(FAILED_OP));
	}

	@Test(expected = Exception.class)
	public void exceptionCmdTest() throws Exception {
		container.getClient().execute("bad-format");
	}

	@Test
	public void standaloneClientTest() throws Exception {
		try (EapClient client = new EapClient<>(EapConfiguration.builder().build())) {
			assertTrue(client.execute(":whoami"));
			assertTrue(client.getCommandResult().asString().contains(SUCCESS_OP));
		}
	}
}
