package com.tinkerpop.bench.benchmark;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import com.tinkerpop.bench.Bench;
import com.tinkerpop.bench.ConsoleUtils;
import com.tinkerpop.bench.DatabaseEngine;
import com.tinkerpop.bench.GlobalConfig;
import com.tinkerpop.bench.GraphDescriptor;
import com.tinkerpop.bench.GraphUtils;
import com.tinkerpop.bench.LogUtils;
import com.tinkerpop.bench.Workload;
import com.tinkerpop.bench.cache.Cache;
import com.tinkerpop.bench.generator.GraphGenerator;
import com.tinkerpop.bench.generator.SimpleBarabasiGenerator;
import com.tinkerpop.bench.log.SummaryLogWriter;
import com.tinkerpop.bench.operation.OperationDeleteGraph;
import com.tinkerpop.bench.operation.operations.*;
import com.tinkerpop.bench.operationFactory.OperationFactory;
import com.tinkerpop.bench.operationFactory.OperationFactoryGeneric;
import com.tinkerpop.bench.operationFactory.factories.OperationFactoryRandomVertex;
import com.tinkerpop.bench.operationFactory.factories.OperationFactoryRandomVertexPair;
import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.impls.hollow.HollowGraph;
import com.tinkerpop.blueprints.pgm.impls.sql.SqlGraph;

import edu.harvard.pass.cpl.CPL;
import edu.harvard.pass.cpl.CPLException;
import edu.harvard.pass.cpl.CPLFile;

import joptsimple.OptionParser;
import joptsimple.OptionSet;


/**
 * @author Alex Averbuch (alex.averbuch@gmail.com)
 * @author Daniel Margo (dmargo@eecs.harvard.edu)
 * @author Peter Macko (pmacko@eecs.harvard.edu)
 */
public class BenchmarkMicro extends Benchmark {
	
	/// The default file for ingest
	public static final String DEFAULT_INGEST_FILE = "barabasi_1000_5000.graphml";
	
	/// The defaults
	public static final int DEFAULT_OP_COUNT = 1000;
	public static final int DEFAULT_K_HOPS = 2;
	public static final int DEFAULT_NUM_THREADS = 1;
	
