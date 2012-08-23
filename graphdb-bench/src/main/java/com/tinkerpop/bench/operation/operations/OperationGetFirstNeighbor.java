package com.tinkerpop.bench.operation.operations;

import java.util.Iterator;

import com.tinkerpop.bench.operation.Operation;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;

public class OperationGetFirstNeighbor extends Operation {

	private Vertex startVertex;
	
	@Override
	protected void onInitialize(Object[] args) {
		startVertex = getGraph().getVertex(args[0]);
	}
	
	@Override
	protected void onExecute() throws Exception {
		try {
			Vertex result = null;
			
			Iterator<Vertex> iter = startVertex.getVertices(Direction.OUT).iterator();
			if (iter.hasNext())
				result = iter.next();
			
			setResult(result != null ? result.toString() : "DNE");
		} catch (Exception e) {
			throw e;
		}
	}
}
