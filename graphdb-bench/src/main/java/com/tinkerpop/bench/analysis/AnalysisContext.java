package com.tinkerpop.bench.analysis;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import com.tinkerpop.bench.log.OperationLogEntry;
import com.tinkerpop.bench.log.OperationLogReader;
import com.tinkerpop.bench.log.SummaryLogEntry;
import com.tinkerpop.bench.log.SummaryLogReader;
import com.tinkerpop.bench.operation.Operation;
import com.tinkerpop.bench.web.DatabaseEngineAndInstance;
import com.tinkerpop.bench.web.Job;
import com.tinkerpop.bench.web.JobList;


/**
 * A context for database introspection 
 * 
 * @author Peter Macko (pmacko@eecs.harvard.edu)
 */
public class AnalysisContext {
	
	/// The fraction of extreme values to drop by default
	public static final double DROP_EXTREMES = 0.05;

	
	/*
	 * Cache
	 */
	
	/// The cache
	private static ConcurrentHashMap<DatabaseEngineAndInstance, AnalysisContext> cache =
			new ConcurrentHashMap<DatabaseEngineAndInstance, AnalysisContext>();

	
	/*
	 * Instance fields
	 */
	
	/// The database and instance pair
	private DatabaseEngineAndInstance dbEI;
	
	/// The number of all finished jobs (for cache purposes)
	private int numFinishedJobs;
	
	/// The list of relevant successfully finished jobs
	private List<Job> finishedJobs;
	
	/// The map of operations to finished jobs, limited to non stored procedure operations
	HashMap<String, SortedSet<Job>> operationTypesToJobsPure;
	
	/// The map of operations with tags to finished jobs, limited to non stored procedure operations
	HashMap<String, SortedSet<Job>> operationWithTagsToJobsPure;
	
	/// The map of operations to finished jobs
	HashMap<String, SortedSet<Job>> operationTypesToJobs;
	
	/// The map of operations with tags to finished jobs
	HashMap<String, SortedSet<Job>> operationWithTagsToJobs;
	
	/// Statistics
	DatabaseInstanceStatistics statistics;
	
	
	/*
	 * Caches
	 */
	
	/// The map of operation models
	HashMap<String, OperationModel> operationModels;
	
	/// The map of operation statistics
	HashMap<String, OperationStats> operationStats;
	
	/// The map of pure operation statistics
	HashMap<String, OperationStats> pureOperationStats;

	
	
