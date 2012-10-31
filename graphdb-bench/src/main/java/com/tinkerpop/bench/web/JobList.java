package com.tinkerpop.bench.web;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import com.tinkerpop.bench.util.Pair;


/**
 * A list of jobs
 * 
 * @author Peter Macko (pmacko@eecs.harvard.edu)
 */
public class JobList {
	
	private static JobList instance = new JobList();
	
	
	// Jobs on the main page

	private ArrayList<Job> jobs;
	private HashMap<Integer, Job> jobMap;
	
	
	// Finished jobs
	
	// Map: (database name, database instance) ---> list of finished jobs
	private HashMap<Pair<String, String>, LinkedList<Job>> finishedJobs;
	
	// Map: id ---> finished job
	private HashMap<Integer, Job> finishedJobMap;
	
	
	// Job execution control
	
	private ExecutionThread thread;
	private boolean paused;
	private boolean running;
	
	
	/**
	 * Create an instance of a job list
	 */
	protected JobList() {
		
		
		// Jobs on the main page
		
		jobs = new ArrayList<Job>();
		jobMap = new HashMap<Integer, Job>();
		
		
		// Finished jobs
		
		finishedJobs = new HashMap<Pair<String,String>, LinkedList<Job>>();
		finishedJobMap = new HashMap<Integer, Job>();
		
		Collection<Pair<String, String>> dbis = WebUtils.getDatabaseInstancePairs();
		for (Pair<String, String> p : dbis) {
			LinkedList<Job> l = fetchFinishedJobs(p.first, p.second);
			for (Job j : l) finishedJobMap.put(j.getId(), j);
			finishedJobs.put(p, l);
		}
		
		
		// Job execution control

		thread = new ExecutionThread();
		paused = true;
		running = false;
	}
	
	
	/**
	 * Get an instance of the job list
	 * 
	 * @return an instance of the job list
	 */
	public static JobList getInstance() {
		return instance;
	}


	/**
	 * Return the list of jobs
	 * 
	 * @return the jobs
	 */
	public ArrayList<Job> getJobs() {
		return jobs;
	}
	
	
	/**
	 * Add a job to the list
	 * 
	 * @param job the job to add
	 */
	public synchronized void addJob(Job job) {
		jobs.add(job);
		jobMap.put(job.getId(), job);
	}
	
	
	/**
	 * Remove a job from the list
	 * 
	 * @param job the job to remove
	 */
	public synchronized void removeJob(Job job) {
		jobs.remove(job);
		jobMap.remove(job.getId());
	}
	
	
	/**
	 * Move a job to the bottom of the list
	 * 
	 * @param job the job to move
	 */
	public synchronized void moveToBottom(Job job) {
		jobs.remove(job);
		jobs.add(job);
	}


	/**
	 * Get a job based on its ID
	 * 
	 * @param id the job ID
	 * @return the job or null if not found
	 */
	public Job getJob(int id) {
		return jobMap.get(id);
	}


