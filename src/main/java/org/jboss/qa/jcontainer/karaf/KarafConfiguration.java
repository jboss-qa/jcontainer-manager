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

import org.jboss.qa.jcontainer.Configuration;

import java.io.File;
import java.util.List;

public class KarafConfiguration extends Configuration {

	protected final File keyFile;

	protected KarafConfiguration(Builder<?> builder) {
		super(builder);
		//Optional
		keyFile = builder.keyFile;
	}

	public static Builder<?> builder() {
		return new Builder2();
	}

	public File getKeyFile() {
		return keyFile;
	}

	public List<String> generateCommand() {
		final List<String> cmd = super.generateCommand();

		cmd.add("-server");
		cmd.add("-XX:+UnlockDiagnosticVMOptions");
		cmd.add("-XX:+UnsyncloadClass");
		cmd.add("-Dcom.sun.management.jmxremote");
		cmd.add("-Djava.endorsed.dirs=" + System.getProperty("java.endorsed.dirs") + File.pathSeparator + directory
				+ File.separator + "lib" + File.separator + "endorsed");
		cmd.add("-Djava.ext.dirs=" + System.getProperty("java.ext.dirs") + File.pathSeparator + directory
				+ File.separator + "lib" + File.separator + "ext");
		cmd.add("-Dkaraf.instances=" + directory + File.separator + "instances");
		cmd.add("-Dkaraf.home=" + directory);
		cmd.add("-Dkaraf.base=" + directory);
		cmd.add("-Dkaraf.etc=" + directory + File.separator + "etc");
		cmd.add("-Djava.io.tmpdir=" + directory + File.separator + "data" + File.separator + "tmp");
		cmd.add("-Djava.util.logging.config.file=" + directory + File.separator + "etc" + File.separator
				+ "java.util.logging.properties");
		cmd.add("-Dkaraf.startLocalConsole=true");
		cmd.add("-Dkaraf.startRemoteShell=true");
		cmd.add("-classpath");
		cmd.add(directory + File.separator + "lib" + File.separator + "karaf-jaas-boot.jar" + File.pathSeparator
				+ directory + File.separator + "lib" + File.separator + "karaf.jar");
		cmd.add("org.apache.karaf.main.Main");

		return cmd;
	}

	public abstract static class Builder<T extends Builder<T>> extends Configuration.Builder<T> {
		protected File keyFile;

		public Builder() {
			this.port = 8101;
			this.username = "karaf";
			this.password = "karaf";
		}

		public T keyFile(String keyFile) {
			this.keyFile = new File(keyFile);
			return self();
		}

		public KarafConfiguration build() {
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
