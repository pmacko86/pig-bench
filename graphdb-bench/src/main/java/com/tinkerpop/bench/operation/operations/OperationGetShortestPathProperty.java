package com.tinkerpop.bench.operation.operations;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

import com.tinkerpop.bench.DatabaseEngine;
import com.tinkerpop.bench.GlobalConfig;
import com.tinkerpop.bench.operation.Operation;
import com.tinkerpop.bench.util.GraphUtils;
import com.tinkerpop.bench.util.PriorityHashQueue;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.extensions.impls.sql.SqlGraph;
import com.tinkerpop.blueprints.extensions.util.ClosingIterator;

public class OperationGetShortestPathProperty extends Operation {

	private Vertex source;
	private Vertex target;
	private Direction direction;
	private boolean isRDFGraph;
	private boolean isSQLGraph;
	private boolean isHollowGraph;

	@Override
	protected void onInitialize(Object[] args) {
		source = getGraph().getVertex(args[0]);
		target = getGraph().getVertex(args[1]);
		direction = Direction.BOTH;		// undirected
		isRDFGraph = DatabaseEngine.isRDFGraph(getGraph());
		isSQLGraph = getGraph() instanceof SqlGraph;
		isHollowGraph = DatabaseEngine.isHollowGraph(getGraph());
	}
	
	@Override
	protected void onExecute() throws Exception {
		try {
			ArrayList<Vertex> result = new ArrayList<Vertex>();

			if (isSQLGraph && GlobalConfig.useStoredProcedures) {
				Iterable<Vertex> ui = ((SqlGraph) getGraph()).getShortestPath(source, target);
				for (Vertex u : ui) {
					result.add(u);
				}
				GraphUtils.close(ui);
				setResult(result.size());
			} else {
				int get_nbrs = 0;
				int get_vertex = 0;
				int get_property = 0;
				int set_property = 0;
				int remove_property = 0;
				
				// Only for the vertices that are in the queue
				final HashMap<Vertex,Integer> dist = new HashMap<Vertex,Integer>();
				
				final Comparator<Vertex> minDist = new Comparator<Vertex>()
				{
					@SuppressWarnings({ "unchecked", "rawtypes" })
					public int compare(Vertex left, Vertex right) {
						//Integer leftDist = (Integer) left.getProperty("dist");
						//Integer rightDist = (Integer) right.getProperty("dist");
						Integer  leftDist = dist.get( left);
						Integer rightDist = dist.get(right);
						int l = leftDist  != null ?  leftDist.intValue() : Integer.MAX_VALUE;
						int r = rightDist != null ? rightDist.intValue() : Integer.MAX_VALUE;
						return l > r ? 1 : l < r ? -1 : ((Comparable) left.getId()).compareTo(right.getId());

					}
				};
				
				final PriorityHashQueue<Vertex> queue = new PriorityHashQueue<Vertex>(11, minDist);
				
				set_property++;
				source.setProperty("dist", 0);
				queue.add(source);
				dist.put(source, 0);
				
				while (!queue.isEmpty()) {
					
					Vertex u = queue.remove();
					if (u.equals(target)) break;
					int dist_u = dist.remove(u);
					
					get_nbrs++;
					Iterable<Vertex> vi = u.getVertices(direction);
					for (Vertex v : vi) {
						get_vertex++;
						
						int alt = dist_u + 1;
						
						Integer cur = dist.get(v);
						int i_cur;
						if (cur == null) {
							get_property++;
							cur = (Integer) v.getProperty("dist");
							i_cur = cur == null ? Integer.MAX_VALUE : cur.intValue();
						}
						else {
							i_cur = cur.intValue();
						}
					
						if (alt < i_cur) {
							set_property += 2;
							
							if (isRDFGraph)
								v.setProperty("prev", (String) u.getId());
							else
								v.setProperty("prev", ((Long) u.getId()).longValue());
							
							v.setProperty("dist", alt);
							dist.put(v, alt);
							queue.remove(v);
							queue.add(v);
						}
					}
					GraphUtils.close(vi);
				}
				
				
				Vertex u = target;
				
				get_property++;
				Object prevId = u.getProperty("prev");
				while (prevId != null) {
					result.add(0, u);
					
					get_vertex++;
					u = getGraph().getVertex(prevId);
					
					get_property++;
					prevId = u.getProperty("prev");
					
					if (isHollowGraph) break;
				}
				
				for (Vertex v: new ClosingIterator<Vertex>(getGraph().getVertices())) {
					remove_property += 2;
					v.removeProperty("dist");
					v.removeProperty("prev");
				}
	
				setResult(result.size() + ":" + get_nbrs + ":" + get_vertex + ":" + get_property + ":" + set_property + ":" + remove_property);
			}
		} catch (Exception e) {
			throw e;
		}
	}

	@Override
	public boolean isUpdate() {
		return true;
	}
}
