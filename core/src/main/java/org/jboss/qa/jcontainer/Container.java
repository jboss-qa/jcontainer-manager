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

import java.io.Closeable;
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
public abstract class Container<T extends Configuration, U extends Client<T>, V extends User> implements Closeable {

	public static final String JCONTAINER_ID = "jcontainer.id";

	protected T configuration;
	protected U client;

	private final long id;
	private final File stdoutLogFile;
	private Class<T> confClass;
	private Class<U> clientClass;
	private volatile List<Thread> shutdownHooks = new ArrayList<>();

	public Container(T configuration) {
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
	 * Returns log directory represented by string path.
	 *
	 * @return String with log directory path
	 */
	public String getLogDir() {
		try {
			return getLogDirInternal();
		} catch (Exception e) {
			throw new IllegalStateException("Log directory was not found", e);
		}
	}

	protected abstract String getLogDirInternal() throws Exception;

	protected void addShutdownHook(Thread hook) {
		shutdownHooks.add(hook);
		Runtime.getRuntime().addShutdownHook(hook);
	}

	public abstract void addUser(V user) throws Exception;

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

		// Modify EXTRA_JAVA_OPTS
		final String extraJavaOptsEnvName = configuration.getExtraJavaOptsEnvName();
		final StringBuilder extraJavaOpts = new StringBuilder();
		final String oldExtraJavaOpts = processBuilder.environment().get(extraJavaOptsEnvName);
		if (oldExtraJavaOpts != null) {
			extraJavaOpts.append(oldExtraJavaOpts);
		}
		extraJavaOpts.append(String.format(" -D%s=%s", JCONTAINER_ID, id));
		processBuilder.environment().put(extraJavaOptsEnvName, extraJavaOpts.toString());

		processBuilder.redirectErrorStream(true);
		processBuilder.redirectOutput(ProcessBuilder.Redirect.to(getStdoutLogFile()));

		final Process process = processBuilder.start();
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
