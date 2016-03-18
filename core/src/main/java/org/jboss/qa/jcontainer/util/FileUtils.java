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
package org.jboss.qa.jcontainer.util;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class FileUtils {
	private FileUtils() {
	}

	public static boolean isEmpty(File file) throws IOException {
		// Do not use (File.length() == 0) condition. There is an UTF-8 issue connected with BOMs.
		return org.apache.commons.io.FileUtils.readFileToString(file).trim().isEmpty();
	}

	public static void setScriptsExecutable(File folder) {
		setScriptsExecutable(folder, false);
	}

	public static void setScriptsExecutable(File folder, boolean recursive) {
		final String[] extensions = {"sh"};
		final Collection<File> files = org.apache.commons.io.FileUtils.listFiles(folder, extensions, recursive);
		for (final File file : files) {
			log.debug("Setting executable rights for: " + file.getName());
			file.setExecutable(true);
		}
	}
}
