package com.tinkerpop.bench.web;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tinkerpop.bench.Bench;
import com.tinkerpop.bench.DatabaseEngine;
import com.tinkerpop.bench.util.Pair;


/**
 * A collection of miscellaneous web-related utilities
 * 
 * @author Peter Macko (pmacko@eecs.harvard.edu)
 */
public class WebUtils {

	private static Pattern fileNamePattern
		= Pattern.compile("[_a-zA-Z0-9\\-][_a-zA-Z0-9\\-\\.:=]*");

	private static Pattern dbInstancePattern
		= Pattern.compile("^[a-z][a-z0-9_]*$");
	
	
	/**
	 * Check whether a parameter is in the parameter list
	 */
	public static boolean hasParameter(HttpServletRequest request, String name) {
		String s = request.getParameter(name);
		if (s == null) return false;
		if (s.equals("")) return false;
		if (s.contains("'")) throw new RuntimeException("Invalid value");
		return true;
	}
	
	
	/**
	 * Get a value from the parameter list
	 */
	public static String getStringParameter(HttpServletRequest request, String name) {
		String s = request.getParameter(name);
		if (s == null) return null;
		if (s.equals("")) return null;
		if (s.contains("'")) throw new RuntimeException("Invalid value");
		return s;
	}
	
	
	/**
	 * Get multiple values from the parameter list
	 */
	public static String[] getStringParameterValues(HttpServletRequest request, String name) {
		String[] a = request.getParameterValues(name);
		if (a == null) return null;
		for (String s : a) {
			if (s.contains("'")) throw new RuntimeException("Invalid value");
		}
		return a;
	}
	
	
	/**
	 * Get a value from the parameter list and make sure that it passes as
	 * a simple file name without a directory that does not start with a
	 * period
	 */
	public static String getFileNameParameter(HttpServletRequest request, String name) {
		String s = request.getParameter(name);
		if (s == null) return null;
		if (s.equals("")) return null;
		if (!fileNamePattern.matcher(s).matches()) {
			throw new RuntimeException("Invalid file name");
		}
		return s;
	}
	
	
	/**
	 * Get a value from the parameter list
	 */
	public static String[] getFileNameParameterValues(HttpServletRequest request, String name) {
		String[] a = request.getParameterValues(name);
		if (a == null) return null;
		for (String s : a) {
			if (s.contains("'")) throw new RuntimeException("Invalid file name");
			if (!fileNamePattern.matcher(s).matches()) {
				throw new RuntimeException("Invalid file name");
			}
		}
		return a;
	}
	
	
	/**
	 * Get a value from the parameter list
	 */
	public static boolean getBooleanParameter(HttpServletRequest request, String name, boolean defaultValue) {
		String s = request.getParameter(name);
		if (s == null) return defaultValue;
		if (s.equals("")) return defaultValue;
		return s.equalsIgnoreCase("true") || s.equalsIgnoreCase("t");
	}
	
	
	/**
	 * Assert that the database instance name is valid
	 */
	public static void asssertDatabaseInstanceNameValidity(String dbInstance) {
		if (!dbInstancePattern.matcher(dbInstance).matches()) {
			throw new RuntimeException("Invalid database instance name (can contain only lower-case letters, "
					+ "numbers, and _, and has to start with a letter)");
		}
	}


	/**
	 * Get the datasets directory
	 *
	 * @return the directory
	 */
	public static File getDatasetsDirectory() {

		String dirName = Bench.getProperty(Bench.DATASETS_DIRECTORY); /* TODO Do this for real */

		File dir = new File(dirName);
		if (!dir.exists())
			throw new RuntimeException("The datasets directory does not exist");
		if (!dir.isDirectory())
			throw new RuntimeException("The datasets directory is not really a directory");

		return dir;
	}


	/**
	 * Get the results directory
	 *
	 * @return the directory
	 */
	public static File getResultsDirectory() {

		String dirName = Bench.getProperty(Bench.RESULTS_DIRECTORY); /* TODO Do this for real */

		File dir = new File(dirName);
		if (!dir.exists())
			throw new RuntimeException("The results directory does not exist");
		if (!dir.isDirectory())
			throw new RuntimeException("The results directory is not really a directory");

		return dir;
	}


