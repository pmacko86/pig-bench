package com.tinkerpop.bench.operation.operations;

import java.util.ArrayList;
import java.util.Random;

import com.tinkerpop.bench.operation.Operation;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;

public class OperationGetKRandomNeighbors extends Operation {

	private Vertex startVertex;
	private int k;
	private ArrayList<Vertex> result;
	private Random random;
	
	@Override
	protected void onInitialize(Object[] args) {
		startVertex = getGraph().getVertex(args[0]);
		k = args.length > 1 ? (Integer) args[1] : 2;
		result = new ArrayList<Vertex>(k + 1);
		random = new Random();
	}
	
	@Override
	protected void onExecute() throws Exception {
		try {
			int vertex_cnt = 0;
			
			Vertex curr = startVertex;
			final ArrayList<Vertex> next = new ArrayList<Vertex>();
			if (!result.isEmpty()) result.clear();
						
			for(int i = 0; i < k; i++) {
				for (Vertex v : curr.getVertices(Direction.OUT)) {
					vertex_cnt++;
					next.add(v);
				}
				if (next.size() > 0) {
					curr = next.get(random.nextInt(next.size()));
					next.clear();
					result.add(curr);
				} else
					break;
			}
		
			setResult(vertex_cnt + ":" + result.size());	
		} catch (Exception e) {
			throw e;
		}
	}
}
