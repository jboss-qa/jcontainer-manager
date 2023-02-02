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
package org.jboss.qa.jcontainer.eap.test;

import org.jboss.qa.jcontainer.eap.EapClient;
import org.jboss.qa.jcontainer.eap.EapConfiguration;
import org.jboss.qa.jcontainer.eap.EapContainer;
import org.jboss.qa.jcontainer.eap.EapUser;
import org.jboss.qa.jcontainer.wildfly.test.WildflyContainerTest;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RunWith(JUnit4.class)
public class EapContainerTest extends WildflyContainerTest {

	public static final String EAP_HOME = getProperty("eap.home");

	@BeforeClass
	public static void beforeClass() throws Exception {
		final EapConfiguration conf = EapConfiguration.builder().directory(EAP_HOME).xmx("2g").build();
		container = new EapContainer<>(conf);
		final EapUser user = new EapUser();
		user.setUsername(conf.getUsername());
		user.setPassword(conf.getPassword());
		user.setRealm(EapUser.Realm.MANAGEMENT_REALM);
		user.addRoles("role1", "role2");
		container.addUser(user);
		container.start();
	}

	@Test
	@Override
	public void standaloneClientTest() throws Exception {
		try (EapClient client = new EapClient<>(EapConfiguration.builder().build())) {
			client.execute(GOOD_CMD);
			client.getCommandResult().assertSuccess();
		}
	}

	@Test
	public void java17Test() {
		Assume.assumeTrue("Test is for java 17 on only.", Boolean.parseBoolean(System.getProperty("version.from.jdk17", "false")));
		final EapConfiguration config = EapConfiguration.builder().build();
		Assert.assertNull("-XX:MaxPermSize is not null", config.getMaxPermSize());
	}

	@Test
	public void beforeJava17Test() {
		Assume.assumeFalse("Test is for java before version 17.", Boolean.parseBoolean(System.getProperty("version.from.jdk17", "false")));
		final EapConfiguration config = EapConfiguration.builder().build();
		Assert.assertNotNull("-XX:MaxPermSize is null", config.getMaxPermSize());
	}
}
