package com.tinkerpop.bench.operation.operations;

import java.util.ArrayList;
import java.util.Random;

import com.tinkerpop.bench.operation.Operation;
import com.tinkerpop.bench.util.GraphUtils;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;

public class OperationGetKRandomNeighbors extends Operation {

	private Vertex startVertex;
	private int k;
	private ArrayList<Vertex> result;
	private Random random;
	private Direction direction;
	
	@Override
	protected void onInitialize(Object[] args) {
		startVertex = getGraph().getVertex(args[0]);
		k = args.length > 1 ? (Integer) args[1] : 2;
		direction = args.length > 2 ? (Direction) args[2] : Direction.OUT;
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
				Iterable<Vertex> vi = curr.getVertices(direction);
				for (Vertex v : vi) {
					vertex_cnt++;
					next.add(v);
				}
				GraphUtils.close(vi);
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
