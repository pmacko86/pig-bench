package com.tinkerpop.bench.web;

import java.io.IOException;
import java.util.LinkedList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * A servlet for controlling the job list
 * 
 * @author Peter Macko (pmacko@eecs.harvard.edu)
 */
@SuppressWarnings("serial")
public class JobListControl extends HttpServlet {
	
	
	/**
	 * Create an instance of class JobListControl
	 */
	public JobListControl() {
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
		JobList jobList = JobList.getInstance();
		
		// Get the action and the jobs
		
		String action = WebUtils.getStringParameter(request, "action");
		
		String[] jobIds = WebUtils.getStringParameterValues(request, "jobs");
		LinkedList<Job> jobs = new LinkedList<Job>();
		
		if (jobIds != null) {
			for (String j : jobIds) {
				jobs.add(JobList.getInstance().getJob(Integer.parseInt(j)));
			}
		}

		
		// Action: Remove tasks
		
		if ("remove".equals(action)) {
			for (Job j : jobs) {
				if (j.isRunning()) continue;
				jobList.removeJob(j);
			}
		}
		
		
		// Action: Remove all tasks
		
		if ("remove_all".equals(action)) {
			jobs.addAll(jobList.getJobs());
			for (Job j : jobs) {
				if (j.isRunning()) continue;
				jobList.removeJob(j);
			}
		}
		
		
		// Action: Remove all finished tasks
		
		if ("remove_finished".equals(action)) {
			jobs.addAll(jobList.getJobs());
			for (Job j : jobs) {
				if (j.getExecutionCount() > 0) jobList.removeJob(j);
			}
		}
		
		
		// Action: Move to the bottom
		
		if ("move_to_bottom".equals(action)) {
			for (Job j : jobs) {
				if (j.isRunning() || j.getExecutionCount() > 0) continue;
				jobList.moveToBottom(j);
			}
		}
		
		
		// Action: Duplicate
		
		if ("duplicate".equals(action)) {
			for (Job j : jobs) {
				jobList.addJob(j.duplicate());
			}
		}
		
		
		// Finish
		
		response.sendRedirect("/index.jsp");
	}
}
