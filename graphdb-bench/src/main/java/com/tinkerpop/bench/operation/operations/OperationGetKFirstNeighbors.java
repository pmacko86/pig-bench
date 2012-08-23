package com.tinkerpop.bench.operation.operations;

import java.util.ArrayList;
import java.util.Iterator;

import com.tinkerpop.bench.operation.Operation;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;

public class OperationGetKFirstNeighbors extends Operation {

	private Vertex startVertex;
	private int k;
	private ArrayList<Vertex> result;
	
	@Override
	protected void onInitialize(Object[] args) {
		startVertex = getGraph().getVertex(args[0]);
		k = args.length > 1 ? (Integer) args[1] : 2;
		result = new ArrayList<Vertex>(k + 1);
	}
	
	@Override
	protected void onExecute() throws Exception {
		try {
			if (!result.isEmpty()) result.clear();
			Vertex curr = startVertex;
			
			for(int i = 0; i < k; i++) {
				Iterator<Vertex> iter = curr.getVertices(Direction.OUT).iterator();
				if (iter.hasNext()) {
					curr = iter.next();
					result.add(curr);
				} 
				else {
					break;
				}
			}
			
			setResult(result.size());
		} catch (Exception e) {
			throw e;
		}
	}

}
