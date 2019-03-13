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
package org.jboss.qa.jcontainer.util.executor;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;

import org.jboss.qa.jcontainer.util.ProcessUtils;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SubProcessKillTest {

	@Rule
	public Timeout globalTimeout = Timeout.seconds(50);

	private static final int CONTAINER_ID = 1234;

	private static List<String> commands;

	@BeforeClass
	public static void setUp() {
		Assume.assumeFalse(SystemUtils.IS_OS_WINDOWS);

		final Path scriptFolder = Paths.get("src").resolve("test").resolve("resources");

		commands = new ArrayList<>();
		commands.add("bash");
		commands.add("-c");
		commands.add(scriptFolder.resolve("scriptMainProcess.sh").toAbsolutePath().toString() + " \"" + scriptFolder.toAbsolutePath() + "\"");
	}

	@Test
	public void testContainerWithSubprocessIsKilled() throws IOException, InterruptedException {
		final ProcessBuilder pb = new ProcessBuilder(commands);
		final Process process = ProcessBuilderExecutor.asyncExecute(pb);
		final ExecutorService executor = Executors.newSingleThreadExecutor();
		executor.submit(new Runnable() {
			@Override
			public void run() {
				try {
					Assert.assertEquals("Response code should be 137=128+9 SIGKILL", 137, process.waitFor());
				} catch (InterruptedException e) {
					log.error("Error during waiting for process finish", e);
				}
			}
		});
		TimeUnit.SECONDS.sleep(5);
		final String javaPid = ProcessUtils.getJavaPidByContainerId(CONTAINER_ID);
		Assert.assertTrue("Java PID should be a number", StringUtils.isNumeric(javaPid));

		final String parentPid = ProcessUtils.getParentPidOfPid(javaPid);
		Assert.assertTrue("Parent PID should be a number", StringUtils.isNumeric(parentPid));

		ProcessUtils.killAllJavaContainerProcesses(CONTAINER_ID, javaPid, parentPid);
	}
}
