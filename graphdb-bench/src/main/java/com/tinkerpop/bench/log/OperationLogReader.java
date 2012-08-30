package com.tinkerpop.bench.log;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Vector;

import au.com.bytecode.opencsv.CSVReader;

import com.tinkerpop.bench.util.LogUtils;

/**
 * @author Alex Averbuch (alex.averbuch@gmail.com)
 */
public class OperationLogReader implements Iterable<OperationLogEntry> {

	private final char logDelim = LogUtils.LOG_DELIMITER.charAt(0);
	private File logFile = null;

	public OperationLogReader(File logFile) {
		super();
		this.logFile = logFile;
	}

	public Iterator<OperationLogEntry> iterator() {
		try {
			return new OperationLogEntryIterator(logFile);
		}
		catch (IOException e) {
			throw new RuntimeException(
					"Could not create OperationLogEntryIterator: Cannot open '" + logFile + "'", e.getCause());
		}
	}

	private class OperationLogEntryIterator implements
			Iterator<OperationLogEntry> {

		private OperationLogEntry nextLogEntry = null;
		private CSVReader reader = null;

		public OperationLogEntryIterator(File logFile)
				throws IOException {
			this.reader = new CSVReader(new FileReader(logFile), logDelim);

			// skip first line: .csv headers
			reader.readNext();
		}

		@Override
		public boolean hasNext() {
			if (nextLogEntry != null)
				return true;

			try {
				return ((nextLogEntry = parseLogEntry()) != null);
			} 
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public OperationLogEntry next() {
			if (nextLogEntry == null)
				throw new NoSuchElementException();

			OperationLogEntry logEntry = nextLogEntry;
			nextLogEntry = null;
			return logEntry;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		private OperationLogEntry parseLogEntry() throws IOException {
			String[] tokens = reader.readNext();
			if (tokens == null) {
				reader.close();
				reader = null;
				return null;
			}

			return extractLogEntry(tokens);
		}

		private OperationLogEntry extractLogEntry(String[] tokens) {
			int opId = -1;
			String name = null;
			String type = null;
			String[] args = null;
			long time = -1;
			String result = null;
			long memory = -1;

			for (int index = 0; index < tokens.length; index++) {
				String token = tokens[index];

				switch (index) {
				case 0:
					opId = Integer.parseInt(token);
					break;
				case 1:
					name = token;
					break;
				case 2:
					type = token;
					break;
				case 3:
					args = extractArgs(token);
					break;
				case 4:
					time = Long.parseLong(token);
					break;
				case 5:
					result = token;
					break;
				case 6:
					memory = Long.parseLong(token);
					break;
				}
			}

			return new OperationLogEntry(opId, name, type, args, time, result, memory);
		}

		private String[] extractArgs(String argsStr) {
			Vector<String> argsVector = new Vector<String>();
			for (String arg : argsStr.replaceAll("[\\[\\]]", "").split(", "))
				argsVector.add(arg);

			return argsVector.toArray(new String[argsVector.size()]);
		}

	}

}