	/// The number of threads
	private static int numThreads = DEFAULT_NUM_THREADS;
	
	
	/**
	 * Print the help
	 */
	protected static void help() {
		
		System.err.println("Usage: runBenchmarkSuite.sh OPTIONS");
		System.err.println("");
		System.err.println("General options:");
		System.err.println("  --annotation TEXT       Include an annotation in the " +
										"disclosed provenance");
		System.err.println("  --dir, -d DIR           Set the database and results directory");
		System.err.println("  --dumb-terminal         Use the dumb terminal settings");
		System.err.println("  --help                  Print this help message");
		System.err.println("  --no-color              Disable color output to the terminal");
		System.err.println("  --no-provenance         Disable provenance collection");
		System.err.println("  --no-warmup             Disable the initial warmup run");
		System.err.println("  --single-db-connection  Use one shared database connection " +
										"for all threads");
		System.err.println("  --threads N             Run N copies of the benchmark concurrently");
		System.err.println("  --tx-buffer N           Set the size of the transaction buffer");
		System.err.println("");
		System.err.println("Options to select a database engine (select one):");
		for (String k : DatabaseEngine.ENGINES.keySet()) {
			DatabaseEngine e = DatabaseEngine.ENGINES.get(k);
			String l = e.getShortName();
			if (e.hasOptionalArgument()) l += " [DB_NAME]";
			System.err.printf("  --%-20s  %s\n", l, e.getDescription());
		}
		System.err.println("");
		System.err.println("Database engine options:");
		System.err.println("  --database, -D NAME     Select a specific graph or a database instance");
		System.err.println("  --sql-addr [ADDR]       Specify the SQL connection string (w/o DB name)");
		System.err.println("  --warmup-sql [DB_NAME]  Specify the SQL database name for warmup");
		System.err.println("");
		System.err.println("Options to select a workload (select multiple):");
		for (String k : Workload.WORKLOADS.keySet()) {
			Workload w = Workload.WORKLOADS.get(k);
			String l = w.getShortName();
			if (w.getOptionalArgument() != null) l += " [" + w.getOptionalArgument() + "]";
			System.err.printf("  --%-20s  %s\n", l, w.getDescription());
		}
		System.err.println("");
		System.err.println("Benchmark and workload options:");
		System.err.println("  --ingest-as-undirected  Ingest a graph as undirected by " +
										"doubling-up the edges");
		System.err.println("  --k-hops K              Set the number of k-hops");
		System.err.println("  --k-hops K1:K2          Set a range of k-hops");
		System.err.println("  --op-count N            Set the number of operations");
		System.err.println("  --warmup-ingest FILE    Set a different file for ingest during " +
										"the warmup");
		System.err.println("  --warmup-op-count N     Set the number of warmup operations");
		System.err.println("");
		System.err.println("Options for model \"Barabasi\":");
		System.err.println("  --barabasi-n N          The number of vertices");
		System.err.println("  --barabasi-m M          The number of incoming edges "+
										"at each new vertex");
		System.err.println("");
		System.err.println("Miscellaneous commands:");
		System.err.println("  --export-graphml FILE   Export the database to a GraphML file");
		System.err.println("  --export-n-graphml FILE Export the database to a normalized GraphML file");
	}

	
	/**
	 * Run the benchmarking program
	 * 
	 * @param args the command-line arguments
	 * @throws Exception on error
	 */
	public static void run(String[] args) throws Exception {
		
		/*
		 * Initialize
		 */
		
		// Pre-parse the command-line arguments
		
		for (String a : args) {
			if (a.equals("--no-color")) ConsoleUtils.useColor = false;
			if (a.equals("--dumb-terminal")) {
				ConsoleUtils.useColor = false;
				ConsoleUtils.useEscapeSequences = false;
			}
		}
				

		/*
		 * Parse the command-line arguments
		 */
		
		OptionParser parser = new OptionParser();
		
		parser.accepts("annotation").withRequiredArg().ofType(String.class);
		
		parser.accepts("D").withRequiredArg().ofType(String.class);
		parser.accepts("database").withRequiredArg().ofType(String.class);
		parser.accepts("d").withRequiredArg().ofType(String.class);
		parser.accepts("dir").withRequiredArg().ofType(String.class);
		parser.accepts("dumb-terminal");
		parser.accepts("help");
		parser.accepts("no-color");
		parser.accepts("no-provenance");
		parser.accepts("no-warmup");
		parser.accepts("single-db-connection");
		parser.accepts("sql-addr").withRequiredArg().ofType(Integer.class);
		parser.accepts("threads").withRequiredArg().ofType(Integer.class);
		parser.accepts("tx-buffer").withRequiredArg().ofType(Integer.class);
		parser.accepts("warmup-sql").withRequiredArg().ofType(String.class);
		
		
		// Databases
		
		for (DatabaseEngine e : DatabaseEngine.ENGINES.values()) {
			if (e.hasOptionalArgument()) {
				parser.accepts(e.getShortName()).withOptionalArg().ofType(String.class);
			}
			else {
				parser.accepts(e.getShortName());
			}
		}
		
		
		// Workloads
		
		for (Workload w : Workload.WORKLOADS.values()) {
			if (w.getOptionalArgument() != null) {
				parser.accepts(w.getShortName()).withOptionalArg().ofType(String.class);
			}
			else {
				parser.accepts(w.getShortName());
			}
		}
		
		
		// Ingest modifiers
		
		parser.accepts("f").withRequiredArg().ofType(String.class);
		parser.accepts("file").withRequiredArg().ofType(String.class);	/* deprecated */
		
		
		// Benchmark and workload modifiers
		
		parser.accepts("ingest-as-undirected");
		parser.accepts("k-hops").withRequiredArg().ofType(String.class);
		parser.accepts("op-count").withRequiredArg().ofType(Integer.class);
		parser.accepts("warmup-op-count").withRequiredArg().ofType(Integer.class);
		parser.accepts("warmup-ingest").withRequiredArg().ofType(String.class);
		
		
		// Generator modifiers
		
		parser.accepts("barabasi-n").withRequiredArg().ofType(Integer.class);
		parser.accepts("barabasi-m").withRequiredArg().ofType(Integer.class);
		
		
		// Miscellaneous commands
		
		parser.accepts("export-graphml").withOptionalArg().ofType(String.class);
		parser.accepts("export-n-graphml").withOptionalArg().ofType(String.class);
		
		
		// Parse the options
		
		OptionSet options;
		
		try {
			options = parser.parse(args);
		}
		catch (Exception e) {
			ConsoleUtils.error("Invalid options (please use --help for a list): " + e.getMessage());
			System.exit(1);
			return;
		}
		
		List<String> nonOptionArguments = options.nonOptionArguments();
		if (!nonOptionArguments.isEmpty()) {
			ConsoleUtils.error("Invalid options (please use --help for a list): " + nonOptionArguments);
			System.exit(1);
			return;
		}
		
		
		/*
		 * Handle generic options
		 */
		
		if (options.has("no-color")) {
			ConsoleUtils.useColor = false;
		}
		
		if (options.has("dumb-terminal")) {
			ConsoleUtils.useColor = false;
			ConsoleUtils.useEscapeSequences = false;
		}
		
		if (options.has("help") || !options.hasOptions()) {
			help();
			return;
		}
		
		String ingestFile = DEFAULT_INGEST_FILE;
		if (options.has("f") || options.has("file")) {
			ingestFile = options.valueOf(options.has("f") ? "f" : "file").toString();
		}
		if (options.has("ingest")) {
			if (options.hasArgument("ingest")) {
				ingestFile = options.valueOf("ingest").toString();
			}
		}
		
		String warmupIngestFile = ingestFile;
		if (options.has("warmup-ingest")) {
			warmupIngestFile = options.valueOf("warmup-ingest").toString();
		}
		
		boolean ingestAsUndirected = false;
		if (options.has("ingest-as-undirected")) {
			ingestAsUndirected = true;
		}
		
		boolean warmup = true;
		if (options.has("no-warmup")) {
			warmup = false;
		}
		
		boolean provenance = true;
		if (options.has("no-provenance")) {
			provenance = false;
		}
		
		if (options.has("single-db-connection")) {
			GlobalConfig.oneDbConnectionPerThread = false;
		}
		
		if (options.has("threads")) {
			numThreads = (Integer) options.valueOf("threads");
			if (numThreads < 1) {
				ConsoleUtils.error("Invalid number of threads -- must be at least 1");
				System.exit(1);
				return;
			}
		}
		
		if (options.has("tx-buffer")) {
			GlobalConfig.transactionBufferSize = (Integer) options.valueOf("tx-buffer");
			if (GlobalConfig.transactionBufferSize < 1) {
				ConsoleUtils.error("Invalid size of the transaction buffer -- must be at least 1");
				System.exit(1);
				return;
			}
		}
		
		int opCount = DEFAULT_OP_COUNT;
		if (options.has("op-count")) opCount = (Integer) options.valueOf("op-count");
		
		int warmupOpCount = opCount;
		if (options.has("warmup-op-count")) warmupOpCount = (Integer) options.valueOf("warmup-op-count");
		
		int[] kHops;
		if (options.has("k-hops")) {
			String kHopsStr = (String) options.valueOf("k-hops");
			int kc = kHopsStr.indexOf(':');
			if (kc >= 0) {
				int k1, k2;
				try {
					k1 = Integer.parseInt(kHopsStr.substring(0, kc));
					k2 = Integer.parseInt(kHopsStr.substring(kc + 1));
				}
				catch (NumberFormatException e) {
					ConsoleUtils.error("Invalid range of k hops (not a number).");
					System.exit(1);
					return;
				}
				if (k1 <= 0 || k1 > k2) {
					ConsoleUtils.error("Invalid range of k hops.");
					System.exit(1);
					return;
				}
				kHops = new int[k2-k1+1];
				for (int k = k1; k <= k2; k++) kHops[k-k1] = k;
			}
			else {
				kHops = new int[1];
				try {
					kHops[0] = Integer.parseInt(kHopsStr);
				}
				catch (NumberFormatException e) {
					ConsoleUtils.error("Invalid number of k hops (not a number).");
					System.exit(1);
					return;
				}
				if (kHops[0] <= 0) {
					ConsoleUtils.error("Invalid number of k hops (must be positive).");
					System.exit(1);
					return;
				}
			}
		}
		else {
			kHops = new int[1];
			kHops[0] = DEFAULT_K_HOPS;
		}
		
		if (provenance) {
			if (!CPL.isInstalled()) {
				ConsoleUtils.error("CPL is not installed. Use --no-provenance to disable provenance collection.");
				System.exit(1);
				return;
			}
			else {
				try {
					CPL.attachODBC(Bench.getProperty(Bench.CPL_ODBC_DSN, "DSN=CPL"));
				}
				catch (CPLException e) {
					ConsoleUtils.error("Could not initialize provenance collection:");
					ConsoleUtils.error("  " + e.getMessage());
					System.exit(1);
					return;
				}
			}
		}
		
		
		/*
		 * Get the name of the database instance
		 */
		
		String dbInstanceName = null;
		if (options.has("D") || options.has("database")) {
			dbInstanceName = options.valueOf(options.has("D") ? "D" : "database").toString();
			if (!Pattern.matches("^[a-z][a-z0-9_]*$", dbInstanceName)) {
	    		throw new RuntimeException("Invalid database name (can contain only lower-case letters, "
	    				+ "numbers, and _, and has to start with a letter)");
	    	}
		}
		
		
		/*
		 * Arguments specific to the various database engines
		 */
		
		String dbShortName = null;
		Class<?> dbClass = null;
		DatabaseEngine dbEngine = null;
		for (DatabaseEngine e : DatabaseEngine.ENGINES.values()) {
			if (options.has(e.getShortName())) {
				if (dbEngine != null) {
					ConsoleUtils.error("Multiple databases are selected, but only one is allowed.");
					System.exit(1);
					return;
				}
				dbEngine = e;
				dbShortName = dbEngine.getShortName();
				dbClass = dbEngine.getBlueprintsClass();
			}
		}
		if (dbShortName == null) {
			ConsoleUtils.error("No database is selected (please use --help for a list of options).");
			System.exit(1);
			return;
		}
		
		boolean withGraphPath = true;
		if (dbClass == HollowGraph.class) withGraphPath = false;
		
		
		// SQL
		
		String sqlDbAddr = null;
		String sqlDbNamePrefix = null;
		String sqlDbNamePrefixWarmup = null;
		String sqlDbName = null;
		String sqlDbNameWarmup = null;
		
		if (options.has("sql")) {
			
			// Get the connection address
			
			if (options.has("sql-addr")) {
				sqlDbAddr = options.valueOf("sql-addr").toString();
			}
			else {
				String sqlDbAddr_property = Bench.getProperty(Bench.DB_SQL_ADDR);
				if (sqlDbAddr_property != null) {
					sqlDbAddr = sqlDbAddr_property;
				}
				if (sqlDbAddr == null) {
					ConsoleUtils.error("The SQL database address is not specified.");
					System.exit(1);
					return;
				}
			}
					
			
			// Get the table name prefixes
			
			if (options.hasArgument("sql")) {
				sqlDbNamePrefix = options.valueOf("sql").toString();
			}
			else {
				String sqlDbNamePrefix_property = Bench.getProperty(Bench.DB_SQL_DB_NAME_PREFIX);
				if (sqlDbNamePrefix_property != null) {
					sqlDbNamePrefix = sqlDbNamePrefix_property;
				}
				else {
					ConsoleUtils.error("The SQL database name is not specified.");
					System.exit(1);
					return;
				}
			}
			
			if (options.has("warmup-sql")) {
				sqlDbNamePrefixWarmup = options.valueOf("warmup-sql").toString();
			}
			else {
				String sqlDbNamePrefixWarmup_property = Bench.getProperty(Bench.DB_SQL_DB_NAME_PREFIX_WARMUP);
				if (sqlDbNamePrefixWarmup_property != null) {
					sqlDbNamePrefixWarmup = sqlDbNamePrefixWarmup_property;
				}
				else {
					ConsoleUtils.error("The SQL warmup database name is not specified.");
					System.exit(1);
					return;
				}
			}
			
			
			// Compose the database names
			
			sqlDbName = sqlDbNamePrefix;
			sqlDbNameWarmup = sqlDbNamePrefixWarmup;
			
			if (dbInstanceName != null) {
				sqlDbName += "__" + dbInstanceName;
				sqlDbNameWarmup += "__" + dbInstanceName;
			}
			
			
			// Create the databases if they don't already exist
			
			SqlGraph.createDatabase(sqlDbAddr, sqlDbName);
			if (warmup) SqlGraph.createDatabase(sqlDbAddr, sqlDbNameWarmup);
		}
		
		
		/*
		 * Get the list of workloads
		 */
		
		LinkedList<Workload> workloads = new LinkedList<Workload>(); 
		for (Workload w : Workload.WORKLOADS.values()) {
			if (options.has(w.getShortName())) {
				workloads.add(w);
			}
		}

		
		/*
		 * Setup the graph generator
		 */
		
		GraphGenerator graphGenerator = null;
		if (options.has("generate")) {
			String model = options.hasArgument("generate")
					? options.valueOf("generate").toString().toLowerCase()
					: "barabasi";
			
			if (model.equals("barabasi")) {
				int n = options.has("barabasi-n") ? (Integer) options.valueOf("barabasi-n") : 1000;
				int m = options.has("barabasi-m") ? (Integer) options.valueOf("barabasi-m") : 5;
				graphGenerator = new SimpleBarabasiGenerator(n, m);
			}
			
			if (graphGenerator == null) {
				ConsoleUtils.error("Unrecognized graph generation model");
				System.exit(1);
				return;
			}
		}
		
		
		/*
		 * Get the name of the results directory
		 */
		
		String dirResults;
		if (options.has("d") || options.has("dir")) {
			dirResults = options.valueOf(options.has("d") ? "d" : "dir").toString();
			if (!dirResults.endsWith("/")) dirResults += "/";
		}
		else {
			String propDirResults = Bench.getProperty(Bench.RESULTS_DIRECTORY);
			if (propDirResults == null) {
				ConsoleUtils.error("Property \"" + Bench.RESULTS_DIRECTORY + "\" is not set and --dir is not specified.");
				System.exit(1);
				return;
			}
			if (!propDirResults.endsWith("/")) propDirResults += "/";
			dirResults = propDirResults + "Micro/";
		}
		
		
		/*
		 * Get the name of the ingest file (if necessary)
		 */
		
		if (options.has("ingest")) {
			if (!(new File(ingestFile)).exists()) {
				String dirGraphML = Bench.getProperty(Bench.DATASETS_DIRECTORY);
				if (dirGraphML == null) {
					ConsoleUtils.warn("Property \"" + Bench.DATASETS_DIRECTORY + "\" is not set.");
					ConsoleUtils.error("File \"" + ingestFile + "\" does not exist.");
					System.exit(1);
					return;
				}
				if (!dirGraphML.endsWith("/")) dirGraphML += "/";
				if (!(new File(dirGraphML + ingestFile)).exists()) {
					ConsoleUtils.error("File \"" + ingestFile + "\" does not exist.");
					System.exit(1);
					return;
				}
				else {
					ingestFile = dirGraphML + ingestFile;
				}
			}
			if (!(new File(warmupIngestFile)).exists()) {
				String dirGraphML = Bench.getProperty(Bench.DATASETS_DIRECTORY);
				if (dirGraphML == null) {
					ConsoleUtils.warn("Property \"" + Bench.DATASETS_DIRECTORY + "\" is not set.");
					ConsoleUtils.error("File \"" + warmupIngestFile + "\" does not exist.");
					System.exit(1);
					return;
				}
				if (!dirGraphML.endsWith("/")) dirGraphML += "/";
				if (!(new File(dirGraphML + warmupIngestFile)).exists()) {
					ConsoleUtils.error("File \"" + warmupIngestFile + "\" does not exist.");
					System.exit(1);
					return;
				}
				else {
					warmupIngestFile = dirGraphML + warmupIngestFile;
				}
			}
		}
		else {
			ingestFile = null;
			warmupIngestFile = null;
		}
		
		
		/*
		 * Setup the benchmark
		 */
		
		String[] graphmlFiles = new String[] { ingestFile };
		String[] warmupGraphmlFiles = new String[] { warmupIngestFile };
		GraphGenerator[] graphGenerators = new GraphGenerator[] { graphGenerator };
		
		Benchmark warmupBenchmark = new BenchmarkMicro(
				warmupGraphmlFiles, graphGenerators, options, warmupOpCount, kHops,
				ingestAsUndirected);
		
		Benchmark benchmark = new BenchmarkMicro(
				graphmlFiles, graphGenerators, options, opCount, kHops,
				ingestAsUndirected);
		
		
		/*
		 * Build the argument string to be used as a part of the log file name
		 */
		
		GraphDescriptor graphDescriptor = null;
		
		StringBuilder sb = new StringBuilder();
		for (String s : args) {
			String argName = s.charAt(0) == '-' ? s.substring(s.charAt(1) == '-' ? 2 : 1) : s;
			if (s.charAt(0) == '-') sb.append("__"); else sb.append("_");
			sb.append(argName);
		}
		
		sb.append("__mem_");
		sb.append(Math.round(Runtime.getRuntime().maxMemory() / 1024768.0f));
		sb.append("m");	
		
		sb.append("__date_");
		sb.append((new SimpleDateFormat("yyyyMMdd-HHmmss")).format(new Date()));
		
		String argString = sb.toString().replaceAll("\\s+", "");
		
		
		/*
		 * Set the file, directory, and database names
		 */
		
		String dbPrefix = dirResults + dbShortName;
		if (dbInstanceName != null && !dbInstanceName.equals("")) dbPrefix += "_" + dbInstanceName;
		dbPrefix += "/";
		
		String warmupDbDir = null;
		String dbDir = null;
		if (!options.has("sql")) {
			warmupDbDir = dbPrefix + "warmup";
			dbDir = dbPrefix + "db";
		}
		
		String warmupDbPath = null;
		String dbPath = null;
		if (withGraphPath) {
			if (options.has("sql")) {
				warmupDbPath = sqlDbAddr + "|" + sqlDbNameWarmup;
				dbPath = sqlDbAddr + "|" + sqlDbName;
			}
			else {
				warmupDbPath = warmupDbDir + (options.has("dex") ? "/graph.dex" : "");
				dbPath = dbDir + (options.has("dex") ? "/graph.dex" : "");
			}
		}
		
		String logPrefix = dbPrefix + dbShortName;
		if (dbInstanceName != null && !dbInstanceName.equals("")) logPrefix += "_" + dbInstanceName;
		String warmupLogFile = logPrefix + "-warmup" + argString + ".csv";
		String logFile = logPrefix + argString + ".csv";
		String summaryLogFile = logPrefix + "-summary" + argString + ".csv";
		String summaryLogFileText = logPrefix + "-summary" + argString + ".txt";
		
		
		/*
		 * Non-benchmark commands
		 */
		
		// TODO Ensure that there is only a single command specified or that there is only benchmark workload specified
		
		if (options.has("export-graphml") || options.has("export-n-graphml")) {
			
			boolean normalize = options.has("export-n-graphml");
			String arg = normalize ? "export-n-graphml" : "export-graphml";
			String file = options.hasArgument(arg) ? "" + options.valueOf(arg) : null;
			
			PrintStream out = System.out;
			if (file != null) {
				try {
					File f = new File(file);
					out = new PrintStream(f);
				}
				catch (IOException e) {
					ConsoleUtils.error("Cannot write to the specified file: " + file);
					System.exit(1);
					return;
				}
			}
			
			try {
				graphDescriptor = new GraphDescriptor(dbClass, dbDir, dbPath);
				Graph g = graphDescriptor.openGraph();
				GraphUtils.printGraphML(out, g, false);
				graphDescriptor.shutdownGraph();
			}
			finally {
				if (file != null) out.close();
			}
			
			return;
		}
		
		
		/*
		 * Check to make sure that we have specified workloads to run
		 */
		
		if (workloads.isEmpty()) {
			ConsoleUtils.error("There are no workloads specified (please use --help for a list)");
			System.exit(1);
			return;
		}

		
		/*
		 * Print benchmark info
		 */
		
		ConsoleUtils.sectionHeader("Tinkubator Graph Database Benchmark");
		
		System.out.println("Database    : " + dbShortName);
		System.out.println("Instance    : " + (dbInstanceName != null && !dbInstanceName.equals("") ? dbInstanceName : "<default>"));
		System.out.println("Directory   : " + dirResults);
		System.out.println("Log File    : " + logFile);
		System.out.println("Summary Log : " + summaryLogFile);
		System.out.println("Summary File: " + summaryLogFileText);
		
		
		/*
		 * Run the benchmark
		 */
		
		LinkedHashMap<String, String> resultFiles = new LinkedHashMap<String, String>();
		
		if (warmup) {
			ConsoleUtils.sectionHeader("Warmup Run");
			graphDescriptor = new GraphDescriptor(dbClass, warmupDbDir, warmupDbPath);
			try {
				warmupBenchmark.runBenchmark(graphDescriptor, warmupLogFile, numThreads);
			}
			catch (Throwable t) {
				ConsoleUtils.error(t.getMessage());
				t.printStackTrace(System.err);
				System.exit(1);
			}
			if (CPL.isAttached() && options.has("annotation")) {
				CPLFile.lookup(new File(warmupLogFile)).addProperty("ANNOTATION",
						options.valueOf("annotation").toString());
			}
			Cache.dropAll();
		}
		
		ConsoleUtils.sectionHeader("Benchmark Run");
		graphDescriptor = new GraphDescriptor(dbClass, dbDir, dbPath);
		try {
			benchmark.runBenchmark(graphDescriptor, logFile, numThreads);
		}
		catch (Throwable t) {
			ConsoleUtils.error(t.getMessage());
			t.printStackTrace(System.err);
			System.exit(1);
		}
		resultFiles.put(dbShortName, logFile);
		if (CPL.isAttached() && options.has("annotation")) {
			CPLFile.lookup(new File(logFile)).addProperty("ANNOTATION",
					options.valueOf("annotation").toString());
		}
		
		
		/*
		 * Create file with summarized results from all databases and operations
		 */
		
		ConsoleUtils.sectionHeader("Summary");
		
		SummaryLogWriter summaryLogWriter = new SummaryLogWriter(resultFiles);
		summaryLogWriter.writeSummary(summaryLogFile);
		summaryLogWriter.writeSummaryText(summaryLogFileText);
		summaryLogWriter.writeSummaryText(null);
		if (CPL.isAttached() && options.has("annotation")) {
			CPLFile.lookup(new File(summaryLogFile)).addProperty("ANNOTATION",
					options.valueOf("annotation").toString());
			CPLFile.lookup(new File(summaryLogFileText)).addProperty("ANNOTATION",
					options.valueOf("annotation").toString());
		}
	}

	
	/*
	 * Instance Code
	 */
	
