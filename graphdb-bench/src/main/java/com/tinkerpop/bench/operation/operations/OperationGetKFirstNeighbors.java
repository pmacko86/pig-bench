package com.tinkerpop.bench.operation.operations;

import java.util.ArrayList;
import java.util.Iterator;

import com.tinkerpop.bench.operation.Operation;
import com.tinkerpop.bench.util.GraphUtils;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;

public class OperationGetKFirstNeighbors extends Operation {

	private Vertex startVertex;
	private int k;
	private ArrayList<Vertex> result;
	private Direction direction;
	
	@Override
	protected void onInitialize(Object[] args) {
		startVertex = getGraph().getVertex(args[0]);
		k = args.length > 1 ? (Integer) args[1] : 2;
		direction = args.length > 2 ? (Direction) args[2] : Direction.OUT;
		result = new ArrayList<Vertex>(k + 1);
	}
	
	@Override
	protected void onExecute() throws Exception {
		try {
			if (!result.isEmpty()) result.clear();
			Vertex curr = startVertex;
			
			for(int i = 0; i < k; i++) {
				Iterable<Vertex> vi = curr.getVertices(direction);
				Iterator<Vertex> iter = vi.iterator();
				if (iter.hasNext()) {
					curr = iter.next();
					result.add(curr);
					GraphUtils.close(vi);
				} 
				else {
					GraphUtils.close(vi);
					break;
				}
			}
			
			setResult(result.size());
		} catch (Exception e) {
			throw e;
		}
	}

}
