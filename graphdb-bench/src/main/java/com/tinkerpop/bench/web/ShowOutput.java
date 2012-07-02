package com.tinkerpop.bench.web;

import java.io.IOException;
import java.util.LinkedList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * A servlet for showing output of a given job
 * 
 * @author Peter Macko (pmacko@eecs.harvard.edu)
 */
@SuppressWarnings("serial")
public class ShowOutput extends HttpServlet {
	
	
	/**
	 * Create an instance of class ShowOutput
	 */
	public ShowOutput() {
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
		
		// Get the jobs
		
		String[] jobIds = WebUtils.getStringParameterValues(request, "jobs");
		LinkedList<Job> jobs = new LinkedList<Job>();
		
		if (jobIds != null) {
			for (String j : jobIds) {
				jobs.add(JobList.getInstance().getJob(Integer.parseInt(j)));
			}
		}
		
		String jobId = WebUtils.getStringParameter(request, "job");
		if (jobId != null) {
			jobs.add(JobList.getInstance().getJob(Integer.parseInt(jobId)));
		}
		
		
		// Other parameters
		
		String sJoin = WebUtils.getStringParameter(request, "join");
		boolean join = false;
		if (sJoin != null) {
			join = (sJoin.equalsIgnoreCase("true") || sJoin.equalsIgnoreCase("t"));
		}
		
		
		// Start the response
		
        response.setContentType("text/plain");
        response.setStatus(HttpServletResponse.SC_OK);
		
		
		// Attach to the jobs
		
		int count = 0;
		boolean allFinished = true;
		
		for (Job job : jobs) {
			
			if (count > 0) {
				response.getWriter().println("\n");
			}
			
			count++;
			
	        
			// Pretty-print the command-line
			
			response.getWriter().println(job.toMultilineString(false));
			response.flushBuffer();
	        
	        
	        // Show the output
	        
			try {
				
				boolean started = true;
				
				if (join) {
					Listener l = new Listener(response);
					job.addJobOutputListenerToCurrent(l);
					started = job.join() || job.getExecutionCount() > 0;
				}
				else {
					String output = job.getOutput();
					
					if (output != null) {
						response.getWriter().print(output);
					}
					else {
						started = false;
					}
					response.getWriter().flush();
				}
				
				if (started) {
					if (join || !job.isRunning()) {
						if (job.getLastStatus() == 0) {
							response.getWriter().println("\nDone.");
						}
						else {
							response.getWriter().println("\nFailed -- terminated with exit code " + job.getLastStatus());
						}
					}
					else {
						allFinished = false;
						if (jobs.size() > 1) response.getWriter().println("\nStill running...");
					}
				}
				else {
					allFinished = false;
					response.getWriter().println("Not yet executed.");					
				}
			}
			catch (Exception e) {
				response.getWriter().println("\nFailed:");
				e.printStackTrace(response.getWriter());
			}
		}
		
		if (allFinished && !jobs.isEmpty()) response.getWriter().println("\nAll jobs finished.");
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
