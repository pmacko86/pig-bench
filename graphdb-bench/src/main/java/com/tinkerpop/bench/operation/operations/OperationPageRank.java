package com.tinkerpop.bench.operation.operations;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.tooling.GlobalGraphOperations;

import com.sparsity.dex.gdb.EdgesDirection;
import com.tinkerpop.bench.analysis.AnalysisContext;
import com.tinkerpop.bench.analysis.OperationModel;
import com.tinkerpop.bench.analysis.Prediction;
import com.tinkerpop.bench.log.OperationLogEntry;
import com.tinkerpop.bench.operation.Operation;
import com.tinkerpop.bench.util.ConsoleUtils;
import com.tinkerpop.bench.util.FileUtils;
import com.tinkerpop.bench.util.GraphUtils;
import com.tinkerpop.bench.util.OutputUtils;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.extensions.BenchmarkableGraph;
import com.tinkerpop.blueprints.extensions.impls.dex.DexUtils;
import com.tinkerpop.blueprints.impls.dex.DexGraph;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;


/**
 * A simple implementation of PageRank
 * 
 * Pseudo-code:
 *   http://combine.it.lth.se/CrawlSim/report/node20.html
 * 
 * @author Peter Macko
 */
public class OperationPageRank extends Operation {
	
	public static boolean outputDetailedStatistics = false;
	
	
	private LinkedHashMap<Vertex, Info> perVertexInfo;
	
	protected int iterations;	/* the number of iterations */
	protected double q;			/* the damping factor */
	
	protected int[] getAllNeighborsRuntimes = null;
	protected int   getAllNeighborsCount = 0;
	
	
	@Override
	protected void onInitialize(Object[] args) {
		
		perVertexInfo = new LinkedHashMap<Vertex, Info>();
		
		iterations    = args.length > 0 ? (Integer) args[0] :  100;
		q             = args.length > 1 ? (Double ) args[1] : 0.15;
		
		if (outputDetailedStatistics) {
			getAllNeighborsRuntimes = new int[(int) ((BenchmarkableGraph) getGraph()).countVertices() * iterations];
		}
		getAllNeighborsCount = 0;
	}
	
	
	@Override
	protected void onExecute() throws Exception {
		
		if (!perVertexInfo.isEmpty()) perVertexInfo.clear();
		int get_ops = 0, get_vertex = 0;
		
		
		// Initialize
		
		Iterable<Vertex> allVertices = getGraph().getVertices();
		for (Vertex v : allVertices) {
			perVertexInfo.put(v, new Info());
		}
		GraphUtils.close(allVertices);
		
		int N = perVertexInfo.size();
		for (Info I : perVertexInfo.values()) {
			I.pagerank = 1.0 / N;
		}
		
		
		// Iterations
		
		for (int i = 0; i < iterations; i++) {
			
			double auxSum = 0;
			
			
			// Compute the next iteration of PageRank and store it as aux 
			
			for (Entry<Vertex, Info> I : perVertexInfo.entrySet()) {
				
				long start = outputDetailedStatistics ? System.nanoTime() : 0;
				
				double a = 0;
				int c = 0;
				Iterable<Vertex> vi = I.getKey().getVertices(Direction.IN);
				for (Vertex v : vi) {
					a += perVertexInfo.get(v).pagerank;
					c++;
				}
				GraphUtils.close(vi);
				if (c > 0) a /= c;
				
				if (outputDetailedStatistics) {
					getAllNeighborsRuntimes[getAllNeighborsCount] = (int) (System.nanoTime() - start);
				}
				getAllNeighborsCount++;
				
				get_ops++;
				get_vertex += c;
				
				double aux = (q / N) + (1.0 - q) * a;
				auxSum += aux;
				I.getValue().aux = aux;  
			}
			
			
			// Store and normalize the ProvRank
			
			for (Info I : perVertexInfo.values()) {
				I.pagerank = I.aux / auxSum;
				I.aux = 0;
			}
		}
		
		
		// Finish
		
		setResult(N /* unique_vertices */ + ":1" /* real_hops */ + ":" + get_ops + ":" + get_vertex
				+ ":" + iterations);
	}

	
	@Override
	protected void onFinalize() {
		
		if (outputDetailedStatistics) {
			assert getAllNeighborsCount == getAllNeighborsRuntimes.length;
			File logFile = getLogWriter().getLogFile();
			if (logFile == null) {
				ConsoleUtils.warn("Logging is disabled, so operation statistics will not be written out either");
			}
			else {
				String n = FileUtils.getBaseName(logFile) + "__OperationPageRank_" + getId()
						+ "__get-all-neighbors-in.txt";
				File f = new File(logFile.getParentFile(), n);
				PrintWriter w;
				try {
					w = new PrintWriter(new BufferedWriter(new FileWriter(f)));
					for (int i = 0; i < getAllNeighborsCount; i++) {
						w.println(getAllNeighborsRuntimes[i]);
					}
					w.close();
					System.err.println("\nOperation statistics written to: "
							+ OutputUtils.simplifyFileName(f.getAbsolutePath()));
				}
				catch (IOException e) {
					ConsoleUtils.warn("Failed to write the operation statistics: " + e.getMessage());
				}
			}
		}
		
		getAllNeighborsRuntimes = null;
		perVertexInfo = null;
	}

	
	protected class Info {
		public double pagerank = 0;
		public double aux = 0; 
	}
	
	
	/**
	 * The operation model
	 */
	public static class Model extends OperationModel {
		
