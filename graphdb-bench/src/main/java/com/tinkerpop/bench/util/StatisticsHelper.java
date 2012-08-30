package com.tinkerpop.bench.util;

import java.util.Random;

import com.tinkerpop.bench.DatabaseEngine;
import com.tinkerpop.bench.cache.Cache;
import com.tinkerpop.bench.evaluators.EdgeEvaluator;
import com.tinkerpop.bench.evaluators.Evaluator;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.extensions.BenchmarkableGraph;


/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Alex Averbuch (alex.averbuch@gmail.com)
 * @author Martin Neumann (m.neumann.1980@gmail.com)
 * @author Peter Macko (pmacko@eecs.harvard.edu)
 */
public class StatisticsHelper {

	private static Random rand = new Random();
	private static long memory = -1l;
	
	
	// Note: The following functions will change the contents of the file system and database caches,
	// so we need to be careful when we do benchmarking

		
	/**
	 * Probability distributions that can be used for picking random vertices
	 */
	public enum VertexDistribution {
		UNIFORM,
		IN_DEGREE,
		OUT_DEGREE,
		DEGREE
	}
	
	
	/**
	 * Randomly pick vertices from the database using uniform distribution with replacement.
	 * 
	 * @param db the graph database
	 * @param sampleSize the number of samples to return
	 * @return an array of vertices
	 */
	public static Vertex[] getRandomVertices(Graph db, int sampleSize) {
		
		if (db instanceof BenchmarkableGraph) {
			BenchmarkableGraph g = (BenchmarkableGraph) db;
			
			Vertex[] samples = new Vertex[sampleSize];
			for (int i = 0; i < samples.length; i++) {
				samples[i] = g.getRandomVertex();
			}
			return samples;
		}
		else {
			throw new IllegalArgumentException("Not a BenchmarkableGraph");
		}
	}

	
	/**
	 * Randomly pick edges from the database using uniform distribution with replacement.
	 * 
	 * @param db the graph database
	 * @param sampleSize the number of samples to return
	 * @return an array of edges
	 */
	public static Edge[] getRandomEdges(Graph db, int sampleSize) {	
		
		if (db instanceof BenchmarkableGraph) {
			BenchmarkableGraph g = (BenchmarkableGraph) db;
				
			Edge[] samples = new Edge[sampleSize];
			for (int i = 0; i < samples.length; i++) {
				samples[i] = g.getRandomEdge();
			}
			return samples;
		}
		else {
			throw new IllegalArgumentException("Not a BenchmarkableGraph");
		}
	}

	
	/**
	 * Randomly pick vertex IDs from the database using uniform distribution with replacement.
	 * 
	 * @param db the graph database
	 * @param sampleSize the number of samples to return
	 * @return an array of vertex IDs
	 */
	public static Object[] getRandomVertexIds(Graph db, int sampleSize) {	
		
		if (db instanceof BenchmarkableGraph) {
			BenchmarkableGraph g = (BenchmarkableGraph) db;
				
			Object[] samples = new Object[sampleSize];
			for (int i = 0; i < samples.length; i++) {
				samples[i] = g.getRandomVertex().getId();
			}
			return samples;
		}
		else {
			throw new IllegalArgumentException("Not a BenchmarkableGraph");
		}
	}

	
	/**
	 * Randomly pick edge IDs from the database using uniform distribution with replacement.
	 * 
	 * @param db the graph database
	 * @param sampleSize the number of samples to return
	 * @return an array of edge IDs
	 */
	public static Object[] getRandomEdgeIds(Graph db, int sampleSize) {	
		
		// Slower alternative:
		//   return getSampleEdgeIds(db, new com.tinkerpop.bench.evaluators.EdgeEvaluatorUniform(), sampleSize);
		
		if (db instanceof BenchmarkableGraph) {
			BenchmarkableGraph g = (BenchmarkableGraph) db;
				
			Object[] samples = new Object[sampleSize];
			for (int i = 0; i < samples.length; i++) {
				samples[i] = g.getRandomEdge().getId();
			}
			return samples;
		}
		else {
			throw new IllegalArgumentException("Not a BenchmarkableGraph");
		}
	}
	
	
	/**
	 * Randomly pick vertices from the database with replacement.
	 * 
	 * @param db the graph database
	 * @param sampleSize the number of samples to return
	 * @param distribution the probability distribution to use
	 * @return an array of vertices
	 */
	public static Vertex[] getRandomVertices(Graph db, int sampleSize, VertexDistribution distribution) {
		
		Edge[] edges;
		Vertex[] samples;
		
		
		switch (distribution) {
		
		case UNIFORM:
			return getRandomVertices(db, sampleSize);
		
		case IN_DEGREE:
			edges = getRandomEdges(db, sampleSize);
			samples = new Vertex[sampleSize];
			for (int i = 0; i < sampleSize; i++) {
				samples[i] = edges[i].getVertex(Direction.IN);
			}
			return samples;
			
		case OUT_DEGREE:
			edges = getRandomEdges(db, sampleSize);
			samples = new Vertex[sampleSize];
			for (int i = 0; i < sampleSize; i++) {
				samples[i] = edges[i].getVertex(Direction.OUT);
			}
			return samples;
			
		case DEGREE:
			edges = getRandomEdges(db, sampleSize);
			samples = new Vertex[sampleSize];
			for (int i = 0; i < sampleSize; i++) {
				samples[i] = rand.nextBoolean() ? edges[i].getVertex(Direction.IN) : edges[i].getVertex(Direction.OUT);
			}
			return samples;
			
		default:
			throw new IllegalArgumentException("Unknown distribution");
		}
	}
	
	
	/**
	 * Randomly pick vertex IDs from the database with replacement.
	 * 
	 * @param db the graph database
	 * @param sampleSize the number of samples to return
	 * @param distribution the probability distribution to use
	 * @return an array of vertex IDs
	 */
	public static Object[] getRandomVertexIds(Graph db, int sampleSize, VertexDistribution distribution) {
		Vertex[] vertices = getRandomVertices(db, sampleSize, distribution);
		Object[] samples = new Object[sampleSize];
		for (int i = 0; i < sampleSize; i++) samples[i] = vertices[i].getId();
		return samples;
	}

	
	/**
	 * Randomly pick vertex IDs from the database without replacement using
	 * a distribution specified by the given evaluator function.
	 * 
	 * @param db the graph database
	 * @param evaluator the vertex evaluator function
	 * @param sampleSize the number of samples to return
	 * @return an array of vertices
	 */
	@Deprecated
	public static Object[] getSampleVertexIds(Graph db, Evaluator evaluator,
			int sampleSize) {

		Object[] samples = new Object[sampleSize];
		if (DatabaseEngine.isHollowGraph(db)) {
			for (int i = 0; i < samples.length; i++) {
				samples[i] = new Long(0);
			}
			return samples;
		}
		
		Double[] sampleVals = new Double[sampleSize];

		double totalVal = evaluator.evaluateTotal(db);

		for (int i = 0; i < sampleVals.length; i++) {
			sampleVals[i] = rand.nextDouble() * totalVal;
			samples[i] = null;
		}

		boolean finished = true;
		Cache cache = Cache.getInstance(db);
		int max = cache.getVertexIndexRange();
		
		for (int index = 0; index < max; index++) {

			Object id = cache.getVertexID(index);
			if (id == null) continue;
			
			double currentVal = evaluator.evaluate(cache, index);

			finished = true;

			for (int i = 0; i < sampleVals.length; i++) {
				if (samples[i] == null) {
					sampleVals[i] -= currentVal;
					if (sampleVals[i] <= 0)
						samples[i] = id;
					else
						finished = false;
				}
			}

			if (finished == true)
				break;
		}
		
		if (!finished) throw new RuntimeException("getSampleVertexIds() did not finish");
		for (int i = 0; i < sampleVals.length; i++) {
			if (samples[i] == null)
				throw new RuntimeException("getSampleVertexIds() did not finish - some elements are null");
		}

		return samples;
	}
	

