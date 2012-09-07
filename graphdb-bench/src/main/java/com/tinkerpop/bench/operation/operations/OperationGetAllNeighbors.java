package com.tinkerpop.bench.operation.operations;

import java.util.ArrayList;

import com.tinkerpop.bench.operation.Operation;
import com.tinkerpop.bench.util.GraphUtils;
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
			
			Iterable<Vertex> vi = startVertex.getVertices(Direction.OUT);
			for (Vertex v : vi) neighbors.add(v);
			GraphUtils.close(vi);
			
			setResult(neighbors.size());
		} catch (Exception e) {
			throw e;
		}
	}

	@Override
	protected void onFinalize() {
		neighbors = null;
	}
}
