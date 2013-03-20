package com.tinkerpop.bench.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;


/**
 * A handle to an external process 
 * 
 * @author Peter Macko
 */
public class ExternalProcess {
	
	private String[] cmds;
	private Process p;
	private boolean canceled;
	private boolean useBinSH;
	
	
	/**
	 * Initialize the process. Note that running the process using
	 * an external shell might result in cancel() not working properly.
	 * 
	 * @param cmd the command to run using /bin/sh
	 */
	public ExternalProcess(String cmd) {
		this.cmds = new String[1];
		this.cmds[0] = cmd;
		this.p = null;
		this.canceled = false;
		this.useBinSH = true;
	}
	
	
	/**
	 * Initialize the process
	 * 
	 * @param cmds the command and arguments to run using Java's Runtime
	 */
	public ExternalProcess(String[] cmds) {
		this.cmds = new String[cmds.length];
		for (int i = 0; i < cmds.length; i++) this.cmds[i] = cmds[i];
		this.p = null;
		this.canceled = false;
		this.useBinSH = false;
	}
	

	/**
	 * Start a program and return its input stream
	 *
	 * @return the program output
	 * @throws IOException if an error occurred
	 */
	public OutputStream start() throws IOException {
		
		if (p != null) {
			throw new IOException("The program is already running");
		}

		canceled = false;
		if (useBinSH) {
			String[] a = {"/bin/sh", "-c", cmds[0]};
			p = Runtime.getRuntime().exec(a);
		}
		else {
			p = Runtime.getRuntime().exec(cmds);
		}

		return p.getOutputStream();
	}
	
	
	/**
	 * Get the output stream of the process
	 * 
	 * @return the output stream
	 * @throws IOException if an error occurred
	 */
	public InputStream getProcessOutputStream() throws IOException {
		
		if (p == null) {
			throw new IOException("The program is not running");
		}
		
		return p.getInputStream();
	}
	

	/**
	 * Finish the program started with start() - capture the output,
	 * wait for completion, and throw an exception if its execution
	 * failed.
	 *
	 * @return the program output
	 * @throws IOException if an error occurred
	 */
	public String finish() throws IOException {

		String line;
		StringBuilder error = new StringBuilder();
		StringBuilder output = new StringBuilder();
		int result = -1;
		
		if (p == null) {
			throw new IOException("The program is not running");
		}

		try {
			BufferedReader es = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			BufferedReader os = new BufferedReader(new InputStreamReader(p.getInputStream()));

			while ((line = os.readLine()) != null) output.append(line);
			while ((line = es.readLine()) != null) error.append(line);

			es.close();
			os.close();

			result = p.waitFor();
			p = null;
		}
		catch (Exception e) {
			result = -1;
			error.append(e.getMessage());
		}
		
		p = null;

		if (result != 0) {
			if (canceled) throw new IOException("Canceled");
			throw new IOException(error.toString());
		}
		else {
			return output.toString();
		}
	}
	

	/**
	 * Run a program, capture the output, wait for completion, and throw
	 * an exception if its execution failed. If the program is already
	 * running, the behavior of this method is undefined.
	 *
	 * @return the program output
	 * @throws IOException if an error occurred
	 */
	public String run() throws IOException {
		
		start();
		return finish();
	}
	
	
	/**
	 * Kill the process
	 */
	public void kill() {
		
		// NOTE Java's Process.destroy() does not kill subprocesses, so something
		// should be done about this...
		
		try {
			if (p != null) {
				canceled = true;
				p.destroy();
			}
		}
		catch (Exception e) {
			// Silent failover
		}
	}
}
