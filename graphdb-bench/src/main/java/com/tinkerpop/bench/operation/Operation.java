package com.tinkerpop.bench.operation;

import java.lang.management.CompilationMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;

import com.tinkerpop.bench.Bench;
import com.tinkerpop.bench.GlobalConfig;
import com.tinkerpop.bench.GraphDescriptor;
import com.tinkerpop.bench.operationFactory.OperationFactory;
import com.tinkerpop.bench.operationFactory.factories.WithOpCount;
import com.tinkerpop.bench.util.IOStat;
import com.tinkerpop.bench.util.StatisticsHelper;
import com.tinkerpop.bench.util.IOStat.DeviceStat;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.TransactionalGraph.Conclusion;
import com.tinkerpop.blueprints.extensions.AutoTransactionalGraph;
import com.tinkerpop.blueprints.extensions.BenchmarkableGraph;
import com.tinkerpop.blueprints.extensions.impls.neo4j.ExtendedNeo4jGraph;

import edu.harvard.pass.cpl.CPL;
import edu.harvard.pass.cpl.CPLObject;

/**
 * @author Alex Averbuch (alex.averbuch@gmail.com)
 * @author Martin Neumann (m.neumann.1980@gmail.com)
 * @author Peter Macko (pmacko@gmail.com)
 */
public abstract class Operation {

