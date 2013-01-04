package com.tinkerpop.bench.operationFactory.factories;

import java.util.ArrayList;

import com.tinkerpop.bench.operationFactory.OperationArgs;
import com.tinkerpop.bench.operationFactory.OperationFactoryBase;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.extensions.BenchmarkableGraph;
import com.tinkerpop.blueprints.extensions.util.ClosingIterator;

public class OperationFactorySequentialVertex extends OperationFactoryBase implements WithOpCount {
	
	private Class<?> opType = null;
	private int opCount;
	private Object[] args;
	private ArrayList<Vertex> vertices = null;
	private String tag = null;
	private boolean update;
	private int totalCount = 0;
	private int index = 0;
	
	public OperationFactorySequentialVertex(Class<?> opType, int opCount) {
		this(opType, opCount, new Object[] {}, "");
	}
	
	public OperationFactorySequentialVertex(Class<?> opType, int opCount, Object[] args) {
		this(opType, opCount, args, "");
	}
	
	public OperationFactorySequentialVertex(Class<?> opType, int opCount, Object[] args, String tag) {
		super();
		this.opType = opType;
		this.opCount = opCount;
		this.args = args;
		this.tag = tag;
		this.update = isUpdateOperation(opType);
	}

	@Override
	protected void onInitialize() {
		BenchmarkableGraph g = (BenchmarkableGraph) getGraph();
		vertices = new ArrayList<Vertex>((int) g.countVertices());
		for (Vertex v : new ClosingIterator<Vertex>(g.getVertices())) vertices.add(v);
		totalCount = opCount * vertices.size();
	}

	@Override
	public boolean hasNext() {
		return index < totalCount;
	}
	
	@Override
	protected OperationArgs onCreateOperation() throws Exception {
		Object[] myArgs = new Object[1 + args.length];
		myArgs[0] = vertices.get((index++) % vertices.size()).getId();
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
		return totalCount;
	}
	
	/**
	 * Return the number of already executed operations
	 * 
	 * @return the number of already executed operations
	 */
	@Override
	public int getExecutedOpCount() {
		return index;
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
