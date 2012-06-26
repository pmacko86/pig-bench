package com.tinkerpop.bench;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;


/**
 * A workload specification
 * 
 * @author Peter Macko (pmacko@eecs.harvard.edu)
 */
public class Workload {
	
	/**
	 * The set of supported workloads
	 */
	public static final Map<String, Workload> WORKLOADS;
	
	static {
		Map<String, Workload> m = new TreeMap<String, Workload>();
		m.put("add", new Workload("add", "Add nodes and edges",
				"Adding nodes and edges to the database", null));
		m.put("clustering-coeff", new Workload("clustering-coeff", "Clustering coefficients",
				"Compute the clustering coefficients", null));
		m.put("delete-graph", new Workload("delete-graph", "Delete graph",
				"Delete the entire graph", null));
		m.put("shortest-path", new Workload("shortest-path", "Shortest path",
				"Shortest path algorithm", null));
		m.put("shortest-path-prop", new Workload("shortest-path-prop", "Shortest path with properties",
				"Shortest paths with in-DB marking", null));
		m.put("generate", new Workload("generate", "Generate",
				"Generate (or grow) the graph based on the given model", "MODEL"));
		m.put("get", new Workload("get", "Get",
				"\"Get\" microbenchmarks", null));
		m.put("get-k", new Workload("get-k", "Get k-hop",
				"\"Get\" k-hops microbenchmarks", null));
		m.put("get-property", new Workload("get-property", "Get properties",
				"\"Get\" Object store microbenchmarks", null));
		m.put("ingest", new Workload("ingest", "Ingest",
				"Ingest a file to the database (also delete the graph)", "FILE"));
		WORKLOADS = Collections.unmodifiableMap(m);
	}
	

	private String shortName;
	private String longName;
	private String description;
	private String optionalArgument;

	
	/**
	 * Create an instance of {@link Workload}
	 * 
	 * @param shortName the short name
	 * @param longName the long name
	 * @param description the description
	 * @param optionalArgument the name of the optional argument; null otherwise
	 */
	public Workload(String shortName, String longName, String description, String optionalArgument) {
		this.shortName = shortName;
		this.longName = longName;
		this.description = description;
		this.optionalArgument = optionalArgument;
	}


	/**
	 * Return the short name
	 * 
	 * @return the short name
	 */
	public String getShortName() {
		return shortName;
	}


	/**
	 * Return the long (pretty) name
	 * 
	 * @return the long name
	 */
	public String getLongName() {
		return longName;
	}


	/**
	 * Return the description
	 * 
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}


	/**
	 * Return the name of the optional argument
	 * 
	 * @return the name of the optional argument, or null if none
	 */
	public String getOptionalArgument() {
		return optionalArgument;
	}
	
	
	/**
	 * Return the string version of the object
	 * 
	 * @return the string version of the object
	 */
	@Override
	public String toString() {
		return longName;
	}
}
