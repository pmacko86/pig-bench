package com.tinkerpop.bench.log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.WeakHashMap;

import au.com.bytecode.opencsv.CSVParser;

import com.tinkerpop.bench.util.LogUtils;


/**
 * @author Alex Averbuch (alex.averbuch@gmail.com)
 * @author Peter Macko (pmacko@eecs.harvard.edu)
 */
public class OperationLogReader implements Iterable<OperationLogEntry> {

	public static int TAIL_MIN_ROWS = 1;
	public static int TAIL_MAX_ROWS = 10000;
	public static double TAIL_MAX_FRACTION = 0.25;	
	
	private final char logDelim = LogUtils.LOG_DELIMITER.charAt(0);
	
	private File logFile = null;
	private String operationNameFilter;
	
	
	private static WeakHashMap<String, List<OperationLogEntry>> operationEntriesTailCache
		= new WeakHashMap<String, List<OperationLogEntry>>();

	
	public OperationLogReader(File logFile, String operationNameFilter) {
		this.logFile = logFile;
		this.operationNameFilter = operationNameFilter;
	}

	public OperationLogReader(File logFile) {
		this(logFile, null);
	}
	
	
	public static List<OperationLogEntry> getAllEntries(File logFile, String operationName) {
		OperationLogReader reader = new OperationLogReader(logFile, operationName);
		List<OperationLogEntry> r = new ArrayList<OperationLogEntry>();
		for (OperationLogEntry e : reader) {
			if (e.getName().equals(operationName)) r.add(e);
		}
		return r;
	}
	
	public static List<OperationLogEntry> getTailEntries(File logFile, String operationName) {
		synchronized (operationEntriesTailCache) {
			
			String cacheKey = logFile.getAbsolutePath() + "----" + operationName;
			List<OperationLogEntry> entries = operationEntriesTailCache.get(cacheKey);
			
			if (entries == null) {			
				OperationLogReader reader = new OperationLogReader(logFile, operationName);
				ArrayList<OperationLogEntry> r = new ArrayList<OperationLogEntry>();
				for (OperationLogEntry e : reader) {
					if (e.getName().equals(operationName)) r.add(e);
				}
				
				int n = r.size();
				
				int keep1 = TAIL_MAX_ROWS     > 0 ? Math.max(TAIL_MAX_ROWS, n) : n;
				int keep2 = TAIL_MAX_FRACTION > 0 ? (int) Math.round(TAIL_MAX_FRACTION * n) : n;
				int keep  = Math.min(keep1, keep2);
				
				if (keep < TAIL_MIN_ROWS) keep = Math.min(TAIL_MIN_ROWS, n);
			
				if (keep > 0 && n > keep) {
					ArrayList<OperationLogEntry> a = new ArrayList<OperationLogEntry>(keep);
					for (int i = r.size() - keep; i < r.size(); i++) {
						a.add(r.get(i));
					}
					r = a;
				}
				
				entries = r;
				operationEntriesTailCache.put(cacheKey, entries);
			}
			
			return entries;
		}
	}


	public Iterator<OperationLogEntry> iterator() {
		try {
			return new OperationLogEntryIterator(this.logFile);
		} catch (IOException e) {
			throw new RuntimeException(
					"Could not create OperationLogEntryIterator: Cannot open '" + logFile + "'", e.getCause());
		}
	}

	private class OperationLogEntryIterator implements
			Iterator<OperationLogEntry> {

		private OperationLogEntry nextLogEntry = null;
		private CSVParser parser = null;
		private BufferedReader reader = null;

		public OperationLogEntryIterator(File logFile)
				throws IOException {
			
			this.reader = new BufferedReader(new FileReader(logFile));
			this.parser = new CSVParser(logDelim);

			// skip first line: .csv headers
			reader.readLine();
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
			
			String[] tokens = null;
			boolean repeat;
			
			do {
				repeat = false;
					
				String line = reader.readLine();
				if (line == null) {
					reader.close();
					reader = null;
					return null;
				}
				
				if (operationNameFilter != null) {
					if (!line.contains(operationNameFilter)) {
						repeat = true;
						continue;
					}
				}
				
				tokens = parser.parseLine(line);
				
				if (operationNameFilter != null) {
					if (!tokens[1].equals(operationNameFilter)) {
						repeat = true;
						continue;
					}
				}
			}
			while (repeat);

			return tokens == null ? null : extractLogEntry(tokens);
		}

		private OperationLogEntry extractLogEntry(String[] tokens) throws IOException {

			if (tokens.length != 7 && tokens.length != OperationLogWriter.HEADERS.length) {
				throw new IOException("Unexpected number of tokens");
			}
			
			
			// Original fields
			
			int opId      = Integer.parseInt(tokens[0]);
			String name   = tokens[1].intern();
			String type   = tokens[2].intern();
			String args   = tokens[3];
			long time     = Long.parseLong(tokens[4]);
			String result = tokens[5];
			long memory   = Long.parseLong(tokens[6]);
			
			
			// New fields as of 9/27/12
			
			long gcCount  = tokens.length <= 7 ? -1 : Long.parseLong(tokens[7]);
			long gcTime   = tokens.length <= 8 ? -1 : Long.parseLong(tokens[8]);

			
			return new OperationLogEntry(opId, name, type, args, time, result, memory, gcCount, gcTime);
		}
	}
}
