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
package org.jboss.qa.jcontainer.configuration;

import org.jboss.qa.jcontainer.JavaConfiguration;

import java.util.Collections;
import java.util.List;

public class DummyConfiguration extends JavaConfiguration {

	public static final String JAVA_OPTS_ENV_NAME = "DUMMY_OPTS";

	protected DummyConfiguration(Builder<?> builder) {
		super(builder);
	}

	@Override
	public int getBusyPort() {
		return 0;
	}

	@Override
	public List<String> generateCommand() {
		return Collections.EMPTY_LIST;
	}

	public static Builder<?> builder() {
		return new Builder2();
	}

	public abstract static class Builder<T extends Builder<T>> extends JavaConfiguration.Builder<T> {
		@Override
		protected String javaOptsEnvName() {
			return JAVA_OPTS_ENV_NAME;
		}

		@Override
		public JavaConfiguration build() {
			return new DummyConfiguration(this);
		}
	}

	private static class Builder2 extends Builder<Builder2> {
		@Override
		protected Builder2 self() {
			return this;
		}
	}
}
