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

import static org.hamcrest.CoreMatchers.containsString;

import org.apache.commons.lang3.SystemUtils;

import org.jboss.qa.jcontainer.fuse.FuseClient;
import org.jboss.qa.jcontainer.fuse.FuseConfiguration;
import org.jboss.qa.jcontainer.fuse.FuseContainer;
import org.jboss.qa.jcontainer.fuse.FuseUser;
import org.jboss.qa.jcontainer.karaf.BaseKarafContainerTest;
import org.jboss.qa.jcontainer.util.ProcessUtils;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RunWith(JUnit4.class)
public class BaseFuseContainerTest extends BaseKarafContainerTest {

	public static final String FUSE_HOME = getProperty("fuse.home");

	@BeforeClass
	public static void beforeClass() throws Exception {
		final FuseConfiguration conf = FuseConfiguration.builder().directory(FUSE_HOME).xmx("2g").build();
		container = new FuseContainer<>(conf);
		final FuseUser user = new FuseUser();
		user.setUsername(conf.getUsername());
		user.setPassword(conf.getPassword());
		user.addRoles("admin", "SuperUser");
		container.addUser(user);
		container.start();
	}

	@Test
	@Override
	public void successBatchTest() throws Exception {
		final List<String> cmds = new ArrayList<>();
		cmds.add(String.format("config:edit %s", CONFIG));
		cmds.add(String.format("config:propset %s %s", PROP_NAME, PROP_VAL));
		cmds.add("config:update");
		container.getClient().execute(cmds);
		Assert.assertTrue(container.getConfigFile(CONFIG).exists());
	}

	@Ignore("Batch rollback is not supported in JBoss Fuse")
	@Test
	@Override
	public void failBatchTest() throws Exception {
		final List<String> cmds = new ArrayList<>();
		try {
			cmds.add(String.format("config:edit %s", CONFIG));
			cmds.add(String.format("config:propset %s %s", PROP_NAME, PROP_VAL));
			cmds.add("config:update");
			cmds.add(BAD_FORMAT_CMD);
			container.getClient().execute(cmds);
		} catch (Exception e) {
			Assert.assertFalse("Batch test does not work", container.getConfigFile(CONFIG).exists());
		}
	}

	@Test
	@Override
	public void standaloneClientTest() throws Exception {
		try (FuseClient client = new FuseClient<>(FuseConfiguration.builder().build())) {
			client.execute(GOOD_CMD);
			Assert.assertNotNull(client.getCommandResult());
		}
	}

	@Test
	public void javaConfigurationXmxTest() {
		Assume.assumeFalse("Test is not written for Windows.", SystemUtils.IS_OS_WINDOWS);
		final String containerPid = ProcessUtils.getJavaPidByContainerId(container.getId());
		final String result = ProcessUtils.executeCommandUnix("jcmd " + containerPid + " VM.flags");

		log.debug("VM.flags {}", result);
		Assert.assertThat(result, containsString("-XX:MaxHeapSize=2147483648"));
	}

	@Test
	public void java17Test() {
		Assume.assumeTrue("Test is for java 17 on only.", Boolean.parseBoolean(System.getProperty("version.from.jdk17", "false")));
		final FuseConfiguration config = FuseConfiguration.builder().build();
		Assert.assertNull("-XX:PermSize is not null", config.getPermSize());
		Assert.assertNull("-XX:MaxPermSize is not null", config.getMaxPermSize());
	}

	@Test
	public void beforeJava17Test() {
		Assume.assumeFalse("Test is for java before version 17.", Boolean.parseBoolean(System.getProperty("version.from.jdk17", "false")));
		final FuseConfiguration config = FuseConfiguration.builder().build();
		Assert.assertNotNull("-XX:PermSize is null", config.getPermSize());
		Assert.assertNotNull("-XX:MaxPermSize is null", config.getMaxPermSize());
	}
}

