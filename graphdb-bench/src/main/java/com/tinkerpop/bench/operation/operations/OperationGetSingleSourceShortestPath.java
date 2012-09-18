package com.tinkerpop.bench.operation.operations;

import java.util.Comparator;
import java.util.HashMap;

import com.tinkerpop.bench.operation.Operation;
import com.tinkerpop.bench.util.GraphUtils;
import com.tinkerpop.bench.util.PriorityHashQueue;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;

public class OperationGetSingleSourceShortestPath extends Operation {

	private Vertex source;
	private Direction direction;
	
	@Override
	protected void onInitialize(Object[] args) {
		source = getGraph().getVertex(args[0]);
		direction = Direction.BOTH;		// undirected
	}
	
	@Override
	protected void onExecute() throws Exception {
		try {
			int get_nbrs = 0;
			int get_vertex = 0;
			
			final HashMap<Vertex,Vertex> prev = new HashMap<Vertex,Vertex>();
			final HashMap<Vertex,Integer> dist = new HashMap<Vertex,Integer>();
			
			final Comparator<Vertex> minDist = new Comparator<Vertex>()
			{
				@SuppressWarnings({ "unchecked", "rawtypes" })
				public int compare(Vertex left, Vertex right) {
					Integer  leftDist = dist.get( left);
					Integer rightDist = dist.get(right);
					int l = leftDist  != null ?  leftDist.intValue() : Integer.MAX_VALUE;
					int r = rightDist != null ? rightDist.intValue() : Integer.MAX_VALUE;
					return l > r ? 1 : l < r ? -1 : ((Comparable) left.getId()).compareTo(right.getId());
				}
			};
			
			final PriorityHashQueue<Vertex> queue = new PriorityHashQueue<Vertex>(11, minDist);
			
			dist.put(source, 0);
			queue.add(source);
			
			while (!queue.isEmpty()) {
				Vertex u = queue.remove();
				
				int dist_u = dist.get(u);
				
				get_nbrs++;
				Iterable<Vertex> vi = u.getVertices(direction);
				for (Vertex v : vi) {
					get_vertex++;
					
					Integer dist_v = dist.get(v);						
					int alt = dist_u + 1;
					
					if (dist_v == null || alt < dist_v.intValue()) {
						prev.put(v, u);
						dist.put(v, alt);
						queue.remove(v);
						queue.add(v);
					}
				}
				GraphUtils.close(vi);
			}
			
			// format reachable_nodes:get_nbrs:get_vertex
			setResult(dist.size() + ":" + get_nbrs + ":" + get_vertex);
		}
		catch (Exception e) {
			throw e;
		}
	}
}
