package com.tinkerpop.bench.generator;

import com.tinkerpop.bench.Bench;
import com.tinkerpop.bench.cache.Cache;
import com.tinkerpop.bench.util.ConsoleUtils;
import com.tinkerpop.bench.util.GraphUtils;
import com.tinkerpop.bench.util.Pair;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.extensions.BenchmarkableGraph;

import edu.harvard.pass.cpl.CPL;
import edu.harvard.pass.cpl.CPLObject;


/**
 * A simple Barabasi graph generator for the common case with power=1
 *
 * @author Peter Macko <pmacko@eecs.harvard.edu>
 */
public class SimpleBarabasiGenerator extends GraphGenerator {

	protected int N;
	protected int M;
	protected CPLObject cplObject = null;
	
	private int num_addEdge;
	private double time_addEdges;
	private int num_getVertex;
	private double time_addVertices;
	private int num_addVertex;
	private double time_getVertices;


	/**
	 * Create an instance of the SimpleBarabasiGenerator
	 *
	 * @param N the number of vertices to generate
	 * @param M the degree of the generated vertices
	 */
	public SimpleBarabasiGenerator(int N, int M) {
		this.N = N;
		this.M = M;
	}


	/**
	 * Generate the graph
	 *
	 * @param inputGraph the graph
	 * @see com.tinkerpop.bench.generator.GraphGenerator#generate()
	 */
	@Override
	public void generate(Graph inputGraph) {
		
		if (!(inputGraph instanceof BenchmarkableGraph)) {
			throw new IllegalArgumentException("Not a BenchmarkableGraph");
		}
		
		BenchmarkableGraph graph = (BenchmarkableGraph) inputGraph;
		
		
		// Initialize
		
		int n = N;
		int m = M;
		int zeroAppeal = 8;
		
		Object[] newVertices = new Object[n];
		@SuppressWarnings({ "rawtypes" })
		Pair[] newEdges = new Pair[n * m];
		TemporaryObject[] newEdgeIDs = new TemporaryObject[n * m];
		
		
		// Get the number of existing vertices and edges
		
		long numVertices = graph.countVertices();
		long numEdges = graph.countEdges();
		
		
		// Schedule an initial vertex to be created if the graph is empty
		
		boolean empty = true;
		Iterable<Vertex> vi = graph.getVertices();
		for (@SuppressWarnings("unused") Vertex v : vi) {
			empty = false;
			break;
		}
		GraphUtils.close(vi);
		
		Cache c = Cache.getInstance(graph);
		
		if (empty) {
			Object v = new TemporaryObject(0);
			newVertices[0] = v;
			c.addVertexID(v);
		}

		
		// Plan the creation of vertices and edges
		
		Object[] otherVertices = new Object[m];
		int edge_i = 0;
		for (int i = empty ? 1 : 0; i < n; i++) {
			
			// Get an array of m vertices to connect to, weigh by degree + zero appeal
			
			// Original code:
			// Evaluator evaluator = new EvaluatorDegree(1, 8);
			// otherVertices = StatisticsHelper
			// 		.getSampleVertexIds(graph, evaluator, m);
			
			for (int j = 0; j < m; j++) {
				
				// Account for the zero-appeal parameter
				
				long zeroAppealPts = (numVertices + i) * zeroAppeal;
				long totalPts = zeroAppealPts + 2 * numEdges + 2 * edge_i;
				long r = (long) (Math.random() * totalPts);
				
				if (r < zeroAppealPts) {
					if (r < i * zeroAppeal) {
						otherVertices[j] = newVertices[(int) (r / zeroAppeal)];
					}
					else {
						otherVertices[j] = graph.getRandomVertex().getId();
					}
				}
				else {
					if (r - zeroAppealPts < 2 * edge_i) {
						long x = r - zeroAppealPts;
						@SuppressWarnings("unchecked")
						Pair<Object, Object> p = newEdges[(int) (x / 2)];
						otherVertices[j] = (x & 1) == 0 ? p.getFirst() : p.getSecond();
					}
					else {
						Edge e = graph.getRandomEdge();
						otherVertices[j] = ((r & 1) == 0 ? e.getVertex(Direction.IN) : e.getVertex(Direction.OUT)).getId();
					}
				}
			}
			
			
			// Create the new vertex and the appropriate edges
			
			Object v = new TemporaryObject(i);
			newVertices[i] = v;
			c.addVertexID(v);
			
			for (Object o : otherVertices) {
				Pair<Object, Object> e = new Pair<Object, Object>(o /* out/source */, v /* in/target */);
				TemporaryObject t = new TemporaryObject(edge_i);
				newEdges[edge_i] = e;
				newEdgeIDs[edge_i] = t;
				c.addEdgeByID(t, v, o);
				edge_i++;
			}
			
			
			if ((i & 15) == 0 || i == n-1) ConsoleUtils.printProgressIndicator(i+1, n, "Pass 1/4");
		}
		
		
		// Now actually create the vertices
		
		Object[] oldVertices = newVertices;
		newVertices = new Object[n];

		long start = System.nanoTime(); 
		
		for (int i = 0; i < n; i++) {
			Vertex v = graph.addVertex(null);
			newVertices[i] = v;
			
			if ((i & 1023) == 0 || i == n-1) ConsoleUtils.printProgressIndicator(i+1, n, "Pass 2/4");
		}
		
		time_addVertices = System.nanoTime() - start;
		num_addVertex = newVertices.length;
		
		for (int i = 0; i < n; i++) {
			c.replaceVertexID(oldVertices[i], ((Vertex) newVertices[i]).getId());
		}

		
		// Now actually get the other vertices
		
		start = System.nanoTime();
		num_getVertex = 0;

		for (int i = 0; i < newEdges.length; i++) {
			Object a = newEdges[i].getFirst();
			Object b = newEdges[i].getSecond();
			
			if (a instanceof TemporaryObject) {
				a = newVertices[((TemporaryObject) a).value];
			}
			else {
				a = graph.getVertex(a);
				num_getVertex++;
			}
			
			if (b instanceof TemporaryObject) {
				b = newVertices[((TemporaryObject) b).value];
			}
			else {
				b = graph.getVertex(b);
				num_getVertex++;
			}
			
			newEdges[i] = new Pair<Object, Object>(a, b);
			
			if ((i & 255) == 0 || i == newEdges.length-1) {
				ConsoleUtils.printProgressIndicator(i+1, newEdges.length, "Pass 3/4");
			}
		}
		
		time_getVertices = System.nanoTime() - start;

		
		// Now actually create the edges
		
		start = System.nanoTime(); 
		
		for (int i = 0; i < newEdges.length; i++) {
			Object a = newEdges[i].getFirst();
			Object b = newEdges[i].getSecond();
			
			Edge e = graph.addEdge(null, (Vertex) a, (Vertex) b, "");
			c.replaceEdgeID(newEdgeIDs[i], e.getId());
			
			if ((i & 127) == 0 || i == newEdges.length-1) {
				ConsoleUtils.printProgressIndicator(i+1, newEdges.length, "Pass 4/4");
			}
		}
		
		time_addEdges = System.nanoTime() - start;
		num_addEdge = newEdges.length;
	}
	
	
	/**
	 * Return (or create) the generator's CPL object
	 * 
	 * @return the CPL object
	 */
	public CPLObject getCPLObject() {
		
		if (cplObject != null) return cplObject;
		if (!CPL.isAttached()) return null;
		
		cplObject = new CPLObject(Bench.ORIGINATOR,
				getClass().getCanonicalName() + " N=" + N + " M=" + M,
				Bench.TYPE_OPERATION);
		cplObject.addProperty("N", "" + N);
		cplObject.addProperty("M", "" + M);
		
		return cplObject;
	}
	
	
	/**
	 * Safe division
	 */
	private double safediv(double a, int b) {
		return b == 0 ? 0 : a / b;
	}
	
	
	/**
	 * Return a colon-separated (:) list of key-value pairs with additional
	 * statistics about the last graph generation process
	 * 
	 * @return the statistics string
	 */
	public String getStatisticsString() {		
		return     "addVertex=" + num_addVertex + ":addVertex_ns=" + safediv(time_addVertices, num_addVertex)
				+ ":getVertex=" + num_getVertex + ":getVertex_ns=" + safediv(time_getVertices, num_getVertex)
				+ ":addEdge="   + num_addEdge   + ":addEdge_ns="   + safediv(time_addEdges   , num_addEdge  );
	}

	
	/**
	 * A temporary object
	 */
	private class TemporaryObject {
		
		public Integer value;
		
		/**
		 * Create an instance of class TemporaryObject
		 * 
		 * @param value the value
		 */
		public TemporaryObject(int value) {
			this.value = value;
		}
		
		/**
		 * Generate hash code
		 * 
		 * @return the hash code
		 */
		@Override
		public int hashCode() {
			return value.hashCode();
		}
		
		/**
		 * Determine if two objects are equal
		 * 
		 * @param other the other object
		 * @return true if they are
		 */
		@Override
		public boolean equals(Object other) {
			if (other instanceof TemporaryObject) {
				return value.equals(((TemporaryObject) other).value);
			}
			else {
				return false;
			}
		}
	}
}
