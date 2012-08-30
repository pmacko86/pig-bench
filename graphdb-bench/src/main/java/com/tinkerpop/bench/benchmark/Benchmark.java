package com.tinkerpop.bench.benchmark;

import java.io.File;
import java.util.ArrayList;

import com.tinkerpop.bench.BenchRunner;
import com.tinkerpop.bench.GraphDescriptor;
import com.tinkerpop.bench.operationFactory.OperationFactory;

/**
 * @author Alex Averbuch (alex.averbuch@gmail.com)
 * @author Peter Macko (pmacko@eecs.harvard.edu)
 */
public abstract class Benchmark {

	public Benchmark() {
	}

	public abstract ArrayList<OperationFactory> createOperationFactories();

	public final void runBenchmark(GraphDescriptor graphDescriptor, String logOut) throws Exception {
		runBenchmark(graphDescriptor, logOut, 1);
	}
	
	public final void runBenchmark(GraphDescriptor graphDescriptor, String logOut, int threads) throws Exception {
		BenchRunner benchRunner = new BenchRunner(graphDescriptor, logOut == null ? null : new File(logOut), this, threads);
		benchRunner.runBenchmark();
	}
}
