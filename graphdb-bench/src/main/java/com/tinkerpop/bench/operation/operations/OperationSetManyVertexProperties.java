package com.tinkerpop.bench.operation.operations;

import java.util.UUID;

import org.neo4j.graphdb.Node;

import com.sparsity.dex.gdb.Attribute;
import com.sparsity.dex.gdb.AttributeKind;
import com.sparsity.dex.gdb.DataType;
import com.sparsity.dex.gdb.Value;
import com.tinkerpop.bench.operation.Operation;
import com.tinkerpop.bench.util.StatisticsHelper;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.extensions.impls.dex.DexUtils;
import com.tinkerpop.blueprints.extensions.impls.neo4j.Neo4jUtils;
import com.tinkerpop.blueprints.impls.dex.DexGraph;

import edu.harvard.pass.cpl.CPL;
import edu.harvard.pass.cpl.CPLObject;

public class OperationSetManyVertexProperties extends Operation {

	protected String property_key;
	protected String[] property_values;
	
	protected int opCount;
	protected Vertex[] vertexSamples;
	
	@Override
	protected void onInitialize(Object[] args) {
		property_key = (String) args[0];
		
		opCount = args.length > 1 ? (Integer) args[1] : 1000;
		vertexSamples = StatisticsHelper.getRandomVertices(getGraph(), opCount);
		
		property_values = args.length > 2 ? (String[]) args[2] : null;
		if (property_values == null) {
			property_values = new String[vertexSamples.length];
			for (int i = 0; i < property_values.length; i++) {
				property_values[i] = UUID.randomUUID().toString();
			}
		}
	}
	
	@Override
	protected void onExecute() throws Exception {
		for (int i = 0; i < opCount; i++)
			vertexSamples[i].setProperty(property_key, property_values[i]);
			
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
	public static class DEX extends OperationSetManyVertexProperties {
		
		private Value temp;
		private int[] attrIds;
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

			temp = new Value();
			vertices = DexUtils.translateVertices(vertexSamples);
			
			attrIds = new int[vertices.length];
			for (int i = 0; i < vertices.length; i++) {
				int t = graph.getObjectType(vertices[i]);
				int a = graph.findAttribute(t, property_key);
				if (a == Attribute.InvalidAttribute) {
					a = DexUtils.getOrCreateAttributeHandle(graph, t,
							property_key, DataType.String, AttributeKind.Basic);
				}
				attrIds[i] = a;
			}
		}

		
		/**
		 * Execute the operation
		 */
		@Override
		protected void onExecute() throws Exception {
			
			com.sparsity.dex.gdb.Graph graph = ((DexGraph) getGraph()).getRawGraph();

			for (int i = 0; i < opCount; i++) {
				long oid = vertices[i];
				temp.setString(property_values[i]);
				graph.setAttribute(oid, attrIds[i], temp);
			}
			
			setResult(opCount);
		}
	}
	
	
	/**
	 * The operation specialized for Neo4j
	 */
	public static class Neo extends OperationSetManyVertexProperties {
		
		private Node[] vertices;


		/**
		 * Initialize the operation
		 * 
		 * @param args the operation arguments
		 */
		@Override
		protected void onInitialize(Object[] args) {
			super.onInitialize(args);
			vertices = Neo4jUtils.translateVertices(vertexSamples);
		}

		
		/**
		 * Execute the operation
		 */
		@Override
		protected void onExecute() throws Exception {

			for (int i = 0; i < opCount; i++) {
				Node n = vertices[i];
				n.setProperty(property_key, property_values[i]);
			}
			
			setResult(opCount);	
		}
	}
}
