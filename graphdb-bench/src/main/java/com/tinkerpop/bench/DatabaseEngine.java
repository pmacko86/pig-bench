package com.tinkerpop.bench;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.impls.bdb.BdbGraph;
import com.tinkerpop.blueprints.pgm.impls.dex.DexGraph;
import com.tinkerpop.blueprints.pgm.impls.dup.DupGraph;
import com.tinkerpop.blueprints.pgm.impls.hollow.HollowGraph;
import com.tinkerpop.blueprints.pgm.impls.neo4j.Neo4jGraph;
import com.tinkerpop.blueprints.pgm.impls.rdf.impls.NativeStoreRdfGraph;
import com.tinkerpop.blueprints.pgm.impls.sql.SqlGraph;


/**
 * A database engine
 * 
 * @author Peter Macko (pmacko@eecs.harvard.edu)
 */
public class DatabaseEngine {
	
	/**
	 * The set of supported database engines
	 */
	public static final Map<String, DatabaseEngine> ENGINES;
	
	static {
		Map<String, DatabaseEngine> engines = new TreeMap<String, DatabaseEngine>();
		engines.put("bdb", new DatabaseEngine(BdbGraph.class, "bdb", "BerkeleyDB-Basic",
				"BerkeleyDB, using massive indexing", false));
		engines.put("dup", new DatabaseEngine(DupGraph.class, "dup", "BerkeleyDB-Duplicates",
				"BerkeleyDB, duplicates on edge lookups and properties", false));
		engines.put("dex", new DatabaseEngine(DexGraph.class, "dex", "DEX",
				"DEX", false));
		engines.put("hollow", new DatabaseEngine(HollowGraph.class, "hollow", "Hollow",
				"The hollow implementation with no backing database", false));
		engines.put("neo", new DatabaseEngine(Neo4jGraph.class, "neo", "Neo4j",
				"Neo4j", false));
		engines.put("rdf", new DatabaseEngine(NativeStoreRdfGraph.class, "rdf", "Sesame RDF",
				"Sesame RDF", false));
		engines.put("sql", new DatabaseEngine(SqlGraph.class, "sql", "MySQL",
				"MySQL", true));
		ENGINES = Collections.unmodifiableMap(engines);
	}
	

	private Class<? extends Graph> blueprintsClass;
	private String shortName;
	private String longName;
	private String description;
	private boolean hasOptionalArgument;
	
	
	/**
	 * Create an instance of {@link DatabaseEngine}
	 * 
	 * @param blueprintsClass the Blueprints API class
	 * @param shortName the short name
	 * @param longName the long name
	 * @param description the description
	 * @param hasOptionalArgument true if it accepts an optional path/address argument
	 */
	public DatabaseEngine(Class<? extends Graph> blueprintsClass, String shortName,
			String longName, String description, boolean hasOptionalArgument) {
		this.blueprintsClass = blueprintsClass;
		this.shortName = shortName;
		this.longName = longName;
		this.description = description;
		this.hasOptionalArgument = hasOptionalArgument;
	}


	/**
	 * Return the Blueprints class
	 * 
	 * @return the blueprints class
	 */
	public Class<? extends Graph> getBlueprintsClass() {
		return blueprintsClass;
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
	 * Determine whether the command-line option should take an optional path argument
	 * 
	 * @return true if the command-line option should take an optional path argument
	 */
	public boolean hasOptionalArgument() {
		return hasOptionalArgument;
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
