package com.tinkerpop.bench.operation.operations;

import java.util.ArrayList;

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

public class OperationGetAllNeighbors extends Operation {

	protected Vertex startVertex;
	protected ArrayList<Object> neighbors;
	protected Direction direction;
	protected String label;
	
	
	@Override
	protected void onInitialize(Object[] args) {
		startVertex = getGraph().getVertex(args[0]);
		neighbors = new ArrayList<Object>(100);
		direction = args.length > 1 ? (Direction) args[1] : Direction.OUT;
		label = args.length > 2 ? (String) args[2] : null;
	}
	
	@Override
	protected void onExecute() throws Exception {
		if (!neighbors.isEmpty()) neighbors.clear();
		
		Iterable<Vertex> vi = label == null ? startVertex.getVertices(direction) : startVertex.getVertices(direction, label);
		for (Vertex v : vi) neighbors.add(v.getId());
		GraphUtils.close(vi);
		
		setResult(neighbors.size() + ":" + 1 /* real_hops */ + ":" + 1 /* get_ops */ + ":" + neighbors.size());
	}

	@Override
	protected void onFinalize() {
		neighbors = null;
	}
	
	
	/**
	 * The operation specialized for DEX
	 */
	public static class DEX extends OperationGetAllNeighbors {
		
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
			int get_ops = 0, get_vertex = 0;

			long u = ((Long) startVertex.getId()).longValue();
			get_ops++;
			
			for (int t : types) {
				com.sparsity.dex.gdb.Objects objs = graph.neighbors(u, t, d);
				com.sparsity.dex.gdb.ObjectsIterator objsItr = objs.iterator();
						
				while (objsItr.hasNext()) {
					get_vertex++;
					long v = objsItr.nextObject();
					neighbors.add(v);
				}
						
				objsItr.close();
				objs.close();
			}
			
			setResult(neighbors.size() + ":" + 1 /* real_hops */ + ":" + get_ops + ":" + get_vertex);
		}
	}
	
	
	/**
	 * The operation specialized for Neo4j
	 */
	public static class Neo extends OperationGetAllNeighbors {
		
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

			Iterable<Relationship> itr;
			itr = relationshipType == null ? u.getRelationships(d) : u.getRelationships(d, relationshipType);
			for (Relationship r : itr) {
				Node v = r.getOtherNode(u);
				get_vertex++;
				neighbors.add(v.getId());
			}

			setResult(neighbors.size() + ":" + 1 /* real_hops */ + ":" + get_ops + ":" + get_vertex);		
		}
	}
}
