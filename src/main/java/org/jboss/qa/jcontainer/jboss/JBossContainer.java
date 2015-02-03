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
package org.jboss.qa.jcontainer.jboss;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang3.StringUtils;

import org.jboss.qa.jcontainer.Container;

import java.io.File;

public class JBossContainer<T extends JBossConfiguration, U extends JBossClient<T>, V extends JBossUser>
		extends Container<T, U, V> {

	public JBossContainer(T configuration) {
		super(configuration);
	}

	@Override
	public void addUser(V user) throws Exception {
		if (user.getRealm() == null) {
			user.setRealm(JBossUser.Realm.MANAGEMENT_REALM);
		}
		checkMandatoryProperty("username", user.getUsername());
		checkMandatoryProperty("password", user.getUsername());

		File usersFile;
		File rolesFile;
		if (user.getRealm().equals(JBossUser.Realm.APPLICATION_REALM)) {
			usersFile = new File(configuration.getConfigurationFolder(), "application-users.properties");
			rolesFile = new File(configuration.getConfigurationFolder(), "application-roles.properties");
		} else {
			usersFile = new File(configuration.getConfigurationFolder(), "mgmt-users.properties");
			rolesFile = new File(configuration.getConfigurationFolder(), "mgmt-groups.properties");
		}
		final PropertiesConfiguration propConfUsers = new PropertiesConfiguration(usersFile);
		propConfUsers.setProperty(user.getUsername(), DigestUtils.md5Hex(String.format("%s:%s:%s",
				user.getUsername(), user.getRealm().getValue(), user.getPassword())));
		propConfUsers.save();

		final PropertiesConfiguration propConfRoles = new PropertiesConfiguration(rolesFile);
		propConfRoles.setProperty(user.getUsername(), StringUtils.join(user.getRoles(), ","));
		propConfRoles.save();
	}
}
