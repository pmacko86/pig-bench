package com.tinkerpop.bench.web;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import au.com.bytecode.opencsv.CSVWriter;

import com.tinkerpop.bench.DatabaseEngine;
import com.tinkerpop.bench.log.GraphRunTimes;
import com.tinkerpop.bench.log.SummaryLogEntry;
import com.tinkerpop.bench.log.SummaryLogReader;


/**
 * A servlet for showing a given summary log file
 * 
 * @author Peter Macko (pmacko@eecs.harvard.edu)
 */
@SuppressWarnings("serial")
public class ShowSummaryLogFile extends HttpServlet {
	
	/**
	 * Create an instance of class ShowSummaryLogFile
	 */
	public ShowSummaryLogFile() {
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
			throw new IllegalArgumentException("No summary log file is specified");
		}
		
		File dir = WebUtils.getResultsDirectory(dbEngine, dbInstance);
		File logFile = new File(dir, logFileName);
		if (!logFile.exists() || !logFile.isFile()) {
			throw new IllegalArgumentException("The specfied summary log file does not exist or is not a regular file");
		}
		
		
		// Other parameters
		
		String format = WebUtils.getStringParameter(request, "format");
		if (format == null) format = "plain";
		
		
		// Get the writer and write out the log file
		
		PrintWriter writer = response.getWriter();
		printSummaryLogFile(writer, logFile, format, response);
	}
	
	
	/**
	 * Write out a log file to the given writer
	 * 
	 * @param writer the writer
	 * @param logFile the log file
	 * @param format the format
	 * @param response the response, or null if none
	 */
	public static void printSummaryLogFile(PrintWriter writer, File logFile, String format, HttpServletResponse response) {
		
		
		// Get the log file reader and the response writer
		
		SummaryLogReader reader = new SummaryLogReader(logFile);
		
		
		// Depending on the format type...
		
		if ("html".equals(format)) {
			if (response != null) {
		        response.setContentType("text/html");
		        response.setStatus(HttpServletResponse.SC_OK);
			}
	        
			writer.println("<table class=\"basic_table\">");
			writer.println("<tr>");
			writer.println("\t<th>Operation</th>");
			for (String s : reader.getGraphNames()) {
				writer.println("\t<th class=\"numeric\">" + s + "-mean (ms)</th>");
				writer.println("\t<th class=\"numeric\">" + s + "-stdev (ms)</th>");
				writer.println("\t<th class=\"numeric\">" + s + "-min (ms)</th>");
				writer.println("\t<th class=\"numeric\">" + s + "-max (ms)</th>");
			}
			writer.println("</tr>");

			for (SummaryLogEntry e : reader) {
				writer.println("<tr>");
				writer.println("\t<td>" + e.getName() + "</td>");
				for (String s : reader.getGraphNames()) {
					GraphRunTimes r = e.getRunTimes(s);
					writer.println("\t<td class=\"numeric\">" + String.format("%.3f", r.getMean() / 1000000.0) + "</td>");
					writer.println("\t<td class=\"numeric\">" + String.format("%.3f", r.getStdev() / 1000000.0) + "</td>");
					writer.println("\t<td class=\"numeric\">" + String.format("%.3f", r.getMin() / 1000000.0) + "</td>");
					writer.println("\t<td class=\"numeric\">" + String.format("%.3f", r.getMax() / 1000000.0) + "</td>");
				}
				writer.println("</tr>");
			}
			writer.println("</table>");
		}
		
		else if ("csv".equals(format)) {
			if (response != null) {
		        response.setContentType("text/plain");
		        response.setStatus(HttpServletResponse.SC_OK);
			}
	        
	        CSVWriter w = new CSVWriter(writer);
	        String[] buffer = new String[reader.getGraphNames().length * 4 + 1];
	        
	        int index = 0;
	        buffer[index++] = "operation";
	        if (reader.getGraphNames().length == 1) {
	        	buffer[index++] = "mean";
	        	buffer[index++] = "stdev";
	        	buffer[index++] = "min";
	        	buffer[index++] = "max";
	        }
	        else {
		        for (String s : reader.getGraphNames()) {
		        	buffer[index++] = s + "_mean";
		        	buffer[index++] = s + "_stdev";
		        	buffer[index++] = s + "_min";
		        	buffer[index++] = s + "_max";
		        }
	        }
	        
	        w.writeNext(buffer);
	        
	        for (SummaryLogEntry e : reader) {
				
	        	index = 0;
				buffer[index++] = e.getName();
				
				for (String s : reader.getGraphNames()) {
					GraphRunTimes r = e.getRunTimes(s);
					buffer[index++] = Double.toString(r.getMean());
					buffer[index++] = Double.toString(r.getStdev());
					buffer[index++] = Double.toString(r.getMin());
					buffer[index++] = Double.toString(r.getMax());
				}
				
				w.writeNext(buffer);
			}
		}
		
		else {
			if (response != null) {
		        response.setContentType("text/plain");
		        response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
			}
	        
	        writer.println("Invalid format.");
		}		
	}
}
