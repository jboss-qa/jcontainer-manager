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

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProcessBuilderExecutorFile extends ProcessBuilderExecutor {

	private final File outFile;

	public ProcessBuilderExecutorFile(ProcessBuilder processBuilder, File outAndErrFile) {
		super(processBuilder);
		this.outFile = outAndErrFile;
	}

	@Override
	protected ProcessExecutorThread createProcessExecutorThread() {
		return new ProcessExecutorFileThread();
	}

	private class ProcessExecutorFileThread extends ProcessExecutorThread {

		private PrintWriter outWriter;

		@Override
		protected void prepareAndStartConsumers() {
			if (outFile != null) {
				try {
					log.debug("Saving STDOUT to " + outFile);
					this.outWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outFile)));
				} catch (final Exception e) {
					log.error("Could not open outFile " + outFile, e);
				}
			}
			outConsumer = new InputStreamConsumerThread(process.getInputStream(), this.outWriter);
			errConsumer = new InputStreamConsumerThread(process.getErrorStream(), this.outWriter);

			outConsumer.start();
			errConsumer.start();
		}

		@Override
		protected void close() {
			//this class is not responsible to open outWriter, so don't close it
			ExecutorUtil.closeCloseable(outWriter);
			outWriter = null;
		}
	}
}
