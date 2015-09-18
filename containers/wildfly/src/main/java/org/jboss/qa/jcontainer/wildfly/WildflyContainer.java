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
package org.jboss.qa.jcontainer.wildfly;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang3.StringUtils;

import org.jboss.qa.jcontainer.Container;
import org.jboss.qa.jcontainer.wildfly.utils.CoreUtils;

import java.io.File;

public class WildflyContainer<T extends WildflyConfiguration, U extends WildflyClient<T>, V extends WildflyUser>
		extends Container<T, U, V> {

	public WildflyContainer(T configuration) {
		super(configuration);
	}

	@Override
	public void addUser(V user) throws Exception {
		if (user.getRealm() == null) {
			user.setRealm(WildflyUser.Realm.MANAGEMENT_REALM);
		}
		checkMandatoryProperty("username", user.getUsername());
		checkMandatoryProperty("password", user.getUsername());

		File usersFile;
		File rolesFile;
		if (user.getRealm().equals(WildflyUser.Realm.APPLICATION_REALM)) {
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

	@Override
	protected String getBasicCommand() {
		return ":whoami";
	}

	@Override
	public String getLogDirInternal() throws Exception {
		return CoreUtils.getSystemProperty(client, "jboss.server.log.dir");
	}
}
