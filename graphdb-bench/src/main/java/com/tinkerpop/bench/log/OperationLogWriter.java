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
	
	public static final String[] HEADERS = { "id", "name", "type", "args", "time", "result", "memory", "gccount", "gctime" };
	private static final char LOG_DELIM = LogUtils.LOG_DELIMITER.charAt(0);
	
	
	// Log writer
	
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
		
		buffer[0] = Integer.toString(op.getId());
		buffer[1] = op.getName();
		buffer[2] = op.getType();
		buffer[3] = Arrays.toString(op.getArgs());
		buffer[4] = Long.toString(op.getTime());
		buffer[5] = op.getResult().toString();
		buffer[6] = Long.toString(op.getMemory());
		buffer[6] = Long.toString(op.getGCCount());
		buffer[6] = Long.toString(op.getGCTimeMS());
		
		if (writer != null) writer.writeNext(buffer);
		
		
		// Update the internal statistics
		
		if (!op.getClass().equals(OperationDoGC.class)
				&& !op.getClass().equals(OperationOpenGraph.class)
				&& !op.getClass().equals(OperationShutdownGraph.class)) {
			cumulativeBenchmarkTime += op.getTime();
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
}
