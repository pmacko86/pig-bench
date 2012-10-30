package com.tinkerpop.bench.operation.operations;

import com.tinkerpop.bench.operation.Operation;
import com.tinkerpop.blueprints.impls.dex.DexGraph;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;


@Deprecated
public class OperationGetEdge extends Operation {

	protected Object id;
	
	@Override
	protected void onInitialize(Object[] args) {
		id = args[0];
	}
	
	@Override
	protected void onExecute() throws Exception {
		try {
			setResult(getGraph().getEdge(id).toString());
		} catch (Exception e) {
			throw e;
		}
	}
	
	
	/**
	 * The operation specialized for DEX
	 */
	public static class DEX extends OperationGetEdge {
		
		private Long longId;
		
		
		/**
		 * Initialize the operation
		 * 
		 * @param args the operation arguments
		 */
		@Override
		protected void onInitialize(Object[] args) {
			super.onInitialize(args);
			longId = id instanceof Long ? (Long) id : Long.valueOf(id.toString());
		}

		
		/**
		 * Execute the operation
		 */
		@Override
		protected void onExecute() throws Exception {
			setResult(((DexGraph) getGraph()).getRawGraph().getObjectType(longId));
		}
	}
	
	
	/**
	 * The operation specialized for Neo4j
	 */
	public static class Neo extends OperationGetEdge {
		
		private Long longId;
		
		
		/**
		 * Initialize the operation
		 * 
		 * @param args the operation arguments
		 */
		@Override
		protected void onInitialize(Object[] args) {
			super.onInitialize(args);
			longId = id instanceof Long ? (Long) id : Long.valueOf(id.toString());
		}

		
		/**
		 * Execute the operation
		 */
		@Override
		protected void onExecute() throws Exception {
			setResult(((Neo4jGraph) getGraph()).getRawGraph().getRelationshipById(longId));
		}
	}
}