	/**
	 * Randomly pick edge IDs from the database without replacement using
	 * a distribution specified by the given evaluator function.
	 * 
	 * @param db the graph database
	 * @param evaluator the edge evaluator function
	 * @param sampleSize the number of samples to return
	 * @return an array of vertices
	 */
	@Deprecated
	public static Object[] getSampleEdgeIds(Graph db, EdgeEvaluator evaluator,
			int sampleSize) {

		Object[] samples = new Object[sampleSize];
		Double[] sampleVals = new Double[sampleSize];

		double totalVal = evaluator.evaluateTotal(db);

		for (int i = 0; i < sampleVals.length; i++) {
			sampleVals[i] = rand.nextDouble() * totalVal;
			samples[i] = null;
		}

		boolean finished = true;
		Cache cache = Cache.getInstance(db);
		int max = cache.getEdgeIndexRange();

		for (int index = 0; index < max; index++) {

			Object id = cache.getEdgeID(index);
			if (id == null) continue;
			
			double currentVal = evaluator.evaluate(cache, index);

			finished = true;

			for (int i = 0; i < sampleVals.length; i++) {
				if (samples[i] == null) {
					sampleVals[i] -= currentVal;
					if (sampleVals[i] <= 0)
						samples[i] = id;
					else
						finished = false;
				}
			}

			if (finished == true)
				break;
		}

		return samples;
	}
	

	
	/**
	 * A "stopwatch" for memory usage
	 * 
	 * @return the memory usage
	 */
	public static long stopMemory() {
		final Runtime rt = Runtime.getRuntime();
		if (memory == -1) {
			memory = rt.totalMemory() - rt.freeMemory();
			return memory;
		} else {
			long temp = rt.totalMemory() - rt.freeMemory() - memory;
			memory = 1l;
			return temp;
		}
	}
}
