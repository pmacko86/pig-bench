package com.tinkerpop.bench.web;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

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
		= Pattern.compile("[_a-zA-Z0-9\\-][_a-zA-Z0-9\\-\\.]*");

	private static Pattern dbInstancePattern
		= Pattern.compile("^[a-z][a-z0-9_]*$");
	
	
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
	 * Get a value from the parameter list
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

		String dirName = Bench.getProperty(Bench.RESULTS_DIRECTORY) + "/Micro"; /* TODO Do this for real */

		File dir = new File(dirName);
		if (!dir.exists())
			throw new RuntimeException("The results directory does not exist");
		if (!dir.isDirectory())
			throw new RuntimeException("The results directory is not really a directory");

		return dir;
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
			
			if (name.endsWith(".graphml")) {
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
	 * Get a collection of previously executed jobs
	 * 
	 * @param dbEngine the database engine name
	 * @param dbInstance the database instance name
	 * @return the collection of previously executed jobs
	 */
	public static Collection<Job> getFinishedJobs(String dbEngine, String dbInstance) {
		
		LinkedList<Job> r = new LinkedList<Job>();
		File resultsDir = getResultsDirectory();
		
		String subdir = dbEngine;
		if (dbInstance != null && !"".equals(dbInstance)) {
			asssertDatabaseInstanceNameValidity(dbInstance);
			subdir += "_" + dbInstance;
		}
		String logFilePrefix = subdir + "_";
		
		File dir = new File(resultsDir, subdir);
		if (!dir.exists())
			throw new RuntimeException("The specific results directory does not exist");
		if (!dir.isDirectory())
			throw new RuntimeException("The specific results directory is not really a directory");
		
		for (File f : dir.listFiles()) {
			if (f.isDirectory()) continue;
			String name = f.getName();
			
			if (name.startsWith(logFilePrefix) && name.endsWith(".csv")) {
				r.add(new Job(f, dbEngine, dbInstance));
			}
		}
		
		return r;
	}
}
