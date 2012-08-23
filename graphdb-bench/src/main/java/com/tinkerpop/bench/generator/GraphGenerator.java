package com.tinkerpop.bench.generator;

import com.tinkerpop.blueprints.Graph;

import edu.harvard.pass.cpl.CPLObject;

/**
 * Abstract, stateless graph generator
 * 
 * @author Peter Macko <pmacko@eecs.harvard.edu>
 */
public abstract class GraphGenerator {
	
	/**
	 * Constructor for the objects of type Graph
	 * 
	 * @param graph the graph
	 */
	public GraphGenerator() {
	}
	
	
	/**
	 * Generate the graph
	 * 
	 * @param graph the graph
	 */
	public abstract void generate(Graph graph);
	
	
	/**
	 * Return (or create) the generator's CPL object
	 * 
	 * @return the CPL object
	 */
	public abstract CPLObject getCPLObject();
	
	
	/**
	 * Return a colon-separated (:) list of key-value pairs with additional
	 * statistics about the last graph generation process
	 * 
	 * @return the statistics string
	 */
	public abstract String getStatisticsString();
}
