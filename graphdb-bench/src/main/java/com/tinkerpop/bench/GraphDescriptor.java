package com.tinkerpop.bench;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.tinkerpop.bench.util.FileUtils;
import com.tinkerpop.bench.util.MountInfo;
import com.tinkerpop.bench.util.MountInfo.MountInfoRecord;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.extensions.BenchmarkableGraph;
import com.tinkerpop.blueprints.extensions.impls.sql.SqlGraph;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.blueprints.impls.neo4jbatch.Neo4jBatchGraph;

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
	private File graphDir = null;
	private MountInfoRecord graphDirMountPoint = null;
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

	public GraphDescriptor(DatabaseEngine graphEngine, File graphDir, Map<String, String> configuration) {
		this(graphEngine, graphDir, configuration, false);
	}

	public GraphDescriptor(DatabaseEngine graphEngine, File graphDir, Map<String, String> configuration, boolean isNew) {
		this.graphEngine = graphEngine;
		this.graphType = graphEngine.getBlueprintsClass();
		this.graphDir = graphDir;
		try {
			this.graphDirMountPoint = graphDir == null ? null : MountInfo.run().getRecordForFile(graphDir);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
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
	
	public File getDirectory() {
		return graphDir;
	}
	
	public MountInfoRecord getMountPoint() {
		return graphDirMountPoint;
	}
	
	public Map<String, String> getConfiguration() {
		return configuration;
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
		
		
		// Create a new instance of the graph
		
		switch (mode) {
		case DEFAULT : g = graphEngine.newInstance(graphDir.getAbsolutePath(), configuration); break;
		case BULKLOAD: g = graphEngine.newInstanceForBulkload(graphDir.getAbsolutePath(), configuration); break;
		default      : throw new IllegalArgumentException("Invalid graph open mode");
		}
		
		
		// Checks
		
		if (!(g instanceof BenchmarkableGraph)) {
			g.shutdown();
			throw new RuntimeException("The graph is not an instance of BenchmarkableGraph");
		}
		
		if (!(g instanceof Neo4jGraph) && !(g instanceof Neo4jBatchGraph)) {
			int cacheSize = ((BenchmarkableGraph) g).getTotalCacheSize();
			if (cacheSize != GlobalConfig.databaseCacheSize) {
				g.shutdown();
				throw new RuntimeException("The graph database does not have the correct cache size: it has "
						+ cacheSize + " MB, but " + GlobalConfig.databaseCacheSize + " MB is expected");
			}
		}
		
		
		// Internal house-keeping
		
		synchronized (this) {
			lastOpenMode = mode;
			if (graph == null) graph = g;
		}
		
		if (threadLocal) {
			threadLocalGraphs.set(g);
			graphsMap.put(Thread.currentThread().getId(), g);
		}
		
		
		// Finish

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
		if (graphDir != null) cplObject.addProperty("DIR", graphDir.getAbsolutePath());
		for (Map.Entry<String, String> e : configuration.entrySet()) {
			cplObject.addProperty(e.getKey(), e.getValue());
		}
	}
}
