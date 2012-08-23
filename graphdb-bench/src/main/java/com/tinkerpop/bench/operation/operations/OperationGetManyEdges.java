package com.tinkerpop.bench.operation.operations;

import com.tinkerpop.bench.StatisticsHelper;
import com.tinkerpop.bench.operation.Operation;
import com.tinkerpop.blueprints.Graph;

public class OperationGetManyEdges extends Operation {

	private int opCount;
	private Object[] edgeSamples;
	
	@Override
	protected void onInitialize(Object[] args) {
		opCount = args.length > 0 ? (Integer) args[0] : 1000;
		edgeSamples = StatisticsHelper.getRandomEdgeIds(getGraph(), opCount);		
	}
	
	@Override
	protected void onExecute() throws Exception {
		try {
			Graph graph = getGraph();
			
			for (int i = 0; i < opCount; i++)
				graph.getEdge(edgeSamples[i]);
			
			setResult(opCount);
		} catch (Exception e) {
			throw e;
		}
	}

}
