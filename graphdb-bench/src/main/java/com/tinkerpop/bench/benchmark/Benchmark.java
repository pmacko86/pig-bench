package com.tinkerpop.bench.benchmark;

import java.io.File;
import java.util.ArrayList;

import com.tinkerpop.bench.BenchResults;
import com.tinkerpop.bench.BenchRunner;
import com.tinkerpop.bench.GraphDescriptor;
import com.tinkerpop.bench.operationFactory.OperationFactory;

/**
 * @author Alex Averbuch (alex.averbuch@gmail.com)
 * @author Peter Macko (pmacko@eecs.harvard.edu)
 */
public abstract class Benchmark {
	
	private boolean actualRun;
	private boolean captureIostat = false;

	
	/**
	 * Create an instance of class Benchmark
	 * 
	 * @param actualRun true if this is an actual run, false if it is a warmup run
	 */
	public Benchmark(boolean actualRun) {
		this.actualRun = actualRun;
	}
	
	
	/**
	 * Return true if this is an actual run
	 * 
	 * @return true if this is an actual run, false if it is a warmup run
	 */
	public boolean isActualRun() {
		return actualRun;
	}
	
	
	/**
	 * Set whether to capture the iostat
	 * 
	 * @param capture true to capture iostat
	 */
	public void setCapturingIostat(boolean capture) {
		captureIostat = capture;
	}
	
	
	/**
	 * Return whether to capture the iostat
	 * 
	 * @return true to capture iostat
	 */
	public boolean isCapturingIostat() {
		return captureIostat;
	}

	
	/**
	 * Create the operation factories
	 * 
	 * @return a list of operation factories
	 */
	public abstract ArrayList<OperationFactory> createOperationFactories();
	
	
	/**
	 * Run the benchmark
	 * 
	 * @param graphDescriptor the graph descriptor
	 * @param logOut the log file (null disables logging)
	 * @return some benchmark results
	 * @throws Exception on error
	 */
	public final BenchResults runBenchmark(GraphDescriptor graphDescriptor, String logOut) throws Exception {
		return runBenchmark(graphDescriptor, logOut, GraphDescriptor.OpenMode.DEFAULT, 1);
	}
	
	
	/**
	 * Run the benchmark
	 * 
	 * @param graphDescriptor the graph descriptor
	 * @param logOut the log file (null disables logging)
	 * @param threads the number of threads
	 * @return some benchmark results
	 * @throws Exception on error
	 */
	public final BenchResults runBenchmark(GraphDescriptor graphDescriptor, String logOut,
			GraphDescriptor.OpenMode openMode, int threads) throws Exception {
		
		BenchRunner benchRunner = new BenchRunner(graphDescriptor,
				logOut == null ? null : new File(logOut),
				this, openMode, threads);
		
		return benchRunner.runBenchmark();
	}
}
