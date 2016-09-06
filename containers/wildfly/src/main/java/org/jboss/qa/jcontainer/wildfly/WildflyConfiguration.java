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
package org.jboss.qa.jcontainer.wildfly;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

import org.jboss.qa.jcontainer.Configuration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

public class WildflyConfiguration extends Configuration {

	public static final int DEFAULT_PORT_OFFSET = 0;
	public static final int DEFAULT_MANAGEMENT_PORT = 9990;

	@Getter
	protected final int portOffset;
	@Getter
	protected final String profile;
	@Getter
	protected final Mode mode;
	protected final File baseDir;
	protected final File script;
	protected final String nodeName;

	protected WildflyConfiguration(Builder<?> builder) {
		super(builder);
		portOffset = builder.portOffset;
		profile = builder.profile;
		mode = builder.mode;
		baseDir = builder.baseDir;
		script = builder.script;
		nodeName = builder.nodeName;
		// Following environment property ensures that wildfly-modules process will be killed
		// when container is stopped.
		envProps.put("LAUNCH_JBOSS_IN_BACKGROUND", "true");
	}

	public static Builder<?> builder() {
		return new Builder2();
	}

	public File getBaseDir() {
		return (baseDir != null) ? baseDir : new File(directory, Mode.STANDALONE.getValue());
	}

	public File getConfigurationFolder() {
		// Ignores property "jboss.server.config.dir"
		return new File(getBaseDir(), "configuration");
	}

	@Override
	public List<String> generateCommand() {
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
			cmd.add(script.getAbsolutePath());
		}
		cmd.add("-c");
		cmd.add(profile);
		return cmd;
	}

	@Override
	public int getBusyPort() {
		return getManagementPort();
	}

	public int getManagementPort() {
		return DEFAULT_MANAGEMENT_PORT + portOffset;
	}

	public enum Mode {
		STANDALONE("standalone"), DOMAIN("domain");

		private final String mode;

		Mode(String mode) {
			this.mode = mode;
		}

		public String getValue() {
			return mode;
		}
	}

	public abstract static class Builder<T extends Builder<T>> extends Configuration.Builder<T> {
		protected int portOffset;
		protected String profile;
		protected Mode mode;
		protected File baseDir;
		protected File script;
		protected String nodeName;

		public Builder() {
			xms = "64m";
			xmx = "512m";
			maxPermSize = "256m";
			portOffset = DEFAULT_PORT_OFFSET;
			profile = "standalone.xml";
			mode = Mode.STANDALONE;
			logFileName = "server.log";
		}

		public T portOffset(int portOffset) {
			this.portOffset = portOffset;
			return self();
		}

		public T baseDir(File baseDir) {
			this.baseDir = baseDir;
			return self();
		}

		public T profile(String profile) {
			this.profile = profile;
			return self();
		}

		public T mode(Mode mode) {
			this.mode = mode;
			return self();
		}

		public T adminOnly() {
			param("--admin-only");
			return self();
		}

		public T nodeName(String nodeName) {
			this.nodeName = nodeName;
			return self();
		}

		public WildflyConfiguration build() {
			// Set script
			if (mode.equals(Mode.STANDALONE)) {
				script = new File(directory, "bin" + File.separator
						+ (SystemUtils.IS_OS_WINDOWS ? "standalone.bat" : "standalone.sh"));
			} else {
				script = new File(directory, "bin" + File.separator
						+ (SystemUtils.IS_OS_WINDOWS ? "domain.bat" : "domain.sh"));
			}

			// Set JAVA_OPTS
			final StringBuilder javaOpts = new StringBuilder();
			if (!StringUtils.isEmpty(xms)) {
				javaOpts.append(" -Xms").append(xms);
			}
			if (!StringUtils.isEmpty(xmx)) {
				javaOpts.append(" -Xmx").append(xmx);
			}
			if (!StringUtils.isEmpty(permSize)) {
				javaOpts.append(" -XX:PermSize=").append(permSize);
			}
			if (!StringUtils.isEmpty(maxPermSize)) {
				javaOpts.append(" -XX:MaxPermSize=").append(maxPermSize);
			}
			javaOpts.append(" -Djava.net.preferIPv4Stack=true");
			javaOpts.append(" -Djboss.modules.system.pkgs=org.jboss.byteman");
			javaOpts.append(" -Djava.awt.headless=true");
			javaOpts.append(" -Djboss.socket.binding.port-offset=").append(portOffset);
			if (baseDir != null) {
				javaOpts.append(" -Djboss.server.base.dir=").append(baseDir);
			}
			if (nodeName != null) {
				javaOpts.append(" -Djboss.node.name=").append(nodeName);
			}
			envProps.put("JAVA_OPTS", javaOpts.toString());

			return new WildflyConfiguration(this);
		}
	}

	private static class Builder2 extends Builder<Builder2> {
		@Override
		protected Builder2 self() {
			return this;
		}
	}
}
