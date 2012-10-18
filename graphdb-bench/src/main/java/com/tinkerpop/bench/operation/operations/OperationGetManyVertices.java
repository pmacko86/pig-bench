package com.tinkerpop.bench.operation.operations;

import com.tinkerpop.bench.operation.Operation;
import com.tinkerpop.bench.util.StatisticsHelper;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.impls.dex.DexGraph;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;

public class OperationGetManyVertices extends Operation {

	protected int opCount;
	protected Object[] vertexSamples;
	
	@Override
	protected void onInitialize(Object[] args) {
		opCount = args.length > 0 ? (Integer) args[0] : 1000;
		vertexSamples = StatisticsHelper.getRandomVertexIds(getGraph(), opCount);
	}
	
	@Override
	protected void onExecute() throws Exception {
		try {
			Graph graph = getGraph();
			
			for (int i = 0; i < opCount; i++)
				graph.getVertex(vertexSamples[i]);
			
			setResult(opCount);
		} catch (Exception e) {
			throw e;
		}
	}
	
	
	/**
	 * The operation specialized for DEX
	 */
	public static class DEX extends OperationGetManyVertices {
		
		private Long[] longIds;
		
		
		/**
		 * Initialize the operation
		 * 
		 * @param args the operation arguments
		 */
		@Override
		protected void onInitialize(Object[] args) {
			super.onInitialize(args);
			longIds = new Long[vertexSamples.length];
			for (int i = 0; i < vertexSamples.length; i++) {
				longIds[i] = vertexSamples[i] instanceof Long ? (Long) vertexSamples[i] : Long.valueOf(vertexSamples[i].toString());
			}
		}

		
		/**
		 * Execute the operation
		 */
		@Override
		protected void onExecute() throws Exception {
			for (Long l : longIds)
				((DexGraph) getGraph()).getRawGraph().getObjectType(l);
			setResult(opCount);
		}
	}
	
	
	/**
	 * The operation specialized for Neo4j
	 */
	public static class Neo extends OperationGetManyVertices {
		
		private Long[] longIds;
		
		
		/**
		 * Initialize the operation
		 * 
		 * @param args the operation arguments
		 */
		@Override
		protected void onInitialize(Object[] args) {
			super.onInitialize(args);
			longIds = new Long[vertexSamples.length];
			for (int i = 0; i < vertexSamples.length; i++) {
				longIds[i] = vertexSamples[i] instanceof Long ? (Long) vertexSamples[i] : Long.valueOf(vertexSamples[i].toString());
			}
		}

		
		/**
		 * Execute the operation
		 */
		@Override
		protected void onExecute() throws Exception {
			for (Long l : longIds)
				((Neo4jGraph) getGraph()).getRawGraph().getNodeById(l);
			setResult(opCount);
		}
	}
}
