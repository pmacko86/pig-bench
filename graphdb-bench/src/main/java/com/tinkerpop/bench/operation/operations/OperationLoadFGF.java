package com.tinkerpop.bench.operation.operations;

import java.io.File;

import com.tinkerpop.bench.GlobalConfig;
import com.tinkerpop.bench.cache.Cache;
import com.tinkerpop.bench.operation.Operation;
import com.tinkerpop.bench.util.ConsoleUtils;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.extensions.impls.dex.DexCSVLoader;
import com.tinkerpop.blueprints.extensions.impls.dex.DexFGFIncrementalLoader;
import com.tinkerpop.blueprints.extensions.impls.neo4j.Neo4jFGFIncrementalLoader;
import com.tinkerpop.blueprints.extensions.impls.neo4j.Neo4jFGFLoader;
import com.tinkerpop.blueprints.extensions.io.GraphProgressListener;
import com.tinkerpop.blueprints.extensions.io.fgf.FGFGraphLoader;
import com.tinkerpop.blueprints.impls.dex.DexGraph;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.blueprints.impls.neo4jbatch.Neo4jBatchGraph;

import edu.harvard.pass.cpl.CPL;
import edu.harvard.pass.cpl.CPLFile;


/**
 * @author Peter Macko (pmacko@eecs.harvard.edu)
 */
public class OperationLoadFGF extends Operation implements GraphProgressListener {

	private File file = null;
	
	private File dexCsvDir = null;
	private String filePrefix = null;
	
	private String lastProgressString = "";
	private long lastProgressTime = 0;
	private long lastProgressObjectCount = 0;
	
	private boolean indexAllProperties = false;
	private boolean bulkLoad = true;
	

	// args
	// -> 0 graphmlDir
	@Override
	protected void onInitialize(Object[] args) {
		
		this.file = new File((String) args[0]);
		if (!file.getName().endsWith(".fgf")) {
			throw new IllegalArgumentException("The file extension has to be .fgf");
		}
		
		bulkLoad = args.length >= 2 ? ((Boolean) args[1]).booleanValue() : true;
		
		
		// DEX: Require a directory of .csv files for its bulkloader
		
		filePrefix = file.getName().substring(0, file.getName().length() - 4);
		dexCsvDir = new File(file.getParentFile(), file.getName() + "-dex-csvs");
	}

	
	@Override
	protected void onExecute() throws Exception {
		Graph graph = getGraph();
		System.out.print(": ");
		
		
		//
		// Neo4j
		//
		
		if (graph instanceof Neo4jGraph || graph instanceof Neo4jBatchGraph) {
			if (bulkLoad) {
				if (!(graph instanceof Neo4jBatchGraph)) {
					throw new IllegalStateException("Bulk-load must be disabled for a Neo4jGraph");
				}
				Neo4jFGFLoader.load((Neo4jBatchGraph) graph, file, indexAllProperties, this);
			}
			else {
				if (!(graph instanceof Neo4jGraph)) {
					throw new IllegalStateException("Bulk-load must be enabled for a Neo4jBatchGraph");
				}
				//FGFGraphLoader.load(graph, file, GlobalConfig.transactionBufferSize, indexAllProperties, bulkLoad, this);
				Neo4jFGFIncrementalLoader.load((Neo4jGraph) graph, file, GlobalConfig.transactionBufferSize,
						                       indexAllProperties, this);
			}
		}
		
		
		//
		// DEX
		//
		
		else if (graph instanceof DexGraph) {
			if (bulkLoad) {
				DexCSVLoader.load(((DexGraph) graph).getRawGraph(), dexCsvDir, filePrefix, indexAllProperties, this);
			}
			else {
				DexFGFIncrementalLoader.load((DexGraph) graph, file, indexAllProperties, this);
			}
		}
		
		
		//
		// Generic
		//
		
		else {
			FGFGraphLoader.load(graph, file, GlobalConfig.transactionBufferSize, indexAllProperties, bulkLoad, this);
		}
		
		
		//
		// Finish
		//
		
		Cache.getInstance(graph).invalidate();
		setResult("DONE-" + (bulkLoad ? "Bulkload" : "Incremental"));
	}

	
	@Override
	protected void onFinalize() throws Exception {
		if (CPL.isAttached()) {
			if (bulkLoad && getGraph() instanceof DexGraph) {
				for (File f : dexCsvDir.listFiles()) {
					if (f.getName().startsWith(filePrefix) && f.getName().endsWith(".csv")) {
						getCPLObject().dataFlowFrom(CPLFile.lookupOrCreate(f));
					}
				}
			}
			else {
				getCPLObject().dataFlowFrom(CPLFile.lookupOrCreate(file));
			}
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
			String s = String.format(" %d vertices, %d edges (%.2f Kops/s)", vertices, edges, d/(double)dt);
			System.out.print(ConsoleUtils.getBackspaces(lastProgressString.length()));
			System.out.print(s);
			lastProgressString = s;
		}
		else {
			String s = String.format(" %d vertices, %d edges (%.2f Kops/s)... ", vertices, edges, d/(double)dt);
			System.out.print(s);
			lastProgressString = s;			
		}
		System.out.flush();
	}
}
