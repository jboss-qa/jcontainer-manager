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

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang3.StringUtils;

import org.jboss.qa.jcontainer.AbstractContainer;

import java.io.File;

public class JdgContainer<T extends JdgConfiguration, U extends JdgClient<T>, V extends JdgUser>
		extends AbstractContainer<T, U, V> {
	public JdgContainer(T configuration) {
		super(configuration);
	}

	@Override
	protected String getBasicCommand() {
		return null;
	}

	@Override
	protected File getLogDirInternal() {
		return new File(configuration.getDirectory(), "server" + File.separator + "log");
	}

	@Override
	public void addUser(V user) throws Exception {
		final File usersFile = new File(configuration.getConfigurationFolder(), "users.properties");
		final File rolesFile = new File(configuration.getConfigurationFolder(), "groups.properties");
		final PropertiesConfiguration propConfUsers = new PropertiesConfiguration(usersFile);
		propConfUsers.setProperty(user.getUsername(), user.getPassword());
		propConfUsers.save();

		final PropertiesConfiguration propConfRoles = new PropertiesConfiguration(rolesFile);
		propConfRoles.setProperty(user.getUsername(), StringUtils.join(user.getRoles(), ","));
		propConfRoles.save();
	}
}
