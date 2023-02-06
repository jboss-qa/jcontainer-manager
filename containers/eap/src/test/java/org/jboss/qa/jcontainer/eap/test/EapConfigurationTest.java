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

import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import org.jboss.qa.jcontainer.JavaConfiguration;
import org.jboss.qa.jcontainer.eap.EapConfiguration;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Arrays;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RunWith(JUnit4.class)
public class EapConfigurationTest {

	@Test
	public void javaOptsDefaultTest() {
		final String xms = "1303m";
		final String xmx = "1303m";
		final String ipstack = "-Djava.net.preferIPv4Stack=true";
		final String pkgs = "-Djboss.modules.system.pkgs=org.jboss.byteman";
		final String policy = "-Djboss.modules.policy-permissions=true";

		final EapConfiguration conf = EapConfiguration.builder().build();
		final List<String> options = Arrays.asList(conf.getEnvProps().get(EapConfiguration.JAVA_OPTS_ENV_NAME).split(" "));

		assertThat(options, hasItem(equalTo("-Xms" + xms)));
		assertThat(options, hasItem(equalTo("-Xmx" + xmx)));
		assertThat(options, hasItem(equalTo(ipstack)));
		assertThat(options, hasItem(equalTo(pkgs)));
		assertThat(options, hasItem(equalTo(policy)));
	}

	@Test
	public void customJavaOptsTest() {
		final String shouldBePresent = "-DmyProp=my-value/\\&";

		final String withSpaces = "     -DwithoutSpaces";

		final EapConfiguration conf = EapConfiguration.builder()
				.javaOpt(withSpaces)
				.javaOpt(shouldBePresent).build();
		final List<String> options = Arrays.asList(conf.getEnvProps().get(EapConfiguration.JAVA_OPTS_ENV_NAME).split(" "));

		assertThat(options, hasItem(equalTo(shouldBePresent)));
		assertThat(options, hasItem(equalTo(withSpaces.replace(" ", ""))));
		assertThat(options, not(hasItem(equalTo(withSpaces))));
	}

	@Test
	public void overrideDefaultJavaOptsTest() {
		final String xms = "123";
		final String xmx = "1g";

		final EapConfiguration conf = EapConfiguration.builder()
				.xms(xms)
				.xmx(xmx).build();
		final List<String> options = Arrays.asList(conf.getEnvProps().get(EapConfiguration.JAVA_OPTS_ENV_NAME).split(" "));

		assertThat(options, hasItem(equalTo("-Xms" + xms)));
		assertThat(options, hasItem(equalTo("-Xms" + xms)));

		//do not contain default values
		assertThat(options, not(hasItem(equalTo("-Xms1303m"))));
		assertThat(options, not(hasItem(equalTo("-Xmx1303m"))));
	}

	@Test
	public void overrideCustomJavaOptsTest() {
		final String shouldOverride = "-Djava.net.preferIPv4Stack=";
		final String pkgs = "-Djboss.modules.system.pkgs=org.jboss.byteman,mypackage";

		final EapConfiguration conf = EapConfiguration.builder()
				.javaOpt(shouldOverride + false)
				.javaOpt(pkgs)
				.build();
		final List<String> options = Arrays.asList(conf.getEnvProps().get(EapConfiguration.JAVA_OPTS_ENV_NAME).split(" "));

		assertThat(options, hasItem(equalTo(shouldOverride + false)));
		assertThat(options, hasItem(equalTo(pkgs)));

		//not contain defaults
		assertThat(options, not(hasItem(equalTo(shouldOverride + true))));
		assertThat(options, not(hasItem(equalTo("-Djboss.modules.system.pkgs=org.jboss.byteman"))));
	}

	@Test
	public void java17Test() {
		Assume.assumeFalse("Test is for java 17 on only.", JavaConfiguration.BEFORE_JDK17);
		final EapConfiguration config = EapConfiguration.builder().build();
		Assert.assertNull("-XX:MaxPermSize is not null", config.getMaxPermSize());
	}

	@Test
	public void beforeJava17Test() {
		Assume.assumeTrue("Test is for java before version 17.", JavaConfiguration.BEFORE_JDK17);
		final EapConfiguration config = EapConfiguration.builder().build();
		Assert.assertNotNull("-XX:MaxPermSize is null", config.getMaxPermSize());
	}
}
