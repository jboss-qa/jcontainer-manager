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

import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class ProcessExecutorTest extends AbstractProcessBuilderExecutorTest {

	@Test
	public void processExecutor() throws IOException, InterruptedException, ExecutionException {
		final ProcessExecutor ep = ProcessExecutor.builder().commands(commands).build();
		assertEquals(0, ep.syncExecute());
	}

	@Test
	public void processExecutorWithSeparateErrorStream() throws IOException, ExecutionException, InterruptedException {
		final ProcessExecutor ep = ProcessExecutor.builder().commands(commands).redirectError(false).build();
		final Process future = ep.asyncExecute();
		assertEquals(0, future.waitFor());
	}
}
