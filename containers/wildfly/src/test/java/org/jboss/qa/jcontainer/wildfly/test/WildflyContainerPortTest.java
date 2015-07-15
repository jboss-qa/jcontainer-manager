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
package org.jboss.qa.jcontainer.wildfly.test;

import org.jboss.qa.jcontainer.wildfly.WildflyClient;
import org.jboss.qa.jcontainer.wildfly.WildflyConfiguration;
import org.jboss.qa.jcontainer.wildfly.WildflyContainer;
import org.jboss.qa.jcontainer.wildfly.WildflyUser;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RunWith(JUnit4.class)
public class WildflyContainerPortTest extends WildflyContainerTest {

	protected static final int MANAGEMENT_PORT = 9991;
	protected static final int HTTP_PORT = 8181;

	@BeforeClass
	public static void beforeClass() throws Exception {
		final WildflyConfiguration conf = WildflyConfiguration.builder().directory(WILDFLY_HOME).xmx("2g")
				.managementPort(MANAGEMENT_PORT).httpPort(HTTP_PORT).build();
		container = new WildflyContainer<>(conf);
		final WildflyUser user = new WildflyUser();
		user.setUsername(conf.getUsername());
		user.setPassword(conf.getPassword());
		user.setRealm(WildflyUser.Realm.MANAGEMENT_REALM);
		user.addRoles("role1", "role2");
		container.addUser(user);
		container.start();
	}

	@AfterClass
	public static void afterClass() throws Exception {
		if (container != null) {
			container.stop();
		}
	}

	@Test
	public void standaloneClientTest() throws Exception {
		try (WildflyClient client = new WildflyClient<>(WildflyConfiguration.builder()
				.managementPort(MANAGEMENT_PORT).httpPort(HTTP_PORT).build())) {
			client.execute(GOOD_CMD);
			Assert.assertNotNull(client.getCommandResult());
		}
	}
}
