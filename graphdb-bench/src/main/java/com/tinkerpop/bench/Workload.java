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
	 * Update type
	 */
	public enum UpdateCategory {
		
		/**
		 * Read only workloads.
		 */
		READ_ONLY,
		
		/**
		 * Workloads that perform some updates but then revert all changes
		 * before finishing. Note that a failed workload might leave the
		 * database in a different state than when it started.
		 */
		TEMPORARY_UPDATE,
		
		/**
		 * Workloads that make permanent changes to the database. 
		 */
		PERMANENT_UPDATE,
		
		/**
		 * Update workloads used to load and delete graphs.
		 */
		LOAD_UPDATE,
	}
	
	
	/**
	 * The set of supported workloads
	 */
	public static final Map<String, Workload> WORKLOADS;
	
	static {
		Map<String, Workload> m = new TreeMap<String, Workload>();
		m.put("add", new Workload("add", "Add nodes, edges, and properties",
				"Add random nodes, edges, and properties", null, true, UpdateCategory.PERMANENT_UPDATE));
		m.put("blank", new Workload("blank", "A blank operation (noop)",
				"A blank operation (noop)", null, true, UpdateCategory.READ_ONLY));
		m.put("clustering-coeff", new Workload("clustering-coeff", "Global and network average clustering coefficients",
				"Global and network average clustering coefficients", null, false, UpdateCategory.READ_ONLY));
		m.put("clustering-local", new Workload("clustering-local", "Local clustering coefficients",
				"Local clustering coefficients", null, false, UpdateCategory.READ_ONLY));
		m.put("create-index", new Workload("create-index", "Create index",
				"Create an index", "KEYS", false, UpdateCategory.LOAD_UPDATE));
		/*m.put("delete-graph", new Workload("delete-graph", "Delete graph",
				"Delete the entire graph", null, false, UpdateCategory.LOAD_UPDATE));*/
		m.put("shortest-path", new Workload("shortest-path", "Shortest path",
				"Shortest path algorithm", null, true, UpdateCategory.READ_ONLY));
		m.put("shortest-path-prop", new Workload("shortest-path-prop", "Shortest path with properties",
				"Shortest paths with in-DB marking", null, true, UpdateCategory.TEMPORARY_UPDATE));
		m.put("sssp", new Workload("sssp", "Single source shortest path",
				"Single source shortest path algorithm", null, true, UpdateCategory.READ_ONLY));
		m.put("sssp-prop", new Workload("sssp-prop", "Single source shortest path with properties",
				"Single source shortest path with in-DB marking", null, true, UpdateCategory.TEMPORARY_UPDATE));
		m.put("generate", new Workload("generate", "Generate",
				"Generate or grow the graph based on the given model", "MODEL", false, UpdateCategory.LOAD_UPDATE));
		m.put("get", new Workload("get", "Get",
				"\"Get\" microbenchmarks", null, true, UpdateCategory.READ_ONLY));
		m.put("get--micro", new Workload("get--micro", "Get - micro ops only",
				"\"Get\" microbenchmarks -- micro ops only", null, true, UpdateCategory.READ_ONLY));
		m.put("get--traversals", new Workload("get--traversals", "Get - traversals only",
				"\"Get\" microbenchmarks -- traversals only", null, true, UpdateCategory.READ_ONLY));
		/*m.put("get-trav-seq", new Workload("get-trav-seq", "Get - traversals, sequential vertices",
				"\"Get\" traversals, sequential vertices", null, true, UpdateCategory.READ_ONLY));*/
		m.put("get-label", new Workload("get-label", "Get using edge labels",
				"\"Get\" microbenchmarks using edge labels", null, true, UpdateCategory.READ_ONLY));
		m.put("get-k", new Workload("get-k", "Get k-hop",
				"\"Get\" k-hops microbenchmarks", null, true, UpdateCategory.READ_ONLY));
		m.put("get-k-label", new Workload("get-k-label", "Get k-hop using edge labels",
				"\"Get\" k-hops microbenchmarks using edge labels", "LABELS", true, UpdateCategory.READ_ONLY));
		m.put("get-property", new Workload("get-property", "Get properties",
				"\"Get\" Object store microbenchmarks", "KEYS", true, UpdateCategory.READ_ONLY));
		m.put("get-index", new Workload("get-index", "Get using an index",
				"\"Get\" objects using indexes", "KEYS", true, UpdateCategory.READ_ONLY));
		m.put("incr-ingest", new Workload("incr-ingest", "Incremental Ingest",
				"Incrementally load data from a file to the database", "FILE", false, UpdateCategory.LOAD_UPDATE));
		m.put("ingest", new Workload("ingest", "Ingest",
				"Ingest a file to the database (also delete the graph)", "FILE", false, UpdateCategory.LOAD_UPDATE));
		m.put("pagerank", new Workload("pagerank", "Compute PageRank",
				"Compute PageRank", null, false, UpdateCategory.READ_ONLY));
		WORKLOADS = Collections.unmodifiableMap(m);
	}
	

	private String shortName;
	private String longName;
	private String description;
	private String optionalArgument;
	private boolean usesOpCount;
	private UpdateCategory updateCategory;

	
	/**
	 * Create an instance of {@link Workload}
	 * 
	 * @param shortName the short name
	 * @param longName the long name
	 * @param description the description
	 * @param optionalArgument the name of the optional argument; null otherwise
	 * @param usesOpCount true if the workload uses the --op-count parameter
	 * @param updateCategory the category of updates the workload performs
	 */
	public Workload(String shortName, String longName, String description, String optionalArgument,
			boolean usesOpCount, UpdateCategory updateCategory) {
		this.shortName = shortName;
		this.longName = longName;
		this.description = description;
		this.optionalArgument = optionalArgument;
		this.usesOpCount = usesOpCount;
		this.updateCategory = updateCategory;
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
	 * Determine whether the workload uses the --op-count parameter
	 * 
	 * @return true if the workload uses the --op-count parameter
	 */
	public boolean isUsingOpCount() {
		return usesOpCount;
	}
	
	
	/**
	 * Get the category of updates the workload performs
	 * 
	 * @return the category of updates the workload performs
	 */
	public UpdateCategory getUpdateCategory() {
		return updateCategory;
	}
	
	
	/**
	 * Determine whether this workload performs any updates
	 * 
	 * @return true if the workload performs updates
	 */
	public boolean isUpdate() {
		return updateCategory != UpdateCategory.READ_ONLY;
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
