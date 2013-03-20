package com.tinkerpop.bench.log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.WeakHashMap;
import java.util.regex.Pattern;

import au.com.bytecode.opencsv.CSVParser;

import com.tinkerpop.bench.util.LogUtils;


/**
 * @author Alex Averbuch (alex.averbuch@gmail.com)
 * @author Peter Macko (pmacko@eecs.harvard.edu)
 */
public class OperationLogReader implements Iterable<OperationLogEntry> {

	private static Pattern opNamePattern = Pattern.compile("^[A-Za-z][A-Za-z0-9_-]*$");
	
	public static final String CACHE_DIR = "cache";
	public static boolean PERSISTENT_TAIL_CACHE = true;

	public static int TAIL_MIN_ROWS = 1;
	public static int TAIL_MAX_ROWS = 10000;
	public static double TAIL_MAX_FRACTION = 0.25;
	
	private final char logDelim = LogUtils.LOG_DELIMITER.charAt(0);
	
	private File logFile = null;
	private String operationNameFilter;
	
	
	private static WeakHashMap<String, WeakReference<List<OperationLogEntry>>> operationEntriesTailCache
		= new WeakHashMap<String, WeakReference<List<OperationLogEntry>>>();

	
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
	
	
	public static File getCacheFile(File logFile, String operationName, String prefix, String suffix) {
		
		int logDatePos = logFile.getName().indexOf("__date_");
		String logDate = "";
		if (logDatePos >= 0) {
			int logDateEnd = logFile.getName().indexOf("__", logDatePos + "__date_".length());
			if (logDateEnd < 0) logDateEnd = logFile.getName().indexOf('.', logDatePos + "__date_".length());
			if (logDateEnd > 0) {
				logDate = logFile.getName().substring(logDatePos, logDateEnd);
			}
		}
		
		File cacheDir = new File(logFile.getParentFile(), CACHE_DIR);
		String cacheFileName = prefix + logDate + "__hash_" + Integer.toHexString(logFile.hashCode())
				+ "__op_" + operationName;
		if (!suffix.startsWith(".") && !suffix.startsWith("__")) cacheFileName += "__";
		cacheFileName += suffix;
		
		return new File(cacheDir, cacheFileName);
	}
	
	
	@SuppressWarnings("unchecked")
	public static List<OperationLogEntry> getTailEntries(File logFile, String operationName) {
		
		if (!opNamePattern.matcher(operationName).matches()) {
			throw new RuntimeException("Invalid operation name");
		}
		
		synchronized (operationEntriesTailCache) {
			
			String cacheKey = logFile.getAbsolutePath() + "----" + operationName;
			WeakReference<List<OperationLogEntry>> refEntries = operationEntriesTailCache.get(cacheKey);
			List<OperationLogEntry> entries = refEntries == null ? null : refEntries.get();
			
			
			// Read the data from disk, if it was not found in the cache
			
			if (entries == null) {

				// Check the persistent tail cache
				
				File cacheFile = getCacheFile(logFile, operationName, "tail", "tail_" + TAIL_MIN_ROWS
						+ "_" + TAIL_MAX_ROWS + "_" + ((int) (100 * TAIL_MAX_FRACTION)) + ".dat");
						
				if (PERSISTENT_TAIL_CACHE) {
					if (cacheFile.exists()) {
						ObjectInputStream in = null;
						try {
							in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(cacheFile)));
							entries = (List<OperationLogEntry>) in.readObject();
							in.close();
						}
						catch (InvalidClassException e) {
							entries = null;
							try { if (in != null) in.close(); } catch (IOException ex) {};
						}
						catch (IOException e) {
							try { if (in != null) in.close(); } catch (IOException ex) {};
							throw new RuntimeException(e);
						}
						catch (ClassNotFoundException e) {
							try { if (in != null) in.close(); } catch (IOException ex) {};
							throw new RuntimeException(e);
						}
					}
				}
				
				
				// Read the data from the log file, if it was not found in the persistent tail cache
				
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
					
					
					// Store the tail in the persistent cache, if enabled
					
					if (PERSISTENT_TAIL_CACHE) {
						cacheFile.getParentFile().mkdirs();
						try {
							ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(cacheFile));
							out.writeObject(entries);
							out.close();
						}
						catch (IOException e) {
							throw new RuntimeException(e);
						}
					}
				}
				
				
				// Store the loaded tail entries in the in-memory tail cache
				
				operationEntriesTailCache.put(cacheKey, new WeakReference<List<OperationLogEntry>>(entries));
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

			if (tokens.length < 7) {
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
			
			int gcCount  = tokens.length <= 7 ? -1 : Integer.parseInt(tokens[7]);
			int gcTime   = tokens.length <= 8 ? -1 : Integer.parseInt(tokens[8]);
			
			
			// New fields as of 3/20/13
			
			int kbRead    = tokens.length <=  9 ? -1 : Integer.parseInt(tokens[9]);
			int kbWritten = tokens.length <= 10 ? -1 : Integer.parseInt(tokens[10]);

			
			return new OperationLogEntry(opId, name, type, args, time, result, memory,
					gcCount, gcTime, kbRead, kbWritten);
		}
	}
}
