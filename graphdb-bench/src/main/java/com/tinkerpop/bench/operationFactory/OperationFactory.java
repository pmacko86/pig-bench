package com.tinkerpop.bench.operationFactory;

import java.lang.reflect.Constructor;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import com.tinkerpop.bench.DatabaseEngine;
import com.tinkerpop.bench.GlobalConfig;
import com.tinkerpop.bench.GraphDescriptor;
import com.tinkerpop.bench.log.OperationLogEntry;
import com.tinkerpop.bench.operation.Operation;
import com.tinkerpop.bench.operation.OperationDoGC;
import com.tinkerpop.bench.operation.OperationOpenGraph;
import com.tinkerpop.bench.operation.OperationShutdownGraph;
import com.tinkerpop.bench.operation.operations.OperationLoadFGF;
import com.tinkerpop.bench.operation.operations.OperationLoadGraphML;
import com.tinkerpop.bench.util.ConsoleUtils;
import com.tinkerpop.blueprints.Graph;

import edu.harvard.pass.cpl.CPLObject;

/**
 * @author Alex Averbuch (alex.averbuch@gmail.com)
 * @author Martin Neumann (m.neumann.1980@gmail.com)
 * @author Peter Macko (pmacko@eecs.harvard.edu)
 */
public abstract class OperationFactory implements Iterator<Operation>,
		Iterable<Operation> {
	
	/// The graph descriptor
	private GraphDescriptor graphDescriptor = null;
	
	/// The shared operation id counter
	private AtomicInteger sharedIdCounter = null;
	
	/// The current operation id
	private int currentId = -1;
	
	/// The CPL object to be shared among multiple operations produced by this factory
	private CPLObject sharedOperationCPLObject = null;
	
	
	/**
	 * Create an instance of OperationFactory
	 */
	public OperationFactory() {
	}
	
	
	/**
	 * Get the shared CPL object
	 * 
	 * @return the shared CPL object
	 */
	public CPLObject getSharedOperationCPLObject() {
		return sharedOperationCPLObject;
	}
	
	
	/**
	 * Set the shared CPL object
	 * 
	 * @param object the shared CPL object
	 */
	public void setSharedOperationCPLObject(CPLObject object) {
		sharedOperationCPLObject = object;
	}

	
	/**
	 * Get the current operation ID
	 * 
	 * @return the current operation ID
	 */
	public final int getCurrentOpId() {
		return currentId;
	}
	

	/**
	 * Initialize the operation factory
	 * 
	 * @param graphDescriptor the graph descriptor
	 * @param sharedIdCounter the shared operation ID counter
	 */
	public void initialize(GraphDescriptor graphDescriptor,
			AtomicInteger sharedIdCounter) {
		this.graphDescriptor = graphDescriptor;
		this.sharedIdCounter = sharedIdCounter;
		onInitialize();
	}
	
	
	/**
	 * Get the graph descriptor
	 * 
	 * @return the graph descriptor
	 */
	protected final GraphDescriptor getGraphDescriptor() {
		return graphDescriptor;
	}

	
	/**
	 * Get the graph
	 * 
	 * @return the graph
	 */
	protected final Graph getGraph() {
		return graphDescriptor.getGraph();
	}
	
	
	/**
	 * The callback for factory initialization
	 */
	protected abstract void onInitialize();
	

	/**
	 * Get the factory iterator; calling next() would instantiate
	 * the individual operations
	 * 
	 * @return the iterator
	 */
	@Override
	public final Iterator<Operation> iterator() {
		return this;
	}

	
	/**
	 * Remove the last item returned by the iterator (unsupported)
	 */
	@Override
	public final void remove() {
		throw new UnsupportedOperationException();
	}

	
	/**
	 * Instantiate and configure an operation
	 * 
	 * @param type the name of the class that implements the operation (operation type)
	 * @param args the operation arguments
	 * @param name the operation name
	 * @return
	 * @throws Exception
	 */
	protected final Operation createOperation(String type,
			Object[] args, String name) throws Exception {
		
		Class<?> c = Class.forName(type);
		
		
		// If the class defines a public member class that specializes
		// the operation for the selected database engine, use it instead
		// of the generic operation -- but only if the use of specialized
		// procedures is enabled
		
		if (GlobalConfig.useStoredProcedures) {
			DatabaseEngine db = graphDescriptor.getDatabaseEngine();
			boolean found = false;
			for (Class<?> d : c.getDeclaredClasses()) {
				if (d.getSimpleName().equalsIgnoreCase(db.getShortName() + "_StoredProcedure")) {
					if (c.isAssignableFrom(d)) {
						c = d;
						found = true;
						break;
					}
					else {
						throw new IllegalStateException(c.getSimpleName() + "$" + d.getSimpleName() + " found, but "
								+ "it does not extend " + c.getSimpleName());
					}
				}
			}
			
			if (!(OperationOpenGraph.class.isAssignableFrom(c)
					|| OperationLoadFGF.class.isAssignableFrom(c)
					|| OperationLoadGraphML.class.isAssignableFrom(c)
					|| OperationDoGC.class.isAssignableFrom(c)
					|| OperationLogEntry.class.isAssignableFrom(c)
					|| OperationShutdownGraph.class.isAssignableFrom(c))
					&& !found) {
				//ConsoleUtils.warn("Cannot find a stored procedure for " + c.getSimpleName());
				if (GlobalConfig.useStoredProceduresOnly) return null;
			}
		}
		
		if (GlobalConfig.useSpecializedProcedures) {
			DatabaseEngine db = graphDescriptor.getDatabaseEngine();
			for (Class<?> d : c.getDeclaredClasses()) {
				if (d.getSimpleName().equalsIgnoreCase(db.getShortName())) {
					if (c.isAssignableFrom(d)) {
						c = d;
						break;
					}
					else {
						throw new IllegalStateException(c.getSimpleName() + "$" + d.getSimpleName() + " found, but "
								+ "it does not extend " + c.getSimpleName());
					}
				}
			}
		}
		
		
		// Instantiate the operation
		
		Constructor<?> operationConstructor = c.getConstructors()[0];
		Operation operation = (Operation) operationConstructor.newInstance(new Object[] {});
		
		
		// Initialize the operation
		
		int id = sharedIdCounter.incrementAndGet();
		currentId = id;
		operation.setId(id);
		
		operation.setArgs(args);
		operation.setName(name);
		operation.setFactory(this);

		return operation;
	}
	
	
	/**
	 * Determine whether the given operation class will perform an update, assuming
	 * that the operation does not need to be initialized to provide this information
	 * 
	 * @param operationType the operation type (class)
	 * @return true if it will perform an update, or false otherwise
	 */
	public static boolean isUpdateOperation(Class<?> operationType) {
		try {
			return ((Operation) operationType.getConstructors()[0].newInstance(new Object[] {})).isUpdate();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	
	/**
	 * Determine whether any of the operations that the factory creates will perform
	 * any update operations on the database
	 * 
	 * @return true if at least one of the operations will perform an update
	 */
	public abstract boolean isUpdate();
}
