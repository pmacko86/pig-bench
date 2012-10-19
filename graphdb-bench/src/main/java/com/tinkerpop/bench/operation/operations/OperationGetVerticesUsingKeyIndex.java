package com.tinkerpop.bench.operation.operations;

import java.util.Iterator;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.AutoIndexer;
import org.neo4j.graphdb.index.IndexHits;

import com.sparsity.dex.gdb.AttributeKind;
import com.sparsity.dex.gdb.ObjectType;
import com.tinkerpop.bench.operation.Operation;
import com.tinkerpop.bench.util.GraphUtils;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.extensions.impls.dex.DexUtils;
import com.tinkerpop.blueprints.impls.dex.DexGraph;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.blueprints.util.StringFactory;


public class OperationGetVerticesUsingKeyIndex extends Operation {

	protected String key;
	protected Object value;
	
	@Override
	protected void onInitialize(Object[] args) {
		
		if (args.length == 0) {
			key = null; value = null;
			return;
		}
		
		if (args.length != 2) 
			throw new IllegalArgumentException("Invalid arguments - usage: { key, value }");
		
		key = (String) args[0];
		value = (Object) args[1];
	}
	
	@Override
	protected void onExecute() throws Exception {
		
		Graph g = getGraph();
		Iterable<Vertex> itr = g.getVertices(key, value);
		int count = 0;
		for (@SuppressWarnings("unused") Vertex v : itr) count++;
		GraphUtils.close(itr);
		
		setResult(count);
	}
	
	
	/**
	 * The operation specialized for DEX
	 */
	public static class DEX extends OperationGetVerticesUsingKeyIndex {
		
		private int[] types;
		
		
		/**
		 * Initialize the operation
		 * 
		 * @param args the operation arguments
		 */
		@Override
		protected void onInitialize(Object[] args) {
			super.onInitialize(args);
			types = DexUtils.getNodeTypes(((DexGraph) getGraph()).getRawGraph());
		}

		
		/**
		 * Execute the operation
		 */
		@Override
		protected void onExecute() throws Exception {

			int count = 0;
			com.sparsity.dex.gdb.Graph graph = ((DexGraph) getGraph()).getRawGraph();
			
			
			// Get by label

			if (key.compareTo(StringFactory.LABEL) == 0) { // label is "indexed"

				int type = graph.findType(value.toString());
				if (type != com.sparsity.dex.gdb.Type.InvalidType) {
					com.sparsity.dex.gdb.Type tdata = graph.getType(type);
					if (tdata.getObjectType() == ObjectType.Node) {
						com.sparsity.dex.gdb.Objects objs = graph.select(type);
						com.sparsity.dex.gdb.ObjectsIterator objsItr = objs.iterator();

						while (objsItr.hasNext()) {
							@SuppressWarnings("unused")
							long v = objsItr.nextObject();
							count++;
						}

						objsItr.close();
						objs.close();
					}
				}
			}
			else {
				
				// Get by a different property
			
				for (int type : types) {
					int attr = graph.findAttribute(type, key);
					if (com.sparsity.dex.gdb.Attribute.InvalidAttribute != attr) {
						com.sparsity.dex.gdb.Attribute adata = graph.getAttribute(attr);
						
						if (adata.getKind() == AttributeKind.Basic) {
							throw new RuntimeException("Key " + key + " is not indexed");
						}
						else { // use the index
							com.sparsity.dex.gdb.Objects objs = DexUtils.getUsingIndex(graph, adata, value);
							com.sparsity.dex.gdb.ObjectsIterator objsItr = objs.iterator();
	
							while (objsItr.hasNext()) {
								@SuppressWarnings("unused")
								long v = objsItr.nextObject();
								count++;
							}
	
							objsItr.close();
							objs.close();
						}
					}
				}
			}
			
			setResult(count);
		}
	}
	
	
	/**
	 * The operation specialized for Neo4j
	 */
	public static class Neo extends OperationGetVerticesUsingKeyIndex {
		
		private boolean haveIndexer;
		
		
		/**
		 * Initialize the operation
		 * 
		 * @param args the operation arguments
		 */
		@Override
		protected void onInitialize(Object[] args) {
			super.onInitialize(args);
			
			@SuppressWarnings("rawtypes")
			final AutoIndexer indexer = ((Neo4jGraph) getGraph()).getRawGraph().index().getNodeAutoIndexer();
			haveIndexer = indexer.isEnabled() && indexer.getAutoIndexedProperties().contains(key);
			
			if (!haveIndexer) {
				throw new RuntimeException("AutoIndexer is not enabled for key " + key);
			}
		}

		
		/**
		 * Execute the operation
		 */
		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		protected void onExecute() throws Exception {
			
			final AutoIndexer indexer = ((Neo4jGraph) getGraph()).getRawGraph().index().getNodeAutoIndexer();
			Iterable<Node> iterable = indexer.getAutoIndex().get(key, value);
			
			Iterator<Node> i = iterable.iterator();
			int count = 0;
			
			while (i.hasNext()) {
				i.next();
				count++;
			}
			
			if (iterable instanceof IndexHits) {
	            ((IndexHits) iterable).close();
	        }
			
			setResult(count);
		}
	}
}
