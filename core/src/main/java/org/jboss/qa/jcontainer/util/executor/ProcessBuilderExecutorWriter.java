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

import java.io.PrintWriter;
import java.io.Writer;

public class ProcessBuilderExecutorWriter extends ProcessBuilderExecutor {

	private final PrintWriter outWriter;

	private final PrintWriter errWriter;

	public ProcessBuilderExecutorWriter(ProcessBuilder processBuilder, final Writer out, final Writer err) {
		super(processBuilder);
		this.outWriter = ExecutorUtil.toPrintWriter(out);
		this.errWriter = ExecutorUtil.toPrintWriter(err);
	}

	@Override
	protected ProcessExecutorThread createProcessExecutorThread() {
		return new ProcessExecutorWriterThread();
	}

	private class ProcessExecutorWriterThread extends ProcessExecutorThread {

		@Override
		protected void prepareAndStartConsumers() {
			outConsumer = new InputStreamConsumerThread(process.getInputStream(), ProcessBuilderExecutorWriter.this.outWriter);
			errConsumer = new InputStreamConsumerThread(process.getErrorStream(), ProcessBuilderExecutorWriter.this.errWriter);

			outConsumer.start();
			errConsumer.start();
		}

		@Override
		protected void close() {
			//this class is not responsible to open outWriter and errWriter, so don't close them
		}
	}
}
