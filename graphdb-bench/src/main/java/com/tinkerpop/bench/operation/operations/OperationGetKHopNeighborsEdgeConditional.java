package com.tinkerpop.bench.operation.operations;

import java.util.ArrayList;

import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import com.sparsity.dex.gdb.EdgesDirection;
import com.sparsity.dex.gdb.Graph;
import com.sparsity.dex.gdb.Value;
import com.tinkerpop.bench.operation.Operation;
import com.tinkerpop.bench.util.GraphUtils;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.extensions.impls.dex.DexUtils;
import com.tinkerpop.blueprints.extensions.impls.neo4j.Neo4jUtils;
import com.tinkerpop.blueprints.impls.dex.DexGraph;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jVertex;

public class OperationGetKHopNeighborsEdgeConditional extends Operation {

	protected Vertex startVertex;
	protected int k;
	protected ArrayList<Object> result;
	protected Direction direction;
	protected String property;
	protected String label;
	
	@Override
	protected void onInitialize(Object[] args) {
		startVertex = getGraph().getVertex(args[0]);
		k = args.length > 1 ? (Integer) args[1] : 2;
		direction = args.length > 2 ? (Direction) args[2] : Direction.OUT;
		property = args.length > 3 ? (String) args[3] : "time";    /* an integer edge property */
		label = args.length > 4 ? (String) args[4] : null;
		result = new ArrayList<Object>(k + 1);
	}
	
	@Override
	protected void onExecute() throws Exception {
		
		int real_hops, get_ops = 0, get_vertex = 0;
		
		Vertex curr = startVertex;
					
		for (real_hops = 0; real_hops < k; real_hops++) {
			get_ops++;
			
			Vertex next = null;
			Integer nextValue = null;
			
			Iterable<Edge> ei = label == null ? curr.getEdges(direction) : curr.getEdges(direction, label);
			for (Edge e : ei) {
				get_vertex++;
				Integer value = (Integer) e.getProperty(property);
				if (nextValue == null || value.compareTo(nextValue) > 0) {
					if (direction == Direction.BOTH) {
						Vertex v1 = e.getVertex(Direction.IN);
						Vertex v2 = e.getVertex(Direction.OUT);
						next = v1.equals(curr) ? v2 : v1;
					}
					else {
						next = e.getVertex(direction == Direction.IN ? Direction.OUT : Direction.IN);
					}
					nextValue = value;
				}
			}
			GraphUtils.close(ei);
			
			if (next != null) {
				curr = next;
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
	public static class DEX extends OperationGetKHopNeighborsEdgeConditional {
		
		private EdgesDirection d;
		private int[] types;
		private int[] attrs;
		private Value temp;
		
		
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
			attrs = DexUtils.findAttributes(((DexGraph) getGraph()).getRawGraph(), types, property);
			temp = new Value();
		}

		
		/**
		 * Execute the operation
		 */
		@Override
		protected void onExecute() throws Exception {
			
			Graph graph = ((DexGraph) getGraph()).getRawGraph();
			int real_hops, get_ops = 0, get_vertex = 0;

			long curr = ((Long) startVertex.getId()).longValue();
			if (!result.isEmpty()) result.clear();
						
			for (real_hops = 0; real_hops < k; real_hops++) {

				long next = com.sparsity.dex.gdb.Objects.InvalidOID;
				int nextValue = 0;
				
				get_ops++;
				for (int t = 0; t < types.length; t++) {
					com.sparsity.dex.gdb.Objects objs = graph.explode(curr, types[t], d);
					com.sparsity.dex.gdb.ObjectsIterator objsItr = objs.iterator();
							
					while (objsItr.hasNext()) {
						get_vertex++;
						long e = objsItr.nextObject();
						graph.getAttribute(e, attrs[t], temp);
						int value = temp.getInteger();
						if (next == com.sparsity.dex.gdb.Objects.InvalidOID || value > nextValue) {
							next = graph.getEdgePeer(e, curr);
							nextValue = value;
						}
					}
							
					objsItr.close();
					objs.close();
				}
				
				if (next == com.sparsity.dex.gdb.Objects.InvalidOID) break;
				
				curr = next;
				result.add(curr);
			}
			
			setResult(result.size() + ":" + real_hops + ":" + get_ops + ":" + get_vertex);
		}
	}
	
	
	/**
	 * The operation specialized for Neo4j
	 */
	public static class Neo extends OperationGetKHopNeighborsEdgeConditional {
		
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
			if (!result.isEmpty()) result.clear();

			for (real_hops = 0; real_hops < k; real_hops++) {

				get_ops++;
				
				Node next = null;
				Integer nextValue = null;

				Iterable<Relationship> itr;
				itr = relationshipType == null ? curr.getRelationships(d) : curr.getRelationships(d, relationshipType);
				for (Relationship r : itr) {
					get_vertex++;
					Integer value = (Integer) r.getProperty(property);
					if (nextValue == null || value.compareTo(nextValue) > 0) {
						next = r.getOtherNode(curr);
						nextValue = value;
					}
				}

				if (next == null) break;
				
				curr = next;
				result.add(curr.getId());
			}

			setResult(result.size() + ":" + real_hops + ":" + get_ops + ":" + get_vertex);		
		}
	}
}
