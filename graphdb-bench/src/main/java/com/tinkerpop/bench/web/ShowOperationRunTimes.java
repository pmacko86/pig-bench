package com.tinkerpop.bench.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import au.com.bytecode.opencsv.CSVWriter;

import com.tinkerpop.bench.DatabaseEngine;
import com.tinkerpop.bench.log.GraphRunTimes;
import com.tinkerpop.bench.log.SummaryLogEntry;
import com.tinkerpop.bench.log.SummaryLogReader;
import com.tinkerpop.bench.util.Triple;


/**
 * A servlet for showing runtimes of a single operation from one or more jobs
 * 
 * @author Peter Macko (pmacko@eecs.harvard.edu)
 */
@SuppressWarnings("serial")
public class ShowOperationRunTimes extends HttpServlet {
	
	/**
	 * Create an instance of class ShowOperationRunTimes
	 */
	public ShowOperationRunTimes() {
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
		
		// The operation name
		
		String[] operationNames = WebUtils.getStringParameterValues(request, "operations");
		
				
		// Get the list of jobs for each operation name

		TreeMap<String, Collection<Job>> operationsToJobs = new TreeMap<String, Collection<Job>>();
		
		for (String operationName : operationNames) {
			String[] jobIds = WebUtils.getStringParameterValues(request, "jobs-" + operationName);
			LinkedList<Job> jobs = new LinkedList<Job>();
			
			if (jobIds != null) {
				for (String j : jobIds) {
					jobs.add(JobList.getInstance().getFinishedJob(Integer.parseInt(j)));
				}
			}
			
			operationsToJobs.put(operationName, jobs);
		}
		
		
		// Other parameters
		
		String format = WebUtils.getStringParameter(request, "format");
		if (format == null) format = "plain";
		
		String groupBy = WebUtils.getStringParameter(request, "group_by");
		
		
		// Get the writer and write out the log file
		
		PrintWriter writer = response.getWriter();
		printRunTimes(writer, operationsToJobs, format, response, groupBy);
	}
	
	
	/**
	 * Write out a log file to the given writer
	 * 
	 * @param writer the writer
	 * @param operationsToJobs the map of operation names to the jobs
	 * @param format the format
	 * @param response the response, or null if none
	 */
	public static void printRunTimes(PrintWriter writer, Map<String, Collection<Job>> operationsToJobs,
			String format, HttpServletResponse response) {
		printRunTimes(writer, operationsToJobs, format, response, null);
	}
	
	
	/**
	 * Write out a log file to the given writer
	 * 
	 * @param writer the writer
	 * @param operationsToJobs the map of operation names to the jobs
	 * @param format the format
	 * @param response the response, or null if none
	 * @param groupBy the group by column, or null if none
	 */
	public static void printRunTimes(PrintWriter writer, Map<String, Collection<Job>> operationsToJobs,
			String format, HttpServletResponse response, String groupBy) {
		
		
		// Get the run time for each job
		
		LinkedList<Triple<String, Job, GraphRunTimes>> operationsJobsRunTimes
			= new LinkedList<Triple<String,Job,GraphRunTimes>>();

		boolean sameOperation = operationsToJobs.keySet().size() == 1;
		boolean sameDbEngine = true;
		boolean sameDbInstance = true;
		
		for (String operationName : operationsToJobs.keySet()) {
			
			Job firstJob = null;
			
			for (Job job : operationsToJobs.get(operationName)) {
				
				SummaryLogReader reader = new SummaryLogReader(job.getSummaryFile());
				GraphRunTimes g = null;
				for (SummaryLogEntry e : reader) {
					if (e.getName().equals(operationName)) {
						g = e.getDefaultRunTimes();
					}
				}
				if (g == null) {
					if (response != null) {
				        response.setContentType("text/plain");
				        response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
					}
			        
					if (response == null && "html".equals(format)) writer.println("<pre>");
			        writer.println("Error: The job's summary file does not contain a record for the required operation name");
			        writer.println();
			        writer.println("Job " + job.getId() + ": " + job.toString());
			        writer.println("Summary Log File: " + job.getSummaryFile());
			        writer.println("Operation Name: " + operationName);
			        writer.println();
			        writer.println("Available Operation Names:");
					reader = new SummaryLogReader(job.getSummaryFile());
					for (SummaryLogEntry e : reader) {
						writer.println("  " + e.getName().equals(operationName));
					}
					if (response == null && "html".equals(format)) writer.println("</pre>");
			        return;
				}
				operationsJobsRunTimes.add(new Triple<String, Job, GraphRunTimes>(operationName, job, g));
				
				if (firstJob == null) {
					firstJob = job;
				}
				else {
					if (!job.getDbEngine().equals(firstJob.getDbEngine())) {
						sameDbEngine = false;
					}
					if (!job.getDbInstanceSafe().equals(firstJob.getDbInstanceSafe())) {
						sameDbInstance = false;
					}
				}
			}
		}
		
		
		
		// Depending on the format type...
		
		if ("html".equals(format)) {
			if (response != null) {
		        response.setContentType("text/html");
		        response.setStatus(HttpServletResponse.SC_OK);
			}
	        
			writer.println("<table class=\"basic_table\">");
			writer.println("<tr>");
			if (!sameOperation) writer.println("\t<th>Operation</th>");
			if (!sameDbEngine) writer.println("\t<th>Database Engine</th>");
			if (!sameDbInstance) writer.println("\t<th>Database Instance</th>");
			writer.println("\t<th class=\"numeric\">Mean (ms)</th>");
			writer.println("\t<th class=\"numeric\">Stdev (ms)</th>");
			writer.println("\t<th class=\"numeric\">Min (ms)</th>");
			writer.println("\t<th class=\"numeric\">Max (ms)</th>");
			writer.println("</tr>");

			for (Triple<String, Job, GraphRunTimes> p : operationsJobsRunTimes) {
				writer.println("<tr>");
				if (!sameOperation) {
					writer.println("\t<td>" + p.getFirst() + "</td>");
				}
				if (!sameDbEngine) {
					writer.println("\t<td>" + DatabaseEngine.ENGINES.get(p.getSecond().getDbEngine()).getLongName() + "</td>");
				}
				if (!sameDbInstance) {
					writer.println("\t<td>" + (p.getSecond().getDbInstance() == null
													? "&lt;default&gt;" : p.getSecond().getDbInstance()) + "</td>");
				}
				GraphRunTimes r = p.getThird();
				writer.println("\t<td class=\"numeric\">" + String.format("%.3f", r.getMean() / 1000000.0) + "</td>");
				writer.println("\t<td class=\"numeric\">" + String.format("%.3f", r.getStdev() / 1000000.0) + "</td>");
				writer.println("\t<td class=\"numeric\">" + String.format("%.3f", r.getMin() / 1000000.0) + "</td>");
				writer.println("\t<td class=\"numeric\">" + String.format("%.3f", r.getMax() / 1000000.0) + "</td>");
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
	        String[] buffer = new String[8];
	        
	        int index = 0;
	        buffer[index++] = "label";
	        buffer[index++] = "operation";
	        buffer[index++] = "dbengine";
	        buffer[index++] = "dbinstance";
	       	buffer[index++] = "mean";
	       	buffer[index++] = "stdev";
	        buffer[index++] = "min";
	        buffer[index++] = "max";
	        w.writeNext(buffer);
	        
	        String lastOperation = null;
	        
	        for (Triple<String, Job, GraphRunTimes> p : operationsJobsRunTimes) {
	        	
	        	String operation = p.getFirst();
	        	String dbengine = DatabaseEngine.ENGINES.get(p.getSecond().getDbEngine()).getLongName();
	        	String dbinstance = (p.getSecond().getDbInstance() == null ? "<default>" : p.getSecond().getDbInstance());
				
				 
				// Group by placeholders
	        	
	        	if ("operation".equals(groupBy)) {
	        		if (lastOperation != null && !operation.equals(lastOperation)) {
	        			for (int i = 0; i < buffer.length; i++) buffer[i] = "";
	        			buffer[0] = "----" + lastOperation;
		        		w.writeNext(buffer);
	        		}
	        	}
	        	

	        	
	        	// Actual data
				
	        	index = 0;
				String label = "";
				if (!sameOperation) {
					if (!"".equals(label)) label += " : ";
					label += operation;
				}
				if (!sameDbEngine) {
					if (!"".equals(label)) label += " : ";
					label += dbengine;
				}
				if (!sameDbInstance) {
					if (!"".equals(label)) label += " : ";
					label += dbinstance;
				}
				buffer[index++] = label;
	        	
				buffer[index++] = operation;
				buffer[index++] = dbengine;
				buffer[index++] = dbinstance;
			
				GraphRunTimes r = p.getThird();
				buffer[index++] = Double.toString(r.getMean());
				buffer[index++] = Double.toString(r.getStdev());
				buffer[index++] = Double.toString(r.getMin());
				buffer[index++] = Double.toString(r.getMax());
				
				w.writeNext(buffer);
				
				
				// Finish the entry
				
				lastOperation = operation;
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
