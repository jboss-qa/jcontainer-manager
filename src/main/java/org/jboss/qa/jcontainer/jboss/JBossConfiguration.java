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
package org.jboss.qa.jcontainer.jboss;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

import org.jboss.qa.jcontainer.Configuration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class JBossConfiguration extends Configuration {

	protected final int managementPort;
	protected final String profile;
	protected final Mode mode;
	protected final File script;

	protected JBossConfiguration(Builder<?> builder) {
		super(builder);
		managementPort = builder.managementPort;
		profile = builder.profile;
		mode = builder.mode;
		script = builder.script;
		// Following environment property ensures that jboss-modules process will be killed
		// when container is stopped.
		envProps.put("LAUNCH_JBOSS_IN_BACKGROUND", "true");
	}

	public static Builder<?> builder() {
		return new Builder2();
	}

	public String getProfile() {
		return profile;
	}

	public int getManagementPort() {
		return managementPort;
	}

	public Mode getMode() {
		return mode;
	}

	public File getBaseDir() {
		// TODO(mbasovni): Add support of "jboss.server.base.dir"
		final String modeVal = (this.mode != null ? this.mode.getValue() : Mode.STANDALONE.getValue());
		return new File(directory, modeVal);
	}

	public File getConfigurationFolder() {
		// TODO(mbasovni): Add support of "jboss.server.config.dir"
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
			cmd.add("/bin/bash");
			cmd.add(script.getAbsolutePath());
		}
		cmd.add("-c");
		cmd.add(profile);
		return cmd;
	}

	public static enum Mode {
		STANDALONE("standalone"), MANAGEMENT("management");

		private final String mode;

		Mode(String mode) {
			this.mode = mode;
		}

		public String getValue() {
			return mode;
		}
	}

	public abstract static class Builder<T extends Builder<T>> extends Configuration.Builder<T> {
		protected int managementPort;
		protected String profile;
		protected Mode mode;
		protected File script;

		public Builder() {
			xms = "1303m";
			xmx = "1303m";
			maxPermSize = "256m";
			managementPort = 9990;
			profile = "standalone.xml";
			mode = Mode.STANDALONE;
		}

		public T managementPort(int managementPort) {
			this.managementPort = managementPort;
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

		public JBossConfiguration build() {
			// Set script
			if (mode.equals(Mode.STANDALONE)) {
				script = new File(directory, "bin" + File.separator
						+ (SystemUtils.IS_OS_WINDOWS ? "standalone.bat" : "standalone.sh"));
			} else {
				script = new File(directory, "bin" + File.separator
						+ (SystemUtils.IS_OS_WINDOWS ? "domain.bat" : "domain.sh"));
			}

			// Set JAVA_OPTS
			final StringBuffer javaOpts = new StringBuffer();
			if (!StringUtils.isEmpty(xms)) {
				javaOpts.append(" -Xms" + xms);
			}
			if (!StringUtils.isEmpty(xmx)) {
				javaOpts.append(" -Xmx" + xmx);
			}
			if (!StringUtils.isEmpty(permSize)) {
				javaOpts.append(" -XX:PermSize=" + permSize);
			}
			if (!StringUtils.isEmpty(maxPermSize)) {
				javaOpts.append(" -XX:MaxPermSize=" + maxPermSize);
			}
			javaOpts.append(" -Djava.net.preferIPv4Stack=true");
			javaOpts.append(" -Djava.awt.headless=true");
			envProps.put("JAVA_OPTS", javaOpts.toString());

			return new JBossConfiguration(this);
		}
	}

	private static class Builder2 extends Builder<Builder2> {
		@Override
		protected Builder2 self() {
			return this;
		}
	}
}
