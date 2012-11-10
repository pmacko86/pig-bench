package com.tinkerpop.bench.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tinkerpop.bench.DatabaseEngine;
import com.tinkerpop.bench.analysis.AnalysisUtils;
import com.tinkerpop.bench.analysis.ModelAnalysis;
import com.tinkerpop.bench.log.OperationLogEntry;
import com.tinkerpop.bench.log.OperationLogReader;
import com.tinkerpop.bench.util.Triple;


/**
 * A servlet for showing data using customizable display and selection options 
 * 
 * @author Peter Macko (pmacko@eecs.harvard.edu)
 */
@SuppressWarnings("serial")
public class ShowCustomData extends HttpServlet {
	
	/**
	 * Create an instance of class ShowCustomData
	 */
	public ShowCustomData() {
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
		
		// Get the database engine name / instance name pairs (optional)
		
		List<DatabaseEngineAndInstance> dbeis = new ArrayList<DatabaseEngineAndInstance>();
		
		String[] pairs = WebUtils.getStringParameterValues(request, "database_engine_instance");
		if (pairs != null) {
			for (String p : pairs) {
				int d = p.indexOf('|');
				dbeis.add(new DatabaseEngineAndInstance(DatabaseEngine.ENGINES.get(p.substring(0, d)), p.substring(d+1)));
			}
		}
		
		pairs = WebUtils.getStringParameterValues(request, "dbei");
		if (pairs != null) {
			for (String p : pairs) {
				int d = p.indexOf('|');
				dbeis.add(new DatabaseEngineAndInstance(DatabaseEngine.ENGINES.get(p.substring(0, d)), p.substring(d+1)));
			}
		}
		
		
		// The operation names
		
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
			else {
				for (DatabaseEngineAndInstance dbei : dbeis) {
					SortedSet<Job> j = ModelAnalysis.getInstance(dbei).getJobs(operationName);
					if (j != null && !j.isEmpty()) jobs.add(j.last());
				}
			}
			
			operationsToJobs.put(operationName, jobs);
		}
		
		
		// Columns
		
		String[] columns = WebUtils.getStringParameterValues(request, "columns");
		if (columns == null) columns = new String[] { "name", "time_ms" };
		List<String> cl = new ArrayList<String>();
		for (String c : columns) {
			if (c.contains(",")) {
				for (String s : c.split(",")) cl.add(s);
			}
			else {
				cl.add(c);
			}
		}
		columns = cl.toArray(new String[0]);
		
		
		// Other parameters
		
		String delimiter = WebUtils.getStringParameter(request, "delimiter");
		if (delimiter == null) delimiter = " ";
		
		boolean printHeader = WebUtils.getBooleanParameter(request, "print_header", false);
		boolean convertManyOperations = WebUtils.getBooleanParameter(request, "convert_many_operations", false);
		
		
		// Get the writer and write out the log file
		