	private int opId = -1;
	private Object[] args = null;
	private long time = -1;
	private Object result = null;
	private String name = null;
	private long memory = -1;
	private int totalGarbageCollections = 0;
	private int garbageCollectionTime = 0;
	private OperationFactory factory = null;
	private CPLObject cplObject = null;
	private int kbRead = -1;
	private int kbWritten = -1;
	private int cacheHits = -1;
	private int cacheMisses = -1;


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
		return factory.getGraphDescriptor();
	}


	/**
	 * Return the graph
	 * 
	 * @return the graph
	 */
	protected final Graph getGraph() {
		return factory.getGraphDescriptor().getGraph();
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
		String s = getClass().getName();
		if (s.startsWith("com.tinkerpop.bench.operation.")) s = s.substring("com.tinkerpop.bench.operation.".length());
		if (s.startsWith("operations.")) s = s.substring("operations.".length());
		return s;
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
	public final int getGCCount() {
		return totalGarbageCollections;
	}


	/**
	 * Return the approximate time spent in garbage collection while the operation was running
	 * 
	 * @return the time in ms
	 */
	public final int getGCTimeMS() {
		return garbageCollectionTime;
	}


	/**
	 * Return the number of KB read
	 * 
	 * @return the number of KB read
	 */
	public final int getKbRead() {
		return kbRead;
	}


	/**
	 * Return the number of KB written
	 * 
	 * @return the number of KB written
	 */
	public final int getKbWritten() {
		return kbWritten;
	}

	
	/**
	 * Get the number of cache hits
	 * 
	 * @return the number of cache hits, or -1 if unavailable
	 */
	public final int getCacheHits() {
		return cacheHits;
	}

	
	/**
	 * Get the number of cache misses
	 * 
	 * @return the number of cache misses, or -1 if unavailable
	 */
	public final int getCacheMisses() {
		return cacheMisses;
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
		if (factory.getGraphDescriptor() != graphDescriptor) {
			throw new IllegalArgumentException();
		}
		onInitialize(args);
	}


	/**
	 * Execute the operation
	 * 
	 * @throws Exception on error
	 */
	public final void execute() throws Exception {

		int previousMaxBufferSize = 0;
		BenchmarkableGraph graph = (BenchmarkableGraph) getGraph();
		if (graph == null && !(this instanceof OperationDeleteGraph
				|| this instanceof OperationOpenGraph
				|| this instanceof OperationShutdownGraph
				|| this instanceof OperationDoGC)) {
			throw new IllegalStateException("The graph is null");
		}

		onDelayedInitialize();

		// XXX Concurrency?

		IOStat iostatBefore = getFactory().getBenchmark().isCapturingIostat()
				&& getFactory().getGraphDescriptor().getMountPoint() != null ? IOStat.run() : null;
		
		long cacheHitsStart   = graph == null ? -1 : graph.getCacheHitCount();
		long cacheMissesStart = graph == null ? -1 : graph.getCacheMissCount();

		// http://stackoverflow.com/questions/466878/can-you-get-basic-gc-stats-in-java
		long startTotalGarbageCollections = 0;
		long startGarbageCollectionTime = 0;

		for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
			long count = gc.getCollectionCount();
			long time = gc.getCollectionTime();
			if (count >= 0) startTotalGarbageCollections += count;
			if (time  >= 0) startGarbageCollectionTime += time;
		}

		long startCompilationTime = 0;
		if (true) {
			CompilationMXBean c = ManagementFactory.getCompilationMXBean();
			long time = c.getTotalCompilationTime();
			if (time  >= 0) startCompilationTime += time;
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
			if (graph instanceof ExtendedNeo4jGraph) {
				if (!isUsingCustomTransactions()) {
					((ExtendedNeo4jGraph) graph).startTransaction();
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
					// Make sure the transaction completes even if the operation
					// uses custom transactions (in which case the following call
							// should translate to a NO-OP, assuming that there is no
					// running transaction).
					TransactionalGraph g = (TransactionalGraph) graph;
					g.stopTransaction(Conclusion.SUCCESS);
				}
			}
		}

		time = System.nanoTime() - start;
		memory = StatisticsHelper.stopMemory();

		long stopCompilationTime = 0;
		if (true) {
			CompilationMXBean c = ManagementFactory.getCompilationMXBean();
			long time = c.getTotalCompilationTime();
			if (time  >= 0) stopCompilationTime += time;
		}

		long stopTotalGarbageCollections = 0;
		long stopGarbageCollectionTime = 0;

		for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
			long count = gc.getCollectionCount();
			long time = gc.getCollectionTime();
			if (count >= 0) stopTotalGarbageCollections += count;
			if (time  >= 0) stopGarbageCollectionTime += time;
		}

		totalGarbageCollections = (int) (stopTotalGarbageCollections - startTotalGarbageCollections);
		garbageCollectionTime = (int) (stopGarbageCollectionTime - startGarbageCollectionTime);
		
		if (cacheHitsStart >= 0 && cacheMissesStart >= 0) {
			cacheHits   = (int) (graph.getCacheHitCount()  - cacheHitsStart);
			cacheMisses = (int) (graph.getCacheMissCount() - cacheMissesStart);
		}

		if (iostatBefore != null) {
			IOStat iostatAfter = IOStat.run();
			String device = getFactory().getGraphDescriptor().getMountPoint().getDevice();
			DeviceStat devstatAfter  = iostatAfter.getDeviceStat(device);
			DeviceStat devstatBefore = iostatBefore.getDeviceStat(device);
			kbRead = (int) (devstatAfter.getkB_read() - devstatBefore.getkB_read());
			kbWritten = (int) (devstatAfter.getkB_wrtn() - devstatBefore.getkB_wrtn());
		}
		else {
			kbRead = -1;
			kbWritten = -1;
		}

		@SuppressWarnings("unused")
		int ct = (int) (stopCompilationTime - startCompilationTime);
		//if (ct > 0) System.err.println("\nRecompilation: " + ct + " ms");

		onFinalize();
	}



	/*
	 * Abstract methods and methods that return properties of this operation
	 */

	protected abstract void onInitialize(Object[] args);

	protected abstract void onExecute() throws Exception;

	protected void onDelayedInitialize() throws Exception {};

	protected void onFinalize() throws Exception {};

	public boolean isUpdate() { return false; }

	public boolean isUsingCustomTransactions() { return false; }
}