package com.tinkerpop.bench.operation.operations;

import com.tinkerpop.bench.cache.Cache;
import com.tinkerpop.bench.operation.Operation;
import com.tinkerpop.blueprints.Vertex;

import edu.harvard.pass.cpl.CPL;
import edu.harvard.pass.cpl.CPLObject;

public class OperationAddVertex extends Operation {
	
	private Object id;
	
	@Override
	protected void onInitialize(Object[] args) {
		id = args.length > 0 ? args[0] : null;
	}
	
	@Override
	protected void onExecute() throws Exception {
		try {
			Vertex vertex = getGraph().addVertex(id);
			setResult(vertex.toString());
			Cache.getInstance(getGraph()).addVertex(vertex);
		} catch (Exception e) {
			throw e;
		}
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
}
