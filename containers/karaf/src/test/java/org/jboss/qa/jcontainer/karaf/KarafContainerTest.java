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

import org.jboss.qa.jcontainer.test.ContainerTest;

public abstract class KarafContainerTest extends ContainerTest {

	public static final String KARAF_HOME = getProperty("karaf.home");

	protected static final String GOOD_CMD = "version";
	protected static final String BAD_FORMAT_CMD = "xxx";
	protected static final String BAD_RESULT_CMD = "install xxx";

	protected static final String CONFIG = "my.config";
	protected static final String PROP_NAME = "my-prop";
	protected static final String PROP_VAL = "my-value";
}

