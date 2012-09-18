package com.tinkerpop.bench.operation.operations;

import com.tinkerpop.bench.operation.Operation;
import com.tinkerpop.bench.util.GraphUtils;
import com.tinkerpop.blueprints.Vertex;

public class OperationLocalClusteringCoefficient extends Operation {

	private Vertex vertex;
	
	@Override
	protected void onInitialize(Object[] args) {
		vertex = getGraph().getVertex(args[0]);
	}
	
	@Override
	protected void onExecute() throws Exception {
		try {
			GraphUtils.OpStat stat = new GraphUtils.OpStat();
			
			double r = GraphUtils.localClusteringCoefficient(vertex, stat);
			
			setResult(r + ":" + stat);
		} catch (Exception e) {
			throw e;
		}
	}
}
