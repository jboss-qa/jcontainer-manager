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

import org.apache.commons.configuration.AbstractFileConfiguration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.karaf.jaas.modules.BackingEngine;
import org.apache.karaf.jaas.modules.properties.PropertiesBackingEngineFactory;

import org.jboss.qa.jcontainer.AbstractContainer;
import org.jboss.qa.jcontainer.karaf.utils.CoreUtils;
import org.jboss.qa.jcontainer.util.executor.ProcessBuilderExecutor;

import java.io.File;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KarafContainer<T extends KarafConfiguration, U extends KarafClient<T>, V extends KarafUser>
		extends AbstractContainer<T, U, V> {

	private static final String CLIENT_KEY_FILE = "client.key";
	private static final String SSH_GROUP = "_g_:admingroup";
	private static final String KEYS_PROPERTIES = "keys.properties";

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
		for (String group : user.getGroups()) {
			engine.addGroup(user.getUsername(), group);
			for (String role : user.getRoles()) {
				engine.addGroupRole(group, role);
			}
		}
		for (String role : user.getRoles()) {
			engine.addRole(user.getUsername(), role);
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
		if (getConfiguration().getSshPort() != KarafConfiguration.DEFAULT_SSH_PORT) {
			setEtcProperty("sshPort", getConfiguration().getSshPort(), "org.apache.karaf.shell");
		}
		configuration.getEnvProps().put("KARAF_REDIRECT", getStdoutLogFile().getAbsolutePath());
		super.start();
		addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				log.debug("Start shutdown sequence.");
				try {
					final ProcessBuilder processBuilder = new ProcessBuilder(configuration.generateStopCommand());
					ProcessBuilderExecutor.syncExecute(processBuilder);
				} catch (Exception e) {
					throw new IllegalStateException("Karaf container was not stopped", e);
				}
			}
		}));
	}

	/**
	 * If configuration.getKeyFile() is set with a path, this file will be used as client private key,
	 * otherwise a new file client.key in etc folder will be generated and the public key is configured accordingly.
	 * @throws Exception
	 */
	public void setupClientKeys() throws Exception {
		File keyFile = configuration.getKeyFile();
		if (keyFile == null) {
			keyFile = Paths.get(configuration.getDirectory().getAbsolutePath(),
					"etc", CLIENT_KEY_FILE).toAbsolutePath().toFile();
			configuration.setKeyFile(keyFile);
		}
		if (!keyFile.exists()) {
			final File keyProps = this.getConfigFile(KEYS_PROPERTIES);
			final KeyPairGenerator keygen = KeyPairGenerator.getInstance("RSA");
			keygen.initialize(2048);
			final KeyPair kp = keygen.generateKeyPair();
			final RSAPublicKey publicKey = (RSAPublicKey) kp.getPublic();
			final AbstractFileConfiguration conf = new PropertiesConfiguration(keyProps);
			final String publicKeyPropValue = Base64.getEncoder().encodeToString(publicKey.getEncoded()) + "," + SSH_GROUP;
			if (conf.containsKey(configuration.getUsername())) {
				//backup it
				FileUtils.copyFile(keyProps, new File(keyProps.getAbsoluteFile() + "." + String.valueOf(System.currentTimeMillis())));
				conf.setProperty(configuration.getUsername(), publicKeyPropValue);
			} else {
				conf.addProperty(configuration.getUsername(), publicKeyPropValue);
			}
			//update key.properties
			conf.save();
			//write private key
			final StringBuilder result = new StringBuilder();
			result.append("-----BEGIN OPENSSH PRIVATE KEY-----\n");
			result.append(Base64.getEncoder().encodeToString(kp.getPrivate().getEncoded()));
			result.append("\n-----END OPENSSH PRIVATE KEY-----\n");
			FileUtils.write(keyFile, result.toString());
		}
	}

	public File getConfigFile(String name) {
		return new File(configuration.getDirectory(), String.format("etc%s%s.cfg", File.separator, name));
	}

	@Override
	protected String getBasicCommand() {
		return "version";
	}

	@Override
	protected File getLogDirInternal() {
		return new File(CoreUtils.getSystemProperty(client, "karaf.data"), "log");
	}
}
