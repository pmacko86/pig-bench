package com.tinkerpop.bench.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentHashMap;

import com.tinkerpop.bench.log.SummaryLogEntry;
import com.tinkerpop.bench.log.SummaryLogReader;
import com.tinkerpop.bench.operation.operations.OperationCreateKeyIndex;
import com.tinkerpop.bench.operation.operations.OperationLoadFGF;
import com.tinkerpop.bench.web.DatabaseEngineAndInstance;
import com.tinkerpop.bench.web.Job;
import com.tinkerpop.bench.web.JobList;

/**
 * Simple ingest analysis 
 * 
 * @author Peter Macko (pmacko@eecs.harvard.edu)
 */
public class IngestAnalysis {
	
	/// The cache
	private static ConcurrentHashMap<DatabaseEngineAndInstance, IngestAnalysis> cache =
			new ConcurrentHashMap<DatabaseEngineAndInstance, IngestAnalysis>();

	/// The database and instance pair
	private DatabaseEngineAndInstance dbEI;
	
	/// The number of all finished jobs (for cache purposes)
	private int numFinishedJobs;
	
	private Job bulkLoadJob = null;
	private double bulkLoadTime = 0;
	private double bulkLoadShutdownTime = 0;
	private String bulkLoadDataset = null;
	
	private Job incrementalLoadJob = null;
	private double incrementalLoadTime = 0;
	private double incrementalLoadShutdownTime = 0;
	private String incrementalLoadDataset = null;
	
	private double createIndexTime = 0;
	private double createIndexShutdownTime = 0;

	
	/**
	 * Create an instance of the class
	 * 
	 * @param dbei the database engine and instance
	 */
	protected IngestAnalysis(DatabaseEngineAndInstance dbEI) {
		
		this.dbEI = dbEI;
		this.numFinishedJobs = -1;
		
		update();
	}
	
	
	/**
	 * Get an instance of IngestAnalysis for a particular database engine and instance pair
	 * 
	 * @param dbEI the database engine and instance pair
	 */
	public static IngestAnalysis getInstance(DatabaseEngineAndInstance dbEI) {
		
		synchronized (cache) {
			IngestAnalysis m = cache.get(dbEI);
			if (m == null) {
				m = new IngestAnalysis(dbEI);
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
		
		List<Job> finishedJobs = JobList.getInstance().getFinishedJobs(dbEI);
		if (finishedJobs.size() == numFinishedJobs) return false;
		numFinishedJobs = finishedJobs.size();
		
		
		/*
		 * Initialize
		 */
	
		ModelAnalysis m = ModelAnalysis.getInstance(dbEI);
		
		bulkLoadJob = null;
		bulkLoadTime = 0;
		bulkLoadShutdownTime = 0;
		bulkLoadDataset = null;
		
		incrementalLoadJob = null;
		incrementalLoadTime = 0;
		incrementalLoadShutdownTime = 0;
		incrementalLoadDataset = null;
		
		createIndexTime = 0;
		createIndexShutdownTime = 0;
		
		
		/*
		 * Find the load FGF operations
		 */
		
		ArrayList<Job> jobs = new ArrayList<Job>(m.getJobs(OperationLoadFGF.class));
		Collections.reverse(jobs);
		
		for (Job j : jobs) {
			SummaryLogReader reader = new SummaryLogReader(j.getSummaryFile());
			for (SummaryLogEntry e : reader) {
				if (e.getName().startsWith("OperationLoadFGF")) {
					String[] tags = e.getName().split("-");
					if (tags.length < 3) {
						throw new RuntimeException("Error: Invalid number of operation tags in \"" + e.getName()
								+ "\" for \"" + dbEI + "\"");
					}
					
					boolean thisBulkLoad = tags[1].equals("bulkload");
					boolean thisIncremental = tags[1].equals("incremental");
					if (!thisBulkLoad && !thisIncremental) {
						throw new RuntimeException("Error: Invalid operation tags in \"" + e.getName()
								+ "\" for \"" + dbEI + "\"");
					}
					
					if (thisBulkLoad && bulkLoadJob == null) {
						bulkLoadJob = j;
						bulkLoadTime = e.getDefaultRunTimes().getMean() / 1000000.0;	// to ms
						bulkLoadDataset = tags[2];
					}
					
					if (thisIncremental && incrementalLoadJob == null) {
						incrementalLoadJob = j;
						incrementalLoadTime = e.getDefaultRunTimes().getMean() / 1000000.0;	// to ms
						incrementalLoadDataset = tags[2];
					}
				}
				
				if (e.getName().equals("OperationShutdownGraph")) {
					if (j == bulkLoadJob)
						bulkLoadShutdownTime = e.getDefaultRunTimes().getMean() / 1000000.0;	// to ms
					if (j == incrementalLoadJob)
						incrementalLoadShutdownTime = e.getDefaultRunTimes().getMean() / 1000000.0;	// to ms
				}
			}
			if (bulkLoadJob != null && incrementalLoadJob != null) break; 
		}
		
		
		/*
		 * Find the create index job
		 */
		
		SortedSet<Job> createIndexJobs = m.getJobs(OperationCreateKeyIndex.class);
		
		Job createIndexJob = createIndexJobs == null || createIndexJobs.isEmpty() ? null : createIndexJobs.last();
		if (createIndexJob != null) {
			SummaryLogReader reader = new SummaryLogReader(createIndexJob.getSummaryFile());
			for (SummaryLogEntry e : reader) {
				if (e.getName().startsWith("OperationCreateKeyIndex")) {
					createIndexTime += e.getDefaultRunTimes().getMean() / 1000000.0;	// to ms
				}
				if (e.getName().equals("OperationShutdownGraph")) {
					createIndexShutdownTime += e.getDefaultRunTimes().getMean() / 1000000.0;	// to ms
				}
			}
		}
		
		
		/*
		 * Finish 
		 */
		
		return true;
	}
	
	
	/**
	 * Get the bulk load time
	 * 
	 * @return the time in ms, or 0 if none
	 */
	public double getBulkLoadTime() {
		return bulkLoadTime;
	}
	
	
	/**
	 * Get the shutdown time after the bulk load
	 * 
	 * @return the time in ms, or 0 if none
	 */
	public double getBulkLoadShutdownTime() {
		return bulkLoadShutdownTime;
	}
	
	
	/**
	 * Get the incremental load time
	 * 
	 * @return the time in ms, or 0 if none
	 */
	public double getIncrementalLoadTime() {
		return incrementalLoadTime;
	}
	
	
	/**
	 * Get the shutdown time after the incremental load
	 * 
	 * @return the time in ms, or 0 if none
	 */
	public double getIncrementalLoadShutdownTime() {
		return incrementalLoadShutdownTime;
	}
	
	
	/**
	 * Get the index creation time
	 * 
	 * @return the time in ms, or 0 if none
	 */
	public double getCreateIndexTime() {
		return createIndexTime;
	}
	
	
	/**
	 * Get the shutdown time after the index creation
	 * 
	 * @return the time in ms, or 0 if none
	 */
	public double getCreateIndexShutdownTime() {
		return createIndexShutdownTime;
	}
	
	
	/**
	 * Get the total ingest time, including all shutdown times
	 * 
	 * @return the time in ms, or 0 if none
	 */
	public double getTotalTime() {
		double r = 0;
		r += bulkLoadTime + bulkLoadShutdownTime;
		r += incrementalLoadTime + incrementalLoadShutdownTime;
		r += createIndexTime + createIndexShutdownTime;
		return r;
	}
}
