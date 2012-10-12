package com.tinkerpop.bench.operation.operations;

import java.util.ArrayList;
import java.util.Random;

import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import com.sparsity.dex.gdb.EdgesDirection;
import com.sparsity.dex.gdb.Graph;
import com.tinkerpop.bench.operation.Operation;
import com.tinkerpop.bench.util.GraphUtils;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.extensions.impls.dex.DexUtils;
import com.tinkerpop.blueprints.extensions.impls.neo4j.Neo4jUtils;
import com.tinkerpop.blueprints.impls.dex.DexGraph;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jVertex;

public class OperationGetKRandomNeighbors extends Operation {

	protected Vertex startVertex;
	protected int k;
	protected ArrayList<Object> result;
	protected Random random;
	protected Direction direction;
	protected String label;
	
	@Override
	protected void onInitialize(Object[] args) {
		startVertex = getGraph().getVertex(args[0]);
		k = args.length > 1 ? (Integer) args[1] : 2;
		direction = args.length > 2 ? (Direction) args[2] : Direction.OUT;
		result = new ArrayList<Object>(k + 1);
		random = new Random();
		label = args.length > 3 ? (String) args[3] : null;
	}
	
	@Override
	protected void onExecute() throws Exception {
		
		int real_hops, get_ops = 0, get_vertex = 0;
		
		Vertex curr = startVertex;
		final ArrayList<Vertex> next = new ArrayList<Vertex>();
		if (!result.isEmpty()) result.clear();
					
		for (real_hops = 0; real_hops < k; real_hops++) {
			get_ops++;
			Iterable<Vertex> vi = label == null ? curr.getVertices(direction) : curr.getVertices(direction, label);
			for (Vertex v : vi) {
				get_vertex++;
				next.add(v);
			}
			GraphUtils.close(vi);
			if (next.size() > 0) {
				curr = next.get(random.nextInt(next.size()));
				next.clear();
				result.add(curr.getId());
			}
			else {
				break;
			}
		}
	
		setResult(result.size() + ":" + real_hops + ":" + get_ops + ":" + get_vertex);
	}

	@Override
	protected void onFinalize() {
		result = null;
	}
	
	
	/**
	 * The operation specialized for DEX
	 */
	public static class DEX extends OperationGetKRandomNeighbors {
		
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
			
			d = DexUtils.translateDirection(direction);
			types = DexUtils.getEdgeTypes(((DexGraph) getGraph()).getRawGraph(), label);
		}

		
		/**
		 * Execute the operation
		 */
		@Override
		protected void onExecute() throws Exception {
			
			Graph graph = ((DexGraph) getGraph()).getRawGraph();
			int real_hops, get_ops = 0, get_vertex = 0;

			long curr = ((Long) startVertex.getId()).longValue();
			long[] next = new long[100];
			if (!result.isEmpty()) result.clear();
						
			for (real_hops = 0; real_hops < k; real_hops++) {

				int nextSize = 0;
				
				get_ops++;
				for (int t : types) {
					com.sparsity.dex.gdb.Objects objs = graph.neighbors(curr, t, d);
					com.sparsity.dex.gdb.ObjectsIterator objsItr = objs.iterator();
					
					while (objsItr.hasNext()) {
						get_vertex++;
						long v = objsItr.nextObject();
						if (nextSize >= next.length) {
							long[] n = new long[nextSize * 2];
							System.arraycopy(next, 0, n, 0, nextSize);
							next = n;
						}
						next[nextSize++] = v;
					}
						
					objsItr.close();
					objs.close();
				}
				
				if (nextSize == 0) break;
				
				curr = next[random.nextInt(nextSize)];
				result.add(curr);
			}
			
			setResult(result.size() + ":" + real_hops + ":" + get_ops + ":" + get_vertex);
		}
	}
	
	
	/**
	 * The operation specialized for Neo4j
	 */
	public static class Neo extends OperationGetKRandomNeighbors {
		
		private org.neo4j.graphdb.Direction d;
		private DynamicRelationshipType relationshipType; 
		
		
		/**
		 * Initialize the operation
		 * 
		 * @param args the operation arguments
		 */
		@Override
		protected void onInitialize(Object[] args) {
			super.onInitialize(args);
			
			// Translate the direction and the edge label
			
			d = Neo4jUtils.translateDirection(direction);
			relationshipType = label == null ? null : DynamicRelationshipType.withName(label);
		}

		
		/**
		 * Execute the operation
		 */
		@Override
		protected void onExecute() throws Exception {
			
			int real_hops, get_ops = 0, get_vertex = 0;

			Node curr = ((Neo4jVertex) startVertex).getRawVertex();
			ArrayList<Node> next = new ArrayList<Node>();
			if (!result.isEmpty()) result.clear();

			for (real_hops = 0; real_hops < k; real_hops++) {

				get_ops++;

				Iterable<Relationship> itr;
				itr = relationshipType == null ? curr.getRelationships(d) : curr.getRelationships(d, relationshipType);
				for (Relationship r : itr) {
					Node v = r.getOtherNode(curr);
					get_vertex++;
					next.add(v);
				}

				if (next.size() == 0) break;

				curr = next.get(random.nextInt(next.size()));
				result.add(curr.getId());
				next.clear();
			}

			setResult(result.size() + ":" + real_hops + ":" + get_ops + ":" + get_vertex);		
		}
	}
}
