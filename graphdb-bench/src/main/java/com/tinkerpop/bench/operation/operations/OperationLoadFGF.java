package com.tinkerpop.bench.operation.operations;

import java.io.File;

import com.tinkerpop.bench.GlobalConfig;
import com.tinkerpop.bench.cache.Cache;
import com.tinkerpop.bench.operation.Operation;
import com.tinkerpop.bench.util.ConsoleUtils;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.extensions.impls.neo4j.Neo4jFGFLoader;
import com.tinkerpop.blueprints.extensions.io.GraphProgressListener;
import com.tinkerpop.blueprints.extensions.io.fgf.FGFGraphLoader;
import com.tinkerpop.blueprints.impls.neo4jbatch.Neo4jBatchGraph;

import edu.harvard.pass.cpl.CPL;
import edu.harvard.pass.cpl.CPLFile;


/**
 * @author Peter Macko (pmacko@eecs.harvard.edu)
 */
public class OperationLoadFGF extends Operation implements GraphProgressListener {

	private File file = null;
	private String lastProgressString = "";
	private long lastProgressTime = 0;
	private long lastProgressObjectCount = 0;
	

	// args
	// -> 0 graphmlDir
	@Override
	protected void onInitialize(Object[] args) {
		this.file = new File((String) args[0]);
	}

	@Override
	protected void onExecute() throws Exception {
		Graph graph = getGraph();
		try {
			System.out.print(": ");
			
			if (graph instanceof Neo4jBatchGraph)
				Neo4jFGFLoader.load(((Neo4jBatchGraph) graph).getRawGraph(), file, this);
			else
				FGFGraphLoader.load(graph, file, GlobalConfig.transactionBufferSize, this);
			
			Cache.getInstance(graph).invalidate();
			setResult("DONE");
		} catch (Exception e) {
			throw e;
		}
	}

	@Override
	protected void onFinalize() throws Exception {
		if (CPL.isAttached()) {
			getCPLObject().dataFlowFrom(CPLFile.lookupOrCreate(file));
			getGraphDescriptor().getCPLObject().dataFlowFrom(getCPLObject());
		}
	}

	@Override
	public boolean isUpdate() {
		return true;
	}
	
	@Override
	public boolean isUsingCustomTransactions() {
		return true;
	}

	
	/**
	 * Callback for when the given number of vertices and edges were loaded
	 * 
	 * @param graph the graph loaded so far
	 * @param vertices the number of vertices loaded so far
	 * @param edges the number of edges
	 */
	@Override
	public void graphProgress(int vertices, int edges) {
		
		long t = System.currentTimeMillis();
		long dt = t - lastProgressTime;
		if (dt < (ConsoleUtils.useEscapeSequences ? 250 : 2000)) {
			return;
		}
		lastProgressTime = t;
		long objects = vertices + edges;
		long d = objects - lastProgressObjectCount; 
		lastProgressObjectCount = objects;
		
		if (ConsoleUtils.useEscapeSequences) {
			String s = String.format(" %d vertices, %d edges (%.2f ops/s)", vertices, edges, d/(dt/1000.0));
			System.out.print(ConsoleUtils.getBackspaces(lastProgressString.length()));
			System.out.print(s);
			lastProgressString = s;
		}
		else {
			String s = String.format(" %d vertices, %d edges (%.2f ops/s)... ", vertices, edges, d/(dt/1000.0));
			System.out.print(s);
			lastProgressString = s;			
		}
		System.out.flush();
	}
}
