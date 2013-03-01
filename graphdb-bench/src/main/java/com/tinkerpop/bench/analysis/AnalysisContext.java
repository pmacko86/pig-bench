package com.tinkerpop.bench.analysis;

import java.io.File;
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
	
	/*
	 * Configuration
	 */
	
	/// Whether to use robust linear fits
	public static boolean useRobustFits = false;
	
	/// Whether to always use the detailed log files instead of the summary files
	public static boolean alwaysUseDetailedLogs = false;
	
	
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
	
	/// The map of operations to finished jobs
	HashMap<String, SortedSet<Job>> operationTypesToJobs;
	
	/// The map of operations with tags to finished jobs
	HashMap<String, SortedSet<Job>> operationWithTagsToJobs;
	
	/// Statistics
	DatabaseInstanceStatistics statistics;
	
	
	/*
	 * Caches
	 */
	
	/// The map of average operation runtimes
	HashMap<String, Double> averageOperationRuntimes;
	
	/// The map of operation models
	HashMap<String, OperationModel> operationModels;

	
	
	/**
	 * Create an instance of the class
	 * 
	 * @param dbei the database engine and instance
	 */
	protected AnalysisContext(DatabaseEngineAndInstance dbEI) {
		
		this.dbEI = dbEI;
		this.numFinishedJobs = -1;
		
		this.averageOperationRuntimes = new HashMap<String, Double>();
		this.operationModels = new HashMap<String, OperationModel>();
		
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
			
			
			// Exclude jobs with some command-line arguments
			
			if (job.getArguments().contains("--use-stored-procedures")) continue;
			
			
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
				
				int tagStart = name.indexOf('-');
				if (tagStart > 0) name = name.substring(0, tagStart);
				
				ojs = operationTypesToJobs.get(name);
				if (ojs == null) {
					ojs = new TreeSet<Job>();
					operationTypesToJobs.put(name, ojs);
				}
				ojs.add(job);
			}
		}
		
		
		/*
		 * Clear the appropriate caches
		 */
		
		averageOperationRuntimes.clear();
		operationModels.clear();
		
		
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
	public synchronized Double getAverageOperationRuntime(String operationName) {
		
		Double cached = averageOperationRuntimes.get(operationName);
		if (cached != null) return cached;
		
		
		// Initialize
		
		boolean many = AnalysisUtils.isManyOperation(operationName);
		int opCountArg = !many ? -1 : AnalysisUtils.getManyOperationOpCountArgumentIndex(operationName);
		
		
		// Find the correct job
		
		SortedSet<Job> operationJobs = operationWithTagsToJobs.get(operationName);
		if (operationJobs == null || operationJobs.isEmpty()) return null;
		Job job = operationJobs.last();
		
		
		// Get the average operation run time
		
		if (alwaysUseDetailedLogs) {
			
	    	double time = 0; int count = 0;			
			for (OperationLogEntry e : OperationLogReader.getTailEntries(job.getLogFile(), operationName)) {
				
				int c;
				if (many) {
					String s = e.getArgs()[opCountArg >= 0 ? opCountArg : e.getArgs().length + opCountArg];
					int opCount = Integer.parseInt(s);
					if (!s.equals("" + opCount)) throw new NumberFormatException(s);
					c = opCount;
				}
				else {
					c = 1;
				}
				
				count += c;
				time += e.getTime() / 1000000.0;
			}
			
			if (count == 0) return null;
			double r = time / count;
			
			averageOperationRuntimes.put(operationName, r);
			return r;
		}
		else {
			
			SummaryLogEntry entry = SummaryLogReader.getEntryForOperation(job.getSummaryFile(), operationName);
			if (entry == null) return null;
			if (many) entry = AnalysisUtils.convertLogEntryForManyOperation(entry, job);
			
			double r = entry.getDefaultRunTimes().getMean() / 1000000.0;
			
			averageOperationRuntimes.put(operationName, r);
			return r;
		}
	}
}