		/**
		 * Create an instance of class Model
		 * 
		 * @param context the analysis context
		 */
		public Model(AnalysisContext context) {
			super(context, OperationPageRank.class);
		}
		
		
		/**
		 * Create prediction(s) based on the specified operation tags
		 * in the specified context
		 * 
		 * @param tag the operation tag(s)
		 * @return a collection of predictions
		 */
		@Override
		public List<Prediction> predictFromTag(String tag) {
			
			ArrayList<Prediction> r = new ArrayList<Prediction>();
			
			Double getAllNeighbors = getContext().getAverageOperationRuntime("OperationGetAllNeighbors-in");
			if (getAllNeighbors != null) {
				
				// Hack
				
				List<OperationLogEntry> entries = getContext().getTailEntries("OperationPageRank");
				
				if (entries != null) {
					int getOpsCount = 0;
					int count = 0;
					
					for (OperationLogEntry e : entries) {
						String[] result = e.getResult().split(":");
						getOpsCount += Integer.parseInt(result[2]);
						count++;
					}
					
					if (count > 0) {
						r.add(new Prediction(this, tag, "Using GetAllNeighbors and results",
								getAllNeighbors.doubleValue() * getOpsCount / (double) count));
					}
				}
			}
			
			return r;
		}
	}

	
	/**
	 * The operation specialized for DEX
	 */
	public static class DEX extends OperationPageRank {
		