	private int opCount = DEFAULT_OP_COUNT;
	private String PROPERTY_KEY = "_id";
	private int[] kHops;
	private boolean ingestAsUndirected;

	private String[] graphmlFilenames = null;
	private GraphGenerator[] graphGenerators = null;
	private OptionSet options = null;

	public BenchmarkMicro(String[] graphmlFilenames,
			GraphGenerator[] graphGenerators, OptionSet options,
			int opCount, int[] kHops, boolean ingestAsUndirected) {
		this.graphmlFilenames = graphmlFilenames;
		this.graphGenerators = graphGenerators;
		this.options = options;
		this.opCount = opCount;
		this.kHops = kHops;
		this.ingestAsUndirected = ingestAsUndirected;
	}

	@Override
	public ArrayList<OperationFactory> createOperationFactories() {
		ArrayList<OperationFactory> operationFactories = new ArrayList<OperationFactory>();

		for (String graphmlFilename : graphmlFilenames) {

			// DELETE the graph (also invoked for the ingest benchmark)
			if (options.has("ingest") || options.has("delete-graph")) {
				if (numThreads != 1) {
					throw new UnsupportedOperationException("Operations \"ingest\" "
							+"and \"delete-graph\" are not supported in the "
							+"multi-threaded mode");
				}
				operationFactories.add(new OperationFactoryGeneric(
						OperationDeleteGraph.class, 1));
			}

			// INGEST benchmarks
			if (options.has("ingest")) {
				operationFactories.add(new OperationFactoryGeneric(
						OperationLoadGraphML.class, 1,
						new Object[] { graphmlFilename, ingestAsUndirected },
						LogUtils.pathToName(graphmlFilename)));
			}
			
			// GENERATE benchmarks
			if (options.has("generate")) {
				if (numThreads != 1) {
					throw new UnsupportedOperationException("Operation \"generate\" "
							+"is not supported in the multi-threaded mode");
				}
				for (GraphGenerator g : graphGenerators) {
					operationFactories.add(new OperationFactoryGeneric(
							OperationGenerateGraph.class, 1, new GraphGenerator[] { g }));
				}
			}

			// GET microbenchmarks
			if (options.has("get")) {
				operationFactories.add(new OperationFactoryGeneric(
						OperationGetManyVertices.class, 1,
						new Integer[] { opCount }));
				
				operationFactories.add(new OperationFactoryGeneric(
						OperationGetManyEdges.class, 1,
						new Integer[] { opCount }));
				
				// GET_NEIGHBORS ops and variants
				
				operationFactories.add(new OperationFactoryRandomVertex(
						OperationGetFirstNeighbor.class, opCount));
 
				operationFactories.add(new OperationFactoryRandomVertex(
						OperationGetRandomNeighbor.class, opCount));
 
				operationFactories.add(new OperationFactoryRandomVertex(
						OperationGetAllNeighbors.class, opCount));
			}

            // GET-PROPERTY microbenchmarks
            if (options.has("get-property")) {
				operationFactories.add(new OperationFactoryGeneric(
						OperationGetManyVertexProperties.class, 1,
						new Object[] { PROPERTY_KEY, opCount }));

				operationFactories.add(new OperationFactoryGeneric(
						OperationGetManyEdgeProperties.class, 1,
						new Object[] { PROPERTY_KEY, opCount }));
            }
			
			// GET_K_NEIGHBORS ops and variants
			if (options.has("get-k")) {				
				for (int k : kHops) {
					operationFactories.add(new OperationFactoryRandomVertex(
							OperationGetKFirstNeighbors.class, opCount, new Integer[] { k }, "" + k));
				}
				
				for (int k : kHops) {				
					operationFactories.add(new OperationFactoryRandomVertex(
							OperationGetKRandomNeighbors.class, opCount, new Integer[] { k }, "" + k));
				}
				
				for (int k : kHops) {
					operationFactories.add(new OperationFactoryRandomVertex(
							OperationGetKHopNeighbors.class, opCount, new Integer[] { k }, "" + k));
				}
			}
			
			// SHORTEST PATH
			if (options.has("shortest-path")) {
				operationFactories.add(new OperationFactoryRandomVertexPair(
						OperationGetShortestPath.class, opCount / 2));
            }

			if (options.has("shortest-path-property") || options.has("shortest-path-prop")) {	
				if (numThreads != 1) {
					throw new UnsupportedOperationException("Operation \"shortest-path-prop\" "
							+"is not supported in the multi-threaded mode");
				}
				operationFactories.add(new OperationFactoryRandomVertexPair(
						OperationGetShortestPathProperty.class, opCount / 2));
			}
			
			// CLUSTERING COEFFICIENT benchmarks
			if (options.has("clustering-coef") || options.has("clustering-coeff")) {
				//operationFactories.add(new OperationFactoryRandomVertex(
				//		OperationLocalClusteringCoefficient.class, opCount));
				
				operationFactories.add(new OperationFactoryGeneric(
						OperationGlobalClusteringCoefficient.class, 1));
				
				operationFactories.add(new OperationFactoryGeneric(
						OperationNetworkAverageClusteringCoefficient.class, 1));
			}
			
			// ADD/SET microbenchmarks
			if (options.has("add")) {
				if (numThreads != 1 && GlobalConfig.transactionBufferSize != 1) {
					throw new UnsupportedOperationException("Set property operations inside \"add\" "
							+"are not supported in the multi-threaded mode with tx-buffer > 1");
				}
				
				operationFactories.add(new OperationFactoryGeneric(
						OperationAddManyVertices.class, 1,
						new Integer[] { opCount }));
				
				operationFactories.add(new OperationFactoryGeneric(
						OperationSetManyVertexProperties.class, 1,
						new Object[] { PROPERTY_KEY, opCount }));
				
				operationFactories.add(new OperationFactoryGeneric(
						OperationAddManyEdges.class, 1,
						new Integer[] { opCount }));
				
				operationFactories.add(new OperationFactoryGeneric(
						OperationSetManyEdgeProperties.class, 1,
						new Object[] { PROPERTY_KEY, opCount }));
			}
					
		}

		return operationFactories;
	}
}
