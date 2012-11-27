package com.tinkerpop.bench.web;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpServletRequest;

import scala.actors.threadpool.Arrays;

import com.tinkerpop.bench.Bench;
import com.tinkerpop.bench.DatabaseEngine;
import com.tinkerpop.bench.GlobalConfig;
import com.tinkerpop.bench.Workload;
import com.tinkerpop.bench.benchmark.BenchmarkMicro;


/**
 * A job in the web interface
 * 
 * @author Peter Macko (pmacko@eecs.harvard.edu)
 */
public class Job implements Comparable<Job> {
	
	private static SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("[yyyy/MM/dd HH:mm:ss]");
	
	private int id;
	private List<String> arguments;
	private String dbEngine;
	private String dbInstance;
	private boolean unlisted;
	
	protected File logFile;
	protected int status;
	protected int executionCount;
	private Date executionTime;
	
	private ExecutionThread current = null;
	private ExecutionThread last = null;
	private boolean bufferedThreadOutput = false;
	
	private static AtomicInteger lastId = new AtomicInteger(-1);

	
	/**
	 * Create an instance of a Job
	 */
	protected Job() {

		id = lastId.incrementAndGet();
		
		arguments = null;
		dbEngine = null;
		dbInstance = null;
		
		unlisted = false;
		
		status = -1;
		executionCount = 0;
		executionTime = null;
		logFile = null;
	}
	
	
	/**
	 * Create an instance of a Job
	 * 
	 * @param request the HTTP request from which to create the job
	 */
	public Job(HttpServletRequest request) {
		this(request, null, null);
	}
	
	
	/**
	 * Create an instance of a Job
	 * 
	 * @param request the HTTP request from which to create the job
	 * @param dbEngine a specific database engine name to use instead of the one from the request
	 * @param dbInstance a specific database instance to use instead of the one from the request
	 */
	public Job(HttpServletRequest request, String dbEngine, String dbInstance) {
		this();
		loadFromRequest(request, dbEngine, dbInstance);
	}
	
	
	/**
	 * Create an instance of a finished Job from a log file
	 * 
	 * @param file the log file
	 * @param dbEngine a specific database engine name
	 * @param dbInstance a specific database instance name
	 */
	public Job(File file, String dbEngine, String dbInstance) {
		this();
		loadFromLogFile(file, dbEngine, dbInstance);
	}
	
	
	/**
	 * Create an instance of a Job from a list of arguments
	 * 
	 * @param arguments the arguments
	 */
	public Job(final List<String> arguments) {
		this();
		
		this.arguments = new ArrayList<String>();
		this.arguments.add(Bench.graphdbBenchDir + "/runBenchmarkSuite.sh");
		
		
		// Copy the + arguments first 
		
		for (int i = 0; i < arguments.size(); i++) {
			String a = arguments.get(i);
			if (a.startsWith("+")) this.arguments.add(a);
		}
		
		
		// Further arguments that we need to run the job through the web interface

		this.arguments.add("--dumb-terminal");
		
		
		// Copy the arguments and extract the database engine and instance names
		
		for (int i = 0; i < arguments.size(); i++) {
			String a = arguments.get(i);
			if (a.startsWith("+")) continue;
			
			if (a.startsWith("--")) {
				if (DatabaseEngine.ENGINES.containsKey(a.substring(2))) {
					if (dbEngine != null) {
						throw new IllegalArgumentException("Cannot specify more than one database engine");
					}
					dbEngine = a.substring(2);
				}
			}
			
			if (a.equals("--database") || a.equals("-d")) {
				if (i + 1 >= arguments.size() || arguments.get(i + 1).startsWith("-")) {
					throw new IllegalArgumentException("The " + a + " option requires an argument");
				}
				if (dbInstance != null) {
					throw new IllegalArgumentException("Cannot specify more than one database instance");
				}
				dbInstance = arguments.get(i + 1);
				WebUtils.asssertDatabaseInstanceNameValidity(dbInstance);
			}
			
			this.arguments.add(a);
		}
		
		if (dbEngine == null) {
			throw new IllegalArgumentException("No database engine is specified");
		}
	}
	
	
	/**
	 * Create an instance of a Job from an array of arguments
	 * 
	 * @param arguments the arguments
	 */
	@SuppressWarnings("unchecked")
	public Job(final String... arguments) {
		this(Arrays.asList(arguments));
	}
	
	
	/**
	 * Load from an HTTP request
	 * 
	 * @param request the HTTP request from which to create the job
	 * @param _dbEngine a specific database engine name to use instead of the one from the request
	 * @param _dbInstance a specific database instance to use instead of the one from the request
	 */
	protected void loadFromRequest(HttpServletRequest request, String _dbEngine, String _dbInstance) {
		
		arguments = new ArrayList<String>();
		status = -1;
		executionCount = 0;
		executionTime = null;
		logFile = null;
		
		
		// Get the request arguments
		
		dbEngine = WebUtils.getStringParameter(request, "database_name");
		dbInstance = WebUtils.getStringParameter(request, "database_instance");
		if (_dbEngine != null) {
			dbEngine = _dbEngine;
			dbInstance = _dbInstance;
			if (dbInstance != null) {
				if (dbInstance.equals("<new>")) {
					dbInstance = WebUtils.getStringParameter(request, "new_database_instance");
				}
			}
		}
		if (dbInstance != null) {
			if (dbInstance.equals("")) dbInstance = null;
		}
		if (!DatabaseEngine.ENGINES.containsKey(dbEngine)) {
			throw new IllegalArgumentException("Unknown database engine: " + dbEngine);
		}
		if (dbInstance != null) {
			WebUtils.asssertDatabaseInstanceNameValidity(dbInstance);
		}
		
		String s_annotation = WebUtils.getStringParameter(request, "annotation");
		String s_txBuffer = WebUtils.getStringParameter(request, "tx_buffer");
		String s_opCount = WebUtils.getStringParameter(request, "op_count");
		String s_warmupOpCount = WebUtils.getStringParameter(request, "warmup_op_count");
		String s_kHops = WebUtils.getStringParameter(request, "k_hops");
		boolean noProvenance = WebUtils.getBooleanParameter(request, "no_provenance", false);
		boolean forceBlueprints = WebUtils.getBooleanParameter(request, "force_blueprints", false);
		boolean useStoredProcedures = WebUtils.getBooleanParameter(request, "use_stored_procedures", false);
		boolean noWarmup = WebUtils.getBooleanParameter(request, "no_warmup", false);
		boolean noCachePollution = WebUtils.getBooleanParameter(request, "no_cache_pollution", false);
		boolean updateDirectly = WebUtils.getBooleanParameter(request, "update_directly", false);
		String s_javaHeapSize = WebUtils.getStringParameter(request, "java_heap_size");
		String s_dbCacheSize = WebUtils.getStringParameter(request, "db_cache_size");
		
		boolean ingestAsUndirected = WebUtils.getBooleanParameter(request, "ingest_as_undirected", false);
		String s_ingestFile = WebUtils.getStringParameter(request, "ingest_file");
		String s_ingestWarmupFile = WebUtils.getStringParameter(request, "ingest_warmup_file");

		String s_generateModel = WebUtils.getStringParameter(request, "generate_model");
		String s_generateBarabasiN = WebUtils.getStringParameter(request, "generate_barabasi_n");
		String s_generateBarabasiM = WebUtils.getStringParameter(request, "generate_barabasi_m");
		
		String s_vertexPropertyKeys = WebUtils.getStringParameter(request, "vertex_property_keys");
		String s_edgePropertyKeys = WebUtils.getStringParameter(request, "edge_property_keys");
		String s_propertyKeys = combinePropertyKeys(s_vertexPropertyKeys, s_edgePropertyKeys);
		String s_edgeCondProp = WebUtils.getStringParameter(request, "edge_cond_prop");
				
		String s_edgeLabels = WebUtils.getStringParameter(request, "edge_labels");
		String s_edgeLabelsNormalized = combinePropertyKeys(s_edgeLabels, null);

		
		// Get the workloads
		
		String[] a_workloads = WebUtils.getStringParameterValues(request, "workloads");
		HashMap<String, Workload> workloads = new HashMap<String, Workload>();
		
		boolean usesOpCount = false;
		boolean hasUpdates = false;
		@SuppressWarnings("unused")
		boolean hasLoadUpdates = false;
		boolean hasNonLoadUpdates = false;
				
		if (a_workloads != null) {
			for (String w : a_workloads) {
				
				Workload workload = Workload.WORKLOADS.get(w);
				if (workload == null) throw new IllegalArgumentException("Unknown workload: " + w);
				
				workloads.put(w, workload);
				
				if (workload.isUsingOpCount()) usesOpCount = true;
				if (workload.isUpdate()) {
					hasUpdates = true;
					if (workload.getUpdateCategory() == Workload.UpdateCategory.LOAD_UPDATE) hasLoadUpdates = true;
					if (workload.getUpdateCategory() != Workload.UpdateCategory.LOAD_UPDATE) hasNonLoadUpdates = true;
				}
			}
		}

		
		// Build the list of command-line arguments
		
		arguments.add(Bench.graphdbBenchDir + "/runBenchmarkSuite.sh");

		if (s_javaHeapSize   != null) {
			if (!s_javaHeapSize.equalsIgnoreCase(BenchmarkMicro.DEFAULT_JVM_HEAP_SIZE)) {
				arguments.add("+memory:" + s_javaHeapSize);
			}
		}

		arguments.add("--dumb-terminal");
		
		if (dbEngine         != null) { arguments.add("--" + dbEngine); }
		if (dbInstance       != null) { arguments.add("--database"); arguments.add(dbInstance); }
		if (s_annotation     != null) { arguments.add("--annotation"); arguments.add(s_annotation); }
		
		if (s_dbCacheSize != null) {
			if (!s_dbCacheSize.equals(Bench.getProperty(Bench.DB_CACHE_SIZE,
					"" + GlobalConfig.databaseCacheSize))) {
				arguments.add("--db-cache-size"); arguments.add(s_dbCacheSize);
			}
		}
		
		if (s_txBuffer != null) {
			if (!s_txBuffer.equals("" + BenchmarkMicro.DEFAULT_NUM_THREADS)) {
				arguments.add("--tx-buffer"); arguments.add(s_txBuffer);
			}
		}
		
		if (noProvenance) {
			arguments.add("--no-provenance");
		}
		
		if (noCachePollution) {
			arguments.add("--no-cache-pollution");
		}
		
		if (noWarmup) {
			arguments.add("--no-warmup");
		}

		if (forceBlueprints) {
			arguments.add("--force-blueprints");
		}

		if (useStoredProcedures) {
			arguments.add("--use-stored-procedures");
		}
		
		if (updateDirectly && hasUpdates && hasNonLoadUpdates) {
			arguments.add("--update-directly");
		}
		
		if (a_workloads != null) {
			for (String s : a_workloads) {
				arguments.add("--" + s);
				if ("ingest".equals(s) && s_ingestFile != null) {
					arguments.add(s_ingestFile);
				}
				if ("incr-ingest".equals(s) && s_ingestFile != null) {
					arguments.add(s_ingestFile);
				}
				if ("generate".equals(s) && s_generateModel != null) {
					arguments.add(s_generateModel);
				}
				if ("create-index".equals(s) && s_propertyKeys != null
						&& !BenchmarkMicro.DEFAULT_PROPERTY_KEYS.equals(s_propertyKeys)) {
					arguments.add(s_propertyKeys);
				}
				if ("get-index".equals(s) && s_propertyKeys != null
						&& !BenchmarkMicro.DEFAULT_PROPERTY_KEYS.equals(s_propertyKeys)) {
					arguments.add(s_propertyKeys);
				}
				if ("get-property".equals(s) && s_propertyKeys != null
						&& !BenchmarkMicro.DEFAULT_PROPERTY_KEYS.equals(s_propertyKeys)) {
					arguments.add(s_propertyKeys);
				}
				if ("get-label".equals(s) && s_edgeLabelsNormalized != null
						&& !BenchmarkMicro.DEFAULT_EDGE_LABELS.equals(s_edgeLabelsNormalized)) {
					arguments.add(s_edgeLabelsNormalized);
				}
				if ("get-k-label".equals(s) && s_edgeLabelsNormalized != null
						&& !BenchmarkMicro.DEFAULT_EDGE_LABELS.equals(s_edgeLabelsNormalized)) {
					arguments.add(s_edgeLabelsNormalized);
				}
			}
		}
		
		if (usesOpCount) {
			if (s_opCount != null) {
				if (!s_opCount.equals("" + BenchmarkMicro.DEFAULT_OP_COUNT)) {
					arguments.add("--op-count"); arguments.add(s_opCount);
				}
			}
			if (s_warmupOpCount != null) {
				if (!s_warmupOpCount.equals(s_opCount)) {
					arguments.add("--warmup-op-count"); arguments.add(s_warmupOpCount);
				}
			}
		}
		
		if (workloads.containsKey("get-k")) {
			if (s_kHops != null) {
				if (!s_kHops.equals("" + BenchmarkMicro.DEFAULT_K_HOPS)) {
					arguments.add("--k-hops"); arguments.add(s_kHops);
				}
			}
		}
		
		if (s_edgeCondProp != null
				&& !BenchmarkMicro.DEFAULT_EDGE_CONDITIONAL_PROPERTY_KEY.equals(s_edgeCondProp)) {
			if (workloads.containsKey("get") || workloads.containsKey("get-k")
					|| workloads.containsKey("get-label") || workloads.containsKey("get-k-label")) {
				arguments.add("--edge-cond-prop");
				arguments.add(s_edgeCondProp);
			}
		}
		
		if (workloads.containsKey("ingest") || workloads.containsKey("incr-ingest")) {
			if (ingestAsUndirected) {
				arguments.add("--ingest-as-undirected");
			}
			if (s_ingestWarmupFile != null) {
				if (!s_ingestWarmupFile.equals(s_ingestFile)) {
					arguments.add("--warmup-ingest"); arguments.add(s_ingestWarmupFile);
				}
			}
		}
		
		if (workloads.containsKey("generate")) {
			if (s_generateModel == null || "barabasi".equals(s_generateModel)) {
				if (s_generateBarabasiN != null) {
					if (!s_generateBarabasiN.equals("" + BenchmarkMicro.DEFAULT_BARABASI_N)) {
						arguments.add("--barabasi-n"); arguments.add(s_generateBarabasiN);
					}
				}
				if (s_generateBarabasiM != null) {
					if (!s_generateBarabasiM.equals("" + BenchmarkMicro.DEFAULT_BARABASI_M)) {
						arguments.add("--barabasi-m"); arguments.add(s_generateBarabasiM);
					}
				}
			}
		}
	}
	
	
	/**
	 * Combine and normalize property keys
	 * 
	 * @param firstKeys the first set of keys
	 * @param secondKeys the second set of keys (will be added a prefix)
	 * @return a combined normalized string
	 */
	private String combinePropertyKeys(String firstKeys, String secondKeys) {
		StringBuilder r = new StringBuilder();
		
		if (firstKeys != null) {
			for (String s : firstKeys.split(",")) {
				if (r.length() > 0) r.append(',');
				r.append(s.trim());
			}
		}
		
		if (secondKeys != null) {
			for (String s : secondKeys.split(",")) {
				if (r.length() > 0) r.append(',');
				r.append(':');
				r.append(s.trim());
			}
		}
		
		return r.length() == 0 ? null : r.toString();
	}
	
	
	/**
	 * Load a finished job from a log file
	 * 
	 * @param file the log file
	 * @param dbEngine a specific database engine name
	 * @param dbInstance a specific database instance name
	 */
	protected void loadFromLogFile(File file, String dbEngine, String dbInstance) {
		
		if (dbInstance != null) {
			if (dbInstance.equals("")) dbInstance = null;
		}
		
		this.logFile = file;
		this.dbEngine = dbEngine;
		this.dbInstance = dbInstance;
		
		arguments = new ArrayList<String>();
		status = getSummaryFile() == null ? -1 : 0;
		executionCount = 1;
		
		
		// Reconstruct the command-line arguments from the file name
		
		arguments.add(Bench.graphdbBenchDir + "/runBenchmarkSuite.sh");
	
		String[] tokens = file.getName().split("__[_]*");
		boolean first = true;
		
		String date = null;
		String mem = null;
		
		for (String t : tokens) {
			if (first) {
				first = false;
			}
			else if (t.length() == 1) {
				arguments.add("-" + t);
			}
			else if (t.length() >= 2) {
				if (t.charAt(1) == '_') {
					arguments.add("-" + t.charAt(1));
					arguments.add(t.substring(2));
				}
				else {
					int p = t.indexOf('_');
					if (p >= 0) {
						String n = t.substring(0, p);
						String a = t.substring(p + 1);
						if (n.equals("date")) {
							if (a.endsWith(".csv")) {
								a = a.substring(0, a.length() - 4);
							}
							date = a;
						}
						else if (n.equals("mem")) {
							if (a.endsWith(".csv")) {
								a = a.substring(0, a.length() - 4);
							}
							mem = a;
						}
						else {
							arguments.add("--" + n);
							arguments.add(a);
						}
					}
					else {
						arguments.add("--" + t);						
					}
				}
			}
		}
		
		if (date != null) {
			if (date.length() == 8 + 1 + 6 && date.charAt(8) == '-') {
				Calendar c = Calendar.getInstance();
				c.set(Integer.parseInt(date.substring( 0,  4)), Integer.parseInt(date.substring( 4,  6)) - 1,
					  Integer.parseInt(date.substring( 6,  8)), Integer.parseInt(date.substring( 9, 11)),
					  Integer.parseInt(date.substring(11, 13)), Integer.parseInt(date.substring(13    )));
				executionTime = c.getTime();
			}
		}
		
		
		// Reconstruct the JVM memory size argument
		
		if (mem.endsWith("M") || mem.endsWith("m")) {
			int mb = Integer.parseInt(mem.substring(0, mem.length() - 1));
			int gb = mb / 1024;
			if (gb >= 1 && (mb % 1024) < gb * 24) {
				mem = "" + gb + "g";
			}
		}
		else if (mem.endsWith("G") || mem.endsWith("g")) {
			Integer.parseInt(mem.substring(0, mem.length() - 1));	// just check that it parses properly
		}
		else {
			throw new IllegalArgumentException("Invalid _mem_ argument in the file name, must end with \"m\" or \"g\"");
		}
		
		if (!mem.equalsIgnoreCase(BenchmarkMicro.DEFAULT_JVM_HEAP_SIZE)) {
			arguments.add(1, "+memory:" + mem);
		}
	}
	
	
	/**
	 * Create a new, runnable duplicate of the job
	 * 
	 * @return the new job
	 */
	public Job duplicate() {
		
		Job j = new Job();
		
		j.arguments = new ArrayList<String>(arguments);
		j.dbEngine = dbEngine;
		j.dbInstance = dbInstance;
		j.logFile = logFile;
		
		return j;
	}
	
	
	/**
	 * Get the database engine and the instance
	 * 
	 * @return the database engine and the instance
	 */
	public DatabaseEngineAndInstance getDatabaseEngineAndInstance() {
		return new DatabaseEngineAndInstance(DatabaseEngine.ENGINES.get(dbEngine), dbInstance);
	}


