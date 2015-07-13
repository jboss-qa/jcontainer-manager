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
package org.jboss.qa.jcontainer;

import org.jboss.qa.jcontainer.util.ReflectionUtils;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class Container<T extends Configuration, U extends Client<T>, V extends User> implements Closeable {

	private static class ContainerLogger implements Runnable {

		private volatile boolean stop;
		private volatile Process process;

		public ContainerLogger(Process process) {
			this.process = process;
		}

		private void stop() {
			stop = true;
		}

		@Override
		public void run() {
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				while (!stop && reader.readLine() != null) { // ends with server shutdown
				}
				reader.close();
			} catch (Exception e) {
				// stream closed
			}
		}
	}

	protected T configuration;
	protected U client;

	private Class<T> confClass;
	private Class<U> clientClass;
	private volatile Thread shutdownThread;
	private ContainerLogger containerLogger;

	public Container(T configuration) {
		confClass = ReflectionUtils.getGenericClass(getClass(), 0);
		clientClass = ReflectionUtils.getGenericClass(getClass(), 1);
		this.configuration = configuration;
		client = createClient(configuration);
	}

	public abstract void addUser(V user) throws Exception;

	public synchronized void start() throws Exception {
		if (isRunning()) {
			log.warn("Container is already started");
			return;
		}
		if (checkSocket()) {
			throw new IllegalStateException(String.format("Another container is already running on %s:%d",
					configuration.host, configuration.port));
		}
		if (configuration.getDirectory() == null || !configuration.getDirectory().exists()) {
			throw new IllegalArgumentException("Directory of container must exist");
		}
		final List<String> cmd = configuration.generateCommand();
		cmd.addAll(configuration.getParams());
		log.debug("Process arguments: " + cmd.toString());

		final ProcessBuilder processBuilder = new ProcessBuilder(cmd);
		processBuilder.environment().putAll(System.getenv());
		processBuilder.environment().putAll(configuration.getEnvProps());

		final Process process = processBuilder.start();
		int attempts = 30;
		while (!checkSocket()) {
			if (--attempts <= 0) {
				throw new IllegalStateException("Container was not started");
			}
			Thread.sleep(TimeUnit.SECONDS.toMillis(5));
			log.info("Waiting for container...");
		}
		log.info("Container was started");
		shutdownThread = new Thread(new Runnable() {
			public void run() {
				if (process != null) {
					process.destroy();
					containerLogger.stop();
					try {
						process.waitFor();
					} catch (InterruptedException e) {
						throw new IllegalStateException("Container was not stopped", e);
					}
				}
			}
		});

		// Consume container stream
		containerLogger = new ContainerLogger(process);
		new Thread(containerLogger).start();
		Runtime.getRuntime().addShutdownHook(shutdownThread);
	}

	public synchronized void stop() throws Exception {
		if (isRunning()) {
			client.close();
			Runtime.getRuntime().removeShutdownHook(shutdownThread);
			shutdownThread.start();
			shutdownThread.join();
			shutdownThread = null;
			log.info("Container was stopped");
		}
	}

	@Override
	public void close() throws IOException {
		try {
			stop();
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	public boolean isRunning() throws Exception {
		return shutdownThread != null;
	}

	public synchronized boolean checkSocket() {
		try (Socket socket = new Socket(configuration.getHost(), configuration.getPort())) {
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public T getConfiguration() {
		return configuration;
	}

	public U getClient() {
		return client;
	}

	protected U createClient(T configuration) {
		try {
			return clientClass.getConstructor(confClass).newInstance(configuration);
		} catch (Exception e) {
			log.error("Client was not created");
		}
		return null;
	}

	protected void checkMandatoryProperty(String name, Object value) {
		if (value == null) {
			throw new IllegalArgumentException(String.format("Property '%s' is mandatory", name));
		}
	}
}
