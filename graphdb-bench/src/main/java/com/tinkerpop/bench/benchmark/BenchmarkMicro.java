package com.tinkerpop.bench.benchmark;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Pattern;

import com.tinkerpop.bench.Bench;
import com.tinkerpop.bench.BenchResults;
import com.tinkerpop.bench.DatabaseEngine;
import com.tinkerpop.bench.GlobalConfig;
import com.tinkerpop.bench.GraphDescriptor;
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
import com.tinkerpop.bench.util.ConsoleUtils;
import com.tinkerpop.bench.util.FileUtils;
import com.tinkerpop.bench.util.GraphUtils;
import com.tinkerpop.bench.util.LogUtils;
import com.tinkerpop.bench.util.MathUtils;
import com.tinkerpop.bench.util.OutputUtils;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.extensions.impls.sql.SqlGraph;

import edu.harvard.pass.cpl.CPL;
import edu.harvard.pass.cpl.CPLException;
import edu.harvard.pass.cpl.CPLFile;

import joptsimple.OptionParser;
import joptsimple.OptionSet;


/**
 * The benchmark suite
 * 
 * @author Peter Macko (pmacko@eecs.harvard.edu)
 * @author Daniel Margo (dmargo@eecs.harvard.edu)
 * @author Alex Averbuch (alex.averbuch@gmail.com)
 */
public class BenchmarkMicro extends Benchmark {
	
	/// The default file for ingest
	public static final String DEFAULT_INGEST_FILE = "barabasi_1k_5k.graphml";
	
	/// The defaults
	public static final int DEFAULT_NUM_THREADS = 1;
	public static final int DEFAULT_OP_COUNT = 100;
	public static final String DEFAULT_K_HOPS = "1:5";
	public static final int DEFAULT_BARABASI_N = 1000;
	public static final int DEFAULT_BARABASI_M = 5;
	
	/// The number of threads
	private static int numThreads = DEFAULT_NUM_THREADS;
	
