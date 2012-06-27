package com.tinkerpop.bench.web;

import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.tinkerpop.bench.Bench;


/**
 * A job in the web interface
 * 
 * @author Peter Macko (pmacko@eecs.harvard.edu)
 */
public class Job {

	int id;
	private List<String> arguments;
	
	private int status;
	private int executionCount;
	
	
	/**
	 * Create an instance of a Job
	 * 
	 * @param request the HTTP request from which to create the job
	 */
	public Job(HttpServletRequest request) {
		
		arguments = new LinkedList<String>();
		id = -1;
		status = -1;
		executionCount = 0;
		
		
		// Build the list of command-line arguments
		
		String dbName = WebUtils.getStringParameter(request, "database_name");
		String dbInstance = WebUtils.getStringParameter(request, "database_instance");
		String s_txBuffer = WebUtils.getStringParameter(request, "tx_buffer");
		String s_opCount = WebUtils.getStringParameter(request, "op_count");
		String s_warmupOpCount = WebUtils.getStringParameter(request, "warmup_op_count");
		String[] workloads = WebUtils.getStringParameterValues(request, "workloads");
		
		// Note: Remember to validate the input for file names when we add a support for such arguments
		
		arguments.add(Bench.graphdbBenchDir + "/runBenchmarkSuite.sh");
		arguments.add("--dumb-terminal");
		
		if (dbName           != null) { arguments.add("--" + dbName); }
		if (dbInstance       != null) { arguments.add("--database"); arguments.add(dbInstance); }
		if (s_txBuffer       != null) { arguments.add("--tx-buffer"); arguments.add(s_txBuffer); }
		if (s_opCount        != null) { arguments.add("--op-count"); arguments.add(s_opCount); }
		if (s_warmupOpCount  != null) { arguments.add("--warmup-op-count"); arguments.add(s_warmupOpCount); }
		
		if (workloads != null) {
			for (String s : workloads) {
				arguments.add("--" + s);
			}
		}

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
	 * @param useSimpleProgramName true to use a simple (abbreviated) program name
	 * @return the string
	 */
	public String toStringExt(boolean multiline, boolean useSimpleProgramName, String lineStart, String lineEnd) {
		
		boolean first = true;
		StringBuilder sb = new StringBuilder();
		
		for (String s : arguments) {
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
					if (useSimpleProgramName) {
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
	 * @return the string
	 */
	public String toString(boolean useSimpleProgramName) {
		return toStringExt(false, useSimpleProgramName, "", "");
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
	 * @param useSimpleProgramName true to use a simple (abbreviated) program name
	 * @return the string
	 */
	public String toMultilineString(boolean useSimpleProgramName) {
		return toStringExt(true, useSimpleProgramName, "", "\n");
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
	 * Set the status of the last job execution
	 * 
	 * @param status the status code
	 */
	public synchronized void jobTerminated(int status) {
		this.status = status;
		this.executionCount++;
	}
}