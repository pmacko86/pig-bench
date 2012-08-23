package com.tinkerpop.bench;

import java.lang.reflect.Constructor;
import java.util.HashMap;

import com.tinkerpop.blueprints.Graph;
//import com.tinkerpop.blueprints.impls.dex.DexGraph;
import com.tinkerpop.blueprints.extensions.impls.sql.SqlGraph;

import edu.harvard.pass.cpl.CPL;
import edu.harvard.pass.cpl.CPLObject;

public class GraphDescriptor {

	private Class<?> graphType = null;
	private String graphDir = null;
	private String graphPath = null;
	private Graph graph = null;
	private ThreadLocal<Graph> threadLocalGraphs = new ThreadLocal<Graph>();
	private HashMap<Long, Graph> graphsMap = new HashMap<Long, Graph>();
	private CPLObject cplObject = null;
	private boolean threadLocal = GlobalConfig.oneDbConnectionPerThread;

	public GraphDescriptor(Class<?> graphType) {
		this(graphType, null, null);
	}

	public GraphDescriptor(Class<?> graphType, String graphDir, String graphPath) {
		this.graphType = graphType;
		this.graphDir = graphDir;
		this.graphPath = graphPath;
		
		if (CPL.isAttached()) {
			String name = graphType.getSimpleName();
			if (graphPath != null) name += " " + graphPath;
			cplObject = CPLObject.lookupOrCreate(Bench.ORIGINATOR, name, Bench.TYPE_DB);
			if (cplObject.getVersion() == 0) initializeCPLObject();
		}
	}

	//
	// Getter Methods
	// 

	public Class<?> getGraphType() {
		return graphType;
	}

	public Graph getGraph() {
		return threadLocal ? threadLocalGraphs.get() : graph;
	}

	public boolean getPersistent() {
		return graphPath != null;
	}
	
	public CPLObject getCPLObject() {
		return cplObject;
	}
	

	//
	// Functionality
	//

	public Graph openGraph() throws Exception {
		
		Graph g = getGraph();
		if (null != g) return g;
		
		Object[] args = (null == graphPath) ? new Object[] {}
				: new Object[] { graphPath };

		Constructor<?> graphConstructor = (null == graphPath) ? Class.forName(
				graphType.getName()).getConstructor() : Class.forName(
				graphType.getName()).getConstructor(String.class);

		try {
			g = (Graph) graphConstructor.newInstance(args);
		} catch (Exception e) {
			throw e;
		}
		
		synchronized (this) {
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
		
		// XXX Must be single-threaded!!!
		
		if (graphType == SqlGraph.class) {
			((SqlGraph) graph).delete();
			setGraphToNull();
		}
		else {
			shutdownGraph();
			
			if (true == getPersistent()) {
				deleteDir(graphDir);
			}
		}
		
		try {
			openGraph();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		recreateCPLObject();
	}

	private void deleteDir(String pathStr) {
		LogUtils.deleteDir(pathStr);
	}

	public void recreateCPLObject() {
		if (CPL.isAttached()) {
			String name = graphType.getSimpleName();
			if (graphPath != null) name += " " + graphPath;
			cplObject = new CPLObject(Bench.ORIGINATOR, name, Bench.TYPE_DB);
			initializeCPLObject();
		}
	}
	
	private void initializeCPLObject() {
		cplObject.addProperty("CLASS", "" + graphType);
		if (graphDir != null) cplObject.addProperty("DIR", graphDir);
		if (graphPath != null) cplObject.addProperty("PATH", graphPath);
	}
}
