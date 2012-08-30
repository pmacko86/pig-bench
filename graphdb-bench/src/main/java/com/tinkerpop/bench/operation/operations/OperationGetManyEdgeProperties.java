package com.tinkerpop.bench.operation.operations;

import com.tinkerpop.bench.operation.Operation;
import com.tinkerpop.bench.util.StatisticsHelper;
import com.tinkerpop.blueprints.Edge;

public class OperationGetManyEdgeProperties extends Operation {

	private String property_key;
	
	private int opCount;
	private Edge[] edgeSamples;
	
	@Override
	protected void onInitialize(Object[] args) {
		property_key = (String) args[0];
		
		opCount = args.length > 1 ? (Integer) args[1] : 1000;
		edgeSamples = StatisticsHelper.getRandomEdges(getGraph(), opCount);
	}
	
	@Override
	protected void onExecute() throws Exception {
		try {
			for (int i = 0; i < opCount; i++)
				edgeSamples[i].getProperty(property_key);
			
			setResult(opCount);
		} catch (Exception e) {
			throw e;
		}
	}

}
