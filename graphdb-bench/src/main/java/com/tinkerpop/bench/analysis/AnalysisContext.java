package com.tinkerpop.bench.analysis;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

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
	
	/// The cache
	private static ConcurrentHashMap<DatabaseEngineAndInstance, AnalysisContext> cache =
			new ConcurrentHashMap<DatabaseEngineAndInstance, AnalysisContext>();

	/// The database and instance pair
	private DatabaseEngineAndInstance dbEI;
	
	/// The number of all finished jobs (for cache purposes)
	private int numFinishedJobs;
	
	/// The list of relevant successfully finished jobs
	private List<Job> finishedJobs;
	
	/// The map of operations to finished jobs
	HashMap<String, SortedSet<Job>> operationToJobs;
	
	/// The map of operations with tags to finished jobs
	HashMap<String, SortedSet<Job>> operationWithTagsToJobs;

	
	
	/**
	 * Create an instance of the class
	 * 
	 * @param dbei the database engine and instance
	 */
	protected AnalysisContext(DatabaseEngineAndInstance dbEI) {
		
		this.dbEI = dbEI;
		this.numFinishedJobs = -1;
		
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
		operationToJobs = new HashMap<String, SortedSet<Job>>();
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
						|| name.equals("OperationShutdownGraph")) continue;
				
				SortedSet<Job> ojs = operationWithTagsToJobs.get(name);
				if (ojs == null) {
					ojs = new TreeSet<Job>();
					operationWithTagsToJobs.put(name, ojs);
				}
				ojs.add(job);
				
				int tagStart = name.indexOf('-');
				if (tagStart > 0) name = name.substring(0, tagStart);
				
				ojs = operationToJobs.get(name);
				if (ojs == null) {
					ojs = new TreeSet<Job>();
					operationToJobs.put(name, ojs);
				}
				ojs.add(job);
			}
		}
		
		
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
	public SortedSet<Job> getJobs(Class<? extends Operation> operation) {
		SortedSet<Job> operationJobs = operationToJobs.get(operation.getSimpleName());
		if (operationJobs == null || operationJobs.isEmpty()) return null;
		return operationJobs;
	}
	
	
	/**
	 * Get all jobs for the specified operation (base name, not with tags)
	 * 
	 * @param operationName the operation type
	 * @return the jobs sorted by time (ascending), or null if none
	 */
	public SortedSet<Job> getJobs(String operationName) {
		String s = operationName.indexOf('-') > 0 ? operationName.substring(0, operationName.indexOf('-')) : operationName;
		SortedSet<Job> operationJobs = operationToJobs.get(s);
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
}