		private LinkedHashMap<Long, Info> perVertexInfo;
		private int[] edgeTypes;
		private int[] nodeTypes;
		
		
		@Override
		protected void onInitialize(Object[] args) {
			super.onInitialize(args);
			
			perVertexInfo = new LinkedHashMap<Long, Info>();
			edgeTypes = DexUtils.getEdgeTypes(((DexGraph) getGraph()).getRawGraph());
			nodeTypes = DexUtils.getNodeTypes(((DexGraph) getGraph()).getRawGraph());
		}
		
		
		@Override
		protected void onExecute() throws Exception {
			
			if (!perVertexInfo.isEmpty()) perVertexInfo.clear();
			int get_ops = 0, get_vertex = 0;
			
			com.sparsity.dex.gdb.Graph graph = ((DexGraph) getGraph()).getRawGraph();
			
			
			// Initialize
			
			for (int t : nodeTypes) {
				com.sparsity.dex.gdb.Objects objs = graph.select(t);
				com.sparsity.dex.gdb.ObjectsIterator objsItr = objs.iterator();
						
				while (objsItr.hasNext()) {
					long v = objsItr.nextObject();
					perVertexInfo.put(v, new Info());
				}
						
				objsItr.close();
				objs.close();
			}
			
			int N = perVertexInfo.size();
			for (Info I : perVertexInfo.values()) {
				I.pagerank = 1.0 / N;
			}
			
			
			// Iterations
			
			for (int i = 0; i < iterations; i++) {
				
				double auxSum = 0;
				
				
				// Compute the next iteration of PageRank and store it as aux 
				
				for (Entry<Long, Info> I : perVertexInfo.entrySet()) {
					
					long start = outputDetailedStatistics ? System.nanoTime() : 0;
					
					double a = 0;
					int c = 0;
					Long n = I.getKey();
					for (int t : edgeTypes) {
						com.sparsity.dex.gdb.Objects objs = graph.neighbors(n, t, EdgesDirection.Ingoing);
						com.sparsity.dex.gdb.ObjectsIterator objsItr = objs.iterator();
								
						while (objsItr.hasNext()) {
							long v = objsItr.nextObject();
							a += perVertexInfo.get(v).pagerank;
							c++;
						}
								
						objsItr.close();
						objs.close();
					}
					if (c > 0) a /= c;
					
					if (outputDetailedStatistics) {
						getAllNeighborsRuntimes[getAllNeighborsCount] = (int) (System.nanoTime() - start);
					}
					getAllNeighborsCount++;
					
					get_ops++;
					get_vertex += c;
					
					double aux = (q / N) + (1.0 - q) * a;
					auxSum += aux;
					I.getValue().aux = aux;  
				}
				
				
				// Store and normalize the ProvRank
				
				for (Info I : perVertexInfo.values()) {
					I.pagerank = I.aux / auxSum;
					I.aux = 0;
				}
			}
			
			
			// Finish
			
			setResult(N /* unique_vertices */ + ":1" /* real_hops */ + ":" + get_ops + ":" + get_vertex
					+ ":" + iterations);
		}

		
		@Override
		protected void onFinalize() {
			super.onFinalize();
			
			perVertexInfo = null;
		}
	}
	
	
	/**
	 * The operation specialized for Neo4j
	 */
	public static class Neo extends OperationPageRank {
		
		private LinkedHashMap<Node, Info> perVertexInfo;
		
		
		@Override
		protected void onInitialize(Object[] args) {
			super.onInitialize(args);
			
			perVertexInfo = new LinkedHashMap<Node, Info>();
		}
		
		
		@Override
		protected void onExecute() throws Exception {
			
			if (!perVertexInfo.isEmpty()) perVertexInfo.clear();
			int get_ops = 0, get_vertex = 0;
			
			GraphDatabaseService graph = ((Neo4jGraph) getGraph()).getRawGraph();
			
			
			// Initialize
			
			Iterable<Node> allVertices = GlobalGraphOperations.at(graph).getAllNodes();
			for (Node v : allVertices) {
				perVertexInfo.put(v, new Info());
			}
			
			int N = perVertexInfo.size();
			for (Info I : perVertexInfo.values()) {
				I.pagerank = 1.0 / N;
			}
			
			
			// Iterations
			
			for (int i = 0; i < iterations; i++) {
				
				double auxSum = 0;
				
				
				// Compute the next iteration of PageRank and store it as aux 
				
				for (Entry<Node, Info> I : perVertexInfo.entrySet()) {
					
					long start = outputDetailedStatistics ? System.nanoTime() : 0;
					
					double a = 0;
					int c = 0;
					Node n = I.getKey();
					Iterable<Relationship> ri = n.getRelationships(org.neo4j.graphdb.Direction.INCOMING);
					for (Relationship r : ri) {
						a += perVertexInfo.get(r.getOtherNode(n)).pagerank;
						c++;
					}
					if (c > 0) a /= c;
					
					if (outputDetailedStatistics) {
						getAllNeighborsRuntimes[getAllNeighborsCount] = (int) (System.nanoTime() - start);
					}
					getAllNeighborsCount++;
					
					get_ops++;
					get_vertex += c;
					
					double aux = (q / N) + (1.0 - q) * a;
					auxSum += aux;
					I.getValue().aux = aux;  
				}
				
				
				// Store and normalize the ProvRank
				
				for (Info I : perVertexInfo.values()) {
					I.pagerank = I.aux / auxSum;
					I.aux = 0;
				}
			}
			
			
			// Finish
			
			setResult(N /* unique_vertices */ + ":1" /* real_hops */ + ":" + get_ops + ":" + get_vertex
					+ ":" + iterations);
		}

		
		@Override
		protected void onFinalize() {
			super.onFinalize();
			
			perVertexInfo = null;
		}
	}

}