	/**
	 * Return the list of job arguments
	 * 
	 * @return the arguments
	 */
	public List<String> getArguments() {
		return arguments;
	}
	
	
	/**
	 * Get the execution time
	 * 
	 * @return the execution (start) time, or null if never executed or unknown
	 */
	public Date getExecutionTime() {
		return executionTime;
	}
	
	
	/**
	 * Check two jobs for equality
	 * 
	 * @param other the other job
	 * @return true if they are equal
	 */
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof Job)) return false;
		return ((Job) other).id == id;
	}
	
	
	/**
	 * Compute the hash code
	 * 
	 * @return the hash code
	 */
	@Override
	public int hashCode() {
		return id;
	}
	
	
	/**
	 * Compare two objects -- first by time and then by id
	 * 
	 * @param other the other job
	 * @return the result of the comparison
	 */
	@Override
	public int compareTo(Job other) {
		
		if (other.executionTime == null && executionTime != null) return -1;
		if (other.executionTime != null && executionTime == null) return  1;
		
		if (other.executionTime != null && executionTime != null) {
			int r = executionTime.compareTo(other.executionTime);
			if (r != 0) return r;
		}
		
		return id - other.id;
	}
	
	
	/**
	 * Return the job description as a single-line or a multi-line string
	 * 
	 * @param multiline true to return a multi-line string
	 * @param lineStart the line start string
	 * @param lineEnd the line end string
	 * @param simple true to use a bit simpler output
	 * @return the string
	 */
	public String toStringExt(boolean multiline, boolean simple, String lineStart, String lineEnd) {
		
		boolean first = true;
		StringBuilder sb = new StringBuilder();
		
		
		// Process each argument
		
		for (String s : arguments) {
			
			
			// Simplify the output if we need to
			
			if (simple) {
				
				// Remove options that affect the output
				
				if (s.equals("--dumb-terminal")) continue;
				if (s.equals("--no-color")) continue;
			}
			
			
			// Handle the program name, arguments, and arguments of arguments differently
			
			if (s.startsWith("-") && !first) {
				if (multiline) {
					sb.append(lineEnd);
					sb.append(lineStart);
					sb.append("    ");
				}
				else {
					sb.append(" ");
				}
			}
			else {
				if (first) {
					first = false;
					sb.append(lineStart);
					if (simple) {
						sb.append("runBenchmarkSuite.sh");
						continue;
					}
				}
				else {
					sb.append(" ");
				}
			}
			
			if (s.contains(" ") || s.contains("\n") || s.contains("\r") || s.contains("\t")) {
				sb.append("'" + s + "'");
			}
			else {
				sb.append(s);
			}
		}
		
		if (multiline) sb.append(lineEnd);
		return sb.toString();
	}
	
	
	/**
	 * Get the job ID
	 * 
	 * @return the job ID
	 */
	public int getId() {
		return id;
	}
	
	
	/**
	 * Get a hopefully unique, persistent ID that does not change every time
	 * the program starts.
	 * 
	 * @return a unique, persistent ID
	 */
	public long getPersistentId() {
		long r = 0;
		
		r ^= ((long) arguments.hashCode()) << 0;
		r ^= ((long) dbEngine .hashCode()) << 10;
		
		if (dbInstance != null) {
			r ^= ((long) dbInstance.hashCode()) << 20;
		}
		
		if (executionTime != null) {
			r ^= ((long) dateTimeFormatter.format(executionTime).hashCode()) << 30;
		}
		
		return r;
	}
	
	
	/**
	 * Set whether the job should be unlisted
	 * 
	 * @param unlisted true to be unlisted
	 */
	public void setUnlisted(boolean unlisted) {
		this.unlisted = unlisted;
	}
	
	
	/**
	 * Determine whether the job is unlisted
	 * 
	 * @return true if it is unlisted
	 */
	public boolean isUnlisted() {
		return unlisted;
	}
	
	
	/**
	 * Return the job description as a string
	 * 
	 * @param simple true to use a bit simpler output
	 * @return the string
	 */
	public String toString(boolean simple) {
		return toStringExt(false, simple, "", "");
	}
	
	
	/**
	 * Return the job description as a string
	 * 
	 * @return the string
	 */
	@Override
	public String toString() {
		return toStringExt(false, true, "", "");
	}
	
	
	/**
	 * Return the job description as a multi-line string
	 * 
	 * @param simple true to use a bit simpler output
	 * @return the string
	 */
	public String toMultilineString(boolean simple) {
		return toStringExt(true, simple, "", "\n");
	}
	
	
	/**
	 * Return the job description as a multi-line string
	 * 
	 * @return the string
	 */
	public String toMultilineString() {
		return toStringExt(true, true, "", "\n");
	}


	/**
	 * Get the status of the last job execution
	 * 
	 * @return the status
	 */
	public int getLastStatus() {
		return status;
	}


	/**
	 * Return the number of times the job was executed
	 * 
	 * @return the execution count
	 */
	public int getExecutionCount() {
		return executionCount;
	}


	/**
	 * Return the short name of the database engine
	 * 
	 * @return the short name of the database engine, or null if not specified
	 */
	public String getDbEngine() {
		return dbEngine;
	}


	/**
	 * Return the name of the database instance
	 * 
	 * @return the name of the database instance, or null if not specified
	 */
	public String getDbInstance() {
		return dbInstance;
	}


	/**
	 * Return the name of the database instance
	 * 
	 * @return the name of the database instance, or "" if not specified
	 */
	public String getDbInstanceSafe() {
		return dbInstance == null ? "" : dbInstance;
	}
	
	
	/**
	 * Return the log file (usually known only for already-finished jobs)
	 * 
	 * @return the log file, or null if not known or if it does not exist
	 */
	public File getLogFile() {
		if (logFile == null) return null;
		if (!logFile.exists()) return null;
		return logFile;
	}
	
	
	/**
	 * Return the log file object, optionally even if the file does not exist (usually known only for already-finished jobs)
	 * 
	 * @param checkExistence true to check the existence of the file, false to return the file even if it does not exist
	 * @return the log file, or null if not known
	 */
	public File getLogFile(boolean checkExistence) {
		if (checkExistence && !logFile.exists()) return null;
		return logFile;
	}
	
	
	/**
	 * Return the summary file (usually known only for already-finished jobs)
	 * 
	 * @return the summary file, or null if not known or if it does not exist
	 */
	public File getSummaryFile() {
		if (logFile == null) return null;
		String name = logFile.getName();
		
		String logFilePrefix = dbEngine;
		if (dbInstance != null) {
			logFilePrefix += "_" + dbInstance;
		}
		
		boolean ok = false;
		for (String logFilePrefixExt : WebUtils.getAllLogFilePrefixes(dbEngine, dbInstance)) {
			if (name.startsWith(logFilePrefixExt)) {
				name = logFilePrefix + "-summary_" + name.substring(logFilePrefixExt.length());
				ok = true;
				continue;
			}
		}
		if (!ok) {
			throw new IllegalStateException("The log file does not have a valid prefix: " + logFile.getName());
		}
		
		File f = new File(logFile.getParentFile(), name);
		if (!f.exists()) f = null;
		if (f != null && !f.isFile()) f = null;
		
		return f;
	}
	
	
	/**
	 * Return the warmup log file (usually known only for already-finished jobs)
	 * 
	 * @return the warmup log file, or null if not known or if it does not exist
	 */
	public File getWarmupLogFile() {
		if (logFile == null) return null;
		
		String name = logFile.getName();
		String logFilePrefix = dbEngine;
		if (dbInstance != null) {
			logFilePrefix += "_" + dbInstance;
		}

		boolean ok = false;
		for (String logFilePrefixExt : WebUtils.getAllLogFilePrefixes(dbEngine, dbInstance)) {
			if (name.startsWith(logFilePrefixExt)) {
				name = logFilePrefix + "-warmup" + name.substring(logFilePrefixExt.length());
				ok = true;
				continue;
			}
		}
		if (!ok) {
			throw new IllegalStateException("The log file does not have a valid prefix: " + logFile.getName());
		}
		
		File f = new File(logFile.getParentFile(), name);
		if (!f.exists()) f = null;
		if (f != null && !f.isFile()) f = null;
		
		return f;
	}
	
	
	/**
	 * Return the job argument tokens
	 * 
	 * @return the list of argument tokens
	 */
	public List<String> getArgumentTokens() {
		
		boolean first = true;
		List<String> r = new ArrayList<String>();
		String t = "";
		
		
		// Process each argument
		
		for (String s : arguments) {
			
			// Handle the program name, arguments, and arguments of arguments differently
			
			if (s.startsWith("-") && !first) {
				if (!"".equals(t)) r.add(t);
				t = "";
			}
			else {
				if (first) {
					first = false;
					continue;
				}
				else {
					t += " ";
				}
			}
			
			if (s.contains(" ") || s.contains("\n") || s.contains("\r") || s.contains("\t")) {
				t += ("'" + s + "'");
			}
			else {
				t += s;
			}
		}
		
		if (!"".equals(t)) r.add(t);
		return r;
	}
	
	
	/**
	 * Find differences in the argument tokens between two or more processes
	 * 
	 * @param jobs the jobs
	 * @param ignoreWorkloads true to ignore workload arguments
	 * @return the map of difference strings
	 */
	public static Map<Job, String> findDiffrenceStrings(Collection<Job> jobs, boolean ignoreWorkloads) {
		
		HashMap<Job, List<String>> tokens = new HashMap<Job, List<String>>();
		for (Job j : jobs) {
			tokens.put(j, j.getArgumentTokens());
		}
		
		HashMap<Job, String> r = new HashMap<Job, String>();
		for (Job j : jobs) {
			String uniqueTokens = "";
			for (String t : tokens.get(j)) {
				
				if (ignoreWorkloads) {
					String s = t;
					if (s.startsWith("-")) s = s.substring(1);
					if (s.startsWith("-")) s = s.substring(1);
					int space = s.indexOf(' ');
					if (space > 0) s = s.substring(0, space);
					if (Workload.WORKLOADS.containsKey(s)) continue;
				}
				
				boolean unique = true;
				for (Job x : jobs) {
					if (x == j) continue;
					if (tokens.get(x).contains(t)) {
						unique = false;
						break;
					}
				}
				if (unique) {
					if (!"".equals(uniqueTokens)) {
						uniqueTokens += " ";
					}
					uniqueTokens += t;
				}
			}
			r.put(j, uniqueTokens);
		}
		
		return r;
	}
	
	
	/**
	 * Start the job and return immediately
	 */
	public synchronized void start() {
		
		if (current != null) {
			throw new IllegalStateException("The job is already running");
		}
		
		executionTime = new Date();
		
		current = new ExecutionThread();
		current.start();
	}
	
	
	/**
	 * Determine if the job is currently running
	 * 
	 * @return if the job is currently running
	 */
	public boolean isRunning() {
		return current != null;
	}
	
	
	/**
	 * Join the execution of the thread
	 * 
	 * @return true if the job was joined, false otherwise
	 * @throws InterruptedException if interrupted
	 */
	public boolean join() throws InterruptedException {
		ExecutionThread t = current;
		if (t == null) return false;
		t.join();
		return true;
	}
	
	
	/**
	 * Add a job output listener for the current instance of the job
	 * 
	 * @param listener the listener
	 */
	public synchronized void addJobOutputListenerToCurrent(JobOutputListener listener) {
		
		// TODO This method has several possible race conditions, but they are all quite rare
		
		ExecutionThread t = current;
		if (t == null) {
			if (last != null) {
				listener.jobOutput(last.output.toString());
			}
			return;
		}

		while (t.newOutputListener != null) {
			Thread.yield();
			if (t != current) {
				if (last != null) {
					listener.jobOutput(last.output.toString());
				}
				return;
			}
		}
		
		String output = t.output.toString();
		listener.jobOutput(output);
		t.newOutputCharsSent = output.length();
		
		t.newOutputListener = listener;
	}
	
	
	/**
	 * Get the output of the current/last instance of the job
	 * 
	 * @return the output or null if no instance has been run
	 */
	public String getOutput() {
		
		ExecutionThread t = current;
		if (t == null) t = last;
		if (t == null) return null;
		
		return t.output.toString();
	}
	
	
	/**
	 * The process execution thread
	 */
	private class ExecutionThread extends Thread {
		
		public Appendable output;
		public List<JobOutputListener> outputListeners;
		public JobOutputListener newOutputListener;
		public int newOutputCharsSent;
		
		
		/**
		 * Create an instance of ExecutionThread
		 */
		public ExecutionThread() {
			output = new StringBuffer();
			outputListeners = new LinkedList<JobOutputListener>();
			newOutputListener = null;
			newOutputCharsSent = 0;
		}
		
		
		/**
		 * Run the job
		 */
		@Override
		public void run() {
			
			int status = Integer.MIN_VALUE;
			
			try {
		        
		        // Execute the program and capture the output
		        
				try {
					
					ProcessBuilder pb = new ProcessBuilder(getArguments());
					pb.redirectErrorStream(true);
					Process p = pb.start();
		
					if (bufferedThreadOutput) {
						BufferedReader es = new BufferedReader(new InputStreamReader(p.getErrorStream()));
						
						while (true) {
							String l = es.readLine();
							if (l == null) break;
							l += "\n";
							output.append(l);
							
							Iterator<JobOutputListener> I = outputListeners.iterator();
							while (I.hasNext()) {
								JobOutputListener x = I.next();
								if (!x.jobOutput(l)) I.remove();
							}
							
							if (newOutputListener != null) {
								String s = output.toString();
								if (s.length() > newOutputCharsSent) {
									newOutputListener.jobOutput(s.substring(newOutputCharsSent));
								}
								outputListeners.add(newOutputListener);
								newOutputListener = null;
							}
						}
						
						es.close();
					}
					else {
						InputStreamReader es = new InputStreamReader(p.getInputStream());
						
						while (true) {
							int r = es.read();
							if (r < 0) break;
							output.append((char) r);
							
							Iterator<JobOutputListener> I = outputListeners.iterator();
							while (I.hasNext()) {
								JobOutputListener x = I.next();
								if (!x.jobOutput("" + (char) r)) I.remove();
							}
							
							if (newOutputListener != null) {
								String s = output.toString();
								if (s.length() > newOutputCharsSent) {
									newOutputListener.jobOutput(s.substring(newOutputCharsSent));
								}
								outputListeners.add(newOutputListener);
								newOutputListener = null;
							}
						}
			
						es.close();
					}
					
					if (newOutputListener != null) {
						newOutputListener.jobOutput(output.toString());
						outputListeners.add(newOutputListener);
						newOutputListener = null;
					}
	
					status = p.waitFor();
				}
				catch (RuntimeException e) {
					throw e;
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
			finally {
				
				// Extract the log file name from the output
				
				String s = output.toString();
				int p_start = s.indexOf("Tinkubator Graph Database Benchmark");
				int p_key = p_start < 0 ? -1 : s.indexOf("Log File", p_start);
				int p = p_key < 0 ? -1 : s.indexOf(':', p_key);
				if (p > 0) {
					p += 2;
					int p_end = s.indexOf('\n', p);
					logFile = new File(s.substring(p, p_end));
				}
				
				
				// Finish
				
				Job.this.status = status;
				Job.this.executionCount++;
				
				last = this;
				current = null;		// This must be the very last statement
			}
		}
	}
}
