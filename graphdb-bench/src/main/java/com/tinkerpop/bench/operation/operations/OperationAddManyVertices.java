package com.tinkerpop.bench.operation.operations;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;

import com.sparsity.dex.gdb.Attribute;
import com.sparsity.dex.gdb.AttributeKind;
import com.sparsity.dex.gdb.DataType;
import com.sparsity.dex.gdb.Type;
import com.sparsity.dex.gdb.Value;
import com.tinkerpop.bench.operation.Operation;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.extensions.BenchmarkableGraph;
import com.tinkerpop.blueprints.extensions.impls.dex.DexUtils;
import com.tinkerpop.blueprints.extensions.io.fgf.FGFGraphLoader;
import com.tinkerpop.blueprints.impls.dex.DexGraph;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;

import edu.harvard.pass.cpl.CPL;
import edu.harvard.pass.cpl.CPLObject;


/**
 * Add many vertices and assign them an ID as a property
 */
public class OperationAddManyVertices extends Operation {
	
	protected int opCount;
	protected int previousVertexCount;
	
	@Override
	protected void onInitialize(Object[] args) {
		opCount = args.length > 0 ? (Integer) args[0] : 1000;
	}
	
	@Override
	protected void onDelayedInitialize() throws Exception {
		previousVertexCount = (int) ((BenchmarkableGraph) getGraph()).countVertices();
	}
	
	@Override
	protected void onExecute() throws Exception {
		Graph graph = getGraph();
		int id = previousVertexCount;
			
		for (int i = 0; i < opCount; i++) {
			Vertex vertex = graph.addVertex(null);
			vertex.setProperty(FGFGraphLoader.KEY_ORIGINAL_ID, id++);
		}
			
		setResult(opCount);
	}

	@Override
	protected void onFinalize() throws Exception {
		if (CPL.isAttached()) {
			CPLObject obj = getCPLObject();
			obj.addProperty("COUNT", "" + opCount);
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
	public static class DEX extends OperationAddManyVertices {
		
		private int type;
		private Value temp;
		private int attrId;
		
		
		/**
		 * Initialize the operation
		 * 
		 * @param args the operation arguments
		 */
		@Override
		protected void onInitialize(Object[] args) {
			super.onInitialize(args);
			
			com.sparsity.dex.gdb.Graph graph = ((DexGraph) getGraph()).getRawGraph();

			type = graph.findType(DexGraph.DEFAULT_DEX_VERTEX_LABEL);
			if (type == Type.InvalidType) {
				type = DexUtils.getNodeTypes(graph)[0];
			}
			
			temp = new Value();
			
			attrId = graph.findAttribute(this.type, FGFGraphLoader.KEY_ORIGINAL_ID);
			if (this.attrId == Attribute.InvalidAttribute) {
				attrId = DexUtils.getOrCreateAttributeHandle(graph, this.type,
						FGFGraphLoader.KEY_ORIGINAL_ID, DataType.Integer, AttributeKind.Unique);
			}
		}

		
		/**
		 * Execute the operation
		 */
		@Override
		protected void onExecute() throws Exception {
			
			com.sparsity.dex.gdb.Graph graph = ((DexGraph) getGraph()).getRawGraph();
			int id = previousVertexCount;

			for (int i = 0; i < opCount; i++) {
				long oid = graph.newNode(type);
				temp.setInteger(id++);
				graph.setAttribute(oid, attrId, temp);
			}
			
			setResult(opCount);
		}
	}
	
	
	/**
	 * The operation specialized for Neo4j
	 */
	public static class Neo extends OperationAddManyVertices {
		
		
		/**
		 * Initialize the operation
		 * 
		 * @param args the operation arguments
		 */
		@Override
		protected void onInitialize(Object[] args) {
			super.onInitialize(args);
		}

		
		/**
		 * Execute the operation
		 */
		@Override
		protected void onExecute() throws Exception {

			GraphDatabaseService graph = ((Neo4jGraph) getGraph()).getRawGraph();
			int id = previousVertexCount;

			for (int i = 0; i < opCount; i++) {
				Node n = graph.createNode();
				n.setProperty(FGFGraphLoader.KEY_ORIGINAL_ID, id++);
			}
			
			setResult(opCount);	
		}
	}
}