	/**
	 * Get the results directory for a specific database name and an instance name
	 *
	 * @param dbEngine the database engine name
	 * @param dbInstance the database instance name
	 * @return the directory
	 */
	public static File getResultsDirectory(String dbEngine, String dbInstance) {

		File resultsDir = getResultsDirectory();
		
		String subdir = dbEngine;
		if (dbInstance != null && !"".equals(dbInstance)) {
			asssertDatabaseInstanceNameValidity(dbInstance);
			subdir += "_" + dbInstance;
		}
		
		File dir = new File(resultsDir, subdir);
		if (!dir.exists())
			throw new RuntimeException("The specific results directory does not exist");
		if (!dir.isDirectory())
			throw new RuntimeException("The specific results directory is not really a directory");

		return dir;
	}

	
	/**
	 * Get the file name prefix for log files
	 *
	 * @param dbEngine the database engine name
	 * @param dbInstance the database instance name
	 * @return the prefix
	 */
	public static String getLogFilePrefix(String dbEngine, String dbInstance) {

		String subdir = dbEngine;
		if (dbInstance != null && !"".equals(dbInstance)) {
			asssertDatabaseInstanceNameValidity(dbInstance);
			subdir += "_" + dbInstance;
		}

		return subdir + "-log_";
	}

	
	/**
	 * Get all file name prefix for log files, including the ones that are already deprecated
	 *
	 * @param dbEngine the database engine name
	 * @param dbInstance the database instance name
	 * @return an array of prefixes
	 */
	public static String[] getAllLogFilePrefixes(String dbEngine, String dbInstance) {

		String subdir = dbEngine;
		if (dbInstance != null && !"".equals(dbInstance)) {
			asssertDatabaseInstanceNameValidity(dbInstance);
			subdir += "_" + dbInstance;
		}

		return new String[] { subdir + "-log_", subdir + "_" };
	}

	
	/**
	 * Get the file name prefix for log files
	 *
	 * @param dbEngine the database engine name
	 * @param dbInstance the database instance name
	 * @return the prefix
	 */
	public static String getWarmupLogFilePrefix(String dbEngine, String dbInstance) {

		String subdir = dbEngine;
		if (dbInstance != null && !"".equals(dbInstance)) {
			asssertDatabaseInstanceNameValidity(dbInstance);
			subdir += "_" + dbInstance;
		}

		return subdir + "-warmup_";
	}

	
	/**
	 * Get the file name prefix for log files
	 *
	 * @param dbEngine the database engine name
	 * @param dbInstance the database instance name
	 * @return the prefix
	 */
	public static String getSummaryFilePrefix(String dbEngine, String dbInstance) {

		String subdir = dbEngine;
		if (dbInstance != null && !"".equals(dbInstance)) {
			asssertDatabaseInstanceNameValidity(dbInstance);
			subdir += "_" + dbInstance;
		}

		return subdir + "-summary_";
	}
	
	
	/**
	 * Get a collection of datasets
	 * 
	 * @return the collection of datasets - base file names
	 */
	public static Collection<String> getDatasets() {
		
		TreeSet<String> r = new TreeSet<String>();
		File dir = getDatasetsDirectory();
		
		for (File f : dir.listFiles()) {
			if (f.isDirectory()) continue;
			String name = f.getName();
			
			if (name.endsWith(".graphml") || name.endsWith(".fgf")) {
				r.add(name);
			}
		}
		
		return r;
	}
	
	
	/**
	 * Get a collection of database instances / graph names
	 * 
	 * @return the collection of database instance names, not including the default instance name
	 */
	public static Collection<String> getAllDatabaseInstanceNames() {
		
		TreeSet<String> r = new TreeSet<String>();
		File dir = getResultsDirectory();
		
		for (File f : dir.listFiles()) {
			if (!f.isDirectory()) continue;
			String name = f.getName();
			
			if (name.contains("_")) {
				int d = name.indexOf('_');
				String dbName = name.substring(0, d);
				String dbInstance = name.substring(d + 1);
				if (DatabaseEngine.ENGINES.containsKey(dbName)) {
					r.add(dbInstance);
				}
			}
		}
		
		return r;
	}
	
	
	/**
	 * Get a collection of existing database engine / instance pairs
	 * 
	 * @return the collection of database engine short names / instance name pairs
	 */
	public static Collection<Pair<String, String>> getDatabaseInstancePairs() {
		
		HashSet<Pair<String, String>> r = new HashSet<Pair<String, String>>();
		File dir = getResultsDirectory();
		
		for (File f : dir.listFiles()) {
			if (!f.isDirectory()) continue;
			String name = f.getName();
			
			if (name.contains("_")) {
				int d = name.indexOf('_');
				String dbName = name.substring(0, d);
				String dbInstance = name.substring(d + 1);
				if (DatabaseEngine.ENGINES.containsKey(dbName)) {
					r.add(new Pair<String, String>(dbName, dbInstance));
				}
			}
			else {
				if (DatabaseEngine.ENGINES.containsKey(name)) {
					r.add(new Pair<String, String>(name, ""));
				}
			}
		}
		
		return r;
	}
	
	
	/**
	 * Get a collection of existing database engine / instance pairs
	 * 
	 * @return the set of all database engine / instance name pairs, sorted first by the engine, then by the instance
	 */
	public static SortedSet<DatabaseEngineAndInstance> getAllDatabaseEnginesAndInstances() {
		
		Collection<Pair<String, String>> pairs = getDatabaseInstancePairs();
		SortedSet<DatabaseEngineAndInstance> dbeis = new TreeSet<DatabaseEngineAndInstance>();
		
		if (pairs != null) {
			for (Pair<String, String> p : pairs) {
				dbeis.add(new DatabaseEngineAndInstance(DatabaseEngine.ENGINES.get(p.getFirst()), p.getSecond()));
			}
		}
		
		return dbeis;
	}
	
	
	/**
	 * Check whether job control is enabled
	 * 
	 * @param response the response where to write an error message; null if no message should be produced
	 * @return true if enabled, false if not
	 * @throws IOException on I/O error
	 */
	public static boolean isJobControlEnabled(HttpServletResponse response) throws IOException {
		
		boolean enabled = Bench.getBooleanProperty(Bench.WEB_JOBCONTROL_ENABLE, true);
		
		if (enabled) {
			return true;
		}
		else {
			if (response != null) {
		        response.setContentType("text/plain");
		        response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
		        response.getWriter().println("No such file.");
			}
			return false;
		}
	}
}
