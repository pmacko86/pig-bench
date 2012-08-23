package com.tinkerpop.bench.operation.operations;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;

import com.tinkerpop.bench.GlobalConfig;
import com.tinkerpop.bench.operation.Operation;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.extensions.impls.sql.SqlGraph;

public class OperationGetShortestPath extends Operation {

	private Vertex source;
	private Vertex target;
	private boolean isSQLGraph;
	private Direction direction;
	
	@Override
	protected void onInitialize(Object[] args) {
		source = getGraph().getVertex(args[0]);
		target = getGraph().getVertex(args[1]);
		isSQLGraph = getGraph() instanceof SqlGraph;
		direction = Direction.BOTH;		// undirected
	}
	
	@Override
	protected void onExecute() throws Exception {
		try {
			ArrayList<Vertex> result = new ArrayList<Vertex>();
			
			if (isSQLGraph && GlobalConfig.useStoredProcedures) {
				for (Vertex u : ((SqlGraph) getGraph()).getShortestPath(source, target)) {
					result.add(u);
				}
				setResult(result.size());
			} else {
				int get_nbrs = 0;
				int get_vertex = 0;
				
				final HashMap<Vertex,Vertex> prev = new HashMap<Vertex,Vertex>();
				final HashMap<Vertex,Integer> dist = new HashMap<Vertex,Integer>();
				
				final Comparator<Vertex> minDist = new Comparator<Vertex>()
				{
					public int compare(Vertex left, Vertex right) {
						int leftDist = dist.containsKey(left) ? dist.get(left) : Integer.MAX_VALUE;
						int rightDist = dist.containsKey(right) ? dist.get(right) : Integer.MAX_VALUE;
						return leftDist > rightDist ? 1 : leftDist < rightDist ? -1 : 0;
					}
				};
				
				//dmargo: 11 is the Java default initial capacity...don't ask me why.
				final PriorityQueue<Vertex> queue = new PriorityQueue<Vertex>(11, minDist);
				
				dist.put(source, 0);
				queue.add(source);
				
				while (!queue.isEmpty()) {
					Vertex u = queue.remove();
					
					if (u.equals(target))
						break;
					
					get_nbrs++;
					for (Vertex v : u.getVertices(direction)) {
						get_vertex++;
						
						int alt = dist.get(u) + 1;
						int cur = dist.containsKey(v) ? dist.get(v) : Integer.MAX_VALUE;
						
						if (alt < cur) {
							prev.put(v, u);
							dist.put(v, alt);
							queue.remove(v);
							queue.add(v);
						}
					}
				}
								
				Vertex u = target;
				while (prev.containsKey(u)) {
					result.add(0, u);
					u = prev.get(u);
				}
				
				// format path_len:get_nbrs:get_vertex
				setResult(result.size() + ":" + get_nbrs + ":" + get_vertex);
			}
		} catch (Exception e) {
			throw e;
		}
	}
}
