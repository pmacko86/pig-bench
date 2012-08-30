package com.tinkerpop.bench.operationFactory;

import java.io.File;
import java.util.Iterator;

import com.tinkerpop.bench.log.OperationLogEntry;
import com.tinkerpop.bench.operation.Operation;
import com.tinkerpop.bench.util.LogUtils;

/**
 * @author Alex Averbuch (alex.averbuch@gmail.com)
 * @author Peter Macko (pmacko@eecs.harvard.edu)
 */
public final class OperationFactoryLog extends OperationFactory {

	private Iterator<OperationLogEntry> operationLogIterator = null;

	public OperationFactoryLog(File file) {
		operationLogIterator = LogUtils.getOperationLogReader(file).iterator();
	}

	@Override
	protected void onInitialize() {
	}

	@Override
	public boolean hasNext() {
		return operationLogIterator.hasNext();
	}

	@Override
	public Operation next() {
		OperationLogEntry operationLogEntry = operationLogIterator.next();
		try {
			return createOperation(operationLogEntry.getType(), operationLogEntry.getArgs(),
					operationLogEntry.getName());
		} catch (Exception e) {
			throw new RuntimeException("Error in loadOperation", e.getCause());
		}
	}
	
	@Override
	public boolean isUpdate() {
		return false;
	}
}
