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
package org.jboss.qa.jcontainer.configuration;

import static org.jboss.qa.jcontainer.configuration.DummyConfiguration.JAVA_OPTS_ENV_NAME;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;

import org.jboss.qa.jcontainer.JavaConfiguration;

import org.junit.After;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JavaConfigurationTest {

	@After
	public void tearDown() throws Exception {
		setEnv(Collections.EMPTY_MAP);
	}

	@Test
	public void simpleConfiguration() {
		final JavaConfiguration cfg = DummyConfiguration.builder().logFileName("file.log").build();
		assertNull(cfg.getXms());
		assertNull(cfg.getXmx());
		assertNull(cfg.getPermSize());
		assertNull(cfg.getMaxPermSize());
		assertEquals("DUMMY_OPTS", cfg.getJavaOptsEnvName());
		assertThat(cfg.getEnvProps(), equalTo(Collections.EMPTY_MAP));
	}

	@Test
	public void memoryConfiguration() {
		final JavaConfiguration cfg = DummyConfiguration.builder()
				.logFileName("file.log")
				.xms("128m")
				.build();

		assertEquals("128m", cfg.getXms());
		assertThat(cfg.getEnvProps().keySet(), hasItem(JAVA_OPTS_ENV_NAME));
		assertEquals("-Xms128m", cfg.getEnvProps().get(cfg.getJavaOptsEnvName()));
	}

	@Test
	public void removeXmxFromConfiguration() {
		final JavaConfiguration cfg = DummyConfiguration.builder()
				.logFileName("file.log")
				.xmx("128m")
				.xmx(null)
				.build();

		assertNull(cfg.getXmx());
		assertEquals(JAVA_OPTS_ENV_NAME, cfg.getJavaOptsEnvName());
		assertThat(cfg.getEnvProps().get(cfg.getJavaOptsEnvName()), not(containsString("-Xmx")));
	}

	@Test
	public void replaceInJavaOpts() throws Exception {
		final Map<String, String> env = new HashMap<>();
		env.putAll(System.getenv());
		env.put(JAVA_OPTS_ENV_NAME, "-XX:PermSize=100M");
		setEnv(env);

		final JavaConfiguration cfg = DummyConfiguration.builder()
				.logFileName("file.log")
				.permSize("200M")
				.build();

		assertEquals("200M", cfg.getPermSize());
		assertEquals(JAVA_OPTS_ENV_NAME, cfg.getJavaOptsEnvName());
		assertThat(cfg.getEnvProps().keySet(), hasItem(JAVA_OPTS_ENV_NAME));
		assertThat(cfg.getEnvProps().get(cfg.getJavaOptsEnvName()), containsString("-XX:PermSize=200M"));
		assertThat(cfg.getEnvProps().get(cfg.getJavaOptsEnvName()), not(containsString("-XX:PermSize=100M")));
	}

	@Test
	public void deleteFromJavaOpts() throws Exception {
		final Map<String, String> env = new HashMap<>();
		env.putAll(System.getenv());
		env.put(JAVA_OPTS_ENV_NAME, "-XX:MaxPermSize=200M -XX:PermSize=100M");
		setEnv(env);

		final JavaConfiguration cfg = DummyConfiguration.builder()
				.logFileName("file.log")
				.maxPermSize("")
				.build();

		assertEquals("", cfg.getMaxPermSize());
		assertEquals(JAVA_OPTS_ENV_NAME, cfg.getJavaOptsEnvName());
		assertThat(cfg.getEnvProps().keySet(), hasItem(JAVA_OPTS_ENV_NAME));
		assertThat(cfg.getEnvProps().get(cfg.getJavaOptsEnvName()), not(containsString("-XX:MaxPermSize")));
	}

	@Test
	public void addCustomJavaOpts() {
		final JavaConfiguration cfg = DummyConfiguration.builder()
				.logFileName("file.log")
				.javaOpt("-Dfirst=value")
				.javaOpt("-Dsecound=value")
				.build();

		assertThat(cfg.getEnvProps().keySet(), hasItem(JAVA_OPTS_ENV_NAME));
		assertThat(cfg.getEnvProps().get(cfg.getJavaOptsEnvName()), containsString("-Dfirst=value"));
		assertThat(cfg.getEnvProps().get(cfg.getJavaOptsEnvName()), containsString("-Dsecound=value"));
	}

	/**
	 * Enhance an environment variable FOR THE CURRENT RUN OF THE JVM
	 * Should only be used for testing purposes!
	 *
	 * @param newenv Map of variable to set
	 */
	private static void setEnv(Map<String, String> newenv) throws Exception {
		try {
			final Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
			final Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
			theEnvironmentField.setAccessible(true);
			final Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
			env.putAll(newenv);
			final Field theCaseInsensitiveEnvironmentField = processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment");
			theCaseInsensitiveEnvironmentField.setAccessible(true);
			final Map<String, String> cienv = (Map<String, String>) theCaseInsensitiveEnvironmentField.get(null);
			cienv.putAll(newenv);
		} catch (NoSuchFieldException e) {
			final Class[] classes = Collections.class.getDeclaredClasses();
			final Map<String, String> env = System.getenv();
			for (Class cl : classes) {
				if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
					final Field field = cl.getDeclaredField("m");
					field.setAccessible(true);
					final Object obj = field.get(env);
					final Map<String, String> map = (Map<String, String>) obj;
					map.clear();
					map.putAll(newenv);
				}
			}
		}
	}
}
