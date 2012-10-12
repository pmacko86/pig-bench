package com.tinkerpop.bench.operation.operations;

import com.tinkerpop.bench.operation.Operation;
import com.tinkerpop.bench.util.GraphUtils;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;

public class OperationGetVerticesUsingKeyIndex extends Operation {

	private String key;
	private Object value;
	
	@Override
	protected void onInitialize(Object[] args) {
		
		if (args.length == 0) {
			key = null; value = null;
			return;
		}
		
		if (args.length != 2) 
			throw new IllegalArgumentException("Invalid arguments - usage: { key, value }");
		
		key = (String) args[0];
		value = (Object) args[1];
	}
	
	@Override
	protected void onExecute() throws Exception {
		
		Graph g = getGraph();
		Iterable<Vertex> itr = g.getVertices(key, value);
		int count = 0;
		for (@SuppressWarnings("unused") Vertex v : itr) count++;
		GraphUtils.close(itr);
		
		setResult("" + count);
	}
}
