package com.tinkerpop.bench.operation.operations;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;

import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.kernel.Traversal;

import com.sparsity.dex.algorithms.TraversalBFS;
import com.sparsity.dex.gdb.EdgesDirection;
import com.sparsity.dex.gdb.Graph;
import com.sparsity.dex.gdb.OIDList;
import com.sparsity.dex.gdb.OIDListIterator;
import com.tinkerpop.bench.analysis.AnalysisContext;
import com.tinkerpop.bench.analysis.OperationModel;
import com.tinkerpop.bench.analysis.Prediction;
import com.tinkerpop.bench.log.OperationLogEntry;
import com.tinkerpop.bench.log.OperationLogReader;
import com.tinkerpop.bench.operation.Operation;
import com.tinkerpop.bench.util.GraphUtils;
import com.tinkerpop.bench.web.Job;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.extensions.impls.dex.DexUtils;
import com.tinkerpop.blueprints.extensions.impls.dex.ExtendedDexGraph;
import com.tinkerpop.blueprints.extensions.impls.neo4j.Neo4jUtils;
import com.tinkerpop.blueprints.impls.dex.DexGraph;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jVertex;


//@SuppressWarnings("unused")
public class OperationGetKHopNeighbors extends Operation {

	protected Vertex startVertex;
	protected int k;
	protected Direction direction;
	protected HashSet<Object> result;
	protected String label;
	
	@Override
	protected void onInitialize(Object[] args) {
		startVertex = getGraph().getVertex(args[0]);
		k = args.length > 1 ? (Integer) args[1] : 2;
		direction = args.length > 2 ? (Direction) args[2] : Direction.OUT;
		label = args.length > 3 ? (String) args[3] : null;
	}
	
	@Override
	protected void onDelayedInitialize() {
		result = new HashSet<Object>();
	}
	
	@Override
	protected void onExecute() throws Exception {
		
		int real_hops, get_ops = 0, get_vertex = 0;

		ArrayList<Vertex> curr = new ArrayList<Vertex>();
		ArrayList<Vertex> next = new ArrayList<Vertex>();

		curr.add(startVertex);

		for(real_hops = 0; real_hops < k; real_hops++) {

			for (Vertex u : curr) {

				get_ops++;

				Iterable<Vertex> vi = label == null ? u.getVertices(direction) : u.getVertices(direction, label);
				for (Vertex v : vi) {

					get_vertex++;

					if (result.add(v.getId())) {
						next.add(v);
					}
				}
				GraphUtils.close(vi);
			}

			if(next.size() == 0)
				break;

			ArrayList<Vertex> tmp = curr;
			curr = next;
			tmp.clear();
			next = tmp;
		}

		setResult(result.size() + ":" + real_hops + ":" + get_ops + ":" + get_vertex);
	}

