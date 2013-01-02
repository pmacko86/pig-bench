package com.tinkerpop.bench.operation.operations;

import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.tooling.GlobalGraphOperations;

import com.tinkerpop.bench.operation.Operation;
import com.tinkerpop.bench.util.GraphUtils;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
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
	
	private LinkedHashMap<Vertex, Info> perVertexInfo;
	
	protected int iterations;	/* the number of iterations */
	protected double q;			/* the damping factor */
	
	
	@Override
	protected void onInitialize(Object[] args) {
		
		perVertexInfo = new LinkedHashMap<Vertex, Info>();
		
		iterations    = args.length > 0 ? (Integer) args[0] :  100;
		q             = args.length > 1 ? (Double ) args[1] : 0.15;
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
		
		for (int i = 1; i < iterations; i++) {
			
			double auxSum = 0;
			
			
			// Compute the next iteration of PageRank and store it as aux 
			
			for (Entry<Vertex, Info> I : perVertexInfo.entrySet()) {
				
				double a = 0;
				int c = 0;
				Iterable<Vertex> vi = I.getKey().getVertices(Direction.IN);
				for (Vertex v : vi) {
					a += perVertexInfo.get(v).pagerank;
					c++;
				}
				GraphUtils.close(vi);
				if (c > 0) a /= c;
				
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
		perVertexInfo = null;
	}

	
	protected class Info {
		public double pagerank = 0;
		public double aux = 0; 
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
			
			for (int i = 1; i < iterations; i++) {
				
				double auxSum = 0;
				
				
				// Compute the next iteration of PageRank and store it as aux 
				
				for (Entry<Node, Info> I : perVertexInfo.entrySet()) {
					
					double a = 0;
					int c = 0;
					Node n = I.getKey();
					Iterable<Relationship> ri = n.getRelationships(org.neo4j.graphdb.Direction.INCOMING);
					for (Relationship r : ri) {
						a += perVertexInfo.get(r.getOtherNode(n)).pagerank;
						c++;
					}
					if (c > 0) a /= c;
					
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
			perVertexInfo = null;
		}
	}

}
