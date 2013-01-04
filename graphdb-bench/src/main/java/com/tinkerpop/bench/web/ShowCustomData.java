package com.tinkerpop.bench.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import scala.actors.threadpool.Arrays;

import com.tinkerpop.bench.DatabaseEngine;
import com.tinkerpop.bench.analysis.AnalysisContext;
import com.tinkerpop.bench.analysis.AnalysisUtils;
import com.tinkerpop.bench.analysis.ModelAnalysis;
import com.tinkerpop.bench.log.OperationLogEntry;
import com.tinkerpop.bench.log.OperationLogReader;
import com.tinkerpop.bench.util.OutputUtils;
import com.tinkerpop.bench.util.Triple;
import com.tinkerpop.blueprints.Direction;


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
		
		String[] a_operationNames = WebUtils.getStringParameterValues(request, "operations");
		@SuppressWarnings("unchecked")
		List<String> operationNames = a_operationNames == null ? Collections.EMPTY_LIST : Arrays.asList(a_operationNames);
		
		
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
		if (delimiter == null) delimiter = "\t";
		
		boolean printHeader = WebUtils.getBooleanParameter(request, "header", false);
		boolean convertManyOperations = WebUtils.getBooleanParameter(request, "convert_many_operations", false);
		
		
		// Presets
		
		String preset = WebUtils.getStringParameter(request, "preset");
		if (preset != null) {
			if (preset.startsWith("margo")) {
				
				columns = new String[] { "#bname", "#type", "#edgelabels", "#size", "opname", "direction",
						"k", "#nodes", "#unique", "#neighborhoods", "time_ms" };
				
				dbeis = new ArrayList<DatabaseEngineAndInstance>(WebUtils.getAllDatabaseEnginesAndInstances());
				operationNames = new ArrayList<String>();
				convertManyOperations = true;
				printHeader = true;
				delimiter = ",";
				
				if (preset.equals("margo-khops")) {
					for (Direction d : ModelAnalysis.DIRECTIONS) {
						String dt = OutputUtils.toTag(d);
						operationNames.add("OperationGetAllNeighbors-" + dt);
						for (int k = 1; k <= 5; k++) {
							operationNames.add("OperationGetKHopNeighbors-" + dt + "-" + k);
						}
					}
				}
				
				else if (preset.equals("margo-sp")) {
					operationNames.add("OperationGetShortestPath");
				}

				else {
			        response.setContentType("text/plain");
			        response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
					PrintWriter writer = response.getWriter();
					writer.println("Error: Invalid preset " + preset);
					return;	
				}
			}
			else {
		        response.setContentType("text/plain");
		        response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
				PrintWriter writer = response.getWriter();
				writer.println("Error: Invalid preset " + preset);
				return;	
			}
		}
		
		
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
					SortedSet<Job> j = AnalysisContext.getInstance(dbei).getJobs(operationName);
					if (j != null && !j.isEmpty()) jobs.add(j.last());
				}
			}
			
			operationsToJobs.put(operationName, jobs);
		}
		
		
		// Get the writer and write out the log file
		
		PrintWriter writer = response.getWriter();
		printData(writer, operationsToJobs, columns, delimiter, printHeader, convertManyOperations, 10000, 0.01, response);
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
	 * @param tail return at most this number of runs (use 0 or a negative number to disable)
	 * @param dropExtremes the fraction of extreme top and bottom values to drop (use a negative number to disable)
	 * @param response the response, or null if none
	 */
	public static void printData(PrintWriter writer, Map<String, Collection<Job>> operationsToJobs,
			String[] columns, String delimiter, boolean printHeader, boolean convertManyOperations,
			int tail, double dropExtremes, HttpServletResponse response) {

		
		// Get the data for each operation
		
		ArrayList<Triple<String, Job, OperationLogEntry>> operationsJobsEntries
			= new ArrayList<Triple<String, Job, OperationLogEntry>>();
		ArrayList<Triple<String, Job, OperationLogEntry>> currentJobsEntries
			= new ArrayList<Triple<String, Job, OperationLogEntry>>();
		
		for (String operationName : operationsToJobs.keySet()) {
			for (Job job : operationsToJobs.get(operationName)) {
				currentJobsEntries.clear();
				
				OperationLogReader reader = new OperationLogReader(job.getLogFile(), operationName);
				for (OperationLogEntry e : reader) {
					if (e.getName().equals(operationName)) {
						
						if (convertManyOperations && AnalysisUtils.isManyOperation(operationName)) {
							e = AnalysisUtils.convertLogEntryForManyOperation(e);
						}
						
						String s = e.getName();
						currentJobsEntries.add(new Triple<String, Job, OperationLogEntry>(s, job, e));
					}
				}
				
				if (tail > 0 && currentJobsEntries.size() > tail) {
					ArrayList<Triple<String, Job, OperationLogEntry>> a
						= new ArrayList<Triple<String, Job, OperationLogEntry>>();
					for (int i = currentJobsEntries.size() - tail; i < currentJobsEntries.size(); i++) {
						a.add(currentJobsEntries.get(i));
					}
					currentJobsEntries = a;
				}

				if (dropExtremes > 0 && currentJobsEntries.size() > 0) {
					
					long[] a = new long[currentJobsEntries.size()];
					for (int i = 0; i < a.length; i++) a[i] = currentJobsEntries.get(i).getThird().getTime();
					Arrays.sort(a);
					long min = a[(int) (dropExtremes * a.length)];
					long max = a[(int) ((1 - dropExtremes) * a.length)];
					
					ArrayList<Triple<String, Job, OperationLogEntry>> l
						= new ArrayList<Triple<String, Job, OperationLogEntry>>();
					for (Triple<String, Job, OperationLogEntry> t : currentJobsEntries) {
						long v = t.getThird().getTime();
						if (v >= min && v <= max) l.add(t);
					}
					currentJobsEntries = l;
				}
				
				operationsJobsEntries.addAll(currentJobsEntries);
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
		
		int row_index = -1;
		
		for (Triple<String, Job, OperationLogEntry> data : operationsJobsEntries) {
			OperationLogEntry entry = data.getThird();
			Job job = data.getSecond();
			DatabaseEngineAndInstance dbei = job.getDatabaseEngineAndInstance();
			row_index++;
		
			for (int i = 0; i < columns.length; i++) {
				if (i != 0) writer.print(delimiter);
				String column = columns[i];
				
				
				// Column name aliases
				
				if      ("#unique"       .equalsIgnoreCase(column)) column = "result[0]";
				else if ("#depth"        .equalsIgnoreCase(column)) column = "result[1]";
				else if ("#neighborhoods".equalsIgnoreCase(column)) column = "result[2]";
				else if ("#nodes"        .equalsIgnoreCase(column)) column = "result[3]";
				
				if (entry.getName().startsWith("OperationGetShortestPath")) {
					if ("k".equalsIgnoreCase(column) ||  "#length".equalsIgnoreCase(column)) column = "result[1]";
				}
				
				if (entry.getName().startsWith("OperationLocalClusteringCoefficient")) {
					if ("k".equalsIgnoreCase(column)) column = "result[1]";
				}
				
				
				
				// Depending on the column...
				
				if      ("name"       .equalsIgnoreCase(column)) writer.print(entry.getName());
				else if ("opname"     .equalsIgnoreCase(column)) writer.print(entry.getName());
				else if ("optype"     .equalsIgnoreCase(column)) writer.print(entry.getType());
				else if ("opid"       .equalsIgnoreCase(column)) writer.print(entry.getOpId());
				else if ("#bname"     .equalsIgnoreCase(column)) writer.print(dbei.getEngine().getLongName());
				else if ("#type"      .equalsIgnoreCase(column) || "type".equalsIgnoreCase(column)) {
					String s = dbei.getInstanceSafe("default");
					if (s.startsWith("b") && s.length() >= 2 && Character.isDigit(s.charAt(1))) writer.print("b");
					else if (s.startsWith("k") && s.length() >= 2 && Character.isDigit(s.charAt(1))) writer.print("k");
					else if (s.startsWith("amazon")) writer.print("amazon");
					else writer.print(s);
				}
				else if ("#edgelabels".equalsIgnoreCase(column) || "edgelabels".equalsIgnoreCase(column)) {
					String s = dbei.getInstanceSafe("");
					String[] t = s.split("_"); String el = t[t.length-1];
					writer.print(el.equals("1el") || el.equals("2el") ? el : "none");
				}
				else if ("#size"      .equalsIgnoreCase(column)) {
					String s = dbei.getInstanceSafe(""); String[] t = s.split("_");
					if (s.startsWith("b") && s.length() >= 2 && Character.isDigit(s.charAt(1))) writer.print(t[0].substring(1));
					else if (s.startsWith("k") && s.length() >= 2 && Character.isDigit(s.charAt(1))) writer.print(t[0].substring(1));
					else if (s.startsWith("amazon")) writer.print(s.substring("amazon".length()));
					else writer.print("none");
				}
				else if ("direction"  .equalsIgnoreCase(column)) {
					String s = entry.getName();
					if (s.contains("-in")) writer.print("in");
					else if (s.contains("-out")) writer.print("out");
					else if (s.contains("-both")) writer.print("both");
					else writer.print("none");
				}
				else if ("k"          .equalsIgnoreCase(column)) {
					String s = entry.getName(); String[] t = s.split("-"); String k = "none";
					for (String x : t) if (x.length() == 1 && Character.isDigit(x.charAt(0))) { k = x; break; }
					if (s.startsWith("OperationGetAllNeighbors")) writer.print("0");
					else if (s.startsWith("OperationGetKHopNeighbors")) writer.print(k);
					else writer.print("none");
				}
				else if ("time_ns"    .equalsIgnoreCase(column)) writer.print(entry.getTime());
				else if ("time_ms"    .equalsIgnoreCase(column)) writer.print(entry.getTime() / 1000000.0);
				else if ("time_s"     .equalsIgnoreCase(column)) writer.print(entry.getTime() / 1000000000.0);
				else if ("result"     .equalsIgnoreCase(column)) writer.print(entry.getResult());
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
			if (row_index % 100 == 0) writer.flush();
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
		if      ("opname"   .equalsIgnoreCase(column)) return "Operation Name";
		else if ("optype"   .equalsIgnoreCase(column)) return "Operation Type";
		else if ("opid"     .equalsIgnoreCase(column)) return "Operation ID";
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
