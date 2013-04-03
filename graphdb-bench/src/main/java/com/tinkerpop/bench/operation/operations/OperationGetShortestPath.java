package com.tinkerpop.bench.operation.operations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.kernel.Traversal;

import com.sparsity.dex.gdb.EdgesDirection;
import com.sparsity.dex.gdb.Graph;
import com.tinkerpop.bench.analysis.AnalysisContext;
import com.tinkerpop.bench.analysis.OperationModel;
import com.tinkerpop.bench.analysis.OperationStats;
import com.tinkerpop.bench.analysis.Prediction;
import com.tinkerpop.bench.operation.Operation;
import com.tinkerpop.bench.util.GraphUtils;
import com.tinkerpop.bench.util.LongComparator;
import com.tinkerpop.bench.util.LongIntHashMap;
import com.tinkerpop.bench.util.LongLongHashMap;
import com.tinkerpop.bench.util.LongPriorityHashQueue;
import com.tinkerpop.bench.util.MathUtils;
import com.tinkerpop.bench.util.PriorityHashQueue;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.extensions.impls.dex.DexUtils;
import com.tinkerpop.blueprints.extensions.impls.neo4j.Neo4jUtils;
import com.tinkerpop.blueprints.impls.dex.DexGraph;


public class OperationGetShortestPath extends Operation {

	protected Vertex source;
	protected Vertex target;
	protected Direction direction;
	
	@Override
	protected void onInitialize(Object[] args) {
		source = getGraph().getVertex(args[0]);
		target = getGraph().getVertex(args[1]);
		direction = Direction.BOTH;
	}
	
