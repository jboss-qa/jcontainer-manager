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

import org.apache.commons.lang3.SystemUtils;

import org.jboss.qa.jcontainer.AbstractContainer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class ProcessUtils {
	private ProcessUtils() {
	}

	public static void killJavaByName(String processName) {
		final List<String> pids = getJavaPidsByName(processName);
		if (pids != null) {
			for (String pid : pids) {
				kill(pid);
			}
		}
	}

	public static void killJavaByContainerId(long id) {
		if (!SystemUtils.IS_OS_WINDOWS) { // Needs sudo for unix-like OS
			log.error("Killing by container id is not unsupported for {}", SystemUtils.OS_NAME);
			return;
		}
		final String pid = getJavaPidByContainerId(id);
		if (pid != null) {
			kill(pid);
		} else {
			log.error("Process representing container with id {} was not found", id);
		}
	}

	public static String getJavaPidByContainerId(long id) {
		String result = null;
		try {
			final Long currentPid = PidUtils.getPID();
			for (Map.Entry<String, String> entry : getJavaProcesses().entrySet()) {
				final String pid = entry.getKey();
				if (currentPid != null && String.valueOf(currentPid).equals(pid)) {
					//it freezes jvm on Windows
					log.debug("We are not going to execute jinfo for our pid " + currentPid);
					continue;
				}
				final String command = "jinfo -sysprops " + pid;
				log.info("Executing: " + command);
				final Process p = Runtime.getRuntime().exec(command);
				final BufferedReader is = new BufferedReader(new InputStreamReader(p.getInputStream()));
				String line;
				while ((line = is.readLine()) != null) {
					if (result == null && line.contains(AbstractContainer.JCONTAINER_ID) && line.contains(String.valueOf(id))) {
						result = pid;
						//Don't break it here. We need to consume whole STDOUT of executed command.
					}
				}
				final int exitValue = p.waitFor();
				if (exitValue != 0) {
					log.error("Process 'jinfo' ended with exit value {}", exitValue);
				}
				if (result != null) {
					break;
				}
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return result;
	}

	public static List<String> getJavaPidsByName(String processName) {
		final List<String> pids = new ArrayList<>();
		for (Map.Entry<String, String> entry : getJavaProcesses().entrySet()) {
			if (entry.getValue().contains(processName)) {
				pids.add(entry.getKey());
			}
		}
		return pids;
	}

	public static void kill(String pid) {
		if (pid != null && !pid.isEmpty()) {
			String cmd = null;
			if (SystemUtils.IS_OS_WINDOWS) {
				cmd = "taskkill /F /T /PID " + pid;
			} else { // UNIX-like
				cmd = "kill -9 " + pid;
			}
			try {
				final Process p = Runtime.getRuntime().exec(cmd);
				if (p.waitFor() == 0) {
					log.info("Process {} was killed", pid);
				} else {
					log.error("Process {} was not killed", pid);
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}

			try {
				//Give some time to kill. Sometimes I got an error message like "port 8080 is alreagy opened" on Windows machine when didn't wait here.
				Thread.sleep(15 * 1000L);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	public static Map<String, String> getJavaProcesses() {
		final Map<String, String> pids = new HashMap<>();
		try {
			final Process p = Runtime.getRuntime().exec("jps -l");
			final BufferedReader inStream = new BufferedReader(new InputStreamReader(p.getInputStream()));
			final String pattern = "(\\d+)\\s+(.*)";
			final Pattern r = Pattern.compile(pattern);
			String line;
			while ((line = inStream.readLine()) != null) {
				final Matcher m = r.matcher(line);
				if (m.find()) {
					pids.put(m.group(1), m.group(2));
				}
			}
			final int exitValue = p.waitFor();
			if (exitValue != 0) {
				log.error("Process 'jps' ended with exit value {}", exitValue);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return pids;
	}
}
