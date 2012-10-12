package com.tinkerpop.bench.operation.operations;

import java.util.ArrayList;
import java.util.HashSet;

import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import com.sparsity.dex.gdb.EdgesDirection;
import com.sparsity.dex.gdb.Graph;
import com.sparsity.dex.gdb.OIDList;
import com.sparsity.dex.gdb.OIDListIterator;
import com.tinkerpop.bench.operation.Operation;
//import com.tinkerpop.bench.util.BloomFilter;
import com.tinkerpop.bench.util.GraphUtils;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.extensions.impls.dex.DexUtils;
import com.tinkerpop.blueprints.extensions.impls.neo4j.Neo4jUtils;
import com.tinkerpop.blueprints.impls.dex.DexGraph;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jVertex;

public class OperationGetKHopNeighbors extends Operation {

	protected Vertex startVertex;
	protected int k;
	protected Direction direction;
	protected HashSet<Object> result;
	protected String label;
	//private BloomFilter result;
	
	@Override
	protected void onInitialize(Object[] args) {
		startVertex = getGraph().getVertex(args[0]);
		k = args.length > 1 ? (Integer) args[1] : 2;
		direction = args.length > 2 ? (Direction) args[2] : Direction.OUT;
		label = args.length > 3 ? (String) args[3] : null;
	}
	
	@Override
	protected void onDelayedInitialize() {
		result = new HashSet<Object>();
	}
	
	@Override
	protected void onExecute() throws Exception {
		
		int real_hops, get_ops = 0, get_vertex = 0;

		ArrayList<Vertex> curr = new ArrayList<Vertex>();
		ArrayList<Vertex> next = new ArrayList<Vertex>();

		curr.add(startVertex);

		for(real_hops = 0; real_hops < k; real_hops++) {

			for (Vertex u : curr) {

				get_ops++;

				Iterable<Vertex> vi = label == null ? u.getVertices(direction) : u.getVertices(direction, label);
				for (Vertex v : vi) {

					get_vertex++;

					if (result.add(v.getId())) {
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
	}

	@Override
	protected void onFinalize() {
		result = null;
	}
	
	
	/**
	 * The operation specialized for DEX
	 */
	public static class DEX extends OperationGetKHopNeighbors {
		
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

			OIDList curr = new OIDList();
			OIDList next = new OIDList();
			
			curr.add(((Long) startVertex.getId()).longValue());
			
			for (real_hops = 0; real_hops < k; real_hops++) {

				int nextSize = 0;
				
				OIDListIterator currItr = curr.iterator();
				while (currItr.hasNext()) {
					long u = currItr.nextOID();
					
					get_ops++;
					
					for (int t : types) {
						com.sparsity.dex.gdb.Objects objs = graph.neighbors(u, t, d);
						com.sparsity.dex.gdb.ObjectsIterator objsItr = objs.iterator();
						
						while (objsItr.hasNext()) {
							get_vertex++;
							long v = objsItr.nextObject();
							if (result.add(v)) {
								next.add(v);
								nextSize++;
							}
						}
						
						objsItr.close();
						objs.close();
					}
				}
				
				if (nextSize == 0) break;
				
				OIDList tmp = curr;
				curr = next;
				tmp.clear();
				next = tmp;
			}
			
			setResult(result.size() + ":" + real_hops + ":" + get_ops + ":" + get_vertex);
		}
	}
	
	
	/**
	 * The operation specialized for Neo4j
	 */
	public static class Neo extends OperationGetKHopNeighbors {
		
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

			ArrayList<Node> curr = new ArrayList<Node>();
			ArrayList<Node> next = new ArrayList<Node>();

			curr.add(((Neo4jVertex) startVertex).getRawVertex());

			for(real_hops = 0; real_hops < k; real_hops++) {

				for (Node u : curr) {

					get_ops++;

					Iterable<Relationship> itr;
					itr = relationshipType == null ? u.getRelationships(d) : u.getRelationships(d, relationshipType);
					for (Relationship r : itr) {
						Node v = r.getOtherNode(u);

						get_vertex++;

						if (result.add(v.getId())) {
							next.add(v);
						}
					}
				}

				if(next.size() == 0)
					break;

				ArrayList<Node> tmp = curr;
				curr = next;
				tmp.clear();
				next = tmp;
			}

			setResult(result.size() + ":" + real_hops + ":" + get_ops + ":" + get_vertex);		
		}
	}
}
