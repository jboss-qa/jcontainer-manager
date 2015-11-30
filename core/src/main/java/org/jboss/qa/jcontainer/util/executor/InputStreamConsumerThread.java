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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InputStreamConsumerThread extends Thread {

	private static final String[] STREAM_CLOSED_ERR_MESSAGES = {
			"stream closed",
			"bad file number",
			"interrupted system call"
	};

	private InputStream in;
	private PrintWriter outWriter;

	public InputStreamConsumerThread(final InputStream in) {
		this(in, (Writer) null);
	}

	public InputStreamConsumerThread(final InputStream in, final OutputStream out) {
		this(in, toWriter(out));
	}

	public InputStreamConsumerThread(final InputStream in, final Writer out) {
		setDaemon(true);
		this.in = in;
		if (out != null) {
			if (out instanceof PrintWriter) {
				this.outWriter = (PrintWriter) out;
			} else {
				this.outWriter = new PrintWriter(out);
			}
		}
	}

	private static Writer toWriter(final OutputStream out) {
		return out != null ? new OutputStreamWriter(out) : null;
	}

	@Override
	public void run() {
		try {
			consume(outWriter);
		} catch (final Exception e) {
			if (!isIoExceptionCausedByInterruptedProcess(e)) {
				log.error("Problem with consuming InputStream.", e);
			}
			//in case of writing to output failed, consume rest of the input (input consumption is priority)
			consumeRestOfInput();
		}
	}

	private void consume(final PrintWriter w) throws IOException {
		final BufferedReader r = new BufferedReader(new InputStreamReader(in));
		String line;
		while ((line = r.readLine()) != null) {
			if (w != null) {
				w.println(line);
			}
		}
	}

	private void consumeRestOfInput() {
		final byte[] buf = new byte[4096];
		try {
			while (in.read(buf) >= 0) {
				//do nothing, just read input
			}
		} catch (final Exception e) {
			if (!isIoExceptionCausedByInterruptedProcess(e)) {
				log.error("Problem with consuming rest of InputStream.", e);
			}
		}
	}

	private static boolean isIoExceptionCausedByInterruptedProcess(final Exception e) {
		final String msg = (e != null && e.getMessage() != null ? e.getMessage().toLowerCase() : null);
		if (msg != null && e instanceof IOException) {
			for (final String scMsg : STREAM_CLOSED_ERR_MESSAGES) {
				if (msg.contains(scMsg)) {
					return true;
				}
			}
		}
		return false;
	}
}
