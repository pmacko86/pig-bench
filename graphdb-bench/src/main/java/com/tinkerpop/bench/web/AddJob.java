package com.tinkerpop.bench.web;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * A servlet for adding a job
 * 
 * @author Peter Macko (pmacko@eecs.harvard.edu)
 */
@SuppressWarnings("serial")
public class AddJob extends HttpServlet {
	
	
	/**
	 * Create an instance of class AddJob
	 */
	public AddJob() {
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
		// The case when a single database engine is specified
		
		String dbEngine = WebUtils.getStringParameter(request, "database_engine");
		if (dbEngine != null) {
			Job job = new Job(request);
			JobList.getInstance().addJob(job);
		}

		
		// The case when multiple database engine/instances are specified
		
		String[] pairs = WebUtils.getStringParameterValues(request, "database_engine_instance");
		if (pairs != null) {
			for (String p : pairs) {
				int d = p.indexOf('|');
				Job job = new Job(request, p.substring(0, d), p.substring(d+1));
				JobList.getInstance().addJob(job);
			}
		}
		
		
		// Redirect
		
		response.sendRedirect("/index.jsp");
    }
}
