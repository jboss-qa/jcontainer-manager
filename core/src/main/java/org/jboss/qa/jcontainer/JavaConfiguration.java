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

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class JavaConfiguration extends Configuration {

	public static final String JAVA_OPTS_ENV_NAME = "JAVA_OPTS";

	protected final String xms;
	protected final String xmx;
	protected final String permSize;
	protected final String maxPermSize;
	protected final List<String> javaOpts;
	protected final String javaOptsEnvName;

	protected JavaConfiguration(Builder<?> builder) {
		super(builder);
		checkMandatoryProperty("javaOptsEnvName", javaOptsEnvName = builder.javaOptsEnvName);
		xms = builder.xms;
		xmx = builder.xmx;
		permSize = builder.permSize;
		maxPermSize = builder.maxPermSize;
		javaOpts = builder.javaOpts;

		if (javaOpts.size() != 0) {
			envProps.put(javaOptsEnvName, StringUtils.join(javaOpts, " "));
		}
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

	public String getJavaOptsEnvName() {
		return javaOptsEnvName;
	}

	public abstract static class Builder<T extends Builder<T>> extends Configuration.Builder<T> {

		protected String xms;
		protected String xmx;
		protected String permSize;
		protected String maxPermSize;
		protected List<String> javaOpts = new ArrayList<>();
		protected String javaOptsEnvName;

		public Builder() {
			super();
			javaOptsEnvName = javaOptsEnvName();
			final List<String> envJavaOpts = System.getenv(javaOptsEnvName()) == null ? Collections.EMPTY_LIST : Arrays.asList(System.getenv(javaOptsEnvName()).split("(?=\\s-)"));
			for (String opt : envJavaOpts) {
				javaOpts.add(opt.trim());
			}
		}

		protected abstract T self();

		public abstract JavaConfiguration build();

		protected String javaOptsEnvName() {
			return JAVA_OPTS_ENV_NAME;
		}

		public T xms(String xms) {
			this.xms = xms;
			if (!StringUtils.isEmpty(xms)) {
				replaceJavaOptIfExists("-Xms", xms);
			} else {
				removeJavaOptIfExists("-Xms");
			}
			return self();
		}

		public T xmx(String xmx) {
			this.xmx = xmx;
			if (!StringUtils.isEmpty(xmx)) {
				replaceJavaOptIfExists("-Xmx", xmx);
			} else {
				removeJavaOptIfExists("-Xmx");
			}
			return self();
		}

		public T permSize(String permSize) {
			this.permSize = permSize;
			if (!StringUtils.isEmpty(permSize)) {
				replaceJavaOptIfExists("-XX:PermSize=", permSize);
			} else {
				removeJavaOptIfExists("-XX:PermSize=");
			}
			return self();
		}

		public T maxPermSize(String maxPermSize) {
			this.maxPermSize = maxPermSize;
			if (!StringUtils.isEmpty(maxPermSize)) {
				replaceJavaOptIfExists("-XX:MaxPermSize=", maxPermSize);
			} else {
				removeJavaOptIfExists("-XX:MaxPermSize=");
			}
			return self();
		}

		public T javaOpt(String opt) {
			this.javaOpts.add(opt);
			return self();
		}

		protected void replaceJavaOptIfExists(String prefix, String value) {
			removeJavaOptIfExists(prefix);
			javaOpts.add(prefix + value);
		}

		protected void addJavaOptIfNotExists(String prefix, String value) {
			for (int i = 0; i < javaOpts.size(); i++) {
				if (javaOpts.get(i).startsWith(prefix)) {
					return;
				}
			}
			javaOpts.add(prefix + value);
		}

		protected void removeJavaOptIfExists(String prefix) {
			for (int i = 0; i < javaOpts.size(); i++) {
				if (javaOpts.get(i).startsWith(prefix)) {
					javaOpts.remove(i);
				}
			}
		}
	}
}
