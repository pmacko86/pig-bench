package com.tinkerpop.bench.operationFactory.factories;

import java.util.HashSet;
import java.util.LinkedList;

import com.tinkerpop.bench.operationFactory.OperationArgs;
import com.tinkerpop.bench.operationFactory.OperationFactoryBase;
import com.tinkerpop.bench.util.StatisticsHelper;
import com.tinkerpop.blueprints.Edge;

public class OperationFactoryRandomEdgePropertyValue extends OperationFactoryBase implements WithOpCount {
	
	private Class<?> opType = null;
	private int opCount;
	private Object[] args;
	private LinkedList<Object> valueSamples = null;
	private String tag = null;
	private String key = null;
	private boolean update;
	
	public OperationFactoryRandomEdgePropertyValue(Class<?> opType, int opCount) {
		this(opType, opCount, null, new Object[] {}, "");
	}
	
	public OperationFactoryRandomEdgePropertyValue(Class<?> opType, int opCount, String key, Object[] args) {
		this(opType, opCount, key, args, "");
	}
	
	public OperationFactoryRandomEdgePropertyValue(Class<?> opType, int opCount, String key, Object[] args, String tag) {
		super();
		this.opType = opType;
		this.opCount = opCount;
		this.args = args;
		this.tag = tag;
		this.key = key;
		this.update = isUpdateOperation(opType);
	}

	@Override
	protected void onInitialize() {
		valueSamples = new LinkedList<Object>();
		HashSet<Object> samples = new HashSet<Object>();
		for (Edge e : StatisticsHelper.getRandomEdges(getGraph(), opCount)) {
			Object x = e.getProperty(key);
			if (x != null) samples.add(x);
		}
		valueSamples.addAll(samples);
	}

	@Override
	public boolean hasNext() {
		return valueSamples.isEmpty() == false;
	}
	
	@Override
	protected OperationArgs onCreateOperation() throws Exception {
		Object[] myArgs = new Object[2 + args.length];
		System.arraycopy(args, 0, myArgs, 0, args.length);
		myArgs[args.length] = key;
		myArgs[args.length+1] = valueSamples.remove(0);
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
		return opCount - valueSamples.size();
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
