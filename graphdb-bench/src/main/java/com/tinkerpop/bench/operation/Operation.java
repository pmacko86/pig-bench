package com.tinkerpop.bench.operation;

import com.tinkerpop.bench.Bench;
import com.tinkerpop.bench.GlobalConfig;
import com.tinkerpop.bench.GraphDescriptor;
import com.tinkerpop.bench.log.OperationLogWriter;
import com.tinkerpop.bench.operationFactory.OperationFactory;
import com.tinkerpop.bench.operationFactory.factories.WithOpCount;
import com.tinkerpop.bench.util.StatisticsHelper;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.TransactionalGraph;

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
	private OperationLogWriter logWriter = null;
	private OperationFactory factory = null;
	private CPLObject cplObject = null;
	
	public Operation() {
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

	public final int getId() {
		return opId;
	}

	public final Object[] getArgs() {
		return args;
	}

	public final long getTime() {
		return time;
	}

	public final Object getResult() {
		return result;
	}

	protected final GraphDescriptor getGraphDescriptor() {
		return graphDescriptor;
	}

	protected final Graph getGraph() {
		return graphDescriptor.getGraph();
	}

	public final String getName() {
		return name;
	}

	public final String getType() {
		return getClass().getName();
	}
	
	public final long getMemory() {
		return memory;
	}
	
	public final OperationLogWriter getLogWriter() {
		return logWriter;
	}
	
	public final OperationFactory getFactory() {
		return factory;
	}
	
	public CPLObject getCPLObject() {
		if (!CPL.isAttached()) return null;
		if (cplObject != null) return cplObject;
		
		if (factory != null) {
			if (factory.getSharedOperationCPLObject() != null) {
				cplObject = factory.getSharedOperationCPLObject();
				return cplObject;
			}
		}
		
		cplObject = new CPLObject(Bench.ORIGINATOR,
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

	public final void initialize(GraphDescriptor graphDescriptor) {
		this.graphDescriptor = graphDescriptor;
		onInitialize(args);
	}

	public final void execute() throws Exception {
        
		int previousMaxBufferSize = 0;
        Graph graph = getGraph();
        if (graph == null && !(this instanceof OperationDeleteGraph
           		|| this instanceof OperationOpenGraph
           		|| this instanceof OperationShutdownGraph
        		|| this instanceof OperationDoGC)) {
        	throw new IllegalStateException("The graph is null");
        }
        
		StatisticsHelper.stopMemory();	//XXX multi-threaded???
		long start = System.nanoTime();
		
		/* XXX Transactional graphs / batch graphs?
		if (isUpdate() && !isUsingCustomTransactions()) {
		 
	        if (graph instanceof TransactionalGraph) {
	            previousMaxBufferSize = ((TransactionalGraph) graph).getMaxBufferSize();
	            ((TransactionalGraph) graph).setMaxBufferSize(GlobalConfig.transactionBufferSize);
	        }
		}*/
		
		try {
			onExecute();
		}
		finally {
			/* XXX Transactional graphs / batch graphs?
			if (isUpdate() && !isUsingCustomTransactions()) {
		        if (graph instanceof TransactionalGraph) {
		        	((TransactionalGraph) graph).setMaxBufferSize(previousMaxBufferSize);
		        }
			}*/
		}
		
		time = System.nanoTime() - start;
		memory = StatisticsHelper.stopMemory();
		onFinalize();
	}

	protected abstract void onInitialize(Object[] args);

	protected abstract void onExecute() throws Exception;
	
	protected void onFinalize() throws Exception {};
	
	public boolean isUpdate() { return false; }
	
	public boolean isUsingCustomTransactions() { return false; }
}