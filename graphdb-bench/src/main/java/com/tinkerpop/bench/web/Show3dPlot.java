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
import java.util.regex.Pattern;

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
	
	private static Pattern logScalePattern = Pattern.compile("^[xX]?[yY]?[zZ]?$");
	private static Pattern everyPattern = Pattern.compile("^[0-9]*$");
	
	
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
		
		String logScale = WebUtils.getStringParameter(request, "logscale");
		if (logScale != null) if (!logScalePattern.matcher(logScale).matches()) {
			response.setContentType("text/plain");
	        response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
			PrintWriter writer = new PrintWriter(response.getOutputStream());
			writer.println("Error: Invalid value of parameter \"logscale\"");
			writer.flush();
			return;
		}
		
		String every = WebUtils.getStringParameter(request, "every");
		if (every != null) if (!everyPattern.matcher(every).matches()) {
			response.setContentType("text/plain");
	        response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
			PrintWriter writer = new PrintWriter(response.getOutputStream());
			writer.println("Error: Invalid value of parameter \"every\"");
			writer.flush();
			return;
		}
		
		
		// Get the writer, generate the plot, and print it
		
		OutputStream out = response.getOutputStream();
		print3dPlot(out, operationsToJobs, columns, logScale, every, convertManyOperations, response);
	}
	
	
	/**
	 * Print the data
	 * 
	 * @param out the output stream
	 * @param operationsToJobs the map of operation names to the jobs
	 * @param columns the columns
	 * @param logScale which axes to be in log scale, use null or an empty string to disable
	 * @param every the value of the "every" parameter for thinning the graph, use null to disable
	 * @param convertManyOperations whether convert the "Many" operations into the single operations
	 * @param response the response, or null if none
	 */
	public static void print3dPlot(OutputStream out, Map<String, Collection<Job>> operationsToJobs,
			String[] columns, String logScale, String every, boolean convertManyOperations,
			HttpServletResponse response) {
		
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
		
		if (operationsToJobs.size() != 1) {
			// TODO Remove this restriction
			if (response != null) {
		        response.setContentType("text/plain");
		        response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
				PrintWriter writer = new PrintWriter(out);
				writer.println("Error: Invalid number of operations -- must be 1");
				writer.flush();
				return;
			}
			else throw new IllegalArgumentException("Invalid number of operations -- must be 1");
		}

		
		// Create a temp file with the data
		
		File dataFile = null;
		File gnuplotFile = null;
		
		try {
			dataFile = File.createTempFile("graphdb-bench-data", ".txt");
			dataFile.deleteOnExit();
			
			PrintWriter w = new PrintWriter(new FileWriter(dataFile));
			ShowCustomData.printData(w, operationsToJobs, columns, " ", false, convertManyOperations, 0.01, null);
			w.close();
			
			gnuplotFile = File.createTempFile("graphdb-bench-data", ".plt");
			gnuplotFile.deleteOnExit();
			
			w = new PrintWriter(new FileWriter(gnuplotFile));
			
			w.println("set terminal pngcairo enhanced font \"arial,10\"");
			w.println("set output");
			w.println("set hidden3d");
			w.println("set border 895");
			w.println("set grid z");
			w.println("set ticslevel 0");
			
			if (logScale != null && logScale.length() > 0) w.println("set logscale " + logScale.toLowerCase());
			
			w.println("set xlabel \"" + ShowCustomData.getColumnLabel(columns[0]) + "\" offset -1.5,-1");
			w.println("set ylabel \"" + ShowCustomData.getColumnLabel(columns[1]) + "\" offset 1.5,-1");
			w.println("set zlabel \"" + ShowCustomData.getColumnLabel(columns[2]) + "\" rotate by 90");
			
			w.println();
			
			w.print("splot '" + dataFile.getAbsolutePath() + "'");
			if (every != null && every.length() > 0) w.print(" every " + every);
			w.print(" with points title '" + operationsToJobs.keySet().iterator().next() + "'");
			w.println();
			
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
			
			ArrayList<byte[]> output = new ArrayList<byte[]>();
			
			while ((l = in.read(b)) > 0) {
				byte[] x = new byte[l];
				System.arraycopy(b, 0, x, 0, l);
				output.add(x);
			}
			
			int r = p.waitFor();
			
			while ((l = in.read(b)) > 0) {
				byte[] x = new byte[l];
				System.arraycopy(b, 0, x, 0, l);
				output.add(x);
			}
			
			if (r != 0) {
				if (response != null) {
					response.setContentType("text/plain");
					response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
				}
			}
			else {
				if (response != null) {
					response.setContentType("image/png");
					response.setStatus(HttpServletResponse.SC_OK);
				}
			}
			
			for (byte[] x : output) {
				out.write(x, 0, x.length);
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
