package com.tinkerpop.bench.log;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.WeakHashMap;

import au.com.bytecode.opencsv.CSVReader;

import com.tinkerpop.bench.util.LogUtils;

/**
 * @author Alex Averbuch (alex.averbuch@gmail.com)
 * @author Peter Macko (pmacko@eecs.harvard.edu)
 */
public class OperationLogReader implements Iterable<OperationLogEntry> {

	public static boolean CACHE_BY_DEFAULT = true;
	
	private final char logDelim = LogUtils.LOG_DELIMITER.charAt(0);
	private File logFile = null;
	private Iterable<OperationLogEntry> cachedIterable = null;
	private String operationNameFilter;
	
	private static WeakHashMap<String, Iterable<OperationLogEntry>> cache
		= new WeakHashMap<String, Iterable<OperationLogEntry>>();

	public OperationLogReader(File logFile, String operationNameFilter, boolean cached) {
		this.logFile = logFile;
		this.operationNameFilter = operationNameFilter;
		
		if (cached) {
			String cacheKey = logFile.getAbsolutePath();
			if (operationNameFilter != null) {
				cacheKey += "----" + operationNameFilter;
			}
			synchronized (cache) {
				this.cachedIterable = cache.get(cacheKey);
			}
			if (this.cachedIterable == null) {
				try {
					synchronized (cache) {
						Iterator<OperationLogEntry> i = new OperationLogEntryIterator(this.logFile);
						ArrayList<OperationLogEntry> l = new ArrayList<OperationLogEntry>();
						while (i.hasNext()) l.add(i.next());
						this.cachedIterable = l;
						cache.put(cacheKey, l);
					}
				} catch (IOException e) {
					throw new RuntimeException(
							"Could not create OperationLogEntryIterator: Cannot open '" + logFile + "'", e.getCause());
				}
			}
		}
		else {
			this.cachedIterable = null;
		}
	}

	public OperationLogReader(File logFile) {
		this(logFile, null, CACHE_BY_DEFAULT);
	}

	public OperationLogReader(File logFile, String operationNameFilter) {
		this(logFile, operationNameFilter, CACHE_BY_DEFAULT);
	}
	

	public Iterator<OperationLogEntry> iterator() {
		if (cachedIterable == null) {
			try {
				return new OperationLogEntryIterator(this.logFile);
			} catch (IOException e) {
				throw new RuntimeException(
						"Could not create OperationLogEntryIterator: Cannot open '" + logFile + "'", e.getCause());
			}
		}
		else {
			return cachedIterable.iterator();
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
				return ((nextLogEntry = readNextLogEntry()) != null);
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

		private OperationLogEntry readNextLogEntry() throws IOException {
			
			String[] tokens;
			boolean repeat;
			
			do {
				repeat = false;
					
				tokens = reader.readNext();
				if (tokens == null) {
					reader.close();
					reader = null;
					return null;
				}
				
				if (operationNameFilter != null) {
					if (!tokens[1].equals(operationNameFilter)) repeat = true;
				}
			}
			while (repeat);

			return extractLogEntry(tokens);
		}

		private OperationLogEntry extractLogEntry(String[] tokens) throws IOException {

			if (tokens.length != 7) {
				throw new IOException("Unexpected number of tokens");
			}
			
			int opId = Integer.parseInt(tokens[0]);
			String name = tokens[1].intern();
			String type = tokens[2].intern();
			String[] args = extractArgs(tokens[3]);
			long time = Long.parseLong(tokens[4]);
			String result = tokens[5];
			long memory = Long.parseLong(tokens[6]);

			return new OperationLogEntry(opId, name, type, args, time, result, memory);
		}

		private String[] extractArgs(String argsStr) {
			return argsStr.replaceAll("[\\[\\]]", "").split(", ");
		}
	}
}
