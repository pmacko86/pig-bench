package com.tinkerpop.bench.operation.operations;

import com.sparsity.dex.gdb.Graph;
import com.tinkerpop.bench.operation.Operation;
import com.tinkerpop.bench.util.StatisticsHelper;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.impls.dex.DexGraph;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jEdge;
import com.tinkerpop.blueprints.util.StringFactory;

public class OperationGetManyEdgeProperties extends Operation {

	protected String property_key;
	
	protected int opCount;
	protected Edge[] edgeSamples;
	
	@Override
	protected void onInitialize(Object[] args) {
		property_key = (String) args[0];
		
		opCount = args.length > 1 ? (Integer) args[1] : 1000;
		edgeSamples = StatisticsHelper.getRandomEdges(getGraph(), opCount);
	}
	
	@Override
	protected void onExecute() throws Exception {
		for (int i = 0; i < opCount; i++)
			edgeSamples[i].getProperty(property_key);
			
		setResult(opCount);
	}
	
	
	/**
	 * The operation specialized for DEX
	 */
	public static class DEX extends OperationGetManyEdgeProperties {
		
		/**
		 * Execute the operation
		 */
		@Override
		protected void onExecute() throws Exception {
			
			Graph graph = ((DexGraph) getGraph()).getRawGraph();
			
			for (Edge edge : edgeSamples) {
				
				long oid = ((Long) edge.getId()).longValue();				
				int type = ((DexGraph) getGraph()).getRawGraph().getObjectType(oid);	// Here or inside onInitialize?
				
		        if (property_key.compareTo(StringFactory.LABEL) == 0) {		// Is this necessary? Does Neo4j also do this?
		        	graph.getType(type).getName();
		        	continue;
		        }
		        
		        int attr = graph.findAttribute(type, property_key);
		        if (attr == com.sparsity.dex.gdb.Attribute.InvalidAttribute) {
		        	continue;
		        }
		        
		        com.sparsity.dex.gdb.Attribute adata = graph.getAttribute(attr);
		        assert adata != null;
	
		        com.sparsity.dex.gdb.Value v = new com.sparsity.dex.gdb.Value();
		        graph.getAttribute(oid, attr, v);
		        if (!v.isNull()) {
		            switch (v.getDataType()) {
		                case Boolean:
		                	v.getBoolean();
		                    break;
		                case Integer:
		                	v.getInteger();
		                    break;
		                case String:
		                    v.getString();
		                    break;
		                case Double:
		                	v.getDouble();
		                    break;
		                default:
		                    throw new UnsupportedOperationException("Unsupported attribute type: " + v.getDataType());
		            }
		        }
			}
			
			setResult(opCount);
		}
	}
	
	
	/**
	 * The operation specialized for Neo4j
	 */
	public static class Neo extends OperationGetManyEdgeProperties {
		
		/**
		 * Execute the operation
		 */
		@Override
		protected void onExecute() throws Exception {
			for (Edge edge : edgeSamples) {
				((Neo4jEdge) edge).getRawEdge().getProperty(property_key);
			}
			setResult(opCount);
		}
	}
}
