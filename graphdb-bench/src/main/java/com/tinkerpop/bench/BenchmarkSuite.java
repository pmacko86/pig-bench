package com.tinkerpop.bench;

import com.tinkerpop.bench.benchmark.BenchmarkMicro;


public class BenchmarkSuite {
	
	public static void main(String[] args) throws Exception {
		
		int r = BenchmarkMicro.run(args);
		if (r != 0) System.exit(r);
	}
}
