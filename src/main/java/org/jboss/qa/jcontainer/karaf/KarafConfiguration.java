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
import org.jboss.qa.jcontainer.util.OSDetector;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class KarafConfiguration extends Configuration {

	protected final File keyFile;
	protected final File script;

	protected KarafConfiguration(Builder<?> builder) {
		super(builder);
		script = builder.script;
		//Optional
		keyFile = builder.keyFile;
	}

	public static Builder<?> builder() {
		return new Builder2();
	}

	public File getKeyFile() {
		return keyFile;
	}

	@Override
	public List<String> generateCommand() {
		if (!script.exists()) {
			throw new IllegalStateException(String.format("Script '%s' does not exist", script.getAbsolutePath()));
		}
		final List<String> cmd = new ArrayList<>();
		if (OSDetector.isUnix()) {
			cmd.add("/bin/bash");
			cmd.add(script.getAbsolutePath());
		} else if (OSDetector.isWindows()) {
			// TODO(mbasovni): Not yet tested!
			cmd.add("cmd");
			cmd.add("/c");
			cmd.add(script.getAbsolutePath());
		} else {
			throw new UnsupportedOperationException(String.format("Operation system '%s' is not yer supported",
					OSDetector.OS));
		}
		return cmd;
	}

	public abstract static class Builder<T extends Builder<T>> extends Configuration.Builder<T> {
		protected File keyFile;
		protected File script;

		public Builder() {
			port = 8101;
			username = "karaf";
			password = "karaf";
		}

		public T keyFile(String keyFile) {
			this.keyFile = new File(keyFile);
			return self();
		}

		public KarafConfiguration build() {
			script = new File(directory, "/bin/" + (OSDetector.isWindows() ? "karaf.bat" : "karaf"));
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
