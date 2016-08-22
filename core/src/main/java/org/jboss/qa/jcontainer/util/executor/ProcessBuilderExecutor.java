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
package org.jboss.qa.jcontainer.util.executor;

import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class ProcessBuilderExecutor {

	private ProcessBuilderExecutor() {
	}

	public static int syncExecute(final ProcessBuilder processBuilder) throws InterruptedException, IOException {
		try {
			return ProcessExecutor.builder().processBuilder(processBuilder).redirectError(true).build().syncExecute();
		} catch (ExecutionException e) {
			log.error(e.getMessage(), e);
			return 600;
		}
	}

	public static int syncExecute(final ProcessBuilder processBuilder, final File outAndErrFile) throws InterruptedException, IOException {
		try {
			if (SystemUtils.IS_OS_HP_UX) {
				return executeOnHpUx(processBuilder, outAndErrFile).syncExecute();
			}
			return executeOnOthers(processBuilder, outAndErrFile).syncExecute();
		} catch (ExecutionException e) {
			log.error(e.getMessage(), e);
			return 600;
		}
	}

	public static Process asyncExecute(final ProcessBuilder processBuilder) throws InterruptedException, IOException {
		return asyncExecute(processBuilder, null);
	}

	public static Process asyncExecute(final ProcessBuilder processBuilder, final File outAndErrFile) throws InterruptedException, IOException {
		if (SystemUtils.IS_OS_HP_UX) {
			return executeOnHpUx(processBuilder, outAndErrFile).asyncExecute();
		}
		return executeOnOthers(processBuilder, outAndErrFile).asyncExecute();
	}

	private static ProcessExecutor executeOnHpUx(final ProcessBuilder processBuilder, final File outAndErrFile) throws FileNotFoundException {
		return ProcessExecutor.builder().processBuilder(processBuilder)
				.redirectError(outAndErrFile != null)
				.outputStream(outAndErrFile != null ? new FileOutputStream(outAndErrFile) : System.out)
				.errorStream(outAndErrFile != null ? null : System.err)
				.build();
	}

	private static ProcessExecutor executeOnOthers(final ProcessBuilder processBuilder, final File outAndErrFile) throws IOException {
		return ProcessExecutor.builder().processBuilder(processBuilder)
				.redirectError(outAndErrFile != null)
				.outputStream(outAndErrFile != null ? new FileOutputStream(outAndErrFile) : null)
				.build();
	}
}
