package com.tinkerpop.bench.operation.operations;

import java.util.HashSet;

import com.tinkerpop.bench.GraphUtils;
import com.tinkerpop.bench.operation.Operation;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;

public class OperationGlobalClusteringCoefficient extends Operation {
	
	@Override
	protected void onInitialize(Object[] args) {
	}
	
	@Override
	protected void onExecute() throws Exception {
		try {
			Graph graph = getGraph();
			GraphUtils.OpStat stat = new GraphUtils.OpStat();
			
			int closedTriplets = 0;
			int totalTriplets = 0;
			HashSet<Vertex> neighbors = new HashSet<Vertex>();
			
			stat.num_getAllVertices++;
			for (Vertex v : graph.getVertices()) {
				stat.num_getAllVerticesNext++;
				stat.num_uniqueVertices++;
				neighbors.clear();
				
				// Let all edges be undirected
				
				stat.num_getVertices++;
				for (Vertex w : v.getVertices(Direction.BOTH)) {
					stat.num_getVerticesNext++;
					neighbors.add(w);
				}
				
				int k = neighbors.size();
				totalTriplets += k * (k - 1);
				if (k <= 1) continue;
				
				for (Vertex w : neighbors) {
					stat.num_getVertices++;
					for (Vertex z : w.getVertices(Direction.BOTH)) {
						stat.num_getVerticesNext++;
						if (neighbors.contains(z)) {
							closedTriplets++;
						}
					}
				}
			}
			
			double C = totalTriplets == 0 ? 0 : (closedTriplets / (double) totalTriplets);
			setResult("" + C + ":" + stat);
			
		} catch (Exception e) {
			throw e;
		}
	}
}
