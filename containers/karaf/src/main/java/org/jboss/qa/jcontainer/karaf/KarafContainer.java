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

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang3.SystemUtils;
import org.apache.karaf.jaas.modules.BackingEngine;
import org.apache.karaf.jaas.modules.properties.PropertiesBackingEngineFactory;

import org.jboss.qa.jcontainer.Container;
import org.jboss.qa.jcontainer.karaf.utils.CoreUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KarafContainer<T extends KarafConfiguration, U extends KarafClient<T>, V extends KarafUser>
		extends Container<T, U, V> {

	public KarafContainer(T configuration) {
		super(configuration);
	}

	@Override
	public void addUser(V user) throws Exception {
		final Map<String, String> options = new HashMap<>();
		final File usersFile = new File(configuration.getDirectory(), "etc" + File.separator + "users.properties");
		options.put("users", usersFile.getAbsolutePath());
		final BackingEngine engine = new PropertiesBackingEngineFactory().build(options);
		engine.addUser(user.getUsername(), user.getPassword());
		for (String role : user.getRoles()) {
			engine.addRole(user.getUsername(), role);
		}
		for (String group : user.getGroups()) {
			engine.addGroup(user.getUsername(), group);
		}
	}

	protected void setEtcProperty(String key, Object value, String config) throws Exception {
		final File propsFile = getConfigFile(config);
		final PropertiesConfiguration propConf = new PropertiesConfiguration(propsFile);
		propConf.setProperty(key, value);
		propConf.save();
	}

	@Override
	public synchronized void start() throws Exception {
		final File setEnvFile = new File(configuration.getDirectory(), "bin" + File.separator
				+ (SystemUtils.IS_OS_WINDOWS ? "setenv.bat" : "setenv"));
		if (setEnvFile != null && setEnvFile.exists()) {
			final File setEnvBacFile = new File(setEnvFile.getAbsolutePath() + ".backup");
			setEnvFile.renameTo(setEnvBacFile);
			log.info("File '{}' was renamed to '{}' to ensure the propagation of own environment properties",
					setEnvFile.getName(), setEnvBacFile.getName());
		}
		if (getConfiguration().getSshPort() != KarafConfiguration.DEFAULT_SSH_PORT) {
			setEtcProperty("sshPort", getConfiguration().getSshPort(), "org.apache.karaf.shell");
		}
		super.start();
		addShutdownHook(new Thread(new Runnable() {
			public void run() {
				try {
					final ProcessBuilder processBuilder = new ProcessBuilder(configuration.generateStopCommand());
					processBuilder.inheritIO();
					final Process p = processBuilder.start();
					p.waitFor();
				} catch (Exception e) {
					throw new IllegalStateException("Karaf container was not stopped", e);
				}
			}
		}));
	}

	public File getConfigFile(String name) {
		return new File(configuration.getDirectory(), String.format("etc%s%s.cfg", File.separator, name));
	}

	@Override
	protected String getBasicCommand() {
		return "version";
	}

	@Override
	protected String getLogDirInternal() throws Exception {
		return CoreUtils.getSystemProperty(client, "karaf.data") + File.separator + "log" + File.separator;
	}
}
