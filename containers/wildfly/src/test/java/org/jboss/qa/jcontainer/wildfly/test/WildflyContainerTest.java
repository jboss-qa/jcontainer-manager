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

import org.jboss.qa.jcontainer.Container;
import org.jboss.qa.jcontainer.test.ContainerTest;
import org.jboss.qa.jcontainer.wildfly.WildflyClient;
import org.jboss.qa.jcontainer.wildfly.WildflyConfiguration;
import org.jboss.qa.jcontainer.wildfly.WildflyContainer;
import org.jboss.qa.jcontainer.wildfly.WildflyUser;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RunWith(JUnit4.class)
public class WildflyContainerTest extends ContainerTest {

	public static final String WILDFLY_HOME = getProperty("wildfly.home");

	protected static final String GOOD_CMD = ":whoami";
	protected static final String BAD_FORMAT_CMD = ":bad-operation";
	protected static final String BAD_RESULT_CMD = "/system-property=NONEXISTING:read-resource";

	protected static final String PROP_NAME = "my-prop";
	protected static final String PROP_VAL = "my-value";

	protected static Container container;

	@BeforeClass
	public static void beforeClass() throws Exception {
		final WildflyConfiguration conf = WildflyConfiguration.builder().directory(WILDFLY_HOME).xmx("2g").build();
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

	@Before
	public void before() throws Exception {
		final List<String> cmds = new ArrayList<>();
		cmds.add(String.format("if (outcome == success) of /system-property=%s:read-resource", PROP_NAME));
		cmds.add(String.format("/system-property=%s:remove", PROP_NAME));
		cmds.add("end-if");
		container.getClient().execute(cmds);
	}

	@Test
	public void successCmdTest() throws Exception {
		container.getClient().execute(GOOD_CMD);
	}

	@Test(expected = IllegalArgumentException.class)
	public void badFormatCmdTest() throws Exception {
		container.getClient().execute(BAD_FORMAT_CMD);
	}

	@Test(expected = IllegalArgumentException.class)
	public void badResultCmdTest() throws Exception {
		container.getClient().execute(BAD_RESULT_CMD);
	}

	@Test
	public void successBatchTest() throws Exception {
		final List<String> cmds = new ArrayList<>();
		cmds.add("batch");
		cmds.add(String.format("/system-property=%s:add(value=%s)", PROP_NAME, PROP_VAL));
		cmds.add(GOOD_CMD);
		cmds.add("run-batch");
		container.getClient().execute(cmds);
		container.getClient().execute(String.format("/system-property=%s:read-resource", PROP_NAME));
	}

	@Test(expected = IllegalArgumentException.class)
	public void failBatchTest() throws Exception {
		final List<String> cmds = new ArrayList<>();
		cmds.add("batch");
		cmds.add(String.format("/system-property=%s:add(value=%s)", PROP_NAME, PROP_VAL)); // Step #1
		cmds.add(BAD_RESULT_CMD);
		cmds.add("run-batch");
		try {
			container.getClient().execute(cmds);
		} catch (Exception e) {
			log.info("Batch failed");
		}
		container.getClient().execute(String.format("/system-property=%s:read-resource", PROP_NAME));
	}

	@Test
	public void standaloneClientTest() throws Exception {
		try (WildflyClient client = new WildflyClient<>(WildflyConfiguration.builder().build())) {
			client.execute(GOOD_CMD);
			Assert.assertNotNull(client.getCommandResult());
		}
	}
}
