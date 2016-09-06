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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

import org.jboss.qa.jcontainer.AbstractContainer;

import java.io.BufferedReader;
import java.io.IOException;
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
		final String pid = getJavaPidByContainerId(id);
		log.debug("Container {} has pid {}", id, pid);
		if (pid != null) {
			kill(pid);
		} else {
			log.error("Process representing container with id {} was not found", id);
		}
	}

	public static String getJavaPidByContainerId(long id) {
		if (SystemUtils.IS_OS_WINDOWS) {
			return getJavaPidByContainerIdWindows(id);
		} else {
			return getJavaPidByContainerIdUnix(id);
		}
	}

	private static String getJavaPidByContainerIdUnix(long id) {
		final String command = String.format("ps -ef | grep \\\\-Djcontainer.id=%d | grep -v grep | grep -v \"/bin/bash\" | awk '{print $2}'", id);
		try {
			final Process p = Runtime.getRuntime().exec(new String[] {"bash", "-c", command});
			try (final BufferedReader is = new BufferedReader(new InputStreamReader(p.getInputStream()));
				 final BufferedReader es = new BufferedReader(new InputStreamReader(p.getErrorStream()))
			) {
				String result = null;
				String line;
				while ((line = is.readLine()) != null) {
					result = line;
				}
				final int exitValue = p.waitFor();
				if (exitValue != 0) {
					log.error("Process ended with exit value {}", exitValue);
					while ((line = es.readLine()) != null) {
						log.error(line);
					}
				}
				return result;
			}
		} catch (IOException | InterruptedException e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}

	private static String getJavaPidByContainerIdWindows(long id) {
		String result = null;
		try {
			final Process p = Runtime.getRuntime().exec("wmic process where \"not name='wmic.exe' and CommandLine like '%-D"
					+ AbstractContainer.JCONTAINER_ID + "=" + id + "%'\" get ProcessID");

			final BufferedReader is = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line;
			while ((line = is.readLine()) != null) {
				if (!line.contains("ProcessId")) {
					if (result == null || result.isEmpty()) {
						result = line;
					}
				}
			}
			final int exitValue = p.waitFor();
			if (exitValue != 0) {
				log.error("Process 'wmic' ended with exit value {}", exitValue);
				log.debug("Process 'wmic' error stream: {}", IOUtils.toString(p.getErrorStream()));
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return StringUtils.trimToNull(result);
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
					log.debug("Error stream: {}", IOUtils.toString(p.getErrorStream()));
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
		} else {
			log.debug("Unable kill process because PID is Null or Empty!");
		}
	}

	public static Map<String, String> getJavaProcesses() {
		final Map<String, String> pids = new HashMap<>();
		try {
			final Process p = Runtime.getRuntime().exec("jps -l");
			final List<String> lines = IOUtils.readLines(p.getInputStream());
			final String pattern = "(\\d+)\\s+(.*)";
			final Pattern r = Pattern.compile(pattern);
			final int exitValue = p.waitFor();
			if (exitValue != 0) {
				log.error("Process 'jps' ended with exit value {}", exitValue);
				log.debug("Process 'jps' error stream: {}", IOUtils.toString(p.getErrorStream()));
			}
			for (String line : lines) {
				final Matcher m = r.matcher(line);
				if (m.find()) {
					pids.put(m.group(1), m.group(2));
				}
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return pids;
	}
}
