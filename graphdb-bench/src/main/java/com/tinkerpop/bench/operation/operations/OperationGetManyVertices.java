package com.tinkerpop.bench.operation.operations;

import com.tinkerpop.bench.operation.Operation;
import com.tinkerpop.bench.util.StatisticsHelper;
import com.tinkerpop.blueprints.Graph;

public class OperationGetManyVertices extends Operation {

	private int opCount;
	private Object[] vertexSamples;
	
	@Override
	protected void onInitialize(Object[] args) {
		opCount = args.length > 0 ? (Integer) args[0] : 1000;
		vertexSamples = StatisticsHelper.getRandomVertexIds(getGraph(), opCount);
	}
	
	@Override
	protected void onExecute() throws Exception {
		try {
			Graph graph = getGraph();
			
			for (int i = 0; i < opCount; i++)
				graph.getVertex(vertexSamples[i]);
			
			setResult(opCount);
		} catch (Exception e) {
			throw e;
		}
	}
}
