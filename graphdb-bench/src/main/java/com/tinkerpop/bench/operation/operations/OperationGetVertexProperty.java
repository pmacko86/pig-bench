package com.tinkerpop.bench.operation.operations;

import com.sparsity.dex.gdb.Graph;
import com.tinkerpop.bench.operation.Operation;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.dex.DexGraph;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jVertex;
import com.tinkerpop.blueprints.util.StringFactory;

public class OperationGetVertexProperty extends Operation {

	protected Vertex vertex;
	protected String property_key;
	
	@Override
	protected void onInitialize(Object[] args) {
		vertex = getGraph().getVertex(args[0]);
		property_key = (String) args[1];
	}
	
	@Override
	protected void onExecute() throws Exception {
		setResult(vertex.getProperty(property_key));
	}
	
	
	/**
	 * The operation specialized for DEX
	 */
	public static class DEX extends OperationGetVertexProperty {
		
		/**
		 * Execute the operation
		 */
		@Override
		protected void onExecute() throws Exception {
			
			Graph graph = ((DexGraph) getGraph()).getRawGraph();
			long oid = ((Long) vertex.getId()).longValue();
			
			int type = ((DexGraph) getGraph()).getRawGraph().getObjectType(oid);	// Here or inside onInitialize?

	        if (property_key.compareTo(StringFactory.LABEL) == 0) {		// Is this necessary? Does Neo4j also do this?
	        	setResult(graph.getType(type).getName());
	        	return;
	        }
	        
	        int attr = graph.findAttribute(type, property_key);
	        if (attr == com.sparsity.dex.gdb.Attribute.InvalidAttribute) {
	        	setResult(null);
	        	return;
	        }
	        
	        com.sparsity.dex.gdb.Attribute adata = graph.getAttribute(attr);
	        assert adata != null;

	        com.sparsity.dex.gdb.Value v = new com.sparsity.dex.gdb.Value();
	        graph.getAttribute(oid, attr, v);
	        if (!v.isNull()) {
	            switch (v.getDataType()) {
	                case Boolean:
	                	setResult(v.getBoolean());
	                    break;
	                case Integer:
	                	setResult(v.getInteger());
	                    break;
	                case String:
	                    setResult(v.getString());
	                    break;
	                case Double:
	                	setResult(v.getDouble());
	                    break;
	                default:
	                    throw new UnsupportedOperationException("Unsupported attribute type: " + v.getDataType());
	            }
	        }
		}
	}
	
	
	/**
	 * The operation specialized for Neo4j
	 */
	public static class Neo extends OperationGetVertexProperty {
		
		/**
		 * Execute the operation
		 */
		@Override
		protected void onExecute() throws Exception {
			setResult(((Neo4jVertex) vertex).getRawVertex().getProperty(property_key));
		}
	}
}
