package com.tinkerpop.bench.operation.operations;

import com.tinkerpop.bench.operation.Operation;
import com.tinkerpop.bench.util.GraphUtils;
import com.tinkerpop.bench.util.StatisticsHelper;
import com.tinkerpop.blueprints.Vertex;

public class OperationLocalClusteringCoefficient extends Operation {

	private int opCount;
	private Vertex[] vertices;
	
	@Override
	protected void onInitialize(Object[] args) {
		opCount = args.length > 0 ? (Integer) args[0] : 1000;
		vertices = StatisticsHelper.getRandomVertices(getGraph(), opCount);
	}
	
	@Override
	protected void onExecute() throws Exception {
		try {
			GraphUtils.OpStat stat = new GraphUtils.OpStat();
			
			for (int i = 0; i < opCount; i++) {
				GraphUtils.localClusteringCoefficient(vertices[i], stat);
			}
			
			setResult(opCount + ":" + stat);
		} catch (Exception e) {
			throw e;
		}
	}
}
