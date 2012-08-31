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

	
	/**
	 * Create an instance of class Benchmark
	 */
	public Benchmark() {
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
		return runBenchmark(graphDescriptor, logOut, 1);
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
	public final BenchResults runBenchmark(GraphDescriptor graphDescriptor, String logOut, int threads) throws Exception {
		BenchRunner benchRunner = new BenchRunner(graphDescriptor, logOut == null ? null : new File(logOut), this, threads);
		return benchRunner.runBenchmark();
	}
}
