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
package org.jboss.qa.jcontainer.karaf.utils;

import org.jboss.qa.jcontainer.karaf.KarafClient;

public final class CoreUtils {

	public static String getSystemProperty(KarafClient client, String propertyName) throws Exception {
		return getSystemProperty(client, propertyName, "system:property");
	}

	public static String getSystemProperty(KarafClient client, String propertyName, String command) throws Exception {
		client.execute(String.format("%s %s", command, propertyName));
		final String property = client.getCommandResult().replaceAll("\u001B\\[[;\\d]*m", "");
		return property.length() > 0 ? property.substring(0, property.length() - 1) : property;
	}

	private CoreUtils() {
	}
}
