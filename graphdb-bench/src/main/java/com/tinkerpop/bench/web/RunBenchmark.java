package com.tinkerpop.bench.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
	
	private boolean buffered = false;
	
	
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
				
				ProcessBuilder pb = new ProcessBuilder(job.getArguments());
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
				
				int r = p.waitFor();
				
				if (r == 0) {
					response.getWriter().println("\nDone.");
				}
				else {
					response.getWriter().println("\nFailed -- terminated with exit code " + r);
				}
				job.jobTerminated(r);
			}
			catch (Exception e) {
				response.getWriter().println("\nFailed:");
				e.printStackTrace(response.getWriter());
				job.jobTerminated(Integer.MIN_VALUE);
			}
		}
		
		response.getWriter().println("\nAll jobs finished.");
   }
}
