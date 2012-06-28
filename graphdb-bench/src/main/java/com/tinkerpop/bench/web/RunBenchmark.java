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
		final HttpServletResponse _response = response;
		

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
				
				job.start();
				job.addJobOutputListenerToCurrent(new JobOutputListener() {
					@Override
					public void jobOutput(String str) {
						try {
							_response.getWriter().print(str);
							_response.getWriter().flush();
						} catch (IOException e) {
							// TODO Detach the listener instead
							throw new RuntimeException(e);
						}
					}
				});
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
}
