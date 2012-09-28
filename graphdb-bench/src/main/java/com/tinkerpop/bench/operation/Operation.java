package com.tinkerpop.bench.operation;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;

import com.tinkerpop.bench.Bench;
import com.tinkerpop.bench.GlobalConfig;
import com.tinkerpop.bench.GraphDescriptor;
import com.tinkerpop.bench.log.OperationLogWriter;
import com.tinkerpop.bench.operationFactory.OperationFactory;
import com.tinkerpop.bench.operationFactory.factories.WithOpCount;
import com.tinkerpop.bench.util.StatisticsHelper;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.TransactionalGraph.Conclusion;
import com.tinkerpop.blueprints.extensions.AutoTransactionalGraph;

import edu.harvard.pass.cpl.CPL;
import edu.harvard.pass.cpl.CPLObject;

/**
 * @author Alex Averbuch (alex.averbuch@gmail.com)
 * @author Martin Neumann (m.neumann.1980@gmail.com)
 */
public abstract class Operation {

	private int opId = -1;
	private Object[] args = null;
	private long time = -1;
	private Object result = null;
	private GraphDescriptor graphDescriptor = null;
	private String name = null;
	private long memory = -1;
    private long totalGarbageCollections = 0;
    private long garbageCollectionTime = 0;
	private OperationLogWriter logWriter = null;
	private OperationFactory factory = null;
	private CPLObject cplObject = null;
	
	
	/**
	 * Create an instance of class Operation
	 */
	public Operation() {
		// Nothing to do - the fields are already initialized by now
	}

	
	
	
	/*
	 * Setter Methods
	 */

	public final void setId(int opId) {
		this.opId = opId;
	}

	public final void setArgs(Object[] args) {
		this.args = args;
	}

	public final void setName(String name) {
		this.name = name;
	}

	protected final void setResult(Object result) {
		this.result = result;
	}
	
	public final void setLogWriter(OperationLogWriter logWriter) {
		this.logWriter = logWriter;
	}
	
	public final void setFactory(OperationFactory factory) {
		this.factory = factory;
	}

	
	
	
	/*
	 * Getter Methods
	 */
	
