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

import org.jboss.qa.jcontainer.util.ProcessUtils;
import org.jboss.qa.jcontainer.util.ReflectionUtils;
import org.jboss.qa.jcontainer.util.executor.ProcessBuilderExecutor;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractContainer<T extends Configuration, U extends Client<T>, V extends User> implements Container<T, U, V> {

	public static final String JCONTAINER_ID = "jcontainer.id";
	private final long id;
	private final File stdoutLogFile;
	protected T configuration;
	protected U client;
	private Class<T> confClass;
	private Class<U> clientClass;
	private volatile List<Thread> shutdownHooks = new ArrayList<>();

	public AbstractContainer(T configuration) {
		id = System.nanoTime();
		stdoutLogFile = new File(configuration.getDirectory(), String.format("stdout-%s.log", id));
		confClass = ReflectionUtils.getGenericClass(getClass(), 0);
		clientClass = ReflectionUtils.getGenericClass(getClass(), 1);
		this.configuration = configuration;
		client = createClient(configuration);
		log.info("container id = {}", id);
	}

	public long getId() {
		return id;
	}

	public File getStdoutLogFile() {
		return stdoutLogFile;
	}

	/**
	 * Returns command, which can be used by client.
	 *
	 * @return String command if client is supported, NULL otherwise
	 */
	protected abstract String getBasicCommand();

	/**
	 * Returns log directory.
	 */
	public File getLogDir() {
		try {
			return getLogDirInternal();
		} catch (Exception e) {
			throw new IllegalStateException("Log directory was not found", e);
		}
	}

	/**
	 * Returns default log file.
	 */
	public File getDefaultLogFile() {
		final File logFile = new File(getLogDir(), configuration.getLogFileName());
		if (!logFile.exists()) {
			log.warn("Log file does not exist: {}", logFile.getAbsoluteFile());
		}
		return logFile;
	}

	protected abstract File getLogDirInternal();

	protected void addShutdownHook(Thread hook) {
		shutdownHooks.add(hook);
		Runtime.getRuntime().addShutdownHook(hook);
	}

	@Override
	public synchronized void start() throws Exception {
		if (isRunning()) {
			log.warn("Container is already started");
			return;
		}
		if (checkSocket()) {
			throw new IllegalStateException(String.format("Another container already uses %s:%d",
					configuration.getHost(), configuration.getBusyPort()));
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

		// Modify JAVA_OPTS
		final String javaOptsEnvName = configuration.getJavaOptsEnvName();
		final StringBuilder javaOpts = new StringBuilder();
		final String oldJavaOpts = processBuilder.environment().get(javaOptsEnvName);
		if (oldJavaOpts != null) {
			javaOpts.append(oldJavaOpts);
		}
		javaOpts.append(String.format(" -D%s=%s", JCONTAINER_ID, id));
		processBuilder.environment().put(javaOptsEnvName, javaOpts.toString());

		final Process process = ProcessBuilderExecutor.asyncExecute(processBuilder, getStdoutLogFile());

		addShutdownHook(new Thread(new Runnable() {
			public void run() {
				if (process != null) {
					process.destroy();
					try {
						process.waitFor();
					} catch (InterruptedException e) {
						throw new IllegalStateException("Container was not stopped", e);
					}
				}
			}
		}));
		// This shutdown hook wait until container process exists.
		addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					String pid;
					while ((pid = ProcessUtils.getJavaPidByContainerId(getId())) != null) {
						log.debug("Stopping container (PID {}) ...", pid);
						Thread.sleep(TimeUnit.SECONDS.toMillis(1));
					}
				} catch (InterruptedException e) {
					log.trace(e.getMessage(), e);
					Thread.currentThread().interrupt();
				}
			}
		}));
		waitForStarted();
	}

	public synchronized void stop(long timeout, TimeUnit timeUnit) throws Exception {
		if (isRunning()) {
			client.close();
			final ExecutorService service = Executors.newCachedThreadPool();
			final List<Future> futures = new ArrayList<>();
			for (Thread shutdownHook : shutdownHooks) {
				Runtime.getRuntime().removeShutdownHook(shutdownHook);
				futures.add(service.submit(shutdownHook));
			}
			service.shutdown();
			if (!service.awaitTermination(timeout, timeUnit)) {
				for (Future future : futures) {
					future.cancel(true);
				}
				log.error("Container shutdown process didn't finish in {} {}!", timeout, timeUnit);
			} else {
				log.info("Container was stopped");
			}
			shutdownHooks.clear();
		}
	}

	@Override
	public synchronized void stop() throws Exception {
		stop(1, TimeUnit.MINUTES);
	}

	@Override
	public void close() throws IOException {
		try {
			stop();
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	@Override
	public boolean isRunning() throws Exception {
		return !shutdownHooks.isEmpty();
	}

	protected synchronized void waitForStarted() throws InterruptedException {
		int attempts = 30;
		while (!checkSocket()) {
			if (--attempts <= 0) {
				throw new IllegalStateException("Container was not started");
			}
			Thread.sleep(TimeUnit.SECONDS.toMillis(5));
			log.info("Waiting for container...");
		}
		checkClient();
		log.info("Container was started");
	}

	public void checkClient() {
		if (isClientSupported()) {
			final int clientAttempts = 20;
			final String basicCommand = getBasicCommand();
			for (int i = 0; i < clientAttempts; i++) {
				try {
					Thread.sleep(TimeUnit.SECONDS.toMillis(5));
					client.execute(basicCommand);
					log.debug("Client was connected to container");
					return;
				} catch (Exception e) {
					log.debug("Waiting for client...");
				}
			}
			throw new IllegalStateException("Client was not connected to container");
		}
	}

	@Override
	public boolean isClientSupported() {
		return getBasicCommand() != null;
	}

	public synchronized boolean checkSocket() {
		try (Socket socket = new Socket(configuration.getHost(), configuration.getBusyPort())) {
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public T getConfiguration() {
		return configuration;
	}

	@Override
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
