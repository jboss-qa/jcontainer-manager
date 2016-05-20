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

import org.jboss.qa.jcontainer.eap.EapConfiguration;
import org.jboss.qa.jcontainer.eap.EapContainer;
import org.jboss.qa.jcontainer.eap.EapUser;
import org.jboss.qa.jcontainer.test.ContainerTest;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RunWith(JUnit4.class)
public class EapMultipleNodeTest extends ContainerTest {

	private static final int COUNT = 3;
	private static final String EAP_HOME = getProperty("eap.home");

	private static final Map<Integer, EapContainer> containers = new HashMap<>();

	@BeforeClass
	public static void beforeClass() throws Exception {
		for (int i = 1; i <= COUNT; i++) {
			final String nodeName = getNodeName(i);
			final EapConfiguration conf = EapConfiguration.builder().directory(EAP_HOME).xmx("2g")
					.portOffset(i * 10000)
					.nodeName(nodeName)
					.build();
			final EapContainer container = new EapContainer<>(conf);
			containers.put(i, container);
			final EapUser user = new EapUser();
			user.setUsername(getUsername(i));
			user.setPassword("changeit");
			user.setRealm(EapUser.Realm.APPLICATION_REALM);
			container.addUser(user);
			container.start();
		}
	}

	@AfterClass
	public static void afterClass() throws Exception {
		for (Map.Entry<Integer, EapContainer> entry : containers.entrySet()) {
			entry.getValue().stop();
		}
	}

	private static String getNodeName(int i) {
		return "node" + i;
	}

	private static String getUsername(int i) {
		return "user" + i;
	}

	@Test
	public void testClient() {
		for (EapContainer container : containers.values()) {
			container.checkClient();
		}
	}
}
