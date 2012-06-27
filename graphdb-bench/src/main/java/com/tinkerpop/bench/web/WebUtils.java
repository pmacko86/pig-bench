package com.tinkerpop.bench.web;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.TreeSet;

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
	 * Get a collection of database instances / graph names
	 * 
	 * @return the collection of database instance names, not including the default instance name
	 */
	public static Collection<String> getAllDatabaseInstanceNames() {
		
		TreeSet<String> r = new TreeSet<String>();
		
		String dirName = Bench.getProperty(Bench.RESULTS_DIRECTORY) + "/Micro"; /* TODO Do this for real */
		File dir = new File(dirName);
		if (!dir.exists()) return r;
		if (!dir.isDirectory()) return r;
		
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
		
		String dirName = Bench.getProperty(Bench.RESULTS_DIRECTORY) + "/Micro"; /* TODO Do this for real */
		File dir = new File(dirName);
		if (!dir.exists()) return r;
		if (!dir.isDirectory()) return r;
		
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
}
