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

import org.apache.karaf.jaas.modules.BackingEngine;
import org.apache.karaf.jaas.modules.properties.PropertiesBackingEngineFactory;

import org.jboss.qa.jcontainer.Container;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class KarafContainer<T extends KarafConfiguration, U extends KarafClient<T>, V extends KarafUser>
		extends Container<T, U, V> {

	private static final Logger logger = LoggerFactory.getLogger(KarafContainer.class);

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
}
