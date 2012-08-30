package com.tinkerpop.bench.operation.operations;

import java.util.UUID;

import com.tinkerpop.bench.operation.Operation;
import com.tinkerpop.bench.util.StatisticsHelper;
import com.tinkerpop.blueprints.Edge;

import edu.harvard.pass.cpl.CPL;
import edu.harvard.pass.cpl.CPLObject;

public class OperationSetManyEdgeProperties extends Operation {

	private String property_key;
	private String property_value;
	
	private int opCount;
	private Edge[] edgeSamples;
	
	@Override
	protected void onInitialize(Object[] args) {
		property_key = (String) args[0];
		
		opCount = args.length > 1 ? (Integer) args[1] : 1000;
		edgeSamples = StatisticsHelper.getRandomEdges(getGraph(), opCount);
		
		property_value = args.length > 2 ? (String) args[2] : UUID.randomUUID().toString();
	}
	
	@Override
	protected void onExecute() throws Exception {
		try {
			for (int i = 0; i < opCount; i++)
				edgeSamples[i].setProperty(property_key, property_value);
			
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
