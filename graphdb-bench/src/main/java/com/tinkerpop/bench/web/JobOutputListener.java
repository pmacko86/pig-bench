package com.tinkerpop.bench.web;

/**
 * Listener for job output
 * 
 * @author Peter Macko (pmacko@eecs.harvard.edu)
 */
public interface JobOutputListener {

	/**
	 * Callback for receiving what the job printed to its output
	 * 
	 * @param str the string printed to the output since the last time the method was called
	 */
	public void jobOutput(String str);
}
