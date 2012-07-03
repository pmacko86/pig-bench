package com.tinkerpop.bench.web;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import com.tinkerpop.bench.Bench;
import com.tinkerpop.bench.benchmark.BenchmarkMicro;


/**
 * A job in the web interface
 * 
 * @author Peter Macko (pmacko@eecs.harvard.edu)
 */
public class Job {

	int id;
	private List<String> arguments;
	private String dbEngine;
	private String dbInstance;
	
	private int status;
	private int executionCount;
	
	private ExecutionThread current = null;
	private ExecutionThread last = null;
	private boolean bufferedThreadOutput = false;
	
	
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
		loadFromRequest(request, dbEngine, dbInstance);
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
		id = -1;
		status = -1;
		executionCount = 0;
		
		
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
		if (dbInstance.equals("")) dbInstance = null;
		
		String s_annotation = WebUtils.getStringParameter(request, "annotation");
		String s_txBuffer = WebUtils.getStringParameter(request, "tx_buffer");
		String s_opCount = WebUtils.getStringParameter(request, "op_count");
		String s_warmupOpCount = WebUtils.getStringParameter(request, "warmup_op_count");
		String s_kHops = WebUtils.getStringParameter(request, "k_hops");
		String s_ingestFile = WebUtils.getStringParameter(request, "ingest_file");
		String s_ingestWarmupFile = WebUtils.getStringParameter(request, "ingest_warmup_file");

		String[] workloads = WebUtils.getStringParameterValues(request, "workloads");
		
		
		// Sanitize the input
		
		// Note: Remember to validate the input for file names when we add a support for such arguments
		
		if (dbInstance != null) {
			if (!Pattern.matches("^[a-z][a-z0-9_]*$", dbInstance)) {
	    		throw new RuntimeException("Invalid database instance name (can contain only lower-case letters, "
	    				+ "numbers, and _, and has to start with a letter)");
	    	}
		}

		
		// Build the list of command-line arguments
		
		arguments.add(Bench.graphdbBenchDir + "/runBenchmarkSuite.sh");
		arguments.add("--dumb-terminal");
		
		if (dbEngine         != null) { arguments.add("--" + dbEngine); }
		if (dbInstance       != null) { arguments.add("--database"); arguments.add(dbInstance); }
		if (s_annotation     != null) { arguments.add("--annotation"); arguments.add(s_annotation); }
		
		if (workloads != null) {
			for (String s : workloads) {
				arguments.add("--" + s);
				if ("ingest".equals(s) && s_ingestFile != null) {
					arguments.add(s_ingestFile);
				}
			}
		}
		
		if (s_txBuffer         != null) { arguments.add("--tx-buffer"); arguments.add(s_txBuffer); }
		if (s_opCount          != null) { arguments.add("--op-count"); arguments.add(s_opCount); }
		if (s_warmupOpCount    != null) { arguments.add("--warmup-op-count"); arguments.add(s_warmupOpCount); }
		if (s_kHops            != null) { arguments.add("--k-hops"); arguments.add(s_kHops); }
		if (s_ingestWarmupFile != null) { arguments.add("--warmup-ingest"); arguments.add(s_ingestWarmupFile); }
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
		boolean skip = false;
		StringBuilder sb = new StringBuilder();
		int next_i = 0;
		String s_op_count = "";
		
		
		// Process each argument
		
		for (String s : arguments) {
			int i = next_i++;
			boolean last = next_i == arguments.size();
			if (skip) {
				skip = false;
				continue;
			}
			
			
			// Simplify the output if we need to
			
			if (simple) {
				
				// Remove options that affect the output
				
				if (s.equals("--dumb-terminal")) continue;
				if (s.equals("--no-color")) continue;
				
				
				// Remove default options
				
				if (s.equals("--k-hops") && !last) {
					s_op_count = arguments.get(i+1);
					if (arguments.get(i+1).equals("" + BenchmarkMicro.DEFAULT_K_HOPS)) {skip = true;continue;}
				}
				if (s.equals("--op-count") && !last) {
					s_op_count = arguments.get(i+1);
					if (arguments.get(i+1).equals("" + BenchmarkMicro.DEFAULT_OP_COUNT)) {skip = true;continue;}
				}
				if (s.equals("--warmup-op-count") && !last) {
					if (arguments.get(i+1).equals(s_op_count)) {skip = true;continue;}
				}
				if (s.equals("--tx-buffer") && !last) {
					if (arguments.get(i+1).equals("" + BenchmarkMicro.DEFAULT_NUM_THREADS)) {skip = true;continue;}
				}
				if (s.equals("--k-hops") && !last) {
					if (arguments.get(i+1).equals("" + BenchmarkMicro.DEFAULT_K_HOPS)) {skip = true;continue;}
				}
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
	 * Start the job and return immediately
	 */
	public synchronized void start() {
		
		if (current != null) {
			throw new IllegalStateException("The job is already running");
		}
		
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
				
				Job.this.status = status;
				Job.this.executionCount++;
				
				last = this;
				current = null;		// This must be the very last statement
			}
		}
	}
}
