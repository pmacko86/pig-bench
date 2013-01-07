package com.tinkerpop.bench.operation.operations;

import com.tinkerpop.bench.operation.Operation;


public class OperationBlank extends Operation {
	
	private int result;
	
	@Override
	protected void onInitialize(Object[] args) {
		result = (int) (Math.random() * 10);
	}
	
	@Override
	protected void onExecute() throws Exception {
		getGraph();
		setResult("" + result);		// Return a random number from a small range as a result
	}

	@Override
	protected void onFinalize() {
	}
}
