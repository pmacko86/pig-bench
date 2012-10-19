package com.tinkerpop.bench;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.tinkerpop.bench.benchmark.BenchmarkMicro;
import com.tinkerpop.bench.util.ConsoleUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class Bench {

	public static Logger logger = Logger.getLogger(Bench.class);
	public static Properties benchProperties = new Properties();
	public static String graphdbBenchDir = null;
	
	// CPL - Originator & Types
	public static final String ORIGINATOR = "com.tinkerpop.bench";
	public static final String TYPE_DB = "Database";
	public static final String TYPE_OPERATION = "Operation";

	// DATASETS - Ingest & Databases
	public static final String DATASETS_DIRECTORY = "bench.datasets.directory";
	public static final String DB_SQL_ADDR = "bench.db.sql.addr";
	public static final String DB_SQL_DB_NAME_PREFIX = "bench.db.sql.database_prefix";
	public static final String DB_SQL_DB_NAME_PREFIX_WARMUP = "bench.db.sql.warmup_database_prefix";
	
	// DATABASE ENGINE CACHES
	public static final String DB_CACHE_SIZE = "bench.db.db_cache_size";
	public static final String DB_PREFERRED_BUFFER_POOL_RATIO = "bench.db.preferred_buffer_pool_ratio";
	public static final String DB_NEO_CACHES = "bench.db.neo.caches";
	public static final String DB_NEO_GCR = "bench.db.neo.gcr";

	// LOGS - Operation Logs & Provenance
	public static final String LOGS_DELIMITER = "bench.logs.delimiter";
	public static final String CPL_ODBC_DSN = "bench.cpl.odbc";

	// RESULTS - Logs, Summaries, Plots
	public static final String RESULTS_DIRECTORY = "bench.results.directory";

	// GRAPH GENERAL
	public static final String GRAPH_PROPERTY_ID = "bench.graph.property.id";
	public static final String GRAPH_LABEL = "bench.graph.label";
	public static final String GRAPH_LABEL_FAMILY = "bench.graph.label.family";
	public static final String GRAPH_LABEL_FRIEND = "bench.graph.label.friend";

	// GRAPH FILES
	public static final String GRAPHML_BARABASI = "bench.graph.barabasi.file";
	
	// WEB - The web interface
	public static final String WEB_JOBCONTROL_ENABLE = "bench.web.jobcontrol.enabled";

	
	static {
		
		// Load the properties
		
		try {
			benchProperties.load(Bench.class.getResourceAsStream("bench.properties"));
		}
		catch (IOException e) {
			ConsoleUtils.warn("Could not load bench.properties");
		}
		try {
			PropertyConfigurator.configure(Bench.class.getResource("log4j.properties"));
		} catch (Exception e) {
			ConsoleUtils.warn("Could not load log4j.properties");
		}
		
		
		// Find the graphdb-bench directory
		
		try {
			URL source = BenchmarkMicro.class.getProtectionDomain().getCodeSource().getLocation();
			if ("file".equals(source.getProtocol())) {
				for (File f = new File(source.toURI()); f != null; f = f.getParentFile()) {
					if (f.getName().equals("graphdb-bench")) {
						graphdbBenchDir = f.getAbsolutePath();
						break;
					}
				}
			}
		} catch (Exception e) {
			graphdbBenchDir = null;
		}
		
		if (graphdbBenchDir == null) {
			ConsoleUtils.warn("Could not determine the graphdb-bench directory.");
			graphdbBenchDir = ".";
		}
	}
	
	
	
	/**
	 * Get the given property and expand variables
	 * 
	 * @param key the property key
	 * @param defaultValue the default value
	 * @return the value
	 */
	public static String getProperty(String key, String defaultValue) {
		
		String v = Bench.benchProperties.getProperty(key);
		if (v == null) return defaultValue;
		
		if (v.indexOf("$GRAPHDB_BENCH") >= 0) {
			if (Bench.graphdbBenchDir == null) {
				ConsoleUtils.error("Could not determine the graphdb-bench directory.");
				throw new RuntimeException("Could not expand the $GRAPHDB_BENCH variable");
			}
			v = v.replaceAll("\\$GRAPHDB_BENCH", Bench.graphdbBenchDir);
		}
		
		return v;
	}
	
	
	/**
	 * Get the given property and expand variables
	 * 
	 * @param key the property key
	 * @return the value, or null if not found
	 */
	public static String getProperty(String key) {
		return getProperty(key, null);
	}
	
	
	/**
	 * Get the given boolean property
	 * 
	 * @param key the property key
	 * @param defaultValue the default value if the property is not found or malformed
	 * @return the value
	 */
	public static boolean getBooleanProperty(String key, boolean defaultValue) {
		String s = getProperty(key, null);
		if (s == null) return defaultValue;
		if (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("t")
				|| s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("y")) return true;
		if (s.equalsIgnoreCase("false") || s.equalsIgnoreCase("f")
				|| s.equalsIgnoreCase("no") || s.equalsIgnoreCase("n")) return false;
		return defaultValue;
	}
}