	@Override
	protected void onExecute() throws Exception {
		ArrayList<Vertex> result = new ArrayList<Vertex>();
	
		int get_nbrs = 0;
		int get_vertex = 0;
		
		final HashMap<Vertex,Vertex> prev = new HashMap<Vertex,Vertex>();
		final HashMap<Vertex,Integer> dist = new HashMap<Vertex,Integer>();
		
		final Comparator<Vertex> minDist = new Comparator<Vertex>()
		{
			@SuppressWarnings({ "unchecked", "rawtypes" })
			public int compare(Vertex left, Vertex right) {
				Integer  leftDist = dist.get( left);
				Integer rightDist = dist.get(right);
				int l = leftDist  != null ?  leftDist.intValue() : Integer.MAX_VALUE;
				int r = rightDist != null ? rightDist.intValue() : Integer.MAX_VALUE;
				return l > r ? 1 : l < r ? -1 : ((Comparable) left.getId()).compareTo(right.getId());
			}
		};
		
		final PriorityHashQueue<Vertex> queue = new PriorityHashQueue<Vertex>(11, minDist);
		
		dist.put(source, 0);
		queue.add(source);
		
		while (!queue.isEmpty()) {
			Vertex u = queue.remove();
			
			if (u.equals(target))
				break;
			
			int dist_u = dist.get(u);
			
			get_nbrs++;
			Iterable<Vertex> vi = u.getVertices(direction);
			for (Vertex v : vi) {
				get_vertex++;
				
				Integer dist_v = dist.get(v);						
				int alt = dist_u + 1;
				
				if (dist_v == null || alt < dist_v.intValue()) {
					prev.put(v, u);
					dist.put(v, alt);
					queue.remove(v);
					queue.add(v);
				}
			}
			GraphUtils.close(vi);
		}
						
		Vertex u = target;
		while (prev.containsKey(u)) {
			result.add(u);
			u = prev.get(u);
		}
		Collections.reverse(result);
		
		// format unique_vertices:path_len:get_nbrs:get_vertex
		setResult(dist.size() + ":" + result.size() + ":" + get_nbrs + ":" + get_vertex);
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
			super(context, OperationGetShortestPath.class);
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
			
			OperationStats getAllNeighbors = getContext().getOperationStats("OperationGetAllNeighbors-both");
			if (getAllNeighbors != null) {
				
				// Hack
				
				OperationStats stats = getContext().getOperationStats("OperationGetShortestPath", true);
				if (stats != null) {
					
					double averageNumNeighborhoods = stats.getAverageResult(2);
					r.add(new Prediction(this, tag, "Using GetAllNeighbors and results, based on the number of neighborhood retrievals",
							getAllNeighbors.getAverageOperationRuntime() * averageNumNeighborhoods));
					
					double averageNumUniqueNodes = stats.getAverageResult(0);
					r.add(new Prediction(this, tag, "Using GetAllNeighbors and results, based on the average number of unique nodes in a neighborhood",
							averageNumNeighborhoods * MathUtils.evaluatePolynomial(getAllNeighbors.getLinearFitsForResult(0), averageNumUniqueNodes / averageNumNeighborhoods)));
				}
			}
			
			return r;
		}
	}

	
	/**
	 * The operation specialized for DEX
	 */
	public static class DEX extends OperationGetShortestPath {
		
		private EdgesDirection d;
		private int[] types;
		
		private long source;
		private long target;
		
		
		/**
		 * Initialize the operation
		 * 
		 * @param args the operation arguments
		 */
		@Override
		protected void onInitialize(Object[] args) {
			super.onInitialize(args);
					
			d = DexUtils.translateDirection(direction);
			types = DexUtils.getEdgeTypes(((DexGraph) getGraph()).getRawGraph());
			
			source = DexUtils.translateVertex(super.source);
			target = DexUtils.translateVertex(super.target);
		}

		
		/**
		 * Execute the operation
		 */
		@Override
		protected void onExecute() throws Exception {
			
			Graph graph = ((DexGraph) getGraph()).getRawGraph();
			
			ArrayList<Long> result = new ArrayList<Long>();
			
			int get_nbrs = 0;
			int get_vertex = 0;
			
			final LongLongHashMap prev = new LongLongHashMap();
			final LongIntHashMap  dist = new LongIntHashMap ();
			dist.setKeyNotFoundValue(Integer.MAX_VALUE);
			
			final LongComparator minDist = new LongComparator()
			{
				public int compare(long left, long right) {
					int l = dist.get( left);
					int r = dist.get(right);
					return (l == r) ? (left > right ? 1 : (left < right ? -1 : 0)) : (l - r);
				}
			};
			
			final LongPriorityHashQueue queue = new LongPriorityHashQueue(11, minDist);
			
			dist.put(source, 0);
			queue.add(source);
			
			while (!queue.isEmpty()) {
				long u = queue.remove();
				
				if (u == target)
					break;
				
				int dist_u = dist.get(u);
				
				get_nbrs++;
				for (int t : types) {
					com.sparsity.dex.gdb.Objects objs = graph.neighbors(u, t, d);
					com.sparsity.dex.gdb.ObjectsIterator objsItr = objs.iterator();
					
					while (objsItr.hasNext()) {
						get_vertex++;
						long v = objsItr.nextObject();
						
						int dist_v = dist.get(v);						
						int alt = dist_u + 1;
						
						if (dist_v == dist.getKeyNotFoundValue() || alt < dist_v) {
							prev.put(v, u);
							dist.put(v, alt);
							queue.remove(v);
							queue.add(v);
						}
					}
					
					objsItr.close();
					objs.close();
				}
			}
							
			long u = target;
			while (prev.containsKey(u)) {
				result.add(u);
				u = prev.get(u);
			}
			Collections.reverse(result);
		
			// format unique_vertices:path_len:get_nbrs:get_vertex
			setResult(dist.size() + ":" + result.size() + ":" + get_nbrs + ":" + get_vertex);
		}
	}
	
	
	/**
	 * The operation specialized for Neo4j
	 */
	public static class Neo extends OperationGetShortestPath {
		
		private org.neo4j.graphdb.Direction d;
		
		private Node source;
		private Node target;
		
		
		/**
		 * Initialize the operation
		 * 
		 * @param args the operation arguments
		 */
		@Override
		protected void onInitialize(Object[] args) {
			super.onInitialize(args);
			
			d = Neo4jUtils.translateDirection(direction);
			
			source = Neo4jUtils.translateVertex(super.source);
			target = Neo4jUtils.translateVertex(super.target);
		}

		
		/**
		 * Execute the operation
		 */
		@Override
		protected void onExecute() throws Exception {
			
			// TODO Can we improve the memory overhead?
			
			ArrayList<Node> result = new ArrayList<Node>();
			
			int get_nbrs = 0;
			int get_vertex = 0;
			
			final HashMap<Node,Node> prev = new HashMap<Node,Node>();
			final HashMap<Node,Integer> dist = new HashMap<Node,Integer>();
			
			final Comparator<Node> minDist = new Comparator<Node>()
			{
				@SuppressWarnings({ "unchecked", "rawtypes" })
				public int compare(Node left, Node right) {
					Integer  leftDist = dist.get( left);
					Integer rightDist = dist.get(right);
					int l = leftDist  != null ?  leftDist.intValue() : Integer.MAX_VALUE;
					int r = rightDist != null ? rightDist.intValue() : Integer.MAX_VALUE;
					return l > r ? 1 : l < r ? -1 : ((Comparable) left.getId()).compareTo(right.getId());
				}
			};
			
			final PriorityHashQueue<Node> queue = new PriorityHashQueue<Node>(11, minDist);
			
			dist.put(source, 0);
			queue.add(source);
			
			while (!queue.isEmpty()) {
				Node u = queue.remove();
				
				if (u.equals(target))
					break;
				
				int dist_u = dist.get(u);
				
				get_nbrs++;
				Iterable<Relationship> itr = u.getRelationships(d);
				for (Relationship r : itr) {
					Node v = r.getOtherNode(u);

					get_vertex++;

					Integer dist_v = dist.get(v);						
					int alt = dist_u + 1;
					
					if (dist_v == null || alt < dist_v.intValue()) {
						prev.put(v, u);
						dist.put(v, alt);
						queue.remove(v);
						queue.add(v);
					}
				}
			}
							
			Node u = target;
			while (prev.containsKey(u)) {
				result.add(u);
				u = prev.get(u);
			}
			Collections.reverse(result);
			
			// format unique_vertices:path_len:get_nbrs:get_vertex
			setResult(dist.size() + ":" + result.size() + ":" + get_nbrs + ":" + get_vertex);
		}
	}
	
	
	/**
	 * The operation specialized for Neo4j using its stored procedure
	 */
	public static class Neo_StoredProcedure extends OperationGetShortestPath {
		
		private org.neo4j.graphdb.Direction d;
		
		private Node source;
		private Node target;
		
		
		/**
		 * Initialize the operation
		 * 
		 * @param args the operation arguments
		 */
		@Override
		protected void onInitialize(Object[] args) {
			super.onInitialize(args);
			
			d = Neo4jUtils.translateDirection(direction);
			
			source = Neo4jUtils.translateVertex(super.source);
			target = Neo4jUtils.translateVertex(super.target);
		}

		
		/**
		 * Execute the operation
		 */
		@Override
		protected void onExecute() throws Exception {
			
			int real_hops = 0, get_ops = -1, get_vertex = -1;
			
			PathFinder<Path> finder = GraphAlgoFactory.shortestPath(Traversal.expanderForAllTypes(d),
					Integer.MAX_VALUE);
			Path p = finder.findSinglePath(source, target);

			@SuppressWarnings("unused")
			Node v = p.endNode();
			int d = p.length();
			real_hops = d;
			
			setResult(-1 + ":" + real_hops + ":" + get_ops + ":" + get_vertex);
		}
	}
}
