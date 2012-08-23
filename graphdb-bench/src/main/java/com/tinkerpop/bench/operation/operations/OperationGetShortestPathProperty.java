package com.tinkerpop.bench.operation.operations;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;

import com.tinkerpop.bench.DatabaseEngine;
import com.tinkerpop.bench.GlobalConfig;
import com.tinkerpop.bench.operation.Operation;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.extensions.impls.sql.SqlGraph;

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
				for (Vertex u : ((SqlGraph) getGraph()).getShortestPath(source, target)) {
					result.add(u);
				}
				setResult(result.size());
			} else {
				int get_nbrs = 0;
				int get_vertex = 0;
				int get_property = 0;
				int set_property = 0;
				int remove_property = 0;
				
				final Comparator<Vertex> minDist = new Comparator<Vertex>()
				{
					public int compare(Vertex left, Vertex right) {
						Integer leftDist = (Integer) left.getProperty("dist");
						Integer rightDist = (Integer) right.getProperty("dist");
						if (leftDist == null) leftDist = Integer.MAX_VALUE;
						if (rightDist == null) rightDist = Integer.MAX_VALUE;
						return leftDist.compareTo(rightDist);
					}
				};
				
				//dmargo: 11 is the Java default initial capacity...don't ask me why.
				final PriorityQueue<Vertex> queue = new PriorityQueue<Vertex>(11, minDist);
				
				set_property++;
				source.setProperty("dist", 0);
				queue.add(source);
				
				while (!queue.isEmpty()) {
					Vertex u = queue.remove();
					
					if (u.equals(target))
						break;
					
					get_nbrs++;
					for (Vertex v : u.getVertices(direction)) {
						get_vertex++;
						
						get_property += 2;
						Integer alt = (Integer) u.getProperty("dist") + 1;
						Integer cur = (Integer) v.getProperty("dist");
						if (cur == null) cur = Integer.MAX_VALUE;
					
						if (alt < cur) {
							set_property += 2;
							
							if (isRDFGraph)
								v.setProperty("prev", (String) u.getId());
							else
								v.setProperty("prev", ((Long) u.getId()).longValue());
							
							v.setProperty("dist", alt);
							queue.remove(v);
							queue.add(v);
						}
					}
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
				
				for (Vertex v: getGraph().getVertices()) {
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
