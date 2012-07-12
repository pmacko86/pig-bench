package com.tinkerpop.bench.log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

import au.com.bytecode.opencsv.CSVWriter;

import com.tinkerpop.bench.LogUtils;
import com.tinkerpop.bench.operation.Operation;

import edu.harvard.pass.cpl.CPL;
import edu.harvard.pass.cpl.CPLFile;
import edu.harvard.pass.cpl.CPLObject;

public class OperationLogWriter {
	
	public static final String[] HEADERS = { "id", "name", "type", "args", "time", "result", "memory" };
	
	private final char logDelim = LogUtils.LOG_DELIMITER.charAt(0);
	
	private CSVWriter writer = null;
	private CPLFile cplObject = null;
	private String[] buffer = new String[HEADERS.length];

	
	public OperationLogWriter(File logFile) throws IOException {
		(new File(logFile.getParent())).mkdirs();
		writer = new CSVWriter(new FileWriter(logFile), logDelim);
		writeHeaders();
		
		if (CPL.isAttached()) {
			cplObject = CPLFile.create(logFile);
		}
	}

	// Write .csv log column headers
	private synchronized void writeHeaders() throws IOException {
		writer.writeNext(HEADERS);
	}

	// Write a .csv log data row
	public synchronized void logOperation(Operation op) throws IOException {
		
		buffer[0] = Integer.toString(op.getId());
		buffer[1] = op.getName();
		buffer[2] = op.getType();
		buffer[3] = Arrays.toString(op.getArgs());
		buffer[4] = Long.toString(op.getTime());
		buffer[5] = op.getResult().toString();
		buffer[6] = Long.toString(op.getMemory());
		
		writer.writeNext(buffer);
	}

	public synchronized void close() throws IOException {
		writer.close();
	}
	
	public CPLObject getCPLObject() {
		return cplObject;
	}
}
