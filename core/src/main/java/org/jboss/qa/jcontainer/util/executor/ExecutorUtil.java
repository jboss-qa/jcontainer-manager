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

import java.io.Closeable;
import java.io.PrintWriter;
import java.io.Writer;

public final class ExecutorUtil {

	private ExecutorUtil() {
	}

	public static PrintWriter toPrintWriter(final Writer w) {
		if (w != null) {
			if (w instanceof PrintWriter) {
				return (PrintWriter) w;
			}
			return new PrintWriter(w);
		}
		return null;
	}

	public static void closeCloseable(final Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (final Exception e) {
				//ignore
			}
		}
	}

	public static void closeProcessStreams(final Process process) {
		if (process != null) {
			closeCloseable(process.getInputStream());
			closeCloseable(process.getErrorStream());
			closeCloseable(process.getOutputStream());
		}
	}
}