	/// The results of last successful benchmark
	// TODO Do this properly
	public static BenchResults lastBenchmarkResults = null;
	
	
	/**
	 * Print the help
	 */
	protected static void help() {
		
		System.err.println("Usage: runBenchmarkSuite.sh OPTIONS");
		System.err.println("");
		System.err.println("General options:");
		System.err.println("  --annotation TEXT       Include an annotation in the " +
										"disclosed provenance");
		System.err.println("  --dir DIR               Set the database and results directory");
		System.err.println("  --dumb-terminal         Use the dumb terminal settings");
		System.err.println("  --help                  Print this help message");
		System.err.println("  --no-cache-pollution    Disable cache pollution before benchmarks");
		System.err.println("  --no-color              Disable color output to the terminal");
		System.err.println("  --no-logs               Disable logs");
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
		System.err.println("  --database, -d NAME     Select a specific graph or a database instance");
		System.err.println("  --db-buffer-pool SIZE   Set or check the buffer pool size (in MB)");
		System.err.println("  --db-config K=VAL,...   Specify one or more database configuration properties");
		System.err.println("  --keep-temp-copy        Keep (do not delete) the temp. copy of the instance");
		System.err.println("  --neo-caches A:B:..     Specify neo4j database cache configuration");
		System.err.println("  --neo-gcr A:B           Specify neo4j GCR cache configuration");
		System.err.println("  --sql-addr ADDR         Specify the SQL connection string (w/o DB name)");
		System.err.println("  --warmup-sql DB_NAME    Specify the SQL database name for warmup");
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
		System.err.println("  --update-directly       Run non-load updates directly, not on a temp. copy");
		System.err.println("  --use-specialized       Enable the use of specialized routines");
		System.err.println("  --use-stored-procedures Enable the use of stored procedures");
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
	 * @return 0 on success, otherwise an error code
	 * @throws Exception on error
	 */
	public static int run(String[] args) throws Exception {
		
		/*
		 * Initialize
		 */
		
		
		// Initialize the static non-final variables 
		
		lastBenchmarkResults = null;
		
		
		// Do a quick initial pass over the command-line arguments
		
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
		
		parser.accepts("d").withRequiredArg().ofType(String.class);
		parser.accepts("database").withRequiredArg().ofType(String.class);
		parser.accepts("db-buffer-pool").withRequiredArg().ofType(Integer.class);
		parser.accepts("db-config").withRequiredArg().ofType(String.class).withValuesSeparatedBy(',');
		parser.accepts("dir").withRequiredArg().ofType(String.class);
		parser.accepts("dumb-terminal");
		parser.accepts("help");
		parser.accepts("keep-temp-copy");
		parser.accepts("neo-caches").withRequiredArg().ofType(String.class);
		parser.accepts("neo-gcr").withRequiredArg().ofType(String.class);
		parser.accepts("no-cache-pollution");
		parser.accepts("no-color");
		parser.accepts("no-logs");
		parser.accepts("no-provenance");
		parser.accepts("no-warmup");
		parser.accepts("no-workloads");	/* undocumented on purpose */
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
		parser.accepts("update-directly");
		parser.accepts("use-stored-procedures");
		parser.accepts("use-specialized");
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
			return 1;
		}
		
		List<String> nonOptionArguments = options.nonOptionArguments();
		if (!nonOptionArguments.isEmpty()) {
			ConsoleUtils.error("Invalid options (please use --help for a list): " + nonOptionArguments);
			return 1;
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
			return 0;
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
		
		boolean keepTempCopy = false;
		if (options.has("keep-temp-copy")) {
			keepTempCopy = true;
		}
		
		if (options.has("no-cache-pollution")) {
			GlobalConfig.polluteCache = false;
		}
		
		boolean logs = true;
		if (options.has("no-logs")) {
			logs = false;
		}
		
		boolean provenance = true;
		if (options.has("no-provenance")) {
			provenance = false;
		}
		
		boolean warmup = true;
		if (options.has("no-warmup")) {
			warmup = false;
		}
		
		boolean skipWorkloads = false;
		if (options.has("no-workloads")) {
			skipWorkloads = true;
		}
		
		boolean updateDirectly = false;
		if (options.has("update-directly")) {
			updateDirectly = true;
		}
		
		if (options.has("use-specialized")) {
			GlobalConfig.useSpecializedProcedures = true;
		}
		
		if (options.has("use-stored-procedures")) {
			GlobalConfig.useStoredProcedures = true;
		}
		
		if (options.has("single-db-connection")) {
			GlobalConfig.oneDbConnectionPerThread = false;
		}
		
		if (options.has("db-buffer-pool")) {
			GlobalConfig.databaseBufferPoolSize = (Integer) options.valueOf("db-buffer-pool");
			if (GlobalConfig.databaseBufferPoolSize < 16) {
				ConsoleUtils.error("The specified database buffer pool is too small -- must be at least 16 MB");
				return 1;
			}
		}
		else {
			GlobalConfig.databaseBufferPoolSize = Integer.parseInt(Bench.getProperty(Bench.DB_BUFFER_POOL_SIZE));
			if (GlobalConfig.databaseBufferPoolSize < 16) {
				ConsoleUtils.error("The specified database buffer pool is too small -- must be at least 16 MB");
				return 1;
			}
		}
		
		if (options.has("threads")) {
			numThreads = (Integer) options.valueOf("threads");
			if (numThreads < 1) {
				ConsoleUtils.error("Invalid number of threads -- must be at least 1");
				return 1;
			}
		}
		
		if (options.has("tx-buffer")) {
			GlobalConfig.transactionBufferSize = (Integer) options.valueOf("tx-buffer");
			if (GlobalConfig.transactionBufferSize < 1) {
				ConsoleUtils.error("Invalid size of the transaction buffer -- must be at least 1");
				return 1;
			}
		}
		
		int opCount = DEFAULT_OP_COUNT;
		if (options.has("op-count")) opCount = (Integer) options.valueOf("op-count");
		
		int warmupOpCount = opCount;
		if (options.has("warmup-op-count")) warmupOpCount = (Integer) options.valueOf("warmup-op-count");
		
		int[] kHops;
		String kHopsStr;
		if (options.has("k-hops")) {
			kHopsStr = (String) options.valueOf("k-hops");
		}
		else {
			kHopsStr = DEFAULT_K_HOPS;
		}
	
		int kc = kHopsStr.indexOf(':');
		if (kc >= 0) {
			int k1, k2;
			try {
				k1 = Integer.parseInt(kHopsStr.substring(0, kc));
				k2 = Integer.parseInt(kHopsStr.substring(kc + 1));
			}
			catch (NumberFormatException e) {
				ConsoleUtils.error("Invalid range of k hops (not a number).");
				return 1;
			}
			if (k1 <= 0 || k1 > k2) {
				ConsoleUtils.error("Invalid range of k hops.");
				return 1;
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
				return 1;
			}
			if (kHops[0] <= 0) {
				ConsoleUtils.error("Invalid number of k hops (must be positive).");
				return 1;
			}
		}
		
		if (provenance) {
			if (!CPL.isInstalled()) {
				ConsoleUtils.error("CPL is not installed. Use --no-provenance to disable provenance collection.");
				return 1;
			}
			else if (!CPL.isAttached()) {
				try {
					CPL.attachODBC(Bench.getProperty(Bench.CPL_ODBC_DSN, "DSN=CPL"));
				}
				catch (CPLException e) {
					ConsoleUtils.error("Could not initialize provenance collection:");
					ConsoleUtils.error("  " + e.getMessage());
					return 1;
				}
			}
		}
		
		
		/*
		 * Get the list of workloads
		 */
		
		LinkedList<Workload> workloads = new LinkedList<Workload>();

		@SuppressWarnings("unused")
		boolean hasUpdates = false;
		boolean hasLoadUpdates = false;
		boolean hasNonLoadUpdates = false;
		boolean hasReadOnly = false;
		boolean hasIngest = false;

		for (Workload w : Workload.WORKLOADS.values()) {
			if (options.has(w.getShortName())) {
				
				workloads.add(w);
				
				if (w.isUpdate()) {
					hasUpdates = true;
					if (w.getUpdateCategory() == Workload.UpdateCategory.LOAD_UPDATE) hasLoadUpdates = true;
					if (w.getUpdateCategory() != Workload.UpdateCategory.LOAD_UPDATE) hasNonLoadUpdates = true;
				}
				else {
					hasReadOnly = true;
				}
				
				if (w.getShortName().equalsIgnoreCase("ingest")) hasIngest = true;
			}
		}
		
		
		/*
		 * Sanitize & check consistency
		 */
		
		if (hasIngest) {
			if (workloads.size() != 1) {
				ConsoleUtils.error("Cannot combine --ingest with any other operation");
				return 1;
			}
		}
		
		boolean isIngest = hasIngest;	// Can do this because we have just established that --ingest can be used only by itself
		
		if (hasLoadUpdates) {
			
			if (hasNonLoadUpdates || hasReadOnly) {
				ConsoleUtils.error("Cannot combine load operations (such as --ingest, --generate) with other operations");
				return 1;
			}
			
			if (updateDirectly) {
				ConsoleUtils.warn("Argument --update-directly is implicit with load operations such as --ingest or --generate");
			}
			
			updateDirectly = true;
		}
		else {
			
			if (updateDirectly && !hasNonLoadUpdates) {
				ConsoleUtils.error("The --update-directly option can be used only if there is at least one update workload");
				return 1;
			}
		}
		
		if (keepTempCopy) {
			
			if (updateDirectly) {
				ConsoleUtils.error("The --keep-temp-copy option cannot be combined with --update-directly");
				return 1;
			}
			
			if (hasLoadUpdates) {
				ConsoleUtils.error("Cannot combine --keep-temp-copy with load operations (such as --ingest, --generate)");
				return 1;
			}
			
			if (!hasNonLoadUpdates) {
				ConsoleUtils.error("The --keep-temp-copy option can be used only if there is at least one update workload");
				return 1;
			}
		}

		
		/*
		 * Get the name of the database instance
		 */
		
		String dbInstanceName = null;
		if (options.has("d") || options.has("database")) {
			dbInstanceName = options.valueOf(options.has("d") ? "d" : "database").toString();
			if (!Pattern.matches("^[a-z][a-z0-9_]*$", dbInstanceName)) {
	    		throw new RuntimeException("Invalid database name (can contain only lower-case letters, "
	    				+ "numbers, and _, and has to start with a letter)");
	    	}
		}

		
		/*
		 * Get the database engine
		 */
		
		String dbShortName = null;
		DatabaseEngine dbEngine = null;
		HashMap<String, String> warmupDbConfig = new HashMap<String, String>();
		HashMap<String, String> dbConfig = new HashMap<String, String>();
		
		for (DatabaseEngine e : DatabaseEngine.ENGINES.values()) {
			if (options.has(e.getShortName())) {
				if (dbEngine != null) {
					ConsoleUtils.error("Multiple databases are selected, but only one is allowed.");
					return 1;
				}
				dbEngine = e;
				dbShortName = dbEngine.getShortName();
			}
		}
		if (dbShortName == null) {
			ConsoleUtils.error("No database is selected (please use --help for a list of options).");
			return 1;
		}
		
		
		/*
		 * Get the name of the results directory
		 */
		
		String dirResults;
		if (options.has("dir")) {
			dirResults = options.valueOf("dir").toString();
			if (!dirResults.endsWith("/")) dirResults += "/";
		}
		else {
			String propDirResults = Bench.getProperty(Bench.RESULTS_DIRECTORY);
			if (propDirResults == null) {
				ConsoleUtils.error("Property \"" + Bench.RESULTS_DIRECTORY + "\" is not set and --dir is not specified.");
				return 1;
			}
			if (!propDirResults.endsWith("/")) propDirResults += "/";
			dirResults = propDirResults + "Micro/";
		}
		
		
		/*
		 * Set the file, directory, and database names
		 */
		
		String dbPrefix = dirResults + dbShortName;
		if (dbInstanceName != null && !dbInstanceName.equals("")) dbPrefix += "_" + dbInstanceName;
		dbPrefix += "/";
		
		String warmupDbDir = dbPrefix + "warmup";
		String dbDir = dbPrefix + "db";
		
		boolean duplicateDatabaseInstance = !updateDirectly && hasNonLoadUpdates;
		String warmupDbDirSource = warmupDbDir;
		String dbDirSource = dbDir;
				
		if (duplicateDatabaseInstance) {
			warmupDbDir += "-clone";
			dbDir += "-clone";
		}

		
		/*
		 * Arguments specific to the various database engines
		 */

		// Neo4j

		if (options.has("neo")) {
			
			String[] config;
			
			
			/*
			 * Neo4j buffer pool caches
			 */

			if (options.has("neo-caches")) {
				
				if (options.has("db-buffer-pool")) {
					ConsoleUtils.error("Cannot combine the --neo-caches and the --db-buffer-pool options");
					return 1;
				}
				
				String s = options.valueOf("neo-caches").toString();
				config = s.split(":");
				
				if (config.length == 1) {
					s = Bench.getProperty(Bench.DB_NEO_CACHES);
					int t = Integer.parseInt(config[0]);
					if (t < 16) {
						ConsoleUtils.error("Invalid neo-caches; the total size must be at least 16");
						return 1;
					}
					GlobalConfig.databaseBufferPoolSize = t;
					config = MathUtils.toStringArray(MathUtils.adjustSumApproximate(MathUtils.fromStringArray(s.split(":")), t, 1));
				}
			}
			else {
				
				// Default behavior: Scale the database cache sizes according
				// to the file sizes, or scale the defaults if the database
				// does not yet exist
				
				File dir = new File(dbDir);
				if (dir.exists() && dir.isDirectory() && !isIngest) {
					
					long[] adjustedSizes = FileUtils.getScaledFileSizesMB(dir, GlobalConfig.databaseBufferPoolSize,
							"neostore.nodestore.db", "neostore.relationshipstore.db", "neostore.propertystore.db",
							"neostore.propertystore.db.strings", "neostore.propertystore.db.arrays");
 
					config = MathUtils.toStringArray(adjustedSizes);
				}
				else {
					String s = Bench.getProperty(Bench.DB_NEO_CACHES);
					config = MathUtils.toStringArray(MathUtils.adjustSumApproximate(MathUtils.fromStringArray(s.split(":")),
							GlobalConfig.databaseBufferPoolSize, 1));
				}
			}
			
			
			// Parse and set the neo4j persistent cache sizes

			if (config.length != 5) {
				ConsoleUtils.error("Invalid neo-caches. The format must be A:B:C:D:E, where:");
				System.err.println("           A = neostore.nodestore.db.mapped_memory (in MB)");
				System.err.println("           B = neostore.relationshipstore.db.mapped_memory (in MB)");
				System.err.println("           C = neostore.propertystore.db.mapped_memory (in MB)");
				System.err.println("           D = neostore.propertystore.db.strings.mapped_memory (in MB)");
				System.err.println("           E = neostore.propertystore.db.arrays.mapped_memory (in MB)");
				return 1;
			}

			HashMap<String, String> m = new HashMap<String, String>();
			m.put("neostore.nodestore.db.mapped_memory"            , "" + config[0] + "M");
			m.put("neostore.relationshipstore.db.mapped_memory"    , "" + config[1] + "M");
			m.put("neostore.propertystore.db.mapped_memory"        , "" + config[2] + "M");
			m.put("neostore.propertystore.db.strings.mapped_memory", "" + config[3] + "M");
			m.put("neostore.propertystore.db.arrays.mapped_memory" , "" + config[4] + "M");
			warmupDbConfig.putAll(m);
			dbConfig.putAll(m);
			
			int sum = 0;
			for (String c : config) sum += Integer.parseInt(c);
			GlobalConfig.databaseBufferPoolSize = sum;
			
			
			/*
			 * Neo4j GCR caches
			 */

			if (options.has("neo-gcr")) {
				String s = options.valueOf("neo-gcr").toString();
				config = s.split(":");
			}
			else {
				
				// Default behavior: Scale the database cache sizes according
				// to the number of nodes and relationships, or set to the
				// defaults if the database does not yet exist
				
				File dir = new File(dbDir);
				if (dir.exists() && dir.isDirectory() && !isIngest) {
										
					String s = Bench.getProperty(Bench.DB_NEO_GCR_TOTAL);
					long gcrCacheSize = Long.valueOf(s);
					if (!s.equals("" + gcrCacheSize)) {
						ConsoleUtils.error("Invalid number format of property: " + Bench.DB_NEO_GCR_TOTAL);
						return 1;
					}
					
					long[] onDiskSizes = FileUtils.getFileSizesB(dir, "neostore.nodestore.db", "neostore.relationshipstore.db");
					
					// In-memory size = (in-memory record size) * ((file size) / (on-disk record size))
					// Reference: http://docs.neo4j.org/chunked/stable/configuration-caches.html

					long[] inMemSizesMB = new long[] {
						Math.round(Math.ceil((344.0 * onDiskSizes[0] /  9.0) / 10485767.0)),
						Math.round(Math.ceil((208.0 * onDiskSizes[1] / 33.0) / 10485767.0))
					};
					
					config = MathUtils.toStringArray(MathUtils.adjustSumApproximate(inMemSizesMB, gcrCacheSize, 1));
				}
				else {
					String s = Bench.getProperty(Bench.DB_NEO_GCR);
					config = s.split(":");
				}
			}
			
			
			// Parse and set the neo4j GCR settings

			if (config.length != 2) {
				ConsoleUtils.error("Invalid neo-gcr. The format must be A:B, where:");
				System.err.println("           A = node_cache_size (in MB)");
				System.err.println("           B = relationship_cache_size (in MB)");
				return 1;
			}

			m = new HashMap<String, String>();
			m.put("cache_type"                 , "gcr");
			m.put("node_cache_size"            , "" + config[0] + "M");
			m.put("relationship_cache_size"    , "" + config[1] + "M");
			warmupDbConfig.putAll(m);
			dbConfig.putAll(m);
		}
		
		
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
					return 1;
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
					return 1;
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
					return 1;
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
			
			
			// Set the database paths
			
			warmupDbConfig.put("path", sqlDbAddr + "|" + sqlDbNameWarmup);
			dbConfig.put("path", sqlDbAddr + "|" + sqlDbName);
		}
		
		
		// Database properties
		
		if (options.has("db-config")) {
			for (Object obj : options.valuesOf("db-config")) {
				String property = obj.toString();
				
				int sep = property.indexOf('=');
				if (sep <= 0) {
					ConsoleUtils.error("Each database configuration property must be a key-value pair separated by '='");
					return 1;
				}
				
				String key = property.substring(0, sep);
				String value = property.substring(sep + 1);
				
				if (key.startsWith("+")) {
					dbConfig.put(key.substring(1), value);
				}
				else if (key.startsWith("^")) {
					warmupDbConfig.put(key.substring(1), value);
				}
				else {				
					dbConfig.put(key, value);
					warmupDbConfig.put(key, value);
				}
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
				int n = options.has("barabasi-n") ? (Integer) options.valueOf("barabasi-n") : DEFAULT_BARABASI_N;
				int m = options.has("barabasi-m") ? (Integer) options.valueOf("barabasi-m") : DEFAULT_BARABASI_M;
				graphGenerator = new SimpleBarabasiGenerator(n, m);
			}
			
			if (graphGenerator == null) {
				ConsoleUtils.error("Unrecognized graph generation model");
				return 1;
			}
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
					return 1;
				}
				if (!dirGraphML.endsWith("/")) dirGraphML += "/";
				if (!(new File(dirGraphML + ingestFile)).exists()) {
					ConsoleUtils.error("File \"" + ingestFile + "\" does not exist.");
					return 1;
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
					return 1;
				}
				if (!dirGraphML.endsWith("/")) dirGraphML += "/";
				if (!(new File(dirGraphML + warmupIngestFile)).exists()) {
					ConsoleUtils.error("File \"" + warmupIngestFile + "\" does not exist.");
					return 1;
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
		
		String argString = sb.toString().replaceAll("\\s+", "").replaceAll("/", "-");
		
		
		/*
		 * Set the log file names
		 */
		
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
					return 1;
				}
			}
			
