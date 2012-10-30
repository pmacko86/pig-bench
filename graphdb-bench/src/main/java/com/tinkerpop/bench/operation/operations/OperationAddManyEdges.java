package com.tinkerpop.bench.operation.operations;

import java.util.Iterator;

import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;

import com.sparsity.dex.gdb.Type;
import com.tinkerpop.bench.operation.Operation;
import com.tinkerpop.bench.util.GraphUtils;
import com.tinkerpop.bench.util.StatisticsHelper;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.extensions.impls.dex.DexUtils;
import com.tinkerpop.blueprints.extensions.impls.neo4j.Neo4jUtils;
import com.tinkerpop.blueprints.impls.dex.DexGraph;

import edu.harvard.pass.cpl.CPL;
import edu.harvard.pass.cpl.CPLObject;

public class OperationAddManyEdges extends Operation {

	protected int opCount;
	protected Vertex[] vertexSamples;
	
	protected String label;
	
	@Override
	protected void onInitialize(Object[] args) {
		opCount = args.length > 0 ? (Integer) args[0] : 1000;
		vertexSamples = StatisticsHelper.getRandomVertices(getGraph(), opCount * 2);
		
		label = args.length > 1 ? (String) args[1] : null;
		if (label == null) {
			Iterable<Edge> edges = getGraph().getEdges();
			Iterator<Edge> i = edges.iterator();
			label = i.hasNext() ? i.next().getLabel() : "";
			GraphUtils.close(edges);
		}
	}
	
	@Override
	protected void onExecute() throws Exception {
		
		Graph graph = getGraph();
		int count = 0;
			
		for (int i = 0; i < 2 * opCount; i += 2) {
			try {
				count++;
				graph.addEdge(null, vertexSamples[i], vertexSamples[i+1], label);
			} catch (Exception e) {
				count--;
			}
		}
			
		setResult(count);
	}

	@Override
	protected void onFinalize() throws Exception {
		if (CPL.isAttached()) {
			CPLObject obj = getCPLObject();
			getGraphDescriptor().getCPLObject().dataFlowFrom(obj);
		}
	}

	@Override
	public boolean isUpdate() {
		return true;
	}
	
	
	/**
	 * The operation specialized for DEX
	 */
	public static class DEX extends OperationAddManyEdges {
		
		private int type;
		private long[] vertices;
		
		
		/**
		 * Initialize the operation
		 * 
		 * @param args the operation arguments
		 */
		@Override
		protected void onInitialize(Object[] args) {
			super.onInitialize(args);
			
			com.sparsity.dex.gdb.Graph graph = ((DexGraph) getGraph()).getRawGraph();

			type = graph.findType(label);
			if (type == Type.InvalidType) {
				type = DexUtils.getEdgeTypes(graph)[0];
			}
			
			vertices = DexUtils.translateVertices(vertexSamples);
		}

		
		/**
		 * Execute the operation
		 */
		@Override
		protected void onExecute() throws Exception {
			
			com.sparsity.dex.gdb.Graph graph = ((DexGraph) getGraph()).getRawGraph();

			for (int i = 0; i < 2 * opCount; i += 2) {
				graph.newEdge(type, vertices[i], vertices[i+1]);
			}
			
			setResult(opCount);
		}
	}
	
	
	/**
	 * The operation specialized for Neo4j
	 */
	public static class Neo extends OperationAddManyEdges {
		
		private Node[] vertices;
		private DynamicRelationshipType relationshipType;
		
		
		/**
		 * Initialize the operation
		 * 
		 * @param args the operation arguments
		 */
		@Override
		protected void onInitialize(Object[] args) {
			super.onInitialize(args);
			
			relationshipType = DynamicRelationshipType.withName(label);
			vertices = Neo4jUtils.translateVertices(vertexSamples);
		}

		
		/**
		 * Execute the operation
		 */
		@Override
		protected void onExecute() throws Exception {

			for (int i = 0; i < 2 * opCount; i += 2) {
				vertices[i].createRelationshipTo(vertices[i + 1], relationshipType);
			}
			
			setResult(opCount);	
		}
	}
}
