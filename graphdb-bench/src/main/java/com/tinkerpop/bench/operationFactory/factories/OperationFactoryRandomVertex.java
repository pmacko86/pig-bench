package com.tinkerpop.bench.operationFactory.factories;

import java.util.ArrayList;
import java.util.Arrays;

import com.tinkerpop.bench.operationFactory.OperationArgs;
import com.tinkerpop.bench.operationFactory.OperationFactoryBase;
import com.tinkerpop.bench.util.StatisticsHelper;

public class OperationFactoryRandomVertex extends OperationFactoryBase implements WithOpCount {
	
	private Class<?> opType = null;
	private int opCount;
	private Object[] args;
	private ArrayList<Object> vertexSamples = null;
	private String tag = null;
	private boolean update;
	
	public OperationFactoryRandomVertex(Class<?> opType, int opCount) {
		this(opType, opCount, new Object[] {}, "");
	}
	
	public OperationFactoryRandomVertex(Class<?> opType, int opCount, Object[] args) {
		this(opType, opCount, args, "");
	}
	
	public OperationFactoryRandomVertex(Class<?> opType, int opCount, Object[] args, String tag) {
		super();
		this.opType = opType;
		this.opCount = opCount;
		this.args = args;
		this.tag = tag;
		this.update = isUpdateOperation(opType);
	}

	@Override
	protected void onInitialize() {
		vertexSamples = new ArrayList<Object>(Arrays.asList(StatisticsHelper
				.getRandomVertexIds(getGraph(), opCount)));		
	}

	@Override
	public boolean hasNext() {
		return vertexSamples.isEmpty() == false;
	}
	
	@Override
	protected OperationArgs onCreateOperation() throws Exception {
		Object[] myArgs = new Object[1 + args.length];
		myArgs[0] = vertexSamples.remove(0);
		System.arraycopy(args, 0, myArgs, 1, args.length);
		return new OperationArgs(myArgs, opType, tag);
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
		return opCount - vertexSamples.size();
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
