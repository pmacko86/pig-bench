package com.tinkerpop.bench.operationFactory.factories;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import com.tinkerpop.bench.operationFactory.OperationArgs;
import com.tinkerpop.bench.operationFactory.OperationFactoryBase;
import com.tinkerpop.blueprints.extensions.BenchmarkableGraph;

public class OperationFactoryRandomVertexOriginalID extends OperationFactoryBase implements WithOpCount {
	
	private Class<?> opType = null;
	private int opCount;
	private Object[] args;
	private List<Object> vertexSamples = null;
	private String tag = null;
	private boolean update;
	private Random random;
	
	public OperationFactoryRandomVertexOriginalID(Class<?> opType, int opCount) {
		this(opType, opCount, new Object[] {}, "");
	}
	
	public OperationFactoryRandomVertexOriginalID(Class<?> opType, int opCount, Object[] args) {
		this(opType, opCount, args, "");
	}
	
	public OperationFactoryRandomVertexOriginalID(Class<?> opType, int opCount, Object[] args, String tag) {
		super();
		this.opType = opType;
		this.opCount = opCount;
		this.args = args;
		this.tag = tag;
		this.update = isUpdateOperation(opType);
		this.random = new Random();
	}

	@Override
	protected void onInitialize() {
		int count = (int) ((BenchmarkableGraph) getGraph()).countVertices();
		vertexSamples = new LinkedList<Object>();
		for (int i = 0; i < opCount; i++) {
			vertexSamples.add(new Integer(random.nextInt(count)));
		}
	}

	@Override
	public boolean hasNext() {
		return vertexSamples.isEmpty() == false;
	}
	
	@Override
	protected OperationArgs onCreateOperation() throws Exception {
		Object[] myArgs = new Object[1 + args.length];
		System.arraycopy(args, 0, myArgs, 0, args.length);
		myArgs[args.length] = vertexSamples.remove(0);
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
