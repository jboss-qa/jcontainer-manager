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
package org.jboss.qa.jcontainer.tomcat.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.Matchers.equalTo;

import org.apache.commons.lang3.SystemUtils;

import org.jboss.qa.jcontainer.tomcat.TomcatConfiguration;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Arrays;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RunWith(JUnit4.class)
public class TomcatConfigurationTest {

	@Test
	public void configuredCatalinaHome() {
		final String directory;
		if (SystemUtils.IS_OS_WINDOWS) {
			directory = "C:\\tmp\\catalinaHome";
		} else {
			directory = "/tmp/catalinaHome";
		}

		final TomcatConfiguration conf = TomcatConfiguration.builder().directory(directory).build();
		final String catalinaHome = conf.getEnvProps().get("CATALINA_HOME");

		assertEquals(directory, catalinaHome);
	}

	@Test
	public void javaOptsDefaultTest() {
		final String xms = "64m";

		final TomcatConfiguration conf = TomcatConfiguration.builder().directory("catalinaHome").xms(xms).build();
		final List<String> options = Arrays.asList(conf.getEnvProps().get(TomcatConfiguration.CATALINA_OPTS).split(" "));

		assertThat(options, hasItem(equalTo("-Xms" + xms)));
	}
}
