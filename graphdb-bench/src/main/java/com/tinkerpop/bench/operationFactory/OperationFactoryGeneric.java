package com.tinkerpop.bench.operationFactory;

import com.tinkerpop.bench.operationFactory.factories.WithOpCount;

/**
 * @author Alex Averbuch (alex.averbuch@gmail.com)
 * @author Peter Macko (pmacko@eecs.harvard.edu)
 */
public class OperationFactoryGeneric extends OperationFactoryBase implements WithOpCount {

	private Class<?> operationType = null;
	private int opCount = -1;
	private int remainingCount = -1;
	private Object[] args = null;
	private String tag = null;
	private boolean update;

	public OperationFactoryGeneric(Class<?> operationType) {
		this(operationType, -1, new Object[] {});
	}

	public OperationFactoryGeneric(Class<?> operationType, Object[] args) {
		this(operationType, -1, args);
	}

	public OperationFactoryGeneric(Class<?> operationType, int opCount) {
		this(operationType, opCount, new Object[] {});
	}

	public OperationFactoryGeneric(Class<?> operationType, int opCount,
			Object[] args) {
		this(operationType, opCount, args, "");
	}

	public OperationFactoryGeneric(Class<?> operationType, int opCount,
			Object[] args, String tag) {
		this.operationType = operationType;
		this.opCount = opCount;
		this.remainingCount = opCount;
		this.args = args;
		this.tag = tag;
		this.update = isUpdateOperation(operationType);
	}

	@Override
	public void onInitialize() {
	}

	@Override
	public boolean hasNext() {
		return (-1 == remainingCount) ? true : (remainingCount > 0);
	}

	@Override
	protected OperationArgs onCreateOperation() throws Exception {
		if (-1 != remainingCount)
			remainingCount--;
		return new OperationArgs(args, operationType, tag);
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
		return opCount - remainingCount;
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
