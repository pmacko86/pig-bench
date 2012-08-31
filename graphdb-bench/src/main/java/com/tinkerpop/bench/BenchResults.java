package com.tinkerpop.bench;

public class BenchResults {
	
	long cumulativeBenchmarkTime = 0;

	
	/**
	 * Create an instance of class BenchResults
	 */
	public BenchResults() {
		// Nothing to do
	}
	
	
	/**
	 * Return the cumulative benchmark time. Please note that this does not equal
	 * the elapsed time in the case of multi-threaded benchmarks, and also please
	 * note that this does not include GC, open, and shutdown graph operations.
	 * 
	 * @return the cumulative benchmark time in us
	 */
	public long getCumulativeBenchmarkTime() {
		return cumulativeBenchmarkTime;
	}
}
