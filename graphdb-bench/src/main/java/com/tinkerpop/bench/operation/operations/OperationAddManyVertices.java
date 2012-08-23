package com.tinkerpop.bench.operation.operations;

import com.tinkerpop.bench.cache.Cache;
import com.tinkerpop.bench.operation.Operation;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;

import edu.harvard.pass.cpl.CPL;
import edu.harvard.pass.cpl.CPLObject;

public class OperationAddManyVertices extends Operation {
	
	private int opCount;
	
	private Object id;
	
	@Override
	protected void onInitialize(Object[] args) {
		opCount = args.length > 0 ? (Integer) args[0] : 1000;
		
		id = null; //args.length > 1 ? args[1] : null;
	}
	
	@Override
	protected void onExecute() throws Exception {
		try {
			Graph graph = getGraph();
			
			for (int i = 0; i < opCount; i++) {
				Vertex vertex = graph.addVertex(id);
				Cache.getInstance(getGraph()).addVertex(vertex);
			}
			
			setResult(opCount);
		} catch (Exception e) {
			throw e;
		}
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
}
