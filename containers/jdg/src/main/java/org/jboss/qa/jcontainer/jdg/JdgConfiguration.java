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
package org.jboss.qa.jcontainer.jdg;

import org.apache.commons.lang3.SystemUtils;

import org.jboss.qa.jcontainer.JavaConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

public class JdgConfiguration extends JavaConfiguration {

	public static final int DEFAULT_HTTP_PORT = 11222;
	private static final String START_COMMAND = "server";

	@Getter
	protected final int httpPort;

	public JdgConfiguration(Builder<?> builder) {
		super(builder);
		httpPort = builder.httpPort;
	}

	public static Builder<?> builder() {
		return new Builder2();
	}

	@Override
	public int getBusyPort() {
		return httpPort;
	}

	@Override
	public List<String> generateCommand() {
		final List<String> cmd = new ArrayList<>();
		if (SystemUtils.IS_OS_WINDOWS) {
			cmd.add("cmd");
			cmd.add("/c");
			cmd.add(new File(directory, "bin" + File.separator + START_COMMAND + ".bat").getAbsolutePath());
		} else {
			cmd.add("bash");
			cmd.add(new File(directory, "bin" + File.separator + START_COMMAND + ".sh").getAbsolutePath());
		}
		return cmd;
	}

	public File getConfigurationFolder() {
		return new File(directory, "server" + File.separator + "conf");
	}

	public abstract static class Builder<T extends Builder<T>> extends JavaConfiguration.Builder<T> {

		protected int httpPort;

		public Builder() {
			super();
			httpPort(DEFAULT_HTTP_PORT);
			password("");
			logFileName("server.log");
		}

		public T httpPort(int httpPort) {
			this.httpPort = httpPort;
			return self();
		}

		public JdgConfiguration build() {
			return new JdgConfiguration(this);
		}
	}

	private static class Builder2 extends Builder<Builder2> {
		@Override
		protected Builder2 self() {
			return this;
		}
	}
}
