package com.tinkerpop.bench.log;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;

import au.com.bytecode.opencsv.CSVReader;

import com.tinkerpop.bench.util.LogUtils;


public class SummaryLogReader implements Iterable<SummaryLogEntry> {

	private final char logDelim = LogUtils.LOG_DELIMITER.charAt(0);
	private File logFile = null;
	private SummaryLogEntryIterator iterator = null;
	private String[] graphNames = null;
	
	public SummaryLogReader(File logFile) {
		super();
		this.logFile = logFile;
		try {
			this.iterator = new SummaryLogEntryIterator(logFile);
			this.graphNames = this.iterator.getGraphNames();
		}
		catch (IOException e) {
			throw new RuntimeException(
					"Could not create SummaryLogReaderLogEntryIterator: Cannot open '" + logFile + "'", e.getCause());
		}
	}
	
	public String[] getGraphNames() {
		return graphNames;
	}

	public synchronized Iterator<SummaryLogEntry> iterator() {
		try {
			if (iterator == null) iterator = new SummaryLogEntryIterator(logFile);
			SummaryLogEntryIterator r = iterator;
			iterator = null;
			return r;
		}
		catch (IOException e) {
			throw new RuntimeException(
					"Could not create SummaryLogReaderLogEntryIterator: Cannot open '" + logFile + "'", e.getCause());
		}
	}

	private class SummaryLogEntryIterator implements
			Iterator<SummaryLogEntry> {

		private SummaryLogEntry nextLogEntry = null;
		private CSVReader reader = null;
		private String[] headers = null;
		private String[] graphNames = null;

		public SummaryLogEntryIterator(File logFile)
				throws IOException {
			this.reader = new CSVReader(new FileReader(logFile), logDelim);

			// Header
			headers = reader.readNext();
			graphNames = new String[(headers.length - 1) / 4];
			for (int i = 1; i < headers.length; i += 4) {
				int p = headers[i].indexOf('-');
				if (p < 0) {
					graphNames[(i-1)/4] = "";
				}
				else {
					graphNames[(i-1)/4] = headers[i].substring(0, p);
				}
			}
		}
		
		@SuppressWarnings("unused")
		public String[] getHeaders() {
			return headers;
		}
		
		public String[] getGraphNames() {
			return graphNames;
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
		public SummaryLogEntry next() {
			if (nextLogEntry == null)
				throw new NoSuchElementException();

			SummaryLogEntry logEntry = nextLogEntry;
			nextLogEntry = null;
			return logEntry;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		private SummaryLogEntry parseLogEntry() throws IOException {
			String[] tokens = reader.readNext();
			if (tokens == null) {
				reader.close();
				reader = null;
				return null;
			}

			return extractLogEntry(tokens);
		}

		private SummaryLogEntry extractLogEntry(String[] tokens) {
			
			String name = tokens[0];
			HashMap<String, GraphRunTimes> runTimes = new HashMap<String, GraphRunTimes>();

			for (int index = 1; index < tokens.length; index += 4) {
				String graphName = graphNames[(index-1)/4];
				double mean = Double.parseDouble(tokens[index]);
				double stdev = Double.parseDouble(tokens[index+1]);
				double min = Double.parseDouble(tokens[index+2]);
				double max = Double.parseDouble(tokens[index+3]);
				GraphRunTimes g = new GraphRunTimes(graphName, name, mean, stdev, min, max);
				runTimes.put(graphName, g);
			}

			return new SummaryLogEntry(name, runTimes);
		}
	}
}

