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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

import org.jboss.qa.jcontainer.Configuration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WildflyConfiguration extends Configuration {

	public static final int DEFAULT_HTTP_PORT = 8080;
	public static final int DEFAULT_MANAGEMENT_PORT = 9990;

	protected final int httpPort;
	protected final int managementPort;
	protected final String profile;
	protected final Mode mode;
	protected final File script;

	protected WildflyConfiguration(Builder<?> builder) {
		super(builder);
		httpPort = builder.httpPort;
		managementPort = builder.managementPort;
		profile = builder.profile;
		mode = builder.mode;
		script = builder.script;
		// Following environment property ensures that wildfly-modules process will be killed
		// when container is stopped.
		envProps.put("LAUNCH_JBOSS_IN_BACKGROUND", "true");
	}

	public static Builder<?> builder() {
		return new Builder2();
	}

	public String getProfile() {
		return profile;
	}

	public int getHttpPort() {
		return httpPort;
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
			cmd.add("bash");
			cmd.add(script.getAbsolutePath());
		}
		cmd.add("-c");
		cmd.add(profile);
		return cmd;
	}

	@Override
	public int getBusyPort() {
		return httpPort;
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

		private static final String EXTRA_JAVA_OPTS_SCRIPT_WIN = "wildflyExtraJavaOpts.conf.bat";
		private static final String EXTRA_JAVA_OPTS_SCRIPT = "wildflyExtraJavaOpts.conf";

		protected int httpPort;
		protected int managementPort;
		protected String profile;
		protected Mode mode;
		protected File script;
		protected File scriptConf;

		public Builder() {
			xms = "1303m";
			xmx = "1303m";
			maxPermSize = "256m";
			httpPort = DEFAULT_HTTP_PORT;
			managementPort = DEFAULT_MANAGEMENT_PORT;
			profile = "standalone.xml";
			mode = Mode.STANDALONE;
		}

		public T httpPort(int httpPort) {
			this.httpPort = httpPort;
			return self();
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

		public WildflyConfiguration build() {
			// Set script
			if (mode.equals(Mode.STANDALONE)) {
				script = new File(directory, "bin" + File.separator
						+ (SystemUtils.IS_OS_WINDOWS ? "standalone.bat" : "standalone.sh"));
				scriptConf = new File(directory, "bin" + File.separator
						+ (SystemUtils.IS_OS_WINDOWS ? "standalone.conf.bat" : "standalone.conf"));
			} else {
				script = new File(directory, "bin" + File.separator
						+ (SystemUtils.IS_OS_WINDOWS ? "domain.bat" : "domain.sh"));
				scriptConf = new File(directory, "bin" + File.separator
						+ (SystemUtils.IS_OS_WINDOWS ? "domain.conf.bat" : "domain.conf"));
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
			javaOpts.append(" -Djboss.management.http.port=" + managementPort);
			javaOpts.append(" -Djboss.http.port=" + httpPort);
			//true JAVA_OPTS (not EXTRA_JAVA_OPTS), because it could contain -Xmx etc...
			envProps.put("JAVA_OPTS", javaOpts.toString());

			try {
				addExtraJavaOptsIntoScriptConf();
			} catch (final IOException e) {
				log.error("Problem while adding EXTRA_JAVA_OPTS into " + scriptConf.getAbsolutePath(), e);
			}
			return new WildflyConfiguration(this);
		}

		/**
		 * This will modify eap/wildfly conf script (standalone.conf[.bat]|domain.conf[.bat]) to use EXTRA_JAVA_OPTS env.
		 */
		private void addExtraJavaOptsIntoScriptConf() throws IOException {
			if (!scriptConf.exists()) {
				log.warn("Script conf file " + scriptConf.getAbsolutePath() + " does not exists.");
				return;
			}
			if (!scriptConf.canRead()) {
				log.warn("We could not read script conf file " + scriptConf.getAbsolutePath());
				return;
			}
			if (hasExtraJavaOpts(scriptConf)) {
				//already got EXTRA_JAVA_OPTS in the conf
				return;
			}

			final String extraJavaOptsScript = loadExtraJavaOptsScript();
			try (final PrintWriter w = new PrintWriter(new FileWriter(scriptConf, true))) {
				w.println();
				w.println(extraJavaOptsScript);
			}
		}

		private static boolean hasExtraJavaOpts(final File file) throws IOException {
			try (final BufferedReader r = new BufferedReader(new FileReader(file))) {
				String line;
				while ((line = r.readLine()) != null) {
					if (line.contains(EXTRA_JAVA_OPTS_ENV_NAME)) {
						return true;
					}
				}
			}
			return false;
		}

		private String loadExtraJavaOptsScript() throws IOException {
			final String resourceName = SystemUtils.IS_OS_WINDOWS ? EXTRA_JAVA_OPTS_SCRIPT_WIN : EXTRA_JAVA_OPTS_SCRIPT;
			try (final InputStream in = this.getClass().getClassLoader().getResourceAsStream(resourceName)) {
				return IOUtils.toString(in);
			}
		}

	}

	private static class Builder2 extends Builder<Builder2> {
		@Override
		protected Builder2 self() {
			return this;
		}
	}
}
