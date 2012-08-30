package com.tinkerpop.bench.operation.operations;

import java.io.File;
import java.io.FileInputStream;

import com.tinkerpop.bench.GlobalConfig;
import com.tinkerpop.bench.cache.Cache;
import com.tinkerpop.bench.operation.Operation;
import com.tinkerpop.bench.util.ConsoleUtils;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.extensions.graphml.FastGraphMLReader;
import com.tinkerpop.blueprints.extensions.graphml.FastGraphMLReaderProgressListener;

import edu.harvard.pass.cpl.CPL;
import edu.harvard.pass.cpl.CPLFile;

/**
 * @author Alex Averbuch (alex.averbuch@gmail.com)
 * @author Peter Macko (pmacko@eecs.harvard.edu)
 */
public class OperationLoadGraphML extends Operation implements FastGraphMLReaderProgressListener {

	private String graphmlPath = null;
	private String lastProgressString = "";
	private long lastProgressTime = 0;
	private boolean ingestAsUndirected;
	

	// args
	// -> 0 graphmlDir
	@Override
	protected void onInitialize(Object[] args) {
		this.graphmlPath = (String) args[0];
		this.ingestAsUndirected = ((Boolean) args[1]).booleanValue();
	}

	@Override
	protected void onExecute() throws Exception {
		Graph graph = getGraph();
		try {
			System.out.print(": ");
			FastGraphMLReader.inputGraph(graph, new FileInputStream(
					graphmlPath), GlobalConfig.transactionBufferSize,
					null, null, null, this, ingestAsUndirected);
			Cache.getInstance(getGraph()).invalidate();
			setResult("DONE");
		} catch (Exception e) {
			throw e;
		}
	}

	@Override
	protected void onFinalize() throws Exception {
		if (CPL.isAttached()) {
			getCPLObject().dataFlowFrom(CPLFile.lookupOrCreate(new File(graphmlPath)));
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
	public void inputGraphProgress(final Graph graph, int vertices, int edges) {
		
		long t = System.currentTimeMillis();
		long dt = t - lastProgressTime;
		if (dt < (ConsoleUtils.useEscapeSequences ? 250 : 2000)) {
			return;
		}
		lastProgressTime = t;
		
		if (ConsoleUtils.useEscapeSequences) {
			String s = "" + vertices + " vertices, " + edges + " edges";
			System.out.print(ConsoleUtils.getBackspaces(lastProgressString.length()));
			System.out.print(s);
			lastProgressString = s;
		}
		else {
			String s = "" + vertices + " vertices, " + edges + " edges... ";
			System.out.print(s);
			lastProgressString = s;			
		}
		System.out.flush();
	}
}
