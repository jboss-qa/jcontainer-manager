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

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class Configuration {

	protected final File directory;
	protected final String host;
	protected final Integer port;
	protected final String username;
	protected final String password;
	protected final String logFileName;
	protected final Set<String> params;
	protected final Map<String, String> envProps;

	protected Configuration(Builder<?> builder) {
		// Mandatory properties
		checkMandatoryProperty("host", host = builder.host);
		checkMandatoryProperty("port", port = builder.port);
		checkMandatoryProperty("username", username = builder.username);
		checkMandatoryProperty("password", password = builder.password);
		checkMandatoryProperty("logFileName", logFileName = builder.logFileName);

		// Optional properties
		directory = builder.directory; // Mandatory for container but not for standalone client.
		params = builder.params;
		envProps = builder.envProps;
	}

	protected void checkMandatoryProperty(String name, Object value) {
		if (value == null) {
			throw new IllegalArgumentException(String.format("Property '%s' is mandatory", name));
		}
	}

	public File getDirectory() {
		return directory;
	}

	public String getHost() {
		return host;
	}

	public abstract int getBusyPort();

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public String getLogFileName() {
		return logFileName;
	}

	public Set<String> getParams() {
		return params;
	}

	public Map<String, String> getEnvProps() {
		return envProps;
	}

	public abstract List<String> generateCommand();

	public abstract static class Builder<T extends Builder<T>> {

		protected File directory;
		protected String host;
		protected Integer port;
		protected String username;
		protected String password;
		protected String logFileName;
		protected Set<String> params = new HashSet<>();
		protected Map<String, String> envProps = new HashMap<>();

		public Builder() {
			host("localhost");
			port(8080);
			username("admin");
			password("admin");
		}

		protected abstract T self();

		public abstract Configuration build();

		public T directory(String directory) {
			this.directory = new File(directory);
			return self();
		}

		public T host(String host) {
			this.host = host;
			return self();
		}

		public T port(Integer port) {
			this.port = port;
			return self();
		}

		public T username(String username) {
			this.username = username;
			return self();
		}

		public T password(String password) {
			this.password = password;
			return self();
		}

		public T logFileName(String logFileName) {
			this.logFileName = logFileName;
			return self();
		}

		public T params(Collection<String> params) {
			this.params.addAll(params);
			return self();
		}

		public T params(String... params) {
			this.params.addAll(Arrays.asList(params));
			return self();
		}

		public T param(String param) {
			this.params.add(param);
			return self();
		}

		public T envProps(Map<String, String> envProps) {
			this.envProps.putAll(envProps);
			return self();
		}

		public T envProp(String key, String value) {
			this.envProps.put(key, value);
			return self();
		}
	}
}
