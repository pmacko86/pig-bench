package com.tinkerpop.bench.operation.operations;

import java.util.ArrayList;
import java.util.HashSet;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import com.sparsity.dex.gdb.EdgesDirection;
import com.sparsity.dex.gdb.Graph;
import com.sparsity.dex.gdb.OIDList;
import com.sparsity.dex.gdb.OIDListIterator;
import com.tinkerpop.bench.operation.Operation;
import com.tinkerpop.bench.util.GraphUtils;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.extensions.impls.dex.DexUtils;
import com.tinkerpop.blueprints.extensions.impls.neo4j.Neo4jUtils;
import com.tinkerpop.blueprints.impls.dex.DexGraph;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jVertex;

@SuppressWarnings("unused")
public class OperationLocalClusteringCoefficient extends Operation {

	protected Vertex vertex;
	protected GraphUtils.OpStat stat;
	
	@Override
	protected void onInitialize(Object[] args) {
		vertex = getGraph().getVertex(args[0]);
		stat = new GraphUtils.OpStat();
	}
	
	@Override
	protected void onExecute() throws Exception {
		double r = GraphUtils.localClusteringCoefficient(vertex, stat);
		setResult("" + stat.num_uniqueVertices + ":2:" + stat.num_getVertices + ":" + stat.num_getVerticesNext + ":" + r);
	}
	
	
	/**
	 * The operation specialized for DEX
	 */
	public static class DEX extends OperationLocalClusteringCoefficient {
		
		private EdgesDirection d;
		private int[] types; 
		
		
		/**
		 * Initialize the operation
		 * 
		 * @param args the operation arguments
		 */
		@Override
		protected void onInitialize(Object[] args) {
			super.onInitialize(args);
					
			// Translate the direction and the edge label
			
			d = DexUtils.translateDirection(Direction.BOTH);
			types = DexUtils.getEdgeTypes(((DexGraph) getGraph()).getRawGraph());
		}

		
		/**
		 * Execute the operation
		 */
		@Override
		protected void onExecute() throws Exception {

			Graph graph = ((DexGraph) getGraph()).getRawGraph();
			int get_ops = 0, get_vertex = 0, unique = 0;

			/*long start = ((Long) vertex.getId()).longValue();
			OIDList neighbors = new OIDList();
			HashSet<Long> neighborSet = new HashSet<Long>();
			get_ops++;
			unique++;
			
			for (int t : types) {
				com.sparsity.dex.gdb.Objects objs = graph.neighbors(start, t, d);
				com.sparsity.dex.gdb.ObjectsIterator objsItr = objs.iterator();
						
				while (objsItr.hasNext()) {
					get_vertex++;
					long v = objsItr.nextObject();
					neighbors.add(v);
					neighborSet.add(v);
					unique++;
				}
						
				objsItr.close();
				objs.close();
			}
						
			OIDListIterator currItr = neighbors.iterator();
			int triangles = 0;
			while (currItr.hasNext()) {
				long u = currItr.nextOID();
				
				get_ops++;
				
				for (int t : types) {
					com.sparsity.dex.gdb.Objects objs = graph.neighbors(u, t, d);
					com.sparsity.dex.gdb.ObjectsIterator objsItr = objs.iterator();
					
					while (objsItr.hasNext()) {
						get_vertex++;
						long v = objsItr.nextObject();
						if (v == start) continue;
						
						if (neighborSet.contains(v)) {
							triangles++;
						}
						else {
							unique++;
						}
					}
						
					objsItr.close();
					objs.close();
				}
			}
			
			neighbors.delete();*/
			
			
			long start = ((Long) vertex.getId()).longValue();
			com.sparsity.dex.gdb.Objects[] neighbors = new com.sparsity.dex.gdb.Objects[types.length];
			HashSet<Long> neighborSet = new HashSet<Long>();
			get_ops++;
			unique++;
			
			for (int i = 0; i < types.length; i++) {
				neighbors[i] = graph.neighbors(start, types[i], d);
				com.sparsity.dex.gdb.ObjectsIterator objsItr = neighbors[i].iterator();
				
				int size = 0;
				while (objsItr.hasNext()) {
					get_vertex++;
					size++;
					long v = objsItr.nextObject();
					neighborSet.add(v);
					unique++;
				}
				
				objsItr.close();
				if (size == 0) {
					neighbors[i].close();
					neighbors[i] = null;
				}
			}
			
			int triangles = 0;
			for (int j = 0; j < types.length; j++) {
				for (int i = 0; i < types.length; i++) {
					if (neighbors[i] == null) continue;
					
					com.sparsity.dex.gdb.Objects objs = graph.neighbors(neighbors[i], types[j], d);
					com.sparsity.dex.gdb.ObjectsIterator objsItr = objs.iterator();
						
					while (objsItr.hasNext()) {
						get_vertex++;
						long v = objsItr.nextObject();
						if (v == start) continue;
							
						if (neighborSet.contains(v)) {
							triangles++;
						}
						else {
							unique++;
						}
					}
							
					objsItr.close();
					objs.close();
				}			
			}
			
			for (int i = 0; i < types.length; i++) {
				if (neighbors[i] == null) continue;
				get_ops += neighbors[i].count();
				neighbors[i].close();
			}
			
			
			int K = neighborSet.size();
			double r = K <= 1 ? 0 : (triangles / (double) (K * (K-1)));
			
			setResult("" + unique + ":2:" + get_ops + ":" + get_vertex + ":" + r);
		}
	}
	
	
	/**
	 * The operation specialized for Neo4j
	 */
	public static class Neo extends OperationLocalClusteringCoefficient {
		
		private org.neo4j.graphdb.Direction d;
		
		
		/**
		 * Initialize the operation
		 * 
		 * @param args the operation arguments
		 */
		@Override
		protected void onInitialize(Object[] args) {
			super.onInitialize(args);
			d = Neo4jUtils.translateDirection(Direction.BOTH);
		}

		
		/**
		 * Execute the operation
		 */
		@Override
		protected void onExecute() throws Exception {
		
			int get_ops = 0, get_vertex = 0, unique = 0;
		
			ArrayList<Node> neighbors = new ArrayList<Node>();
			HashSet<Node> neighborSet = new HashSet<Node>();

			Node start = ((Neo4jVertex) vertex).getRawVertex();
			get_ops++;
			unique++;

			Iterable<Relationship> itr;
			itr = start.getRelationships(d);
			for (Relationship r : itr) {
				Node v = r.getOtherNode(start);
				get_vertex++;
				unique++;
				neighbors.add(v);
				neighborSet.add(v);
			}

			int triangles = 0;
			for (Node u : neighbors) {

				get_ops++;

				itr = u.getRelationships(d);
				for (Relationship r : itr) {
					Node v = r.getOtherNode(u);
					get_vertex++;
					if (v.equals(start)) continue;

					if (neighborSet.contains(v)) {
						triangles++;
					}
					else {
						unique++;
					}
				}
			}
			
			int K = neighborSet.size();
			double r = K <= 1 ? 0 : (triangles / (double) (K * (K-1)));
			
			setResult("" + unique + ":2:" + get_ops + ":" + get_vertex + ":" + r);
		}
	}
}