			try {
				graphDescriptor = new GraphDescriptor(dbEngine, dbDir, dbConfig);
				Graph g = graphDescriptor.openGraph(GraphDescriptor.OpenMode.DEFAULT);
				GraphUtils.printGraphML(out, g, false);
				graphDescriptor.shutdownGraph();
			}
			finally {
				if (file != null) out.close();
			}
			
			return 0;
		}
		
		
		/*
		 * Check to make sure that we have specified workloads to run
		 */
		
		if (workloads.isEmpty()) {
			ConsoleUtils.error("There are no workloads specified (please use --help for a list)");
			return 1;
		}

		
		/*
		 * Print benchmark info
		 */
		
		ConsoleUtils.sectionHeader("Tinkubator Graph Database Benchmark");
		
		System.out.println("Database    : " + dbShortName);
		System.out.println("Instance    : " + (dbInstanceName != null && !dbInstanceName.equals("") ? dbInstanceName : "<default>"));
		System.out.println("Buffer Pool : " + GlobalConfig.databaseBufferPoolSize + " MB");
		System.out.println("Directory   : " + dirResults);
		
		if (logs) {
			System.out.println("Log File    : " + logFile);
			System.out.println("Summary Log : " + summaryLogFile);
			System.out.println("Summary File: " + summaryLogFileText);
		}
		
		if (dbConfig.size() > 0) {
			System.out.println("");
			System.out.println("Database Configuration:");
			TreeSet<String> t = new TreeSet<String>(dbConfig.keySet());
			for (String k : t) {
				System.out.println("    " + k + " = " + dbConfig.get(k));
			}
		}
		
		
		/*
		 * Run the warmup benchmark
		 */
		
		LinkedHashMap<String, String> resultFiles = new LinkedHashMap<String, String>();
		
		if (warmup) {
			ConsoleUtils.sectionHeader("Warmup Run");
			
			if (duplicateDatabaseInstance) {
				System.out.print("Creating a temporary copy: ");
				long start = System.currentTimeMillis();
				try {
					dbEngine.duplicateDatabase(warmupDbDirSource, warmupDbDir, warmupDbConfig);
				}
				catch (Throwable t) {
					long tm = System.currentTimeMillis() - start;
					System.out.println("failed [" + OutputUtils.formatTimeMS(tm) + "]");
					ConsoleUtils.error(t.getMessage());
					t.printStackTrace(System.err);
					return 1;
				}
				long t = System.currentTimeMillis() - start;
				System.out.println("done [" + OutputUtils.formatTimeMS(t) + "]");
			}
			
			graphDescriptor = new GraphDescriptor(dbEngine, warmupDbDir, warmupDbConfig);
			
			try {
				if (!skipWorkloads) {
					warmupBenchmark.runBenchmark(graphDescriptor, logs ? warmupLogFile : null,
							isIngest ? GraphDescriptor.OpenMode.BULKLOAD : GraphDescriptor.OpenMode.DEFAULT, numThreads);
				}
			}
			catch (Throwable t) {
				ConsoleUtils.error(t.getMessage());
				t.printStackTrace(System.err);
				return 1;
			}
			if (CPL.isAttached() && options.has("annotation") && logs) {
				CPLFile.lookup(new File(warmupLogFile)).addProperty("ANNOTATION",
						options.valueOf("annotation").toString());
			}
			
			if (duplicateDatabaseInstance && !keepTempCopy) {
				System.out.print("Deleting the temporary copy: ");
				long start = System.currentTimeMillis();
				try {
					if (warmupDbDir.equals(warmupDbDirSource)) throw new InternalError();
					dbEngine.deleteDatabase(warmupDbDir, warmupDbConfig);
				}
				catch (Throwable t) {
					long tm = System.currentTimeMillis() - start;
					System.out.println("failed [" + OutputUtils.formatTimeMS(tm) + "]");
					ConsoleUtils.error(t.getMessage());
					t.printStackTrace(System.err);
					return 1;
				}
				long t = System.currentTimeMillis() - start;
				System.out.println("done [" + OutputUtils.formatTimeMS(t) + "]");
			}
			
			Cache.dropAll();
		}
		
		
		/*
		 * Run the benchmark
		 */
		
		ConsoleUtils.sectionHeader("Benchmark Run");
		
		if (duplicateDatabaseInstance) {
			System.out.print("Creating a temporary copy: ");
			long start = System.currentTimeMillis();
			try {
				dbEngine.duplicateDatabase(dbDirSource, dbDir, warmupDbConfig);
			}
			catch (Throwable t) {
				long tm = System.currentTimeMillis() - start;
				System.out.println("failed [" + OutputUtils.formatTimeMS(tm) + "]");
				ConsoleUtils.error(t.getMessage());
				t.printStackTrace(System.err);
				return 1;
			}
			long t = System.currentTimeMillis() - start;
			System.out.println("done [" + OutputUtils.formatTimeMS(t) + "]");
		}
		
		graphDescriptor = new GraphDescriptor(dbEngine, dbDir, dbConfig);
		
		BenchResults results = null;
		try {
			if (!skipWorkloads) {
				results = benchmark.runBenchmark(graphDescriptor, logs ? logFile : null,
						isIngest ? GraphDescriptor.OpenMode.BULKLOAD : GraphDescriptor.OpenMode.DEFAULT, numThreads);
			}
		}
		catch (Throwable t) {
			ConsoleUtils.error(t.getMessage());
			t.printStackTrace(System.err);
			return 1;
		}
		if (logs) resultFiles.put(dbShortName, logFile);
		if (CPL.isAttached() && options.has("annotation") && logs) {
			CPLFile.lookup(new File(logFile)).addProperty("ANNOTATION",
					options.valueOf("annotation").toString());
		}
		
		if (duplicateDatabaseInstance && !keepTempCopy) {
			System.out.print("Deleting the temporary copy: ");
			long start = System.currentTimeMillis();
			try {
				if (dbDir.equals(dbDirSource)) throw new InternalError();
				dbEngine.deleteDatabase(dbDir, dbConfig);
			}
			catch (Throwable t) {
				long tm = System.currentTimeMillis() - start;
				System.out.println("failed [" + OutputUtils.formatTimeMS(tm) + "]");
				ConsoleUtils.error(t.getMessage());
				t.printStackTrace(System.err);
				return 1;
			}
			long t = System.currentTimeMillis() - start;
			System.out.println("done [" + OutputUtils.formatTimeMS(t) + "]");
		}
		
		
		/*
		 * Create file with summarized results from all databases and operations
		 */
		
		if (logs) {
				
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
		 * Finish
		 */
		
		lastBenchmarkResults = results;
		
		return 0;
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

		for (String ingestFile : graphmlFilenames) {
			
			
			/*
			 * Load workloads
			 */

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
				if (ingestFile.endsWith(".graphml")) {
					operationFactories.add(new OperationFactoryGeneric(
							OperationLoadGraphML.class, 1,
							new Object[] { ingestFile, ingestAsUndirected },
							LogUtils.pathToName(ingestFile)));
				}
				else if (ingestFile.endsWith(".fgf")) {
					if (ingestAsUndirected) {
						throw new UnsupportedOperationException("--ingest-as-undirected is not supported by the .fgf loader");
					}
					operationFactories.add(new OperationFactoryGeneric(
							OperationLoadFGF.class, 1,
							new Object[] { ingestFile },
							LogUtils.pathToName(ingestFile)));
				}
				else {
					throw new IllegalArgumentException("Unknown ingest file type");
				}
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
			
			
			/*
			 * Update workloads
			 */
			
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
			
			
			/*
			 * Read-only workloads & workloads with temporary updates
			 */

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
				for (int k : kHops) {
					operationFactories.add(new OperationFactoryRandomVertex(
							OperationGetKFirstNeighbors.class, opCount, new Object[] { k, Direction.BOTH }, "" + k + "-undirected"));
				}
				
				for (int k : kHops) {				
					operationFactories.add(new OperationFactoryRandomVertex(
							OperationGetKRandomNeighbors.class, opCount, new Object[] { k, Direction.BOTH }, "" + k + "-undirected"));
				}
				
				for (int k : kHops) {
					operationFactories.add(new OperationFactoryRandomVertex(
							OperationGetKHopNeighbors.class, opCount, new Object[] { k, Direction.BOTH }, "" + k + "-undirected"));
				}
			}
			
			// SHORTEST PATH
			if (options.has("shortest-path")) {
				operationFactories.add(new OperationFactoryRandomVertexPair(
						OperationGetShortestPath.class, opCount));
            }

			if (options.has("shortest-path-property") || options.has("shortest-path-prop")) {	
				if (numThreads != 1) {
					throw new UnsupportedOperationException("Operation \"shortest-path-prop\" "
							+"is not supported in the multi-threaded mode");
				}
				operationFactories.add(new OperationFactoryRandomVertexPair(
						OperationGetShortestPathProperty.class, opCount));
			}
			
			if (options.has("sssp")) {
				operationFactories.add(new OperationFactoryRandomVertex(
						OperationGetSingleSourceShortestPath.class, opCount));
            }
			
			if (options.has("sssp-prop")) {
				if (numThreads != 1) {
					throw new UnsupportedOperationException("Operation \"sssp-prop\" "
							+"is not supported in the multi-threaded mode");
				}
				operationFactories.add(new OperationFactoryRandomVertex(
						OperationGetSingleSourceShortestPathProperty.class, opCount));
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
			
			if (options.has("clustering-local")) {
				operationFactories.add(new OperationFactoryRandomVertex(
						OperationLocalClusteringCoefficient.class, opCount));
            }					
		}

		return operationFactories;
	}
}
