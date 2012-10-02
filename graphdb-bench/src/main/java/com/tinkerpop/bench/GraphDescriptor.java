package com.tinkerpop.bench;

import java.util.HashMap;
import java.util.Map;

import com.tinkerpop.bench.util.FileUtils;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.extensions.impls.sql.SqlGraph;

import edu.harvard.pass.cpl.CPL;
import edu.harvard.pass.cpl.CPLObject;

public class GraphDescriptor {
	
	/**
	 * Graph open mode
	 */
	public enum OpenMode {
		DEFAULT,
		BULKLOAD
	}

	private DatabaseEngine graphEngine = null;
	private Class<? extends Graph> graphType = null;
	private String graphDir = null;
	private Map<String, String> configuration = new HashMap<String, String>();
	private Graph graph = null;
	private ThreadLocal<Graph> threadLocalGraphs = new ThreadLocal<Graph>();
	private HashMap<Long, Graph> graphsMap = new HashMap<Long, Graph>();
	private CPLObject cplObject = null;
	private boolean threadLocal = GlobalConfig.oneDbConnectionPerThread;
	private OpenMode lastOpenMode = null;

	public GraphDescriptor(DatabaseEngine graphEngine) {
		this(graphEngine, null, null, false);
	}

	public GraphDescriptor(DatabaseEngine graphEngine, String graphDir, Map<String, String> configuration) {
		this(graphEngine, graphDir, configuration, false);
	}

	public GraphDescriptor(DatabaseEngine graphEngine, String graphDir, Map<String, String> configuration, boolean isNew) {
		this.graphEngine = graphEngine;
		this.graphType = graphEngine.getBlueprintsClass();
		this.graphDir = graphDir;
		this.configuration.putAll(configuration);
		
		if (CPL.isAttached()) {
			if (isNew) {
				cplObject = CPLObject.create(Bench.ORIGINATOR, getCPLObjectName(), Bench.TYPE_DB);
			}
			else {
				cplObject = CPLObject.lookupOrCreate(Bench.ORIGINATOR, getCPLObjectName(), Bench.TYPE_DB);
			}
			if (cplObject.getVersion() == 0) initializeCPLObject();
		}
	}

	//
	// Getter Methods
	// 

	public Class<? extends Graph> getGraphType() {
		return graphType;
	}

	public DatabaseEngine getDatabaseEngine() {
		return graphEngine;
	}

	public Graph getGraph() {
		return threadLocal ? threadLocalGraphs.get() : graph;
	}
	
	public CPLObject getCPLObject() {
		return cplObject;
	}
	

	//
	// Functionality
	//

	public Graph openGraph(OpenMode mode) throws Exception {
		
		if (lastOpenMode != null) {
			if (!mode.equals(lastOpenMode)) {
				throw new IllegalStateException("Cannot reopen the graph using a different mode");
			}
		}
		
		Graph g = getGraph();
		if (null != g) return g;
		
		switch (mode) {
		case DEFAULT : g = graphEngine.newInstance(graphDir, configuration); break;
		case BULKLOAD: g = graphEngine.newInstanceForBulkload(graphDir, configuration); break;
		default      : throw new IllegalArgumentException("Invalid graph open mode");
		}
		
		synchronized (this) {
			lastOpenMode = mode;
			if (graph == null) graph = g;
		}
		
		if (threadLocal) {
			threadLocalGraphs.set(g);
			graphsMap.put(Thread.currentThread().getId(), g);
		}

		//if (TransactionalGraph.class.isAssignableFrom(graphType))
		//	((TransactionalGraph) g).setTransactionMode(TransactionalGraph.Mode.AUTOMATIC);

		return g;
	}
	
	private void setGraphToNull() {
		if (threadLocal) {
			threadLocalGraphs.remove();
			graphsMap.remove(Thread.currentThread().getId());
			synchronized (this) {
				if (graphsMap.isEmpty())
					graph = null;
				else
					graph = graphsMap.values().iterator().next();
			}
		}
		else {
			graph = null;
		}		
	}

	public void shutdownGraph() {
		Graph g = getGraph();
		if (null != g) {
			g.shutdown();
			setGraphToNull();
		}
	}

	public synchronized void deleteGraph() {
		
		// Must be single-threaded!!!
		
		if (graphType == SqlGraph.class) {
			((SqlGraph) graph).delete();
			setGraphToNull();
		}
		else {
			shutdownGraph();
			
			if (getDatabaseEngine().isPersistent()) {
				FileUtils.deleteDir(graphDir);
			}
		}
		
		try {
			openGraph(lastOpenMode == null ? OpenMode.DEFAULT : lastOpenMode);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		recreateCPLObject();
	}
	
	protected String getCPLObjectName() {
		String name = graphType.getSimpleName();
		if (graphDir != null) name += " " + graphDir;
		return name;
	}

	public void recreateCPLObject() {
		if (CPL.isAttached()) {
			cplObject = CPLObject.create(Bench.ORIGINATOR, getCPLObjectName(), Bench.TYPE_DB);
			initializeCPLObject();
		}
	}
	
	private void initializeCPLObject() {
		cplObject.addProperty("CLASS", "" + graphType);
		if (graphDir != null) cplObject.addProperty("DIR", graphDir);
		for (Map.Entry<String, String> e : configuration.entrySet()) {
			cplObject.addProperty(e.getKey(), e.getValue());
		}
	}
}
