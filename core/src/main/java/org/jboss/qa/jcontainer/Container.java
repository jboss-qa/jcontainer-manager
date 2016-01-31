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

import java.io.Closeable;

public interface Container<T extends Configuration, U extends Client<T>, V extends User> extends Closeable {

	/**
	 * Starts container.
	 */
	void start() throws Exception;

	/**
	 * Stops container.
	 */
	void stop() throws Exception;

	/**
	 * Checks if container is running.
	 */
	boolean isRunning() throws Exception;

	/**
	 * Returns configuration of container.
	 */
	T getConfiguration();

	/**
	 * Adds new user.
	 */
	void addUser(V user) throws Exception;

	/**
	 * Checks if client is supported.
	 */
	boolean isClientSupported();

	/**
	 * Returns client of container.
	 */
	U getClient();
}
