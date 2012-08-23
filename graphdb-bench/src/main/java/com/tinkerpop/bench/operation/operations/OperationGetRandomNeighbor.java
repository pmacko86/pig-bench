package com.tinkerpop.bench.operation.operations;

import java.util.ArrayList;
import java.util.Random;

import com.tinkerpop.bench.operation.Operation;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;

public class OperationGetRandomNeighbor extends Operation {

	private Vertex startVertex;
	private Random random;
	
	@Override
	protected void onInitialize(Object[] args) {
		startVertex = getGraph().getVertex(args[0]);
		random = new Random();
	}
	
	@Override
	protected void onExecute() throws Exception {
		try {
			Vertex result = null;
			
			final ArrayList<Vertex> vertices = new ArrayList<Vertex>();
			for (Vertex v : startVertex.getVertices(Direction.OUT))
				vertices.add(v);
			if (vertices.size() > 0)
				result = vertices.get(random.nextInt(vertices.size()));
			
			setResult(result != null ? result.toString() : "DNE");
			} catch (Exception e) {
			throw e;
		}
	}
}
