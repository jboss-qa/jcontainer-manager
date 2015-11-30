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

import java.lang.management.ManagementFactory;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class PidUtils {
	private PidUtils() {
	}

	private static Long cachedPid;

	public static Long getPID() {
		if (cachedPid == null) {
			initPid();
		}
		log.debug("My PID is {}", cachedPid);
		return cachedPid;
	}

	private static void initPid() {
		Long pid = null;

		// TODO(mswiech): try to use JDK9 api in future: http://download.java.net/jdk9/docs/api/java/lang/Process.html#getPid--

		try {
			pid = getPidByRuntimeMXBean();
		} catch (Exception e) {
			log.error("There is a problem with getting PID using RuntimeMXBean way.", e);
		}

		if (pid != null) {
			cachedPid = pid;
		} else {
			log.error("Could not obtain PID.");
		}
	}

	private static Long getPidByRuntimeMXBean() {
		final String processName;
		try {
			processName = ManagementFactory.getRuntimeMXBean().getName();
		} catch (Exception e) {
			log.error("There is some problem with getting RuntimeMXBean name.", e);
			return null;
		}

		if (processName != null && processName.indexOf("@") > 0) {
			final String pid = processName.split("@")[0];
			try {
				return Long.parseLong(pid);
			} catch (final NumberFormatException e) {
				log.error("Could not parse PID " + pid + " from " + processName);
			}
		} else {
			log.error("RuntimeMXBean name " + processName + " does not contain '@'.");
		}
		return null;
	}
}