		PrintWriter writer = response.getWriter();
		printData(writer, operationsToJobs, columns, delimiter, printHeader, convertManyOperations, response);
	}
	
	
	/**
	 * Print the data
	 * 
	 * @param writer the writer
	 * @param operationsToJobs the map of operation names to the jobs
	 * @param columns the columns
	 * @param delimiter the column delimiters
	 * @param printHeader true to print header
	 * @param convertManyOperations whether convert the "Many" operations into the single operations
	 * @param response the response, or null if none
	 */
	public static void printData(PrintWriter writer, Map<String, Collection<Job>> operationsToJobs,
			String[] columns, String delimiter, boolean printHeader, boolean convertManyOperations,
			HttpServletResponse response) {

		
		// Get the data for each operation
		
		ArrayList<Triple<String, Job, OperationLogEntry>> operationsJobsEntries
			= new ArrayList<Triple<String, Job, OperationLogEntry>>();
		
		for (String operationName : operationsToJobs.keySet()) {
			for (Job job : operationsToJobs.get(operationName)) {
				
				OperationLogReader reader = new OperationLogReader(job.getLogFile(), operationName);
				for (OperationLogEntry e : reader) {
					if (e.getName().equals(operationName)) {
						
						if (convertManyOperations && AnalysisUtils.isManyOperation(operationName)) {
							e = AnalysisUtils.convertLogEntryForManyOperation(e);
						}
						
						String s = e.getName();
						operationsJobsEntries.add(new Triple<String, Job, OperationLogEntry>(s, job, e));
					}
				}
			}
		}
		
		
		// Start the response
	
		if (response != null) {
	        response.setContentType("text/plain");
	        response.setStatus(HttpServletResponse.SC_OK);
		}
		
		
		// Print the header
		
		if (printHeader) {
			for (int i = 0; i < columns.length; i++) {
				if (i != 0) writer.print(delimiter);
				writer.print(columns[i]);
			}
			writer.println();
		}
		
		
		// Print the data
		
		for (Triple<String, Job, OperationLogEntry> data : operationsJobsEntries) {
			
			for (int i = 0; i < columns.length; i++) {
				if (i != 0) writer.print(delimiter);
				String column = columns[i];
				OperationLogEntry entry = data.getThird();
				
				if      ("name"     .equalsIgnoreCase(column)) writer.print(entry.getName());
				else if ("type"     .equalsIgnoreCase(column)) writer.print(entry.getType());
				else if ("time_ns"  .equalsIgnoreCase(column)) writer.print(entry.getTime());
				else if ("time_ms"  .equalsIgnoreCase(column)) writer.print(entry.getTime() / 1000000.0);
				else if ("time_s"   .equalsIgnoreCase(column)) writer.print(entry.getTime() / 1000000000.0);
				else if ("result"   .equalsIgnoreCase(column)) writer.print(entry.getResult());
				else if (column.startsWith("result")) {
					String r = column.substring("result".length());
					if (r.startsWith("[") && r.endsWith("]")) r = r.substring(1, r.length() - 1);
					int index = Integer.parseInt(r);
					String[] l = entry.getResult().split(":");
					if (index < 0 || index >= l.length) {
						writer.println();
						writer.println();
						writer.println("Error: Invalid result index " + r);
						writer.println("       The unparsed result is " + entry.getResult());
						writer.flush();
						if (response == null) throw new IllegalArgumentException("Invalid result index: " + r);
						return;
					}
					writer.print(l[index]);
				}
				else {
					writer.println();
					writer.println();
					writer.println("Error: Invalid column " + column);
					writer.flush();
					if (response == null) throw new IllegalArgumentException("Invalid column: " + column);
					return;
				}
			}
			
			writer.println();
		}
	}
	
	
	/**
	 * Get a label for the given column name
	 * 
	 * @param column the column name
	 * @return the label, or the column name verbatim if not known
	 */
	public static String getColumnLabel(String column) {
		if      ("name"     .equalsIgnoreCase(column)) return "Operation Name";
		else if ("type"     .equalsIgnoreCase(column)) return "Operation Type";
		else if ("time_ns"  .equalsIgnoreCase(column)) return "Time (ns)";
		else if ("time_ms"  .equalsIgnoreCase(column)) return "Time (ms)";
		else if ("time_s"   .equalsIgnoreCase(column)) return "Time (s)";
		else if ("result"   .equalsIgnoreCase(column)) return "Result";
		else if (column.startsWith("result")) {
			String r = column.substring("result".length());
			if (r.startsWith("[") && r.endsWith("]")) r = r.substring(1, r.length() - 1);
			switch (Integer.parseInt(r)) {
			case 0: return "Returned Unique Nodes";
			case 1: return "Maximum Depth";
			case 2: return "Retrieved Neighborhoods";
			case 3: return "Retrieved Nodes";
			case 4: return "Operation Result";
			default: return "Result[" + r + "]";
			}
		}
		else {
			return column;
		}
	}
}
