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

import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.Matchers.equalTo;

import org.jboss.qa.jcontainer.fuse.FuseConfiguration;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Arrays;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RunWith(JUnit4.class)
public class FuseConfigurationTest {

	@Test
	public void javaOptsDefaultTest() {
		final String xms = "512m";
		final String xmx = "1024m";
		final String permSize = "128m";
		final String maxPermSize = "256m";

		final FuseConfiguration conf = FuseConfiguration.builder().build();
		final List<String> options = Arrays.asList(conf.getEnvProps().get(FuseConfiguration.KARAF_OPTS_ENV_NAME).split(" "));

		assertThat(options, hasItem(equalTo("-Xms" + xms)));
		assertThat(options, hasItem(equalTo("-Xmx" + xmx)));
		assertThat(options, hasItem(equalTo("-XX:PermSize=" + permSize)));
		assertThat(options, hasItem(equalTo("-XX:MaxPermSize=" + maxPermSize)));
	}
}
