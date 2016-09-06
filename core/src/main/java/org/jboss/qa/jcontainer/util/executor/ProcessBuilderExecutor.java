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

import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class ProcessBuilderExecutor {

	private final ProcessBuilder processBuilder;

	private ProcessExecutorThread executorThread;

	private Integer exitValue;

	private final Object exitValueSync = new Object();

	public static int syncExecute(final ProcessBuilder processBuilder) throws InterruptedException, IOException {
		return syncExecute(processBuilder, null);
	}

	public static int syncExecute(final ProcessBuilder processBuilder, final File outAndErrFile) throws InterruptedException, IOException {
		if (SystemUtils.IS_OS_HP_UX) {
			final ProcessBuilderExecutor pbe = executeOnHpUx(processBuilder, outAndErrFile);
			return pbe.waitFor();
		}
		final Process p = executeOnOthers(processBuilder, outAndErrFile);
		return p.waitFor();
	}

	public static Process asyncExecute(final ProcessBuilder processBuilder) throws InterruptedException, IOException {
		return asyncExecute(processBuilder, null);
	}

	public static Process asyncExecute(final ProcessBuilder processBuilder, final File outAndErrFile) throws InterruptedException, IOException {
		if (SystemUtils.IS_OS_HP_UX) {
			final ProcessBuilderExecutor pbe = executeOnHpUx(processBuilder, outAndErrFile);
			return pbe.getProcess();
		}
		return executeOnOthers(processBuilder, outAndErrFile);
	}

	private static ProcessBuilderExecutor executeOnHpUx(final ProcessBuilder processBuilder, final File outAndErrFile) {
		final ProcessBuilderExecutor pbe;
		if (outAndErrFile != null) {
			pbe = new ProcessBuilderExecutorFile(processBuilder, outAndErrFile);
		} else {
			pbe = new ProcessBuilderExecutorWriter(processBuilder, new PrintWriter(System.out), new PrintWriter(System.err));
		}
		pbe.start();
		return pbe;
	}

	private static Process executeOnOthers(final ProcessBuilder processBuilder, final File outAndErrFile) throws IOException {
		if (outAndErrFile != null) {
			processBuilder.redirectErrorStream(true);
			processBuilder.redirectOutput(ProcessBuilder.Redirect.to(outAndErrFile));
		} else {
			processBuilder.inheritIO();
		}
		return processBuilder.start();
	}

	public ProcessBuilderExecutor(final ProcessBuilder processBuilder) {
		this.processBuilder = processBuilder;
	}

	public synchronized void start() {
		if (executorThread != null) {
			throw new IllegalStateException("Already started.");
		}

		this.executorThread = createProcessExecutorThread();
		this.executorThread.start();

		waitForStartProcess(10000L);
	}

	private void waitForStartProcess(final long timeout) {
		synchronized (this.executorThread.processSyncStart) {
			if (this.executorThread.process == null) {
				try {
					this.executorThread.processSyncStart.wait(timeout);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}
	}

	protected abstract ProcessExecutorThread createProcessExecutorThread();

	public int waitFor() throws InterruptedException {
		if (executorThread == null) {
			throw new IllegalStateException("Not started.");
		}
		while (true) {
			if (!executorThread.isAlive()) {
				if (exitValue != null) {
					return exitValue;
				}
				log.error("Exit value is not set. Returning " + Short.MIN_VALUE);
				return Short.MIN_VALUE;
			}
			synchronized (exitValueSync) {
				if (exitValue != null) {
					return exitValue;
				}
				exitValueSync.wait(1000L);
			}
		}
	}

	public Process getProcess() {
		if (executorThread == null) {
			throw new IllegalStateException("Not started.");
		}
		return executorThread.process;
	}

	protected abstract class ProcessExecutorThread extends Thread {

		protected Process process;

		private final Object processSyncStart = new Object();

		protected InputStreamConsumerThread outConsumer;

		protected InputStreamConsumerThread errConsumer;

		@Override
		public void run() {
			try {
				try {
					log.debug("Starting " + processBuilder.command());
					synchronized (this.processSyncStart) {
						this.process = processBuilder.start();
						this.processSyncStart.notifyAll();
					}
					log.debug("Started " + processBuilder.command());
				} catch (final IOException e) {
					log.error("Problem while executing ProcessBuilder. Exit value will be " + Short.MIN_VALUE, e);
					setExitValue(Short.MIN_VALUE);
					return;
				}

				prepareAndStartConsumers();

				try {
					log.debug("Waiting for End of process " + processBuilder.command());
					final int ev = process.waitFor();
					log.debug("Process done " + processBuilder.command() + "; exit value: " + ev);
					setExitValue(ev);
					ExecutorUtil.closeProcessStreams(process);
				} catch (final Exception e) {
					log.warn("Interrupted process. Exit value will be " + Short.MIN_VALUE, e);
					setExitValue(Short.MIN_VALUE);
					ExecutorUtil.closeProcessStreams(process);
					process.destroy();
				}
			} finally {
				close();
			}
		}

		private void setExitValue(final int value) {
			synchronized (ProcessBuilderExecutor.this.exitValueSync) {
				ProcessBuilderExecutor.this.exitValue = value;
				ProcessBuilderExecutor.this.exitValueSync.notifyAll();
			}
		}

		protected abstract void prepareAndStartConsumers();

		protected abstract void close();
	}
}
