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

import org.apache.commons.lang.SystemUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.Pipe;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Builder
public class ProcessExecutor {

	@Singular
	private List<String> commands;
	private OutputStream outputStream;
	private OutputStream errorStream;
	private boolean redirectError;

	private ProcessBuilder processBuilder;

	public int syncExecute() throws IOException, InterruptedException, ExecutionException {
		return asyncExecute().waitFor();
	}

	public Process asyncExecute() throws IOException {
		if (processBuilder == null) {
			processBuilder = new ProcessBuilder(commands);
		}

		if (outputStream == null) {
			if (SystemUtils.IS_OS_HP_UX) {
				outputStream = System.out;
			} else {
				processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
			}
		}
		if (errorStream == null && !redirectError) {
			if (SystemUtils.IS_OS_HP_UX) {
				outputStream = System.err;
			} else {
				processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
			}
		}
		processBuilder.redirectErrorStream(redirectError);

		final Process process = processBuilder.start();

		final ExecutorService executeService = Executors.newCachedThreadPool();
		final List<Future> futures = new ArrayList<>();

		if (outputStream != null) {
			final Pipe pipe = Pipe.open();
			futures.add(executeService.submit(new CopyIntoChannel(Channels.newChannel(process.getInputStream()), pipe.sink())));
			futures.add(executeService.submit(new CopyIntoChannel(pipe.source(), Channels.newChannel(outputStream))));
		}
		if (errorStream != null && !redirectError) {
			final Pipe pipe = Pipe.open();
			futures.add(executeService.submit(new CopyIntoChannel(Channels.newChannel(process.getErrorStream()), pipe.sink())));
			futures.add(executeService.submit(new CopyIntoChannel(pipe.source(), Channels.newChannel(errorStream))));
		}

		final Future<Integer> future = executeService.submit(new Callable<Integer>() {
			@Override
			public Integer call() throws Exception {
				process.waitFor();
				for (Future f : futures) {
					f.get();
				}
				return process.exitValue();
			}
		});

		final Process proxyProcess = new ProxyProcess(process, future);

		executeService.shutdown();
		return proxyProcess;
	}

	@Slf4j
	@AllArgsConstructor
	static class ProxyProcess extends Process {

		private Process process;
		private Future<Integer> future;

		@Override
		public OutputStream getOutputStream() {
			return process.getOutputStream();
		}

		@Override
		public InputStream getInputStream() {
			return process.getInputStream();
		}

		@Override
		public InputStream getErrorStream() {
			return process.getErrorStream();
		}

		@Override
		public int waitFor() throws InterruptedException {
			try {
				return future.get();
			} catch (ExecutionException e) {
				log.error(e.getMessage(), e);
				return 500;
			}
		}

		@Override
		public int exitValue() {
			return process.exitValue();
		}

		@Override
		public void destroy() {
			process.destroy();
		}
	}

	@Slf4j
	@AllArgsConstructor
	static class CopyIntoChannel implements Runnable {

		@NonNull
		private ReadableByteChannel sourceChannel;
		@NonNull
		private WritableByteChannel sinkChannel;

		@Override
		public void run() {
			final ByteBuffer buf = ByteBuffer.allocate(48);
			try {
				while (sourceChannel.isOpen() && sourceChannel.read(buf) != -1) {
					buf.flip();  //make buffer ready for read
					if (!sinkChannel.isOpen()) {
						return;
					}
					sinkChannel.write(buf);
					buf.clear(); //make buffer ready for writing
				}
			} catch (IOException e) {
				if (e.getMessage().contains("Stream closed")) {
					log.debug(e.getMessage());
				} else {
					log.error(e.getMessage(), e);
				}
			} finally {
				if (sinkChannel instanceof Pipe.SinkChannel) {
					try {
						sinkChannel.close();
					} catch (IOException e) {
						log.error(e.getMessage(), e);
					}
				}
			}
		}
	}
}
