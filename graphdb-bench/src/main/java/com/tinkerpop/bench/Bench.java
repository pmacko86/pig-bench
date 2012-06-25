package com.tinkerpop.bench;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.IOException;
import java.util.Properties;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public class Bench {

	public static Logger logger = Logger.getLogger(Bench.class);
	public static Properties benchProperties = new Properties();
	
	// CPL - Originator & Types
	public static final String ORIGINATOR = "com.tinkerpop.bench";
	public static final String TYPE_DB = "Database";
	public static final String TYPE_OPERATION = "Operation";

	// DATASETS - GraphML & Databases
	public static final String DATASETS_DIRECTORY = "bench.datasets.directory";
	public static final String DB_SQL_PATH = "bench.db.sql.path";
	public static final String DB_SQL_PATH_WARMUP = "bench.db.sql.warmup.path";

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

	static {
		try {
			benchProperties.load(Bench.class
					.getResourceAsStream("bench.properties"));
			//System.out.println(benchProperties);
		} catch (IOException e) {
			//e.printStackTrace();
			ConsoleUtils.warn("Could not load bench.properties");
		}
		try {
			PropertyConfigurator.configure(Bench.class
				.getResource("log4j.properties"));
		} catch (Exception e) {
			//e.printStackTrace();
			ConsoleUtils.warn("Could not load log4j.properties");
		}
	}
}
