package com.tinkerpop.bench.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tinkerpop.bench.Bench;


/**
 * A servlet for running single benchmarks
 * 
 * @author Peter Macko (pmacko@eecs.harvard.edu)
 */
@SuppressWarnings("serial")
public class RunBenchmark extends HttpServlet {
	
	private boolean buffered = false;
	
	
	/**
	 * Create an instance of class RunBenchmark
	 */
	public RunBenchmark() {
	}
	
	
	/**
	 * Get a value from the parameter list
	 */
	protected String getStringParameter(HttpServletRequest request, String name) {
		String s = request.getParameter(name);
		if (s == null) return null;
		if (s.equals("")) return null;
		if (s.contains("'")) throw new RuntimeException("Invalid value");
		return s;
	}
	
	
	/**
	 * Get a value from the parameter list
	 */
	protected String[] getStringParameterValues(HttpServletRequest request, String name) {
		String[] a = request.getParameterValues(name);
		if (a == null) return null;
		for (String s : a) {
			if (s.contains("'")) throw new RuntimeException("Invalid value");
		}
		return a;
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
		
		// Build the list of command-line arguments
		
		String dbName = getStringParameter(request, "database_name");
		String dbInstance = getStringParameter(request, "database_instance");
		String s_txBuffer = getStringParameter(request, "tx_buffer");
		String s_opCount = getStringParameter(request, "op_count");
		String s_warmupOpCount = getStringParameter(request, "warmup_op_count");
		String[] workloads = getStringParameterValues(request, "workloads");
		
		// Note: Remember to validate the input for file names when we add a support for such arguments
		
		List<String> arguments = new LinkedList<String>();
		arguments.add(Bench.graphdbBenchDir + "/runBenchmarkSuite.sh");
		arguments.add("--dumb-terminal");
		
		if (dbName           != null) { arguments.add("--" + dbName); }
		if (dbInstance       != null) { arguments.add("--database"); arguments.add(dbInstance); }
		if (s_txBuffer       != null) { arguments.add("--tx-buffer"); arguments.add(s_txBuffer); }
		if (s_opCount        != null) { arguments.add("--op-count"); arguments.add(s_opCount); }
		if (s_warmupOpCount  != null) { arguments.add("--warmup-op-count"); arguments.add(s_warmupOpCount); }
		
		if (workloads != null) {
			for (String s : workloads) {
				arguments.add("--" + s);
			}
		}
		
		
		// Start the response
		
        response.setContentType("text/plain");
        response.setStatus(HttpServletResponse.SC_OK);
		
        
		// Pretty-print the command-line
		
		boolean first = true;
		for (String s : arguments) {
			if (s.startsWith("-")) {
				response.getWriter().print("\n    ");
			}
			else {
				if (!first) response.getWriter().print(" ");
				first = false;
			}
			
			if (s.contains(" ") || s.contains("\n") || s.contains("\r") || s.contains("\t")) {
				response.getWriter().print("'" + s + "'");
			}
			else {
				response.getWriter().print(s);
			}
		}
		
		response.getWriter().println();
		response.getWriter().println();
		response.flushBuffer();
        
        
        // Execute the program and capture the output
        
		try {
			
			ProcessBuilder pb = new ProcessBuilder(arguments);
			pb.redirectErrorStream(true);
			Process p = pb.start();

			if (buffered) {
				BufferedReader es = new BufferedReader(new InputStreamReader(p.getErrorStream()));
				
				while (true) {
					String l = es.readLine();
					if (l == null) break;
					response.getWriter().println(l);
					response.flushBuffer();
				}
				
				es.close();
			}
			else {
				InputStreamReader es = new InputStreamReader(p.getInputStream());
				
				while (true) {
					int r = es.read();
					if (r < 0) break;
					response.getWriter().print((char) r);
					response.flushBuffer();
				}
	
				es.close();
			}
			
			response.getWriter().println("\nDone.");
		}
		catch (Exception e) {
			response.getWriter().println("\nFailed:");
			e.printStackTrace(response.getWriter());
		}
    }
}
