package com.tinkerpop.bench.operation.operations;

import java.util.ArrayList;

import com.tinkerpop.bench.operation.Operation;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;

public class OperationGetAllNeighbors extends Operation {

	private Vertex startVertex;
	private ArrayList<Vertex> neighbors;
	
	@Override
	protected void onInitialize(Object[] args) {
		startVertex = getGraph().getVertex(args[0]);
		neighbors = new ArrayList<Vertex>(100);
	}
	
	@Override
	protected void onExecute() throws Exception {
		try {
			if (!neighbors.isEmpty()) neighbors.clear();
			
			for (Vertex v : startVertex.getVertices(Direction.OUT))
				neighbors.add(v);
			
			setResult(neighbors.size());
		} catch (Exception e) {
			throw e;
		}
	}

}
