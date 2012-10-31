package com.tinkerpop.bench.web;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tinkerpop.bench.DatabaseEngine;


/**
 * A servlet for running analysis jobs
 * 
 * @author Peter Macko (pmacko@eecs.harvard.edu)
 */
@SuppressWarnings("serial")
public class RunAnalysis extends HttpServlet {
	
	
	/**
	 * Create an instance of class DownloadResults
	 */
	public RunAnalysis() {
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
		DatabaseEngineAndInstance dbEI = null;
		

		// The case when a single database engine is specified
		
		String dbEngine = WebUtils.getStringParameter(request, "database_engine");
		if (dbEngine != null) {
			String dbInstance = WebUtils.getStringParameter(request, "database_instance");
			dbEI = new DatabaseEngineAndInstance(DatabaseEngine.ENGINES.get(dbEngine), dbInstance);
		}
	
		
		// The case when multiple database engine/instances are specified
		
		String[] pairs = WebUtils.getStringParameterValues(request, "database_engine_instance");
		if (pairs != null) {
			for (String p : pairs) {
				int d = p.indexOf('|');
				if (dbEI != null) {
			        response.setContentType("text/plain");
			        response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
			        response.getWriter().println("More than one database engine / instance pair selected.");
			        return;
				}
				if (d < 0) {
					dbEI = new DatabaseEngineAndInstance(DatabaseEngine.ENGINES.get(p), null);
				}
				else {
					dbEI = new DatabaseEngineAndInstance(DatabaseEngine.ENGINES.get(p.substring(0, d)), p.substring(d+1));
				}
			}
		}
		
		
		// Run
		
		Job j = dbEI.getInstance() == null
				? new Job("+quiet", "--analysis", "--" + dbEI.getEngine().getShortName())
				: new Job("+quiet", "--analysis", "--" + dbEI.getEngine().getShortName(), "--database", dbEI.getInstance());
		
		j.setUnlisted(true);
		j.start();


		// Pretty-print the command-line

		response.getWriter().println(j.toMultilineString(false));
		response.flushBuffer();


		// Show the output

		try {
			Listener l = new Listener(response);
			j.addJobOutputListenerToCurrent(l);
			j.join();
			
			if (j.getLastStatus() == 0) {
				response.getWriter().println("\nDone.");
			}
			else {
				response.getWriter().println("\nFailed -- terminated with exit code " + j.getLastStatus());
			}
		}
		catch (Exception e) {
			response.getWriter().println("\nFailed:");
			e.printStackTrace(response.getWriter());
		}
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
