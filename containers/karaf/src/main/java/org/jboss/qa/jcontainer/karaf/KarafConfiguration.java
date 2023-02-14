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
package org.jboss.qa.jcontainer.karaf;

import org.apache.commons.lang3.SystemUtils;

import org.jboss.qa.jcontainer.JavaConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

public class KarafConfiguration extends JavaConfiguration {

	public static final String KARAF_OPTS_ENV_NAME = "KARAF_OPTS";

	public static final int DEFAULT_HTTP_PORT = 8181;
	public static final int DEFAULT_SSH_PORT = 8101;
	public static final int DEFAULT_FUSE_VERSION = -1;

	@Getter
	protected final int httpPort;
	@Getter
	protected final int sshPort;
	@Getter
	protected File keyFile;
	protected final File script;
	protected final File stopScript;
	@Getter
	protected final int version;

	protected KarafConfiguration(Builder<?> builder) {
		super(builder);
		script = builder.script;
		stopScript = builder.stopScript;
		httpPort = builder.httpPort;
		sshPort = builder.sshPort;
		//Optional
		keyFile = builder.keyFile;
		version = builder.version;
	}

	public static Builder<?> builder() {
		return new Builder2();
	}

	@Override
	public List<String> generateCommand() {
		return generateCommand(script);
	}

	public List<String> generateStopCommand() {
		return generateCommand(stopScript);
	}

	public List<String> generateCommand(File script) {
		if (!script.exists()) {
			throw new IllegalStateException(String.format("Script '%s' does not exist", script.getAbsolutePath()));
		}
		final List<String> cmd = new ArrayList<>();
		if (SystemUtils.IS_OS_WINDOWS) {
			cmd.add("cmd");
			cmd.add("/c");
			cmd.add(script.getAbsolutePath());
		} else {
			cmd.add("bash");
			cmd.add("-c");
			cmd.add(script.getAbsolutePath());
		}
		return cmd;
	}

	@Override
	public int getBusyPort() {
		return sshPort;
	}

	public void setKeyFile(File keyFile) {
		this.keyFile = keyFile;
	}

	public abstract static class Builder<T extends Builder<T>> extends JavaConfiguration.Builder<T> {
		protected int httpPort;
		protected int sshPort;
		protected File keyFile;
		protected File script;
		protected File stopScript;
		protected int version;

		public Builder() {
			super();
			httpPort(DEFAULT_HTTP_PORT);
			sshPort(DEFAULT_SSH_PORT);
			username("karaf");
			password("karaf");
			logFileName("karaf.log");
			version(DEFAULT_FUSE_VERSION);
			//needed in jdk17
			if (!JavaConfiguration.BEFORE_JDK17) {
				javaOpt("--add-opens java.base/java.lang=ALL-UNNAMED");
			}
		}

		@Override
		protected String javaOptsEnvName() {
			return KARAF_OPTS_ENV_NAME;
		}

		public T httpPort(int httpPort) {
			this.httpPort = httpPort;
			return self();
		}

		public T sshPort(int sshPort) {
			this.sshPort = sshPort;
			return self();
		}

		public T keyFile(String keyFile) {
			this.keyFile = new File(keyFile);
			return self();
		}

		public T version(int version) {
			this.version = version;
			return self();
		}

		public KarafConfiguration build() {
			script = new File(directory, "bin/" + (SystemUtils.IS_OS_WINDOWS ? "start.bat" : "start"));
			stopScript = new File(directory, "bin/" + (SystemUtils.IS_OS_WINDOWS ? "stop.bat" : "stop"));
			return new KarafConfiguration(this);
		}
	}

	private static class Builder2 extends Builder<Builder2> {
		@Override
		protected Builder2 self() {
			return this;
		}
	}
}
