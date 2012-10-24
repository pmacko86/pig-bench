package com.tinkerpop.bench.operation.operations;

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

public class OperationGetNeighborEdgeConditional extends Operation {

	protected Vertex startVertex;
	protected Direction direction;
	protected String property;
	protected String label;
	
	@Override
	protected void onInitialize(Object[] args) {
		startVertex = getGraph().getVertex(args[0]);
		direction = args.length > 1 ? (Direction) args[1] : Direction.OUT;
		property = args.length > 2 ? (String) args[2] : "time";    /* an integer edge property */
		label = args.length > 3 ? (String) args[3] : null;
	}
	
	@Override
	protected void onExecute() throws Exception {
		@SuppressWarnings("unused")
		Vertex result = null;
		
		Vertex next = null;
		Integer nextValue = null;
		int count = 0;
		
		Iterable<Edge> ei = label == null ? startVertex.getEdges(direction) : startVertex.getEdges(direction, label);
		for (Edge e : ei) {
			Integer value = (Integer) e.getProperty(property);
			if (nextValue == null || value.compareTo(nextValue) > 0) {
				if (direction == Direction.BOTH) {
					Vertex v1 = e.getVertex(Direction.IN);
					Vertex v2 = e.getVertex(Direction.OUT);
					next = v1.equals(startVertex) ? v2 : v1;
				}
				else {
					next = e.getVertex(direction == Direction.IN ? Direction.OUT : Direction.IN);
				}
				nextValue = value;
			}
			count++;
		}
		GraphUtils.close(ei);
		
		setResult("" + (next == null ? 0 : 1) + ":" + 1 /* real_hops */ + ":" + 1 /* get_ops */ + ":" + count);
	}
	
	
	/**
	 * The operation specialized for DEX
	 */
	public static class DEX extends OperationGetNeighborEdgeConditional {
		
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
			int get_ops = 0, get_vertex = 0;

			long u = ((Long) startVertex.getId()).longValue();
			get_ops++;
			
			long next = com.sparsity.dex.gdb.Objects.InvalidOID;
			int nextValue = 0;
			
			for (int t = 0; t < types.length; t++) {
				com.sparsity.dex.gdb.Objects objs = graph.explode(u, types[t], d);
				com.sparsity.dex.gdb.ObjectsIterator objsItr = objs.iterator();
						
				while (objsItr.hasNext()) {
					get_vertex++;
					long e = objsItr.nextObject();
					graph.getAttribute(e, attrs[t], temp);
					int value = temp.getInteger();
					if (next == com.sparsity.dex.gdb.Objects.InvalidOID || value > nextValue) {
						next = graph.getEdgePeer(e, u);
						nextValue = value;
					}
				}
						
				objsItr.close();
				objs.close();
			}
			
			setResult("" + (next == com.sparsity.dex.gdb.Objects.InvalidOID ? 0 : 1)
						 + ":" + 1 /* real_hops */ + ":" + get_ops + ":" + get_vertex);
		}
	}
	
	
	/**
	 * The operation specialized for Neo4j
	 */
	public static class Neo extends OperationGetNeighborEdgeConditional {
		
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
			
			int get_ops = 0, get_vertex = 0;

			Node u = ((Neo4jVertex) startVertex).getRawVertex();
			get_ops++;
			
			Node next = null;
			Integer nextValue = null;

			Iterable<Relationship> itr;
			itr = relationshipType == null ? u.getRelationships(d) : u.getRelationships(d, relationshipType);
			for (Relationship r : itr) {
				get_vertex++;
				Integer value = (Integer) r.getProperty(property);
				if (nextValue == null || value.compareTo(nextValue) > 0) {
					next = r.getOtherNode(u);
					nextValue = value;
				}
			}

			setResult("" + (next == null ? 0 : 1) + ":" + 1 /* real_hops */ + ":" + get_ops + ":" + get_vertex);		
		}
	}
}
