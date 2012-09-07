package com.tinkerpop.bench.operation.operations;

import java.util.ArrayList;
import java.util.HashSet;

import com.tinkerpop.bench.operation.Operation;
import com.tinkerpop.bench.util.GraphUtils;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;

public class OperationGetKHopNeighbors extends Operation {

	private Vertex startVertex;
	private int k;
	private Direction direction;
	private HashSet<Vertex> result;
	
	@Override
	protected void onInitialize(Object[] args) {
		startVertex = getGraph().getVertex(args[0]);
		k = args.length > 1 ? (Integer) args[1] : 2;
		direction = args.length > 2 ? (Direction) args[2] : Direction.OUT;
		result = new HashSet<Vertex>();
	}
	
	@Override
	protected void onExecute() throws Exception {
		try {
			int real_hops, get_ops = 0, get_vertex = 0;
			
			ArrayList<Vertex> curr = new ArrayList<Vertex>();
			ArrayList<Vertex> next = new ArrayList<Vertex>();
			
			curr.add(startVertex);
			
			for(real_hops = 0; real_hops < k; real_hops++) {
				
				for (Vertex u : curr) {
					
					get_ops++;
					
					Iterable<Vertex> vi = u.getVertices(direction);
					for (Vertex v : vi) {
						
						get_vertex++;
						
						if (result.add(v)) {
							next.add(v);
						}
					}
					GraphUtils.close(vi);
				}
				
				if(next.size() == 0)
					break;
				
				ArrayList<Vertex> tmp = curr;
				curr = next;
				tmp.clear();
				next = tmp;
			}
			
			setResult(result.size() + ":" + real_hops + ":" + get_ops + ":" + get_vertex);
			
		} catch (Exception e) {
			throw e;
		}
	}

	@Override
	protected void onFinalize() {
		result = null;
	}
}
