package com.tinkerpop.bench.web;

import java.util.ArrayList;
import java.util.HashMap;


/**
 * A list of jobs
 * 
 * @author Peter Macko (pmacko@eecs.harvard.edu)
 */
public class JobList {
	
	private static JobList instance = new JobList();

	private ArrayList<Job> jobs;
	private HashMap<Integer, Job> jobMap;
	
	private int currentJobIndex;
	private int lastId;
	
	private ExecutionThread thread;
	private boolean paused;
	private boolean running;
	
	
	/**
	 * Create an instance of a job list
	 */
	protected JobList() {
		jobs = new ArrayList<Job>();
		jobMap = new HashMap<Integer, Job>();
		currentJobIndex = 0;
		lastId = -1;
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
	 * Return the index of the current job
	 * 
	 * @return the currentJobIndex
	 */
	public int getCurrentJobIndex() {
		return currentJobIndex;
	}
	
	
	/**
	 * Allocate a new job ID
	 * 
	 * @return the new ID
	 */
	protected synchronized int allocateJobId() {
		return ++lastId;
	}
	
	
	/**
	 * Add a job to the list
	 * 
	 * @param job the job to add
	 */
	public synchronized void addJob(Job job) {
		job.id = allocateJobId();
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
