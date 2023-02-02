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
package org.jboss.qa.jcontainer.jdg.test;

import org.jboss.qa.jcontainer.AbstractContainer;
import org.jboss.qa.jcontainer.jdg.JdgConfiguration;
import org.jboss.qa.jcontainer.jdg.JdgContainer;
import org.jboss.qa.jcontainer.jdg.JdgUser;
import org.jboss.qa.jcontainer.test.ContainerTest;
import org.jboss.qa.jcontainer.util.FileUtils;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RunWith(JUnit4.class)
public class JdgContainerTest extends ContainerTest {
	public static final String JDG_HOME = "/home/federico/Downloads/redhat-datagrid-8.2.0-server"; //getProperty("jdg.home");
	protected static AbstractContainer container;

	@BeforeClass
	public static void beforeClass() throws Exception {
		final JdgConfiguration conf = JdgConfiguration.builder().directory(JDG_HOME).build();
		container = new JdgContainer(conf);
		final JdgUser user = new JdgUser();
		user.setUsername("infinispanUsername");
		user.setPassword("infinispanPassword");
		user.addRoles("admin", "dev");
		container.addUser(user);
		container.start();

		System.out.println(container.isClientSupported());
	}

	@AfterClass
	public static void afterClass() throws Exception {
		if (container != null) {
			container.stop();
		}
	}

	@Test
	public void baseTest() throws Exception {
		Assert.assertTrue(container.isRunning());
	}

	@Test
	public void defaultLogFileTest() throws Exception {
		Assert.assertTrue(container.getDefaultLogFile().exists());
		log.debug("File '{}' -  length = {}", container.getDefaultLogFile().getName(), container.getDefaultLogFile().length());
		Assert.assertFalse(FileUtils.isEmpty(container.getDefaultLogFile()));
	}

	@Test
	public void stdoutLogFileTest() throws Exception {
		Assert.assertTrue(container.getStdoutLogFile().exists());
		log.debug("File '{}' -  length = {}", container.getStdoutLogFile().getName(), container.getStdoutLogFile().length());
		Assert.assertFalse(FileUtils.isEmpty(container.getStdoutLogFile()));
	}
}
