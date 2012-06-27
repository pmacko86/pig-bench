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
	
	
	/**
	 * Create an instance of a job list
	 */
	protected JobList() {
		jobs = new ArrayList<Job>();
		jobMap = new HashMap<Integer, Job>();
		currentJobIndex = 0;
		lastId = -1;
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
	 * Add a job to the list
	 * 
	 * @param job the job to add
	 */
	public synchronized void addJob(Job job) {
		job.id = ++lastId;
		jobs.add(job);
		jobMap.put(job.getId(), job);
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
}
