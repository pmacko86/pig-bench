package com.tinkerpop.bench.web;

import java.io.IOException;
import java.util.LinkedList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * A servlet for running single benchmarks
 * 
 * @author Peter Macko (pmacko@eecs.harvard.edu)
 */
@SuppressWarnings("serial")
public class RunBenchmark extends HttpServlet {
	
	
	/**
	 * Create an instance of class RunBenchmark
	 */
	public RunBenchmark() {
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
		
		// Create and/or get the jobs
		
		String[] jobIds = WebUtils.getStringParameterValues(request, "jobs");
		LinkedList<Job> jobs = new LinkedList<Job>();
		
		if (jobIds != null) {
			for (String j : jobIds) {
				jobs.add(JobList.getInstance().getJob(Integer.parseInt(j)));
			}
		}
		
		String dbEngine = WebUtils.getStringParameter(request, "database_engine");
		if (dbEngine != null) {
			jobs.add(new Job(request));
		}
		
		String[] pairs = WebUtils.getStringParameterValues(request, "database_engine_instance");
		if (pairs != null) {
			for (String p : pairs) {
				int d = p.indexOf('|');
				Job job = new Job(request, p.substring(0, d), p.substring(d+1));
				jobs.add(job);
			}
		}

		
		// Start the response
		
        response.setContentType("text/plain");
        response.setStatus(HttpServletResponse.SC_OK);
		
		
		// Execute the jobs
		
		int count = 0;
		for (Job job : jobs) {
			
			if (count > 0) {
				response.getWriter().println("\n");
			}
			
			count++;
			
	        
			// Pretty-print the command-line
			
			response.getWriter().println(job.toMultilineString(false));
			response.flushBuffer();
	        
	        
	        // Execute the program and capture the output
	        
			try {
				Listener l = new Listener(response);
				
				job.start();
				job.addJobOutputListenerToCurrent(l);
				job.join();
				
				if (job.getLastStatus() == 0) {
					response.getWriter().println("\nDone.");
				}
				else {
					response.getWriter().println("\nFailed -- terminated with exit code " + job.getLastStatus());
				}
			}
			catch (Exception e) {
				response.getWriter().println("\nFailed:");
				e.printStackTrace(response.getWriter());
			}
		}
		
		response.getWriter().println("\nAll jobs finished.");
	}
	
	
	/**
	 * The listener class
	 */
	private class Listener implements JobOutputListener {
		
		private HttpServletResponse response;
		
		
		/**
		 * Create an instance of class Listener
		 * 
		 * @param response the HTTP response
		 * @throws IOException 
		 */
		public Listener(HttpServletResponse response) {
			this.response = response;
		}
		

		/**
		 * Callback for receiving what the job printed to its output
		 * 
		 * @param str the string printed to the output since the last time the method was called
		 * @return true to continue receiving future output or false to detach
		 */
		@Override
		public boolean jobOutput(String str) {
			try {
				response.getWriter().print(str);
				response.getWriter().flush();
				return true;
			}
			catch (IOException e) {
				try {
					response.getWriter().println("\nFailed:");
					e.printStackTrace(response.getWriter());
				}
				catch (IOException _e) {
				}							
				return false;	// Detach
			}
		}
	}
}
