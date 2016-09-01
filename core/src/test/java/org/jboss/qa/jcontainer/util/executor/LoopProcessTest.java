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

import static org.apache.commons.io.FileUtils.getFile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.SystemUtils;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.Timeout;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoopProcessTest {

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Rule
	public Timeout globalTimeout = Timeout.seconds(10);

	private static List<String> commands;

	@BeforeClass
	public static void setUp() {
		Assume.assumeFalse(SystemUtils.IS_OS_WINDOWS); // TODO(tturek): Write batch script to test process interruption.

		commands = new ArrayList<>();
		commands.add("bash");
		commands.add("-c");
		commands.add(getFile("src", "test", "resources", "scriptLoop.sh").getAbsolutePath());
	}

	@Test
	public void loopProcess() throws IOException, InterruptedException {
		final ProcessBuilder pb = new ProcessBuilder(commands);
		final Process process = ProcessBuilderExecutor.asyncExecute(pb);
		Thread.sleep(3000);
		log.info("Terminate process");
		process.destroy();
		assertEquals(0, process.waitFor());
	}

	@Test
	public void loopProcessStreams() throws IOException, InterruptedException {
		final Process process = ProcessExecutor.builder().commands(commands).outputStream(System.out).errorStream(System.err).redirectError(false).build()
				.asyncExecute();
		Thread.sleep(3000);
		log.info("Terminate process");
		process.destroy();
		assertEquals(0, process.waitFor());
	}

	@Test
	public void loopProcessRedirectIntoFile() throws IOException, InterruptedException {
		final File file = temporaryFolder.newFile();
		final ProcessBuilder pb = new ProcessBuilder(commands);
		final Process process = ProcessBuilderExecutor.asyncExecute(pb, file);
		Thread.sleep(3000);
		log.info("Terminate process");
		process.destroy();
		assertEquals(0, process.waitFor());
		assertThat(FileUtils.readFileToString(file), is(containsString("Sleep over")));
	}
}