	/**
	 * Get a finished job based on its ID
	 * 
	 * @param id the job ID
	 * @return the finished job, or null if not found, not yet executed, or still running
	 */
	public Job getFinishedJob(int id) {
		return finishedJobMap.get(id);
	}
	
	
	/**
	 * Get the next job that has not yet been executed
	 * 
	 * @return the next job that has not yet been executed or null if none
	 */
	public synchronized Job getNextRunnableJob() {
		
		for (Job j : jobs) {
			if (!j.isRunning() && j.getExecutionCount() == 0) {
				return j;
			}
		}
		
		return null;
	}
	
	
	/**
	 * Start executing the jobs
	 */
	public synchronized void start() {	
		paused = false;
		if (!running) {
			thread = new ExecutionThread();
			thread.start();
		}
	}
	
	
	/**
	 * Pause executing the jobs, but do not terminate the current job
	 */
	public synchronized void pause() {	
		paused = true;
	}
	
	
	/**
	 * Determine whether the job execution is paused. Note that if the
	 * execution is paused, it does not mean that no jobs are currently
	 * running, but it means that once the current job finishes (if 
	 * applicable), the next one would not start.
	 * 
	 * @return true if it is paused
	 */
	public boolean isPaused() {
		// Note: this is not synchronized in order to reduce the overhead,
		// so do not use this as a synchronization primitive
		return paused;
	}
	
	
	/**
	 * Get the currently executing job
	 * 
	 * @return the currently executing job, or null if none
	 */
	public Job getCurrentJob() {
		return thread.current;
	}
	
	
	/**
	 * Fetch a collection of previously executed jobs
	 * 
	 * @param dbEngine the database engine name
	 * @param dbInstance the database instance name
	 * @return the collection of previously executed jobs
	 */
	private LinkedList<Job> fetchFinishedJobs(String dbEngine, String dbInstance) {
		
		LinkedList<Job> r = new LinkedList<Job>();
		File dir = WebUtils.getResultsDirectory(dbEngine, dbInstance);
		String logFilePrefix = WebUtils.getLogFilePrefix(dbEngine, dbInstance);
		String logFilePrefixWarmup = WebUtils.getWarmupLogFilePrefix(dbEngine, dbInstance);
				
		for (File f : dir.listFiles()) {
			if (f.isDirectory()) continue;
			String name = f.getName();
			
			if (name.endsWith(".csv")) {
				if (name.startsWith(logFilePrefix)) {
					r.add(new Job(f, dbEngine, dbInstance));
				}
				if (name.startsWith(logFilePrefixWarmup)) {
					File log = new File(dir, name.substring(0, logFilePrefix.length() - 1)
							+ "_" + name.substring(logFilePrefixWarmup.length()));
					if (!log.exists()) {
						r.add(new Job(log, dbEngine, dbInstance));
					}
				}
			}
		}
		
		return r;
	}
	
	
	/**
	 * Get a collection of previously executed jobs
	 * 
	 * @param dbEngine the database engine name
	 * @param dbInstance the database instance name
	 * @return the collection of previously executed jobs
	 */
	public List<Job> getFinishedJobs(String dbEngine, String dbInstance) {
		
		Pair<String, String> key = new Pair<String, String>(dbEngine,
				dbInstance == null ? "" : dbInstance);
		if (finishedJobs.containsKey(key)) {
			return finishedJobs.get(key);
		}
		else {
			return Collections.<Job>emptyList();
		}
	}
	
	
	/**
	 * Get a collection of previously executed jobs
	 * 
	 * @param dbEI the database engine and instance name
	 * @return the collection of previously executed jobs
	 */
	public List<Job> getFinishedJobs(DatabaseEngineAndInstance dbEI) {
		return getFinishedJobs(dbEI.getEngine().getShortName(), dbEI.getInstance());
	}

	
	/**
	 * A thread for running the jobs
	 */
	private class ExecutionThread extends Thread {
		
		public Job current;
		
		
		/**
		 * Create an instance of class ExecutionThread
		 */
		public ExecutionThread() {
			current = null;
		}
		
		
		/**
		 * Run
		 */
		@Override
		public void run() {
			
			try {
				running = true;
				
				while (!paused) {
					
					current = getNextRunnableJob();
					if (current == null) break;
					
					current.start();
					current.join();
					
					Pair<String, String> key = new Pair<String, String>(current.getDbEngine(),
							current.getDbInstanceSafe());
					if (finishedJobs.containsKey(key)) {
						finishedJobs.get(key).add(current);
					}
					else {
						LinkedList<Job> l = new LinkedList<Job>();
						l.add(current);
						finishedJobs.put(key, l);
					}
					finishedJobMap.put(current.getId(), current);
				}
			}
			catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			finally {				
				current = null;
				paused = true;
				running = false;
			}
		}
	}
}
