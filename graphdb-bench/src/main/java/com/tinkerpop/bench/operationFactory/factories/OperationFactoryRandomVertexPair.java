package com.tinkerpop.bench.operationFactory.factories;

import java.util.ArrayList;
import java.util.Arrays;

import com.tinkerpop.bench.StatisticsHelper;
import com.tinkerpop.bench.evaluators.EvaluatorUniform;
import com.tinkerpop.bench.operationFactory.OperationArgs;
import com.tinkerpop.bench.operationFactory.OperationFactoryBase;

public class OperationFactoryRandomVertexPair extends OperationFactoryBase {
	
	private Class<?> opType = null;
	private int opCount;
	private ArrayList<Object> vertexSamples = null;
	
	public OperationFactoryRandomVertexPair(Class<?> opType, int opCount) {
		super();
		this.opType = opType;
		this.opCount = opCount;
	}

	@Override
	protected void onInitialize() {
		vertexSamples = new ArrayList<Object>(Arrays.asList(StatisticsHelper
				.getSampleVertexIds(getGraph(), new EvaluatorUniform(), 2 * opCount)));		
	}

	@Override
	public boolean hasNext() {
		return vertexSamples.isEmpty() == false;
	}
	
	@Override
	protected OperationArgs onCreateOperation() throws Exception {
		return new OperationArgs(
				new Object[] { vertexSamples.remove(0), vertexSamples.remove(0) },
				opType);
	}

}