package com.tinkerpop.bench.operation.operations;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import com.sparsity.dex.gdb.EdgesDirection;
import com.sparsity.dex.gdb.Graph;
import com.tinkerpop.bench.analysis.AnalysisContext;
import com.tinkerpop.bench.analysis.AnalysisUtils;
import com.tinkerpop.bench.analysis.OperationModel;
import com.tinkerpop.bench.analysis.Prediction;
import com.tinkerpop.bench.operation.Operation;
import com.tinkerpop.bench.util.GraphUtils;
import com.tinkerpop.bench.util.MathUtils;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.extensions.impls.dex.DexUtils;
import com.tinkerpop.blueprints.extensions.impls.neo4j.Neo4jUtils;
import com.tinkerpop.blueprints.impls.dex.DexGraph;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jVertex;


public class OperationGetFirstNeighbor extends Operation {

	protected Vertex startVertex;
	protected Direction direction;
	protected String label;
	
	@Override
	protected void onInitialize(Object[] args) {
		startVertex = getGraph().getVertex(args[0]);
		direction = args.length > 1 ? (Direction) args[1] : Direction.OUT;
		label = args.length > 2 ? (String) args[2] : null;
	}
	
	@Override
	protected void onExecute() throws Exception {
		try {
			Vertex result = null;
			
			Iterable<Vertex> vi = label == null ? startVertex.getVertices(direction) : startVertex.getVertices(direction, label);
			Iterator<Vertex> iter = vi.iterator();
			boolean b = iter.hasNext();
			if (b) result = iter.next();
			GraphUtils.close(vi);
			
			setResult("" + (result == null ? 0 : 1) + ":" + 1 /* real_hops */ + ":" + 1 /* get_ops */ + ":" + (b ? 1 : 0));
		} catch (Exception e) {
			throw e;
		}
	}
	
	
	/**
	 * The operation model
	 */
	public static class Model extends OperationModel {
		
		/**
		 * Create an instance of class Model
		 * 
		 * @param context the analysis context
		 */
		public Model(AnalysisContext context) {
			super(context, OperationGetFirstNeighbor.class);
		}
		
		
		/**
		 * Create prediction(s) based on the specified operation tags
		 * in the specified context
		 * 
		 * @param tag the operation tag(s)
		 * @return a collection of predictions
		 */
		@Override
		public List<Prediction> predictFromTag(String tag) {
			
			Direction d = AnalysisUtils.translateDirectionTagToDirection(tag);
			ArrayList<Prediction> r = new ArrayList<Prediction>();
			
			Double readVertex = getContext().getAverageOperationRuntimeNoTag(OperationGetManyVertices.class);
			Double readEdge = getContext().getAverageOperationRuntimeNoTag(OperationGetManyEdges.class);
			
			Double naiveModel = MathUtils.sum(readVertex, readEdge);
			if (naiveModel != null) r.add(new Prediction(this, tag, "Naive", naiveModel));
			
			Double allNeighborsModel = MathUtils.product(naiveModel, getContext().getStatistics().getAverageDegree(d));
			if (allNeighborsModel != null) r.add(new Prediction(this, tag, "All Neighbors", allNeighborsModel));
			
			return r;
		}
	}
	
	
	/**
	 * The operation specialized for DEX
	 */
	public static class DEX extends OperationGetFirstNeighbor {
		
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
					@SuppressWarnings("unused")
					long v = objsItr.nextObject();
					objsItr.close();
					objs.close();
					setResult("" + 1 + ":" + 1 /* real_hops */ + ":" + get_ops + ":" + get_vertex);
					return;
				}
						
				objsItr.close();
				objs.close();
			}
			
			setResult("" + 0 + ":" + 1 /* real_hops */ + ":" + get_ops + ":" + get_vertex);
		}
	}
	
	
	/**
	 * The operation specialized for Neo4j
	 */
	public static class Neo extends OperationGetFirstNeighbor {
		
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
				@SuppressWarnings("unused")
				Node v = r.getOtherNode(u);
				get_vertex++;
				setResult("" + 1 + ":" + 1 /* real_hops */ + ":" + get_ops + ":" + get_vertex);
				return;
			}
			
			setResult("" + 0 + ":" + 1 /* real_hops */ + ":" + get_ops + ":" + get_vertex);		
		}
	}
}
