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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;
import static org.hamcrest.Matchers.is;

import org.apache.commons.io.FileUtils;

import org.junit.Test;

import java.io.File;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProcessBuilderExecutorTest extends AbstractProcessBuilderExecutorTest {
	@Test
	public void syncExecute() throws Exception {
		final int result = ProcessBuilderExecutor.syncExecute(createProcessBuilder());
		assertEquals("Response code should be 0. ", 0, result);
	}

	@Test
	public void asyncExecute() throws Exception {
		final Process process = ProcessBuilderExecutor.asyncExecute(createProcessBuilder());
		assertEquals("Response code should be 0. ", 0, process.waitFor());
	}

	@Test
	public void syncExecuteWithOutputFile() throws Exception {
		final File file = temporaryFolder.newFile();
		final int result = ProcessBuilderExecutor.syncExecute(createProcessBuilder(), file);
		assertEquals("Response code should be 0. ", 0, result);
		assertTrue("File with output does not exists. ", file.exists());
		final String fileContent = FileUtils.readFileToString(file);
		log.debug("File content: {}", fileContent);
		assertThat(fileContent, is(equalToIgnoringWhiteSpace(EXPECTED_RESULT)));
	}

	@Test
	public void asyncExecuteWithOutputFile() throws Exception {
		final File file = temporaryFolder.newFile();
		final Process process = ProcessBuilderExecutor.asyncExecute(createProcessBuilder(), file);
		assertEquals("Response code should be 0. ", 0, process.waitFor());
		assertTrue("File with output does not exists. ", file.exists());
		final String fileContent = FileUtils.readFileToString(file);
		log.debug("File content: {}", fileContent);
		assertThat(fileContent, is(equalToIgnoringWhiteSpace(EXPECTED_RESULT)));
	}
}
