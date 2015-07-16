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
package org.jboss.qa.jcontainer.karaf;

import org.apache.commons.configuration.PropertiesConfiguration;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;

@RunWith(JUnit4.class)
public class KarafContainerPortTest extends BaseKarafContainerTest {

	protected static final int SSH_PORT = 8102;

	@BeforeClass
	public static void beforeClass() throws Exception {
		final KarafConfiguration conf = KarafConfiguration.builder().directory(KARAF_HOME).xmx("2g")
				.sshPort(SSH_PORT).build();
		container = new KarafContainer<>(conf);
		final KarafUser user = new KarafUser();
		user.setUsername(conf.getUsername());
		user.setPassword(conf.getPassword());
		user.addRoles("admin", "SuperUser");
		container.addUser(user);
		container.start();
	}

	@AfterClass
	public static void afterClass() throws Exception {
		if (container != null) {
			container.stop();
			final File propsFile = container.getConfigFile("org.apache.karaf.shell");
			final PropertiesConfiguration propConf = new PropertiesConfiguration(propsFile);
			propConf.setProperty("sshPort", KarafConfiguration.DEFAULT_SSH_PORT);
			propConf.save();
		}
	}

	@Test
	public void standaloneClientTest() throws Exception {
		try (KarafClient client = new KarafClient<>(KarafConfiguration.builder().sshPort(SSH_PORT).build())) {
			client.execute(GOOD_CMD);
			Assert.assertNotNull(client.getCommandResult());
		}
	}
}

