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
package org.jboss.qa.jcontainer.tomcat;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

import org.jboss.qa.jcontainer.Configuration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TomcatConfiguration extends Configuration {

	protected TomcatConfiguration(Builder<?> builder) {
		super(builder);
	}

	@Override
	public List<String> generateCommand() {
		return generateCommand("startup");
	}

	public List<String> generateStopCommand() {
		return generateCommand("shutdown");
	}

	public List<String> generateCommand(String scriptName) {
		final List<String> cmd = new ArrayList<>();
		if (SystemUtils.IS_OS_WINDOWS) {
			cmd.add("cmd");
			cmd.add("/c");
			cmd.add(new File(directory, "bin" + File.separator + scriptName + ".bat").getAbsolutePath());
		} else {
			cmd.add("bash");
			cmd.add(new File(directory, "bin" + File.separator + scriptName + ".sh").getAbsolutePath());
		}
		return cmd;
	}

	public static Builder<?> builder() {
		return new Builder2();
	}

	public abstract static class Builder<T extends Builder<T>> extends Configuration.Builder<T> {

		public Builder() {
			password = "";
		}

		public TomcatConfiguration build() {
			// Set CATALINA_OPTS
			final StringBuffer catalinaOpts = new StringBuffer();
			if (!StringUtils.isEmpty(xms)) {
				catalinaOpts.append(" -Xms" + xms);
			}
			if (!StringUtils.isEmpty(xmx)) {
				catalinaOpts.append(" -Xmx" + xmx);
			}
			if (!StringUtils.isEmpty(permSize)) {
				catalinaOpts.append(" -XX:PermSize=" + permSize);
			}
			if (!StringUtils.isEmpty(maxPermSize)) {
				catalinaOpts.append(" -XX:MaxPermSize=" + maxPermSize);
			}
			envProps.put("CATALINA_OPTS", catalinaOpts.toString());
			return new TomcatConfiguration(this);
		}
	}

	private static class Builder2 extends Builder<Builder2> {
		@Override
		protected Builder2 self() {
			return this;
		}
	}
}
