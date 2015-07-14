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
package org.jboss.as.cli.impl;

import org.jboss.as.cli.CliInitializationException;
import org.jboss.dmr.ModelNode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExtendedCommandContextImpl extends CommandContextImpl {

	private ModelNode lastResult;

	public ExtendedCommandContextImpl() throws CliInitializationException {
		super();
	}

	// https://issues.jboss.org/browse/WFLY-4440
	@Override
	public void printLine(String message) {
		super.printLine(message);
		try {
			lastResult = ModelNode.fromString(message);
		} catch (Exception e) {
			ExtendedCommandContextImpl.log.error("Last result was not reconstructed");
		}
	}

	public ModelNode getLastResult() {
		return lastResult;
	}
}
