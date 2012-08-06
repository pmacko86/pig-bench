package com.tinkerpop.bench.web;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * A servlet for controlling the job execution
 * 
 * @author Peter Macko (pmacko@eecs.harvard.edu)
 */
@SuppressWarnings("serial")
public class JobExecutionControl extends HttpServlet {
	
	
	/**
	 * Create an instance of class JobExecutionControl
	 */
	public JobExecutionControl() {
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
		// Check permissions
		
		if (!WebUtils.isJobControlEnabled(response)) return;

		
		// Get the action
		
		String action = WebUtils.getStringParameter(request, "action");
		
		
		// Action: Start tasks
		
		if ("start".equals(action)) {
			JobList.getInstance().start();
			
			try {
				Thread.sleep(25);
			} 
			catch (InterruptedException e) {
				// Silently ignore
			}
		}
		
		
		// Action: Pause tasks
		
		if ("pause".equals(action)) {
			JobList.getInstance().pause();
		}
		
		
		// Finish
		
		response.sendRedirect("/index.jsp");
	}
}
