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

import org.apache.commons.lang3.SystemUtils;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.Timeout;

import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AbstractProcessBuilderExecutorTest {

	public static final String OUTPUT_TEXT = "output text";
	public static final String ERROR_TEXT = "error text";

	public static final String EXPECTED_RESULT = OUTPUT_TEXT + "\n" + ERROR_TEXT + "\n";

	protected static List<String> commands;

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Rule
	public Timeout globalTimeout = Timeout.seconds(5);

	@BeforeClass
	public static void setUpClass() {
		commands = new ArrayList<>();
		if (SystemUtils.IS_OS_WINDOWS) {
			commands.add("cmd");
			commands.add("/c");
			commands.add(getFile("src", "test", "resources", "script.bat").getAbsolutePath());
		} else {
			commands.add("bash");
			commands.add("-c");
			commands.add(getFile("src", "test", "resources", "script.sh").getAbsolutePath());
		}
	}

	public static ProcessBuilder createProcessBuilder() {
		return new ProcessBuilder(commands);
	}
}
