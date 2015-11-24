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

	public static final String EXTRA_JAVA_OPTS_ENV_NAME = "EXTRA_JAVA_OPTS";

	protected final File directory;
	protected final String host;
	protected final Integer port;
	protected final String username;
	protected final String password;
	protected final String xms;
	protected final String xmx;
	protected final String permSize;
	protected final String maxPermSize;
	protected final Set<String> params;
	protected final Map<String, String> envProps;

	public String getExtraJavaOptsEnvName() {
		return EXTRA_JAVA_OPTS_ENV_NAME;
	}

	protected Configuration(Builder<?> builder) {
		// Mandatory properties
		checkMandatoryProperty("host", host = builder.host);
		checkMandatoryProperty("port", port = builder.port);
		checkMandatoryProperty("username", username = builder.username);
		checkMandatoryProperty("password", password = builder.password);

		// Optional properties
		directory = builder.directory; // Mandatory for container but not for standalone client.
		xms = builder.xms;
		xmx = builder.xmx;
		permSize = builder.permSize;
		maxPermSize = builder.maxPermSize;
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

	public String getXms() {
		return xms;
	}

	public String getXmx() {
		return xmx;
	}

	public String getPermSize() {
		return permSize;
	}

	public String getMaxPermSize() {
		return maxPermSize;
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
		protected String xms;
		protected String xmx;
		protected String permSize;
		protected String maxPermSize;
		protected Set<String> params;
		protected Map<String, String> envProps;

		public Builder() {
			host = "localhost";
			port = 8080;
			username = "admin";
			password = "admin";
			params = new HashSet<>();
			envProps = new HashMap<>();
		}

		protected abstract T self();

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

		public T xms(String xms) {
			this.xms = xms;
			return self();
		}

		public T xmx(String xmx) {
			this.xmx = xmx;
			return self();
		}

		public T permSize(String permSize) {
			this.permSize = permSize;
			return self();
		}

		public T maxPermSize(String maxPermSize) {
			this.maxPermSize = maxPermSize;
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
