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
		
		// Whether it is the warmup file
		
		boolean warmup = WebUtils.getBooleanParameter(request, "warmup", false);
		
		
		// Get the job and the corresponding log file
		
		File logFile = null;
		String jobId = WebUtils.getStringParameter(request, "job");
		if (jobId != null) {
			Job j = JobList.getInstance().getFinishedJob(Integer.parseInt(jobId));
			if (j != null) logFile = warmup ? j.getWarmupLogFile() : j.getLogFile();
		}
		
		
		// Other parameters
		
		String format = WebUtils.getStringParameter(request, "format");
		if (format == null) format = "plain";
		
		
		
		// Get the writer and write out the log file
		
		PrintWriter writer = response.getWriter();
		printLogFile(writer, logFile, format, response);
	}
		
	
	/**
	 * Write out a log file to the given writer
	 * 
	 * @param writer the writer
	 * @param logFile the log file
	 * @param format the format
	 * @param response the response, or null if none
	 */
	public static void printLogFile(PrintWriter writer, File logFile, String format, HttpServletResponse response) {

		if (logFile == null) {
			if (response != null) {
		        response.setContentType("text/plain");
		        response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
			}
	        
	        writer.println("No such file.");
	        return;
		}
		
		OperationLogReader reader = new OperationLogReader(logFile);	/* get all entries, not just the tail */
		
		
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
			writer.println("\t<th class=\"numeric\">GC Count</th>");
			writer.println("\t<th class=\"numeric\">GC Time (ms)</th>");
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
				writer.println("\t<td class=\"numeric\">" + (e.getGCCount()  < 0 ? "N/A" : "" + e.getGCCount() ) + "</td>");
				writer.println("\t<td class=\"numeric\">" + (e.getGCTimeMS() < 0 ? "N/A" : "" + e.getGCTimeMS()) + "</td>");
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
				buffer[7] = Long.toString(e.getGCCount());
				buffer[8] = Long.toString(e.getGCTimeMS());
				
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
