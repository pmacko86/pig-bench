package com.tinkerpop.bench.operation.operations;

import com.tinkerpop.bench.operation.Operation;
import com.tinkerpop.bench.util.GraphUtils;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;

public class OperationGetEdgesUsingKeyIndex extends Operation {

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
		value = (Object[]) args[1];
	}
	
	@Override
	protected void onExecute() throws Exception {
		
		Graph g = getGraph();
		Iterable<Edge> itr = g.getEdges(key, value);
		int count = 0;
		for (@SuppressWarnings("unused") Edge e : itr) count++;
		GraphUtils.close(itr);
		
		setResult("" + count);
	}
}
