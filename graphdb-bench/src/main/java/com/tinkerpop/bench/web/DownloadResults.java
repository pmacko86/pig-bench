package com.tinkerpop.bench.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tinkerpop.bench.log.OperationLogReader;


/**
 * A servlet for downloading results
 * 
 * @author Peter Macko (pmacko@eecs.harvard.edu)
 */
@SuppressWarnings("serial")
public class DownloadResults extends HttpServlet {
	
	
	/**
	 * Create an instance of class DownloadResults
	 */
	public DownloadResults() {
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
		String directories = "";
		String grep = "| grep -v /db | grep -v /warmup | grep -v /org | grep -v /bad "
				+ "| grep -v /wait | grep -v /backup | grep -v /" + OperationLogReader.TAIL_CACHE_DIR;
		String command;
		
		boolean all = WebUtils.getBooleanParameter(request, "all", false);
		
		
		// Compose the tar commands depending on the download type arguments
		
		if (all) {
			command = "tar -czf - `find . -maxdepth 2 " + grep + " | grep '/.*/'`";
		}
		else {
			
			// The case when a single database engine is specified
			
			String dbEngine = WebUtils.getStringParameter(request, "database_engine");
			if (dbEngine != null) {
				String dbInstance = WebUtils.getStringParameter(request, "database_instance");
				directories += " " + WebUtils.getResultsDirectory(dbEngine, dbInstance).getName();
			}
	
			
			// The case when multiple database engine/instances are specified
			
			String[] pairs = WebUtils.getStringParameterValues(request, "database_engine_instance");
			if (pairs != null) {
				for (String p : pairs) {
					int d = p.indexOf('|');
					if (d < 0) {
						directories += " " + WebUtils.getResultsDirectory(p, null).getName();
					}
					else {
						directories += " " + WebUtils.getResultsDirectory(p.substring(0, d), p.substring(d+1)).getName();
					}
				}
			}
			
			
			// Check whether anything is selected
			
			if ("".equals(directories)) {
		        response.setContentType("text/plain");
		        response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
		        response.getWriter().println("No downloads selected.");
		        return;
	        }
			
			
			// Create the tar command
			
			command = "tar -czf - `find" + directories + " -maxdepth 1 " + grep + " | grep /`";
		}
		
		
		// Run the command
		
		ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", command);
		pb.directory(WebUtils.getResultsDirectory());
		Process p = pb.start();
		
		InputStream input = p.getInputStream();
		OutputStream output = response.getOutputStream();
		response.setContentType("application/gzip");
		response.setHeader("Content-disposition", "attachment;filename="
				+ "results-" + (new SimpleDateFormat("yyyyMMdd")).format(new Date()) + ".tar.gz");
		
		byte[] buffer = new byte[10240];
		
		for (int length = 0; (length = input.read(buffer)) > 0;) {
	        output.write(buffer, 0, length);
	    }
    }
}
