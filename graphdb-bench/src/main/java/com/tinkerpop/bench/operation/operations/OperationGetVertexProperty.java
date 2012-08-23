package com.tinkerpop.bench.operation.operations;

import com.tinkerpop.bench.operation.Operation;
import com.tinkerpop.blueprints.Vertex;

public class OperationGetVertexProperty extends Operation {

	private Vertex vertex;
	private String property_key;
	
	@Override
	protected void onInitialize(Object[] args) {
		vertex = getGraph().getVertex(args[0]);
		property_key = (String) args[1];
	}
	
	@Override
	protected void onExecute() throws Exception {
		try {
			setResult(vertex.getProperty(property_key).toString());
		} catch (Exception e) {
			throw e;
		}
	}

}
