package com.tinkerpop.bench.operation.operations;

import java.util.Iterator;

import com.tinkerpop.bench.operation.Operation;
import com.tinkerpop.bench.util.GraphUtils;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;

public class OperationGetFirstNeighbor extends Operation {

	private Vertex startVertex;
	private Direction direction;
	
	@Override
	protected void onInitialize(Object[] args) {
		startVertex = getGraph().getVertex(args[0]);
		direction = args.length > 1 ? (Direction) args[1] : Direction.OUT;
	}
	
	@Override
	protected void onExecute() throws Exception {
		try {
			Vertex result = null;
			
			Iterable<Vertex> vi = startVertex.getVertices(direction);
			Iterator<Vertex> iter = vi.iterator();
			if (iter.hasNext()) result = iter.next();
			GraphUtils.close(vi);
			
			setResult(result != null ? result.toString() : "DNE");
		} catch (Exception e) {
			throw e;
		}
	}
}
