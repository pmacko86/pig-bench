package com.tinkerpop.bench.web;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
import com.tinkerpop.bench.analysis.AnalysisContext;


/**
 * A servlet for showing 3d surface plots 
 * 
 * @author Peter Macko (pmacko@eecs.harvard.edu)
 */
@SuppressWarnings("serial")
public class Show3dPlot extends HttpServlet {
	
	/**
	 * Create an instance of class Show3dPlot
	 */
	public Show3dPlot() {
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
					SortedSet<Job> j = AnalysisContext.getInstance(dbei).getJobs(operationName);
					if (j != null && !j.isEmpty()) jobs.add(j.last());
				}
			}
			
			operationsToJobs.put(operationName, jobs);
		}
		
		
		// Columns
		
		String[] columns = WebUtils.getStringParameterValues(request, "columns");
		if (columns == null) columns = new String[] { "result[3]", "result[0]", "time_ms" };
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
		
		boolean convertManyOperations = WebUtils.getBooleanParameter(request, "convert_many_operations", false);
		
		
		// Get the writer, generate the plot, and print it
		
		OutputStream out = response.getOutputStream();
		print3dPlot(out, operationsToJobs, columns, convertManyOperations, response);
	}
	
	
	/**
	 * Print the data
	 * 
	 * @param out the output stream
	 * @param operationsToJobs the map of operation names to the jobs
	 * @param columns the columns
	 * @param convertManyOperations whether convert the "Many" operations into the single operations
	 * @param response the response, or null if none
	 */
	public static void print3dPlot(OutputStream out, Map<String, Collection<Job>> operationsToJobs,
			String[] columns, boolean convertManyOperations, HttpServletResponse response) {
		
		if (columns.length != 3) {
			if (response != null) {
		        response.setContentType("text/plain");
		        response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
				PrintWriter writer = new PrintWriter(out);
				writer.println("Error: Invalid number of columns -- must be 3");
				writer.flush();
				return;
			}
			else throw new IllegalArgumentException("Invalid number of columns -- must be 3");
		}

		
		// Create a temp file with the data
		
		File dataFile = null;
		File gnuplotFile = null;
		
		try {
			dataFile = File.createTempFile("graphdb-bench-data", ".txt");
			dataFile.deleteOnExit();
			
			PrintWriter w = new PrintWriter(new FileWriter(dataFile));
			ShowCustomData.printData(w, operationsToJobs, columns, " ", false, convertManyOperations, -1, 0.01, null);
			w.close();
			
			gnuplotFile = File.createTempFile("graphdb-bench-data", ".plt");
			gnuplotFile.deleteOnExit();
			
			w = new PrintWriter(new FileWriter(gnuplotFile));
			w.println("set terminal png");
			//w.println("set terminal postscript enhanced color");
			w.println("set output");
			w.println("set hidden3d");
			w.println("set logscale xy");
			w.println("set xlabel \"" + ShowCustomData.getColumnLabel(columns[0]) + "\"");
			w.println("set ylabel \"" + ShowCustomData.getColumnLabel(columns[1]) + "\"");
			w.println("set zlabel \"" + ShowCustomData.getColumnLabel(columns[2]) + "\"");
			w.println();
			w.println("splot '" + dataFile.getAbsolutePath() + "' with points");
			w.close();
		}
		catch (IOException e) {
			if (response != null) {
		        response.setContentType("text/plain");
		        response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
				PrintWriter writer = new PrintWriter(out);
				writer.println("Error: " + e.getMessage());
				writer.flush();
				return;
			}
			else throw new RuntimeException(e);
		}
		
		
		// Run gnuplot
		
		ProcessBuilder pb = new ProcessBuilder("gnuplot", gnuplotFile.getAbsolutePath());
		pb.redirectErrorStream(true);
		
		try {
			
			Process p = pb.start();
			InputStream in = p.getInputStream();
			byte[] b = new byte[64 * 1024];
			int l;
			
			int r = p.waitFor();
			if (r != 0) {
				if (response != null) {
					response.setContentType("text/plain");
					response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
				}
			}
			else {
				if (response != null) {
					response.setContentType("image/png");
					//response.setContentType("application/postscript");
					response.setStatus(HttpServletResponse.SC_OK);
				}
			}
			
			while ((l = in.read(b)) > 0) {
				out.write(b, 0, l);
			}
			
			in.close();
			out.flush();
			
			dataFile.delete();
			gnuplotFile.delete();
			
			if (r != 0) {
				if (response == null) {
					throw new RuntimeException("gnuplot failed with error " + r);
				}
			}
		}
		catch (Exception e) {
			if (response != null) {
		        response.setContentType("text/plain");
		        response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
				PrintWriter writer = new PrintWriter(out);
				writer.println("Error: " + e.getMessage());
				writer.flush();
				return;
			}
			else throw new RuntimeException(e);
		}
	}
}
