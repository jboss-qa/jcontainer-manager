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

import org.jboss.qa.jcontainer.Configuration;

import java.io.File;
import java.util.List;

public class JBossConfiguration extends Configuration {

	protected final int managementPort;
	protected final String profile;
	protected final Mode mode;

	protected JBossConfiguration(Builder<?> builder) {
		super(builder);
		managementPort = builder.managementPort;
		profile = builder.profile;
		mode = builder.mode;
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

	public List<String> generateCommand() {
		final List<String> cmd = super.generateCommand();

		final File jbossModulesJar = new File(directory, "jboss-modules.jar");
		if (!jbossModulesJar.exists()) {
			throw new IllegalStateException(String.format("File %s does not exist", jbossModulesJar));
		}
		cmd.add("-jar");
		cmd.add(jbossModulesJar.getAbsolutePath());

		// Add executable jar
		final File modulesFolder = new File(directory, "modules");
		if (!modulesFolder.exists()) {
			throw new IllegalStateException(String.format("Folder %s does not exist", modulesFolder));
		}
		cmd.add("-mp");
		cmd.add(modulesFolder.getAbsolutePath());

		if (mode.equals(Mode.STANDALONE)) {
			cmd.add("-jaxpmodule");
			cmd.add("javax.xml.jaxp-provider");
			cmd.add("org.jboss.as.standalone");
			cmd.add("-c");
			cmd.add(profile);
		} else {
			cmd.add("org.jboss.as.process-controller");
			cmd.add("-jboss-home");
			cmd.add(directory.getAbsolutePath());
		}

		cmd.add("-Djboss.home.dir=" + directory.getAbsolutePath());
		cmd.add("-Djava.net.preferIPv4Stack=true");
		cmd.add("-Djava.awt.headless=true");

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

		public Builder() {
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
