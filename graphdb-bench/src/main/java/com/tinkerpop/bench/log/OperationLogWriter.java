package com.tinkerpop.bench.log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

import au.com.bytecode.opencsv.CSVWriter;

import com.tinkerpop.bench.operation.Operation;
import com.tinkerpop.bench.operation.OperationDoGC;
import com.tinkerpop.bench.operation.OperationOpenGraph;
import com.tinkerpop.bench.operation.OperationShutdownGraph;
import com.tinkerpop.bench.util.LogUtils;

import edu.harvard.pass.cpl.CPL;
import edu.harvard.pass.cpl.CPLFile;
import edu.harvard.pass.cpl.CPLObject;

public class OperationLogWriter {
	
	public static final String[] HEADERS = { "id", "name", "type", "args", "time", "result", "memory",
		"gccount", "gctime", "kbread", "kbwritten" };
	private static final char LOG_DELIM = LogUtils.LOG_DELIMITER.charAt(0);
	
	
	// Log writer
	
	private File logFile = null;
	private CSVWriter writer = null;
	private CPLFile cplObject = null;
	private String[] buffer = new String[HEADERS.length];
	
	
	// Internal statistics
	
	private long cumulativeBenchmarkTime = 0;

	
	/**
	 * Create an instance of class OperationLogWriter
	 * 
	 * @param logFile the log file (can be null)
	 * @throws IOException on I/O error
	 */
	public OperationLogWriter(File logFile) throws IOException {
		
		this.logFile = logFile;
		
		if (logFile != null) {
			
			(new File(logFile.getParent())).mkdirs();
			writer = new CSVWriter(new FileWriter(logFile), LOG_DELIM);
			writeHeaders();
			
			if (CPL.isAttached()) {
				cplObject = CPLFile.create(logFile);
			}
		}
	}

	
	/**
	 * Write .csv log column headers
	 * 
	 * @throws IOException on I/O errors
	 */
	private synchronized void writeHeaders() throws IOException {
		if (writer != null) writer.writeNext(HEADERS);
	}

	
	/**
	 * Write a .csv log data row
	 * 
	 * @param op the operation
	 * @throws IOException on I/O error
	 */
	public synchronized void logOperation(Operation op) throws IOException {
		
		
		// Log to the file
		
		if (writer != null) {
			
			buffer[ 0] = Integer.toString(op.getId());
			buffer[ 1] = op.getName();
			buffer[ 2] = op.getType();
			buffer[ 3] = Arrays.toString(op.getArgs());
			buffer[ 4] = Long.toString(op.getTime());
			buffer[ 5] = op.getResult() == null ? "null" : op.getResult().toString();
			buffer[ 6] = Long.toString(op.getMemory());
			buffer[ 7] = Long.toString(op.getGCCount());
			buffer[ 8] = Long.toString(op.getGCTimeMS());
			buffer[ 9] = Integer.toString(op.getKbRead());
			buffer[10] = Integer.toString(op.getKbWritten());
			
			writer.writeNext(buffer);
		}
		
		
		// Update the internal statistics
		
		if (!op.getClass().equals(OperationDoGC.class)
				&& !op.getClass().equals(OperationOpenGraph.class)
				&& !op.getClass().equals(OperationShutdownGraph.class)) {
			cumulativeBenchmarkTime += op.getTime();
		}
	}

	
	/**
	 * Write a .csv log data row
	 * 
	 * @param entry the operation log entry
	 * @throws IOException on I/O error
	 */
	public synchronized void write(OperationLogEntry entry) throws IOException {
		
		
		// Log to the file
		
		if (writer != null) {
			
			buffer[ 0] = Integer.toString(entry.getOpId());
			buffer[ 1] = entry.getName();
			buffer[ 2] = entry.getType();
			buffer[ 3] = Arrays.toString(entry.getArgs());
			buffer[ 4] = Long.toString(entry.getTime());
			buffer[ 5] = entry.getResult() == null ? "null" : entry.getResult().toString();
			buffer[ 6] = Long.toString(entry.getMemory());
			buffer[ 7] = Long.toString(entry.getGCCount());
			buffer[ 8] = Long.toString(entry.getGCTimeMS());
			buffer[ 9] = Integer.toString(entry.getKbRead());
			buffer[10] = Integer.toString(entry.getKbWritten());
			
			writer.writeNext(buffer);
		}
	}

	
	/**
	 * Close the log writer
	 * 
	 * @throws IOException on I/O error
	 */
	public synchronized void close() throws IOException {
		if (writer != null) writer.close();
	}
	
	
	/**
	 * Get the CPL object associated with the log file
	 * 
	 * @return the CPL file object, or null if none
	 */
	public CPLObject getCPLObject() {
		return cplObject;
	}
	
	
	/**
	 * Return the cumulative benchmark time. Please note that this does not equal
	 * the elapsed time in the case of multi-threaded benchmarks, and also please
	 * note that this does not include GC, open, and shutdown graph operations.
	 * 
	 * @return the cumulative benchmark time in us
	 */
	public long getCumulativeBenchmarkTime() {
		return cumulativeBenchmarkTime;
	}
	
	
	/**
	 * Get the log file
	 * 
	 * @return the log file, or null if logging is not enabled
	 */
	public File getLogFile() {
		return logFile;
	}
}
