package com.tinkerpop.bench.web;

import javax.servlet.http.HttpServletRequest;


/**
 * A collection of miscellaneous web-related utilities
 * 
 * @author Peter Macko (pmacko@eecs.harvard.edu)
 */
public class WebUtils {
	
	
	/**
	 * Get a value from the parameter list
	 */
	protected static String getStringParameter(HttpServletRequest request, String name) {
		String s = request.getParameter(name);
		if (s == null) return null;
		if (s.equals("")) return null;
		if (s.contains("'")) throw new RuntimeException("Invalid value");
		return s;
	}
	
	
	/**
	 * Get a value from the parameter list
	 */
	protected static String[] getStringParameterValues(HttpServletRequest request, String name) {
		String[] a = request.getParameterValues(name);
		if (a == null) return null;
		for (String s : a) {
			if (s.contains("'")) throw new RuntimeException("Invalid value");
		}
		return a;
	}
}
