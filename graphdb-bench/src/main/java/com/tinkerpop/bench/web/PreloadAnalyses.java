package com.tinkerpop.bench.web;

import java.io.IOException;
import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tinkerpop.bench.DatabaseEngine;
import com.tinkerpop.bench.analysis.IngestAnalysis;
import com.tinkerpop.bench.analysis.ModelAnalysis;
import com.tinkerpop.bench.util.Pair;


/**
 * A servlet for preloading analyses
 * 
 * @author Peter Macko (pmacko@eecs.harvard.edu)
 */
@SuppressWarnings("serial")
public class PreloadAnalyses extends HttpServlet {
	
	
	/**
	 * Create an instance of class PreloadAnalyses
	 */
	public PreloadAnalyses() {
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
		Collection<Pair<String, String>> pairs = WebUtils.getDatabaseInstancePairs();
		SortedSet<DatabaseEngineAndInstance> dbeis = new TreeSet<DatabaseEngineAndInstance>();
		
		if (pairs != null) {
			for (Pair<String, String> p : pairs) {
				dbeis.add(new DatabaseEngineAndInstance(DatabaseEngine.ENGINES.get(p.getFirst()), p.getSecond()));
			}
		}
		
		
		// Start the response
		
        response.setContentType("text/plain");
        response.setStatus(HttpServletResponse.SC_OK);


		// Run

		response.getWriter().println("Preloading Analyses:");
		response.getWriter().flush();
		response.flushBuffer();

		try {
			DatabaseEngine lastEngine = null;
			
			for (DatabaseEngineAndInstance dbei : dbeis) {
				
				if (lastEngine == null || !lastEngine.equals(dbei.getEngine())) {
					response.getWriter().println();
					lastEngine = dbei.getEngine();
				}
				
				response.getWriter().println("  " + dbei);
				response.getWriter().flush();
				response.flushBuffer();
				
				ModelAnalysis.getInstance(dbei);
				IngestAnalysis.getInstance(dbei);
			}
			
			response.getWriter().println();
			response.getWriter().println("Done.");
			response.getWriter().flush();
			response.flushBuffer();
		}
		catch (Exception e) {
			response.getWriter().println("\nFailed:");
			e.printStackTrace(response.getWriter());
		}
	}
}