	/**
	 * Create an instance of the class
	 * 
	 * @param dbei the database engine and instance
	 */
	protected AnalysisContext(DatabaseEngineAndInstance dbEI) {
		
		this.dbEI = dbEI;
		this.numFinishedJobs = -1;
		
		this.operationModels = new HashMap<String, OperationModel>();
		this.operationStats = new HashMap<String, OperationStats>();
		this.pureOperationStats = new HashMap<String, OperationStats>();
		
		this.statistics = DatabaseInstanceStatisticsProvider.getStatisticsFor(dbEI);
		
		update();
	}
	
	
	/**
	 * Get an instance of ModelAnalysis for a particular database engine and instance pair
	 * 
	 * @param dbEI the database engine and instance pair
	 */
	public static AnalysisContext getInstance(DatabaseEngineAndInstance dbEI) {
		
		synchronized (cache) {
			AnalysisContext m = cache.get(dbEI);
			if (m == null) {
				m = new AnalysisContext(dbEI);
				cache.put(dbEI, m);
			}
			else {
				m.update();
			}
			return m;
		}
	}
	
	
	/**
	 * Determine if the given job is pure
	 * 
	 * @param job the job
	 * @return true if it is pure
	 */
	private boolean isPure(Job job) {
		return !job.getArguments().contains("--use-stored-procedures");
	}
	
	
	/**
	 * Recompute if necessary
	 * 
	 * @return true if it was recomputed
	 */
	public boolean update() {
		
		List<Job> jobs = JobList.getInstance().getFinishedJobs(dbEI);
		if (jobs.size() == numFinishedJobs) return false;
		
		
		/*
		 * Compute the generic job maps
		 */
	
		finishedJobs = new ArrayList<Job>();
		
		operationTypesToJobsPure = new HashMap<String, SortedSet<Job>>();
		operationWithTagsToJobsPure = new HashMap<String, SortedSet<Job>>();
		operationTypesToJobs = new HashMap<String, SortedSet<Job>>();
		operationWithTagsToJobs = new HashMap<String, SortedSet<Job>>();
		
		numFinishedJobs = jobs.size();
		
		for (Job job : jobs) {
			numFinishedJobs++;
			
			
			// Exclude failed or unfinished jobs
			
			if (job.getExecutionCount() < 0 || job.getLastStatus() != 0 || job.isRunning()) continue;
			File summaryFile = job.getSummaryFile();
			if (summaryFile == null) continue;
			if (job.getExecutionTime() == null) continue;
			
			
			// Exclude jobs with some command-line arguments from some collections 
			
			boolean includeInPure = isPure(job);
			
			
			// We found a relevant job!
			
			finishedJobs.add(job);
			
			
			// Operation Maps
			
			SummaryLogReader reader = new SummaryLogReader(summaryFile);
			for (SummaryLogEntry e : reader) {
				String name = e.getName();
				
				if (name.equals("OperationOpenGraph")
						|| name.equals("OperationDoGC")
						|| name.equals("OperationShutdownGraph")
						|| name.startsWith("--")) continue;
				
				SortedSet<Job> ojs = operationWithTagsToJobs.get(name);
				if (ojs == null) {
					ojs = new TreeSet<Job>();
					operationWithTagsToJobs.put(name, ojs);
				}
				ojs.add(job);
				
				if (includeInPure) {
					ojs = operationWithTagsToJobsPure.get(name);
					if (ojs == null) {
						ojs = new TreeSet<Job>();
						operationWithTagsToJobsPure.put(name, ojs);
					}
					ojs.add(job);
				}
				
				int tagStart = name.indexOf('-');
				if (tagStart > 0) name = name.substring(0, tagStart);
				
				ojs = operationTypesToJobs.get(name);
				if (ojs == null) {
					ojs = new TreeSet<Job>();
					operationTypesToJobs.put(name, ojs);
				}
				ojs.add(job);
				
				if (includeInPure) {
					ojs = operationTypesToJobsPure.get(name);
					if (ojs == null) {
						ojs = new TreeSet<Job>();
						operationTypesToJobsPure.put(name, ojs);
					}
					ojs.add(job); 
				}
			}
		}
		
		
		/*
		 * Clear the appropriate caches
		 */
		
		operationModels.clear();
		operationStats.clear();
		pureOperationStats.clear();
		
		
		/*
		 * Finish
		 */
		
		return true;
	}
	
	
	/**
	 * Get all jobs for the specified operation
	 * 
	 * @param operation the operation type
	 * @return the jobs sorted by time (ascending), or null if none
	 */
	public SortedSet<Job> getJobsForType(Class<? extends Operation> operation) {
		SortedSet<Job> operationJobs = operationTypesToJobs.get(operation.getSimpleName());
		if (operationJobs == null || operationJobs.isEmpty()) return null;
		return operationJobs;
	}
	
	
	/**
	 * Get all jobs for the specified operation (base name, not with tags)
	 * 
	 * @param operationName the operation type
	 * @return the jobs sorted by time (ascending), or null if none
	 */
	public SortedSet<Job> getJobsForType(String operationName) {
		String s = operationName.indexOf('-') > 0 ? operationName.substring(0, operationName.indexOf('-')) : operationName;
		SortedSet<Job> operationJobs = operationTypesToJobs.get(s);
		if (operationJobs == null || operationJobs.isEmpty()) return null;
		return operationJobs;
	}
	
	
	/**
	 * Get all jobs for the specified operation (base name, not with tags)
	 * 
	 * @param operationName the operation type
	 * @param pure true for only pure jobs
	 * @return the jobs sorted by time (ascending), or null if none
	 */
	public SortedSet<Job> getJobsForType(String operationName, boolean pure) {
		String s = operationName.indexOf('-') > 0 ? operationName.substring(0, operationName.indexOf('-')) : operationName;
		SortedSet<Job> operationJobs;
		if (pure) {
			operationJobs = operationTypesToJobsPure.get(s);
		}
		else {
			operationJobs = operationTypesToJobs.get(s);
		}
		if (operationJobs == null || operationJobs.isEmpty()) return null;
		return operationJobs;
	}
	
	
	/**
	 * Get all jobs for the specified operation that includes a specified tag
	 * 
	 * @param operationName the operation name with an optional tag
	 * @return the jobs sorted by time (ascending), or null if none
	 */
	public SortedSet<Job> getJobsWithTag(String operationName) {
		SortedSet<Job> operationJobs = operationWithTagsToJobs.get(operationName);
		if (operationJobs == null || operationJobs.isEmpty()) return null;
		return operationJobs;
	}
	
	
	/**
	 * Get all jobs for the specified operation that includes a specified tag
	 * 
	 * @param operationName the operation name with an optional tag
	 * @param pure true for only pure jobs
	 * @return the jobs sorted by time (ascending), or null if none
	 */
	public SortedSet<Job> getJobsWithTag(String operationName, boolean pure) {
		SortedSet<Job> operationJobs;
		if (pure) {
			operationJobs = operationWithTagsToJobsPure.get(operationName);
		}
		else {
			operationJobs = operationWithTagsToJobs.get(operationName);
		}
		if (operationJobs == null || operationJobs.isEmpty()) return null;
		return operationJobs;
	}
	
	
	/**
	 * Get all successfully finished jobs
	 * 
	 * @return the jobs, or null if none
	 */
	public List<Job> getJobs() {
		return finishedJobs;
	}
	
	
	/**
	 * Get all successfully finished jobs for each operation name including tags
	 * 
	 * @return the map of operations to a sorted set of jobs by time (ascending)
	 */
	public Map<String, SortedSet<Job>> getJobsForAllOperationsWithTags() {
		return operationWithTagsToJobs;
	}
	
	
	/**
	 * Get the statistics for the given instance of the database
	 * 
	 * @return the database statistics
	 */
	public DatabaseInstanceStatistics getStatistics() {
		return statistics;
	}
	
	
	/**
	 * Get an operation model for the given class
	 * 
	 * @param operationClass the operation class
	 * @return the model, or null if not available
	 */
	public synchronized OperationModel getModelFor(Class<? extends Operation> operationClass) {
		
		OperationModel cached = operationModels.get(operationClass.getCanonicalName());
		if (cached != null) return cached;
		
		OperationModel model = OperationModel.getModelFor(this, operationClass);
		if (model != null) operationModels.put(operationClass.getCanonicalName(), model);
		
		return model;
	}
	
	
	/**
	 * Get an operation model for the given class
	 * 
	 * @param operationName the operation name (the tag will be ignored if present)
	 * @return the model, or null if not available
	 */
	public synchronized OperationModel getModelFor(String operationName) {
		
		if (operationName.contains("-")) {
			operationName = operationName.substring(0, operationName.indexOf('-'));
		}
		
		OperationModel cached = operationModels.get(operationName);
		if (cached != null) return cached;
		
		OperationModel model = OperationModel.getModelFor(this, operationName);
		if (model != null) operationModels.put(operationName, model);
		
		return model;
	}
	
	
	/**
	 * Get statistics for the given operation
	 * 
	 * @param operationName the operation name, including all tags
	 * @return the statistics, or null if none
	 */
	public synchronized OperationStats getOperationStats(String operationName) {
		return getOperationStats(operationName, false);
	}
	
	
	/**
	 * Get statistics for the given operation
	 * 
	 * @param operationName the operation name, including all tags
	 * @param pure true if the operation must be pure (i.e. not a stored procedure)
	 * @return the statistics, or null if none
	 */
	public synchronized OperationStats getOperationStats(String operationName, boolean pure) {
		
		OperationStats cached;
		
		if (pure) {
			cached = pureOperationStats.get(operationName);
		}
		else {
			cached = operationStats.get(operationName);
		}
		
		if (cached != null) return cached;
		
		
		// Check the persistent cache
		
		SortedSet<Job> jobs = getJobsWithTag(operationName, pure);
		Job job = jobs == null ? null : jobs.last();
		if (job == null) return null;
	
		File cacheFile = OperationLogReader.getCacheFile(job.getLogFile(), operationName, "stats",
				"drop-extremes_" + ((int) (100 * DROP_EXTREMES)) + ".dat");
		
		if (cacheFile.exists()) {
			OperationStats stats = null;

			ObjectInputStream in = null;
			try {
				in = new ObjectInputStream(new FileInputStream(cacheFile));
				stats = (OperationStats) in.readObject();
				stats.job = job;
				stats.context = this;
				in.close();
			}
			catch (InvalidClassException e) {
				stats = null;
				try { if (in != null) in.close(); } catch (IOException ex) {};
			}
			catch (IOException e) {
				try { if (in != null) in.close(); } catch (IOException ex) {};
				throw new RuntimeException(e);
			}
			catch (ClassNotFoundException e) {
				try { if (in != null) in.close(); } catch (IOException ex) {};
				throw new RuntimeException(e);
			}
			
			if (stats != null) {
				operationStats.put(operationName, stats);
				if (isPure(job)) pureOperationStats.put(operationName, stats);
				return stats;
			}
		}
		
		
		// Compute the statistics
		
		OperationStats stats = new OperationStats(this, job, operationName, DROP_EXTREMES);
		operationStats.put(operationName, stats);
		if (isPure(job)) pureOperationStats.put(operationName, stats);
		
		
		// Store them in the persistent cache
		
		cacheFile.getParentFile().mkdirs();
		try {
			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(cacheFile));
			out.writeObject(stats);
			out.close();
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		return stats;
	}
	
	
	/**
	 * Get a runtime of an operation from the latest job, taking a simple average
	 * if there is more than one within the job. If the operation is of the "Many"
	 * type, then return the average runtime of the simpler operation that gets
	 * repeated.
	 * 
	 * @param operation the operation type (will look for job with no tags)
	 * @return the runtime in ms, or null if not found
	 */
	public Double getAverageOperationRuntimeNoTag(Class<? extends Operation> operation) {
		return getAverageOperationRuntime(operation.getSimpleName());
	}
	
	
	/**
	 * Get a runtime of an operation from the latest job, taking a simple average
	 * if there is more than one within the job. If the operation is of the "Many"
	 * type, then return the average runtime of the simpler operation that gets
	 * repeated.
	 * 
	 * @param operation the operation type
	 * @param tag the tag (null for none)
	 * @return the runtime in ms, or null if not found
	 */
	public Double getAverageOperationRuntime(Class<? extends Operation> operation, String tag) {
		return getAverageOperationRuntime(operation.getSimpleName() + (tag == null ? "" : "-" + tag));
	}
	
	
	/**
	 * Get a runtime of an operation from the latest job, taking a simple average
	 * if there is more than one within the job. If the operation is of the "Many"
	 * type, then return the average runtime of the simpler operation that gets
	 * repeated.
	 * 
	 * @param operationName the operation name including the tag
	 * @return the runtime in ms, or null if not found
	 */
	public Double getAverageOperationRuntime(String operationName) {
		
		OperationStats stats = getOperationStats(operationName);
		return stats == null ? null : stats.getAverageOperationRuntime();
	}
	
	
	/**
	 * Get the tail operation log entries from the latest job for the given operation name
	 * (including tags)
	 * 
	 * @param operationName the operation name including tags
	 * @return a list of the operation log entries from the tail of the log, or null if not available
	 */
	public List<OperationLogEntry> getTailEntries(String operationName) {
		return getTailEntries(operationName, null);
	}
	
	
	/**
	 * Get the tail operation log entries from the given job for the given operation name
	 * (including tags)
	 * 
	 * @param operationName the operation name including tags
	 * @param job the job, null to use the latest successful job for the given operation name
	 * @return a list of the operation log entries from the tail of the log, or null if not available
	 */
	public List<OperationLogEntry> getTailEntries(String operationName, Job job) {
		
		if (job == null) {
			SortedSet<Job> jobs = getJobsWithTag(operationName);
			job = jobs == null ? null : jobs.last();
		}
		
		if (job != null) {
			return OperationLogReader.getTailEntries(job.getLogFile(), operationName);
		}
		else {
			return null;
		}
	}
}
