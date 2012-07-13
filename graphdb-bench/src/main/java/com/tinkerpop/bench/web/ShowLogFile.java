package com.tinkerpop.bench.web;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import au.com.bytecode.opencsv.CSVWriter;

import com.tinkerpop.bench.DatabaseEngine;
import com.tinkerpop.bench.log.OperationLogEntry;
import com.tinkerpop.bench.log.OperationLogReader;
import com.tinkerpop.bench.log.OperationLogWriter;


/**
 * A servlet for showing a given log file
 * 
 * @author Peter Macko (pmacko@eecs.harvard.edu)
 */
@SuppressWarnings("serial")
public class ShowLogFile extends HttpServlet {
	
	
	/**
	 * Create an instance of class ShowLogFile
	 */
	public ShowLogFile() {
	}

	
	/**
	 * Respond to a POST request
	 */
	@Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
		doGet(request, response);
    }
	
	
	/**
	 * Respond to a GET request
	 */
	@Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
		
		// Get the database engine name and the instance name
		
		String dbEngine = WebUtils.getStringParameter(request, "database_name");
		String dbInstance = WebUtils.getStringParameter(request, "database_instance");
		if (dbInstance != null) if (dbInstance.equals("")) dbInstance = null;
		if (dbInstance != null) {
			WebUtils.asssertDatabaseInstanceNameValidity(dbInstance);
		}
		if (!DatabaseEngine.ENGINES.containsKey(dbEngine)) {
			throw new IllegalArgumentException("Unknown database engine: " + dbEngine);
		}
		if (dbInstance != null) {
			WebUtils.asssertDatabaseInstanceNameValidity(dbInstance);
		}
		
		
		// Get the log file name
		
		String logFileName = WebUtils.getFileNameParameter(request, "log_file");
		if (logFileName == null) {
			throw new IllegalArgumentException("No log file is specified");
		}
		
		File dir = WebUtils.getResultsDirectory(dbEngine, dbInstance);
		File logFile = new File(dir, logFileName);
		if (!logFile.exists() || !logFile.isFile()) {
			throw new IllegalArgumentException("The specfied log file does not exist or is not a regular file");
		}
		
		
		// Other parameters
		
		String format = WebUtils.getStringParameter(request, "format");
		if (format == null) format = "plain";
		
		
		// Get the log file reader and the response writer
		
		OperationLogReader reader = new OperationLogReader(logFile);
		PrintWriter writer = response.getWriter();
		
		
		// Depending on the format type...
		
		if ("html".equals(format)) {
	        response.setContentType("text/html");
	        response.setStatus(HttpServletResponse.SC_OK);
	        
			writer.println("<table class=\"basic_table\">");
			writer.println("<tr>");
			writer.println("\t<th>ID</th>");
			writer.println("\t<th>Name</th>");
			writer.println("\t<th>Arguments</th>");
			writer.println("\t<th>Result</th>");
			writer.println("\t<th class=\"numeric\">Time (ms)</th>");
			writer.println("\t<th class=\"numeric\">Memory (MB)</th>");
			writer.println("</tr>");

			for (OperationLogEntry e : reader) {
				String argumentsStr = "";
				String[] a = e.getArgs();
				for (int i = 0; i < a.length; i++) {
					if (i > 0) argumentsStr += ", ";
					argumentsStr += a[i];
				}
				writer.println("<tr>");
				writer.println("\t<td>" + e.getOpId() + "</td>");
				writer.println("\t<td>" + e.getName() + "</td>");
				writer.println("\t<td>" + argumentsStr + "</td>");
				writer.println("\t<td>" + e.getResult() + "</td>");
				writer.println("\t<td class=\"numeric\">" + String.format("%.3f", e.getTime() / 1000000.0) + "</td>");
				writer.println("\t<td class=\"numeric\">" + String.format("%.3f", e.getMemory() / 1000000.0) + "</td>");
				writer.println("</tr>");
			}
			writer.println("</table>");
		}
		
		else if ("csv".equals(format)) {
	        response.setContentType("text/plain");
	        response.setStatus(HttpServletResponse.SC_OK);
	        
	        CSVWriter w = new CSVWriter(writer);
	        w.writeNext(OperationLogWriter.HEADERS);
	        
	        String[] buffer = new String[OperationLogWriter.HEADERS.length];
			for (OperationLogEntry e : reader) {
				
				buffer[0] = Integer.toString(e.getOpId());
				buffer[1] = e.getName();
				buffer[2] = e.getType();
				buffer[3] = Arrays.toString(e.getArgs());
				buffer[4] = Long.toString(e.getTime());
				buffer[5] = e.getResult().toString();
				buffer[6] = Long.toString(e.getMemory());
				
				w.writeNext(buffer);
			}
		}
		
		else {
	        response.setContentType("text/plain");
	        response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
	        
	        writer.println("Invalid format.");
		}
	}
}