	@Override
	protected void onFinalize() {
		result = null;
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
			super(context, OperationGetFirstNeighbor.class);
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
			
			String[] tags = tag.split("-");
			//Direction d = AnalysisUtils.translateDirectionTagToDirection(tags[0]);
			//int k = Integer.parseInt(tags[1]);
			
			ArrayList<Prediction> r = new ArrayList<Prediction>();
			
			Double getAllNeighbors = getContext().getAverageOperationRuntime("OperationGetAllNeighbors-" + tags[0]);
			if (getAllNeighbors != null) {
				
				// Hack
			
				String operationName = "OperationGetKHopNeighbors-" + tag;
				SortedSet<Job> jobs = getContext().getJobsWithTag(operationName);
				Job job = jobs == null ? null : jobs.last();
				
				if (job != null) {
					int getOpsCount = 0;
					int count = 0;
					for (OperationLogEntry e : OperationLogReader.getTailEntries(job.getLogFile(), operationName)) {
						
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
	public static class DEX extends OperationGetKHopNeighbors {
		
		private EdgesDirection d;
		private int[] types; 
		
		
		/**
		 * Initialize the operation
		 * 
		 * @param args the operation arguments
		 */
		@Override
		protected void onInitialize(Object[] args) {
			super.onInitialize(args);
					
			// Translate the direction and the edge label
			
			d = DexUtils.translateDirection(direction);
			types = DexUtils.getEdgeTypes(((DexGraph) getGraph()).getRawGraph(), label);
		}

		
		/**
		 * Execute the operation
		 */
		@Override
		protected void onExecute() throws Exception {
			
			Graph graph = ((DexGraph) getGraph()).getRawGraph();
			int real_hops, get_ops = 0, get_vertex = 0;

			
			/*OIDList curr = new OIDList();
			OIDList next = new OIDList();
			
			curr.add(((Long) startVertex.getId()).longValue());
			
			for (real_hops = 0; real_hops < k; real_hops++) {

				int nextSize = 0;
				
				OIDListIterator currItr = curr.iterator();
				while (currItr.hasNext()) {
					long u = currItr.nextOID();
					
					get_ops++;
					
					for (int t : types) {
						com.sparsity.dex.gdb.Objects objs = graph.neighbors(u, t, d);
						com.sparsity.dex.gdb.ObjectsIterator objsItr = objs.iterator();
						
						while (objsItr.hasNext()) {
							get_vertex++;
							long v = objsItr.nextObject();
							if (result.add(v)) {
								next.add(v);
								nextSize++;
							}
						}
						
						objsItr.close();
						objs.close();
					}
				}
				
				if (nextSize == 0) break;
				
				OIDList tmp = curr;
				curr = next;
				tmp.clear();
				next = tmp;
			}
			
			curr.delete();
			next.delete();*/
			
		
			/*ArrayList<com.sparsity.dex.gdb.Objects> curr = new ArrayList<com.sparsity.dex.gdb.Objects>();
			ArrayList<com.sparsity.dex.gdb.Objects> next = new ArrayList<com.sparsity.dex.gdb.Objects>();
			
			long start = ((Long) startVertex.getId()).longValue();
			get_ops++;
			for (int t : types) {
				com.sparsity.dex.gdb.Objects objs = graph.neighbors(start, t, d);
				com.sparsity.dex.gdb.ObjectsIterator objsItr = objs.iterator();
				
				int size = 0;
				while (objsItr.hasNext()) {
					get_vertex++;
					size++;
					long v = objsItr.nextObject();
					result.add(v);
				}
				
				objsItr.close();
				
				if (size == 0) {
					objs.close();
				}
				else {
					curr.add(objs);
				}
			}
			
			if (curr.isEmpty()) {
				setResult(result.size() + ":" + 0 + ":" + get_ops + ":" + get_vertex);
				return;
			}
			
			for (real_hops = 1; real_hops < k; real_hops++) {

				int nextSize = 0;
				
				for (int t : types) {
					for (com.sparsity.dex.gdb.Objects c : curr) {
					
						com.sparsity.dex.gdb.Objects objs = graph.neighbors(c, t, d);
						com.sparsity.dex.gdb.ObjectsIterator objsItr = objs.iterator();
						
						int size = 0;
						while (objsItr.hasNext()) {
							get_vertex++;
							size++;
							long v = objsItr.nextObject();
							if (result.add(v)) {
								nextSize++;
							}
						}
						
						objsItr.close();
						if (size == 0) {
							objs.close();
						}
						else {
							next.add(objs);
						}
					}
				}
				
				for (com.sparsity.dex.gdb.Objects c : curr) {
					get_ops += c.count();
					c.close();
				}

				if (nextSize == 0) break;
				
				ArrayList<com.sparsity.dex.gdb.Objects> tmp = curr;
				curr = next;
				tmp.clear();
				next = tmp;
			}
			
			for (com.sparsity.dex.gdb.Objects c : curr) {
				c.close();
			}*/
			
			
			OIDList curr = new OIDList();
			OIDList next = new OIDList();
			
			curr.add(((Long) startVertex.getId()).longValue());
			
			for (real_hops = 0; real_hops < k; real_hops++) {

				int nextSize = 0;
				boolean first = true;
				
				for (int t : types) {
					OIDListIterator currItr = curr.iterator();
					while (currItr.hasNext()) {
						long u = currItr.nextOID();
						
						if (first) get_ops++;
						
						com.sparsity.dex.gdb.Objects objs = graph.neighbors(u, t, d);
						com.sparsity.dex.gdb.ObjectsIterator objsItr = objs.iterator();
						
						while (objsItr.hasNext()) {
							get_vertex++;
							long v = objsItr.nextObject();
							if (result.add(v)) {
								next.add(v);
								nextSize++;
							}
						}
						
						objsItr.close();
						objs.close();
					}
					first = false;
				}
				
				if (nextSize == 0) break;
				
				OIDList tmp = curr;
				curr = next;
				tmp.clear();
				next = tmp;
			}
			
			curr.delete();
			next.delete();

			
			
			setResult(result.size() + ":" + real_hops + ":" + get_ops + ":" + get_vertex);
		}
	}
	
	
	/**
	 * The operation specialized for DEX using its stored procedure
	 */
	public static class DEX_StoredProcedure extends OperationGetKHopNeighbors {
		
		private EdgesDirection d;
		private int[] types; 
		
		
		/**
		 * Initialize the operation
		 * 
		 * @param args the operation arguments
		 */
		@Override
		protected void onInitialize(Object[] args) {
			super.onInitialize(args);
					
			// Translate the direction and the edge label
			
			d = DexUtils.translateDirection(direction);
			types = DexUtils.getEdgeTypes(((DexGraph) getGraph()).getRawGraph(), label);
		}

		
		/**
		 * Execute the operation
		 */
		@Override
		protected void onExecute() throws Exception {
			
			TraversalBFS t = new TraversalBFS(((ExtendedDexGraph) getGraph()).getSession(),
					DexUtils.translateVertex(startVertex));
			t.addAllNodeTypes();
			if (label == null) t.addAllEdgeTypes(d); else t.addEdgeType(types[0], d);
			t.setMaximumHops(k);
			
			int real_hops = 0, get_ops = -1, get_vertex = 0;
			
			while (t.hasNext()) {
				long v = t.next();
				get_vertex++;
				result.add(v);
				
				int d = t.getCurrentDepth();
				if (d > real_hops) real_hops = d;
			}
			
			t.close();
			
			setResult(result.size() + ":" + real_hops + ":" + get_ops + ":" + get_vertex);
		}
	}
	
	
	/**
	 * The operation specialized for Neo4j
	 */
	public static class Neo extends OperationGetKHopNeighbors {
		
		private org.neo4j.graphdb.Direction d;
		private DynamicRelationshipType relationshipType; 
		
		
		/**
		 * Initialize the operation
		 * 
		 * @param args the operation arguments
		 */
		@Override
		protected void onInitialize(Object[] args) {
			super.onInitialize(args);
			
			// Translate the direction and the edge label
			
			d = Neo4jUtils.translateDirection(direction);
			relationshipType = label == null ? null : DynamicRelationshipType.withName(label);
		}

		
		/**
		 * Execute the operation
		 */
		@Override
		protected void onExecute() throws Exception {
			
			int real_hops, get_ops = 0, get_vertex = 0;

			ArrayList<Node> curr = new ArrayList<Node>();
			ArrayList<Node> next = new ArrayList<Node>();

			curr.add(((Neo4jVertex) startVertex).getRawVertex());

			for(real_hops = 0; real_hops < k; real_hops++) {

				for (Node u : curr) {

					get_ops++;

					Iterable<Relationship> itr;
					itr = relationshipType == null ? u.getRelationships(d) : u.getRelationships(d, relationshipType);
					for (Relationship r : itr) {
						Node v = r.getOtherNode(u);

						get_vertex++;

						if (result.add(v.getId())) {
							next.add(v);
						}
					}
				}

				if(next.size() == 0)
					break;

				ArrayList<Node> tmp = curr;
				curr = next;
				tmp.clear();
				next = tmp;
			}

			setResult(result.size() + ":" + real_hops + ":" + get_ops + ":" + get_vertex);		
		}
	}
	
	
	/**
	 * The operation specialized for Neo4j using its stored procedure
	 */
	public static class Neo_StoredProcedure extends OperationGetKHopNeighbors {
		
		private org.neo4j.graphdb.Direction d;
		private List<RelationshipType> relationshipTypes; 
		
		
		/**
		 * Initialize the operation
		 * 
		 * @param args the operation arguments
		 */
		@Override
		protected void onInitialize(Object[] args) {
			super.onInitialize(args);
					
			// Translate the direction and the edge label
			
			d = Neo4jUtils.translateDirection(direction);
			
			relationshipTypes = new ArrayList<RelationshipType>();
			if (label == null) {
				@SuppressWarnings("deprecation")
				Iterable<RelationshipType> itr = ((Neo4jGraph) getGraph()).getRawGraph().getRelationshipTypes();
				Iterator<RelationshipType> i = itr.iterator();
				while (i.hasNext()) relationshipTypes.add(i.next());
			}
			else {
				relationshipTypes.add(DynamicRelationshipType.withName(label));
			}	
		}

		
		/**
		 * Execute the operation
		 */
		@Override
		protected void onExecute() throws Exception {
			
			int real_hops = 0, get_ops = -1, get_vertex = 0;
			
			TraversalDescription td = Traversal.description()
		            .breadthFirst()
		            .evaluator(Evaluators.toDepth(k));
		    
			for (RelationshipType t : relationshipTypes) {
				td = td.relationships(t, d);
			}
			
			Traverser t = td.traverse(Neo4jUtils.translateVertex(startVertex));
			
			for (Path p : t) {
				Node v = p.endNode();
				get_vertex++;
				result.add(v);
				
				int d = p.length();
				if (d > real_hops) real_hops = d;
			}
			
			setResult(result.size() + ":" + real_hops + ":" + get_ops + ":" + get_vertex);
		}
	}
}
