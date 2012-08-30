package com.tinkerpop.bench.operationFactory.factories;

import java.util.ArrayList;
import java.util.Arrays;

import com.tinkerpop.bench.operationFactory.OperationArgs;
import com.tinkerpop.bench.operationFactory.OperationFactoryBase;
import com.tinkerpop.bench.util.StatisticsHelper;

public class OperationFactoryRandomVertexPair extends OperationFactoryBase implements WithOpCount {
	
	private Class<?> opType = null;
	private int opCount;
	private ArrayList<Object> vertexSamples = null;
	private boolean update;
	
	public OperationFactoryRandomVertexPair(Class<?> opType, int opCount) {
		super();
		this.opType = opType;
		this.opCount = opCount;
		this.update = isUpdateOperation(opType);
	}

	@Override
	protected void onInitialize() {
		vertexSamples = new ArrayList<Object>(Arrays.asList(StatisticsHelper
				.getRandomVertexIds(getGraph(), 2 * opCount)));		
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
	
	/**
	 * Return the total number of operations
	 * 
	 * @return the total number of operations
	 */
	@Override
	public int getOpCount() {
		return opCount;
	}
	
	/**
	 * Return the number of already executed operations
	 * 
	 * @return the number of already executed operations
	 */
	@Override
	public int getExecutedOpCount() {
		return opCount - vertexSamples.size() / 2;
	}
	
	/**
	 * Determine whether any of the operations that the factory creates will perform
	 * any update operations on the database
	 * 
	 * @return true if at least one of the operations will perform an update
	 */
	@Override
	public boolean isUpdate() {
		return update;
	}
}
