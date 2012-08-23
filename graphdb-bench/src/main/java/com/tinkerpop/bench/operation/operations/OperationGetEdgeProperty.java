package com.tinkerpop.bench.operation.operations;

import com.tinkerpop.bench.operation.Operation;
import com.tinkerpop.blueprints.Edge;

public class OperationGetEdgeProperty extends Operation {

	private Edge edge;
	private String property_key;
	
	@Override
	protected void onInitialize(Object[] args) {
		edge = getGraph().getEdge(args[0]);
		property_key = (String) args[1];
	}
	
	@Override
	protected void onExecute() throws Exception {
		try {
			setResult(edge.getProperty(property_key).toString());
		} catch (Exception e) {
			throw e;
		}
	}

}
