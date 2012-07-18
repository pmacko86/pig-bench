package com.tinkerpop.bench.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.LinkedList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import au.com.bytecode.opencsv.CSVWriter;

import com.tinkerpop.bench.DatabaseEngine;
import com.tinkerpop.bench.log.GraphRunTimes;
import com.tinkerpop.bench.log.SummaryLogEntry;
import com.tinkerpop.bench.log.SummaryLogReader;
import com.tinkerpop.bench.util.Pair;


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
		
		// Get the list of jobs

		String[] jobIds = WebUtils.getStringParameterValues(request, "jobs");
		LinkedList<Job> jobs = new LinkedList<Job>();
		
		if (jobIds != null) {
			for (String j : jobIds) {
				jobs.add(JobList.getInstance().getFinishedJob(Integer.parseInt(j)));
			}
		}
		
		
		// The operation name
		
		String operationName = WebUtils.getStringParameter(request, "operation");
		
		
		// Other parameters
		
		String format = WebUtils.getStringParameter(request, "format");
		if (format == null) format = "plain";
		
		
		// Get the writer and write out the log file
		
		PrintWriter writer = response.getWriter();
		printRunTimes(writer, operationName, jobs, format, response);
	}
	
	
	/**
	 * Write out a log file to the given writer
	 * 
	 * @param writer the writer
	 * @param operationName the operation name
	 * @param jobs the the collection of jobs
	 * @param format the format
	 * @param response the response, or null if none
	 */
	public static void printRunTimes(PrintWriter writer, String operationName, Collection<Job> jobs,
			String format, HttpServletResponse response) {
		
		
		// Get the run time for each job
		
		LinkedList<Pair<Job, GraphRunTimes>> runTimes = new LinkedList<Pair<Job,GraphRunTimes>>();
		Job firstJob = null;
		boolean sameDbEngine = true;
		boolean sameDbInstance = true;
		
		for (Job job : jobs) {
			
			SummaryLogReader reader = new SummaryLogReader(job.getSummaryFile());
			GraphRunTimes g = null;
			for (SummaryLogEntry e : reader) {
				if (e.getName().equals(operationName)) {
					g = e.getDefaultRunTimes();
				}
			}
			runTimes.add(new Pair<Job, GraphRunTimes>(job, g));
			
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
		
		
		
		// Depending on the format type...
		
		if ("html".equals(format)) {
			if (response != null) {
		        response.setContentType("text/html");
		        response.setStatus(HttpServletResponse.SC_OK);
			}
	        
			writer.println("<table class=\"basic_table\">");
			writer.println("<tr>");
			if (!sameDbEngine) writer.println("\t<th>Database Engine</th>");
			if (!sameDbInstance) writer.println("\t<th>Database Instance</th>");
			writer.println("\t<th class=\"numeric\">Mean (ms)</th>");
			writer.println("\t<th class=\"numeric\">Stdev (ms)</th>");
			writer.println("\t<th class=\"numeric\">Min (ms)</th>");
			writer.println("\t<th class=\"numeric\">Max (ms)</th>");
			writer.println("</tr>");

			for (Pair<Job, GraphRunTimes> p : runTimes) {
				writer.println("<tr>");
				if (!sameDbEngine) {
					writer.println("\t<td>" + DatabaseEngine.ENGINES.get(p.getFirst().getDbEngine()).getLongName() + "</td>");
				}
				if (!sameDbInstance) {
					writer.println("\t<td>" + (p.getFirst().getDbInstance() == null
													? "&lt;default&gt;" : p.getFirst().getDbInstance()) + "</td>");
				}
				GraphRunTimes r = p.getSecond();
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
	        String[] buffer = new String[7];
	        
	        int index = 0;
	        buffer[index++] = "dbengine";
	        buffer[index++] = "dbinstance";
	        buffer[index++] = "label";
	       	buffer[index++] = "mean";
	       	buffer[index++] = "stdev";
	        buffer[index++] = "min";
	        buffer[index++] = "max";
	        w.writeNext(buffer);
	        
	        for (Pair<Job, GraphRunTimes> p : runTimes) {
				
	        	index = 0;
				buffer[index++] = DatabaseEngine.ENGINES.get(p.getFirst().getDbEngine()).getLongName();
				buffer[index++] = (p.getFirst().getDbInstance() == null ? "<default>" : p.getFirst().getDbInstance());
				
				String label = "";
				if (!sameDbEngine) label += buffer[0];
				if (!sameDbInstance) {
					if (!sameDbEngine) label += " : ";
					label += buffer[1];
				}
				buffer[index++] = label;
			
				GraphRunTimes r = p.getSecond();
				buffer[index++] = Double.toString(r.getMean());
				buffer[index++] = Double.toString(r.getStdev());
				buffer[index++] = Double.toString(r.getMin());
				buffer[index++] = Double.toString(r.getMax());
				
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
