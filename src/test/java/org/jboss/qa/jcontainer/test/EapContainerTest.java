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

	protected static Container container;

	@BeforeClass
	public static void before() throws Exception {
		final EapConfiguration conf = EapConfiguration.builder().directory(EAP_HOME).xmx("2g").build();
		container = new EapContainer<>(conf);
		final EapUser user = new EapUser();
		user.setUsername("admin");
		user.setPassword("admin");
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
		assertTrue(cmdTest(":whoami"));
	}

	@Test
	public void failCmdTest() throws Exception {
		assertFalse(cmdTest(":bad-operation"));
	}

	@Test(expected = Exception.class)
	public void exceptionCmdTest() throws Exception {
		cmdTest("bad-format");
	}

	public boolean cmdTest(String cmd) throws Exception {
		return container.getClient().execute(cmd);
	}
}
