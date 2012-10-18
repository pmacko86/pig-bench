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

public class OperationGetRandomNeighbor extends Operation {

	protected Vertex startVertex;
	protected Random random;
	protected Direction direction;
	protected String label;
	
	@Override
	protected void onInitialize(Object[] args) {
		startVertex = getGraph().getVertex(args[0]);
		random = new Random();
		direction = args.length > 1 ? (Direction) args[1] : Direction.OUT;
		label = args.length > 2 ? (String) args[2] : null;
	}
	
	@Override
	protected void onExecute() throws Exception {
		@SuppressWarnings("unused")
		Vertex result = null;
		
		final ArrayList<Vertex> vertices = new ArrayList<Vertex>();
		Iterable<Vertex> vi = label == null ? startVertex.getVertices(direction) : startVertex.getVertices(direction, label);
		for (Vertex v : vi) vertices.add(v);
		GraphUtils.close(vi);
		if (vertices.size() > 0)
			result = vertices.get(random.nextInt(vertices.size()));
		
		setResult("" + (vertices.isEmpty() ? 0 : 1) + ":" + 1 /* real_hops */ + ":" + 1 /* get_ops */ + ":" + vertices.size());
	}
	
	
	/**
	 * The operation specialized for DEX
	 */
	public static class DEX extends OperationGetRandomNeighbor {
		
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
			final ArrayList<Long> vertices = new ArrayList<Long>();
			int get_ops = 0, get_vertex = 0;

			long u = ((Long) startVertex.getId()).longValue();
			get_ops++;
			
			for (int t : types) {
				com.sparsity.dex.gdb.Objects objs = graph.neighbors(u, t, d);
				com.sparsity.dex.gdb.ObjectsIterator objsItr = objs.iterator();
						
				while (objsItr.hasNext()) {
					get_vertex++;
					long v = objsItr.nextObject();
					vertices.add(v);
				}
						
				objsItr.close();
				objs.close();
			}
			
			@SuppressWarnings("unused")
			long result = vertices.isEmpty() ? -1 : vertices.get(random.nextInt(vertices.size()));
			setResult("" + (vertices.isEmpty() ? 0 : 1) + ":" + 1 /* real_hops */ + ":" + get_ops + ":" + get_vertex);
		}
	}
	
	
	/**
	 * The operation specialized for Neo4j
	 */
	public static class Neo extends OperationGetRandomNeighbor {
		
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
			final ArrayList<Node> vertices = new ArrayList<Node>();

			Node u = ((Neo4jVertex) startVertex).getRawVertex();
			get_ops++;

			Iterable<Relationship> itr;
			itr = relationshipType == null ? u.getRelationships(d) : u.getRelationships(d, relationshipType);
			for (Relationship r : itr) {
				Node v = r.getOtherNode(u);
				get_vertex++;
				vertices.add(v);
			}

			@SuppressWarnings("unused")
			Node result = vertices.isEmpty() ? null : vertices.get(random.nextInt(vertices.size()));
			setResult("" + (vertices.isEmpty() ? 0 : 1) + ":" + 1 /* real_hops */ + ":" + get_ops + ":" + get_vertex);		
		}
	}
}