	/**
	 * Return the operation ID
	 * 
	 * @return the operation ID
	 */
	public final int getId() {
		return opId;
	}

	
	/**
	 * Return the arguments passed in to the operation
	 * 
	 * @return the operation arguments
	 */
	public final Object[] getArgs() {
		return args;
	}

	
	/**
	 * Return the elapsed time (unit: us)
	 * 
	 * @return the elapsed time as the number of nanoseconds
	 */
	public final long getTime() {
		return time;
	}

	
	/**
	 * Get the result of the operation
	 * 
	 * @return the result of the operation
	 */
	public final Object getResult() {
		return result;
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
	 * Return the graph
	 * 
	 * @return the graph
	 */
	protected final Graph getGraph() {
		return graphDescriptor.getGraph();
	}

	
	/**
	 * Return the operation name
	 * 
	 * @return the operation name
	 */
	public final String getName() {
		return name;
	}

	
	/**
	 * Return the operation type
	 * 
	 * @return the operation type -- the name of the operation class
	 */
	public final String getType() {
		return getClass().getName();
	}
	
	
	/**
	 * Return the difference of JVM memory usage before and after the operation execution
	 * 
	 * @return the memory usage in bytes
	 */
	public final long getMemory() {
		return memory;
	}
	
	
	/**
	 * Return the number of garbage collections while the operation was running
	 * 
	 * @return the number of garbage collections
	 */
	public final long getGCCount() {
		return totalGarbageCollections;
	}
	
	
	/**
	 * Return the approximate time spent in garbage collection while the operation was running
	 * 
	 * @return the time in ms
	 */
	public final long getGCTimeMS() {
		return garbageCollectionTime;
	}
	
	
	/**
	 * Return the log writer
	 * 
	 * @return the log writer
	 */
	public final OperationLogWriter getLogWriter() {
		return logWriter;
	}
	
	
	/**
	 * Return the operation factory responsible for creating this operation
	 * 
	 * @return the operation factory
	 */
	public final OperationFactory getFactory() {
		return factory;
	}
	
	
	/**
	 * Get or create the CPL  object associated with this operation
	 * 
	 * @return the CPL object, or null if not collecting provenance
	 */
	public CPLObject getCPLObject() {
		if (!CPL.isAttached()) return null;
		if (cplObject != null) return cplObject;
		
		if (factory != null) {
			if (factory.getSharedOperationCPLObject() != null) {
				cplObject = factory.getSharedOperationCPLObject();
				return cplObject;
			}
		}
		
		cplObject = CPLObject.create(Bench.ORIGINATOR,
				name, Bench.TYPE_OPERATION);
		cplObject.addProperty("CLASS", getClass().getCanonicalName());

		if (factory != null && factory instanceof WithOpCount) {
			WithOpCount w = (WithOpCount) factory;
			if (w.getOpCount() > 1) {
				cplObject.addProperty("COUNT", "" + w.getOpCount());
				factory.setSharedOperationCPLObject(cplObject);
			}
		}
		
		return cplObject;
	}
	
	
	
	
	/*
	 * Event Methods
	 */

	/**
	 * Initialize the operation
	 *  
	 * @param graphDescriptor the graph descriptor
	 */
	public final void initialize(GraphDescriptor graphDescriptor) {
		this.graphDescriptor = graphDescriptor;
		onInitialize(args);
	}

	
	/**
	 * Execute the operation
	 * 
	 * @throws Exception on error
	 */
	public final void execute() throws Exception {
        
		int previousMaxBufferSize = 0;
        Graph graph = getGraph();
        if (graph == null && !(this instanceof OperationDeleteGraph
           		|| this instanceof OperationOpenGraph
           		|| this instanceof OperationShutdownGraph
        		|| this instanceof OperationDoGC)) {
        	throw new IllegalStateException("The graph is null");
        }
        
        // http://stackoverflow.com/questions/466878/can-you-get-basic-gc-stats-in-java
        long startTotalGarbageCollections = 0;
        long startGarbageCollectionTime = 0;

        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            long count = gc.getCollectionCount();
            long time = gc.getCollectionTime();
            if (count >= 0) startTotalGarbageCollections += count;
            if (time  >= 0) startGarbageCollectionTime += time;
        }
        
		StatisticsHelper.stopMemory();	//XXX multi-threaded???
		long start = System.nanoTime();
		
		if (isUpdate()) {
	        if (graph instanceof AutoTransactionalGraph) {
	        	AutoTransactionalGraph g = (AutoTransactionalGraph) graph;
	        	
	        	if (isUsingCustomTransactions()) {
	        		g.setAutoTransactionControl(false);
	        	}
	        	else {
	        		g.setAutoTransactionControl(true);
		            previousMaxBufferSize = g.getMaxBufferSize();
		            g.setMaxBufferSize(GlobalConfig.transactionBufferSize);
	        	}
	        }
		}
		
		try {
			onExecute();
		}
		finally {

			if (isUpdate()) {
		        if (graph instanceof AutoTransactionalGraph) {
		        	AutoTransactionalGraph g = (AutoTransactionalGraph) graph;
		        	
		        	if (!isUsingCustomTransactions()) {
		        		g.setMaxBufferSize(previousMaxBufferSize);
		        	}
		        }
		        if (graph instanceof TransactionalGraph) {
		        	TransactionalGraph g = (TransactionalGraph) graph;
		        	g.stopTransaction(Conclusion.SUCCESS);
		        }
			}
		}
		
		time = System.nanoTime() - start;
		memory = StatisticsHelper.stopMemory();
		
        long stopTotalGarbageCollections = 0;
        long stopGarbageCollectionTime = 0;

        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            long count = gc.getCollectionCount();
            long time = gc.getCollectionTime();
            if (count >= 0) stopTotalGarbageCollections += count;
            if (time  >= 0) stopGarbageCollectionTime += time;
        }
        
        totalGarbageCollections = stopTotalGarbageCollections - startTotalGarbageCollections;
        garbageCollectionTime = stopGarbageCollectionTime - startGarbageCollectionTime;

		onFinalize();
	}
	
	
	
	/*
	 * Abstract methods and methods that return properties of this operation
	 */

	protected abstract void onInitialize(Object[] args);

	protected abstract void onExecute() throws Exception;
	
	protected void onFinalize() throws Exception {};
	
	public boolean isUpdate() { return false; }
	
	public boolean isUsingCustomTransactions() { return false; }
}