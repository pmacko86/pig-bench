package com.tinkerpop.bench.operation.operations;

import java.util.ArrayList;
import java.util.HashSet;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import com.sparsity.dex.gdb.EdgesDirection;
import com.sparsity.dex.gdb.Graph;
import com.sparsity.dex.gdb.OIDList;
import com.sparsity.dex.gdb.OIDListIterator;
import com.tinkerpop.bench.GlobalConfig;
import com.tinkerpop.bench.operation.Operation;
//import com.tinkerpop.bench.util.BloomFilter;
import com.tinkerpop.bench.util.GraphUtils;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.dex.DexGraph;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jVertex;

public class OperationGetKHopNeighbors extends Operation {

	private Vertex startVertex;
	private int k;
	private Direction direction;
	private HashSet<Object> result;
	//private BloomFilter result;
	
	@Override
	protected void onInitialize(Object[] args) {
		startVertex = getGraph().getVertex(args[0]);
		k = args.length > 1 ? (Integer) args[1] : 2;
		direction = args.length > 2 ? (Direction) args[2] : Direction.OUT;
	}
	
	@Override
	protected void onDelayedInitialize() {
		result = new HashSet<Object>();
	}
	
	@Override
	protected void onExecute() throws Exception {
		
		if (GlobalConfig.useSpecializedProcedures && getGraph() instanceof DexGraph) {
			onExecuteDex(((DexGraph) getGraph()).getRawGraph());
			return;
		}
		
		if (GlobalConfig.useSpecializedProcedures && getGraph() instanceof Neo4jGraph) {
			onExecuteNeo(((Neo4jGraph) getGraph()).getRawGraph());
			return;
		}
		
		int real_hops, get_ops = 0, get_vertex = 0;

		ArrayList<Vertex> curr = new ArrayList<Vertex>();
		ArrayList<Vertex> next = new ArrayList<Vertex>();

		curr.add(startVertex);

		for(real_hops = 0; real_hops < k; real_hops++) {

			for (Vertex u : curr) {

				get_ops++;

				Iterable<Vertex> vi = u.getVertices(direction);
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
	
	protected void onExecuteDex(Graph graph) throws Exception {
		
		int real_hops, get_ops = 0, get_vertex = 0;
		
		com.sparsity.dex.gdb.TypeList tlist = graph.findEdgeTypes();
		com.sparsity.dex.gdb.TypeListIterator typeItr = tlist.iterator();
		int[] types = new int[tlist.count()];
		int ti = 0; while (typeItr.hasNext()) types[ti++] = typeItr.nextType();
		tlist.delete();
		tlist = null;
		
		EdgesDirection d;
		switch (direction) {
		case OUT : d = EdgesDirection.Outgoing; break;
		case IN  : d = EdgesDirection.Ingoing; break;
		case BOTH: d = EdgesDirection.Any; break;
		default  : throw new IllegalArgumentException("Invalid direction"); 
		}

		OIDList curr = new OIDList();
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
		
		setResult(result.size() + ":" + real_hops + ":" + get_ops + ":" + get_vertex);
	}
	
	protected void onExecuteNeo(GraphDatabaseService graph) throws Exception {
		
		int real_hops, get_ops = 0, get_vertex = 0;

		org.neo4j.graphdb.Direction d;
		switch (direction) {
		case OUT : d = org.neo4j.graphdb.Direction.OUTGOING; break;
		case IN  : d = org.neo4j.graphdb.Direction.INCOMING; break;
		case BOTH: d = org.neo4j.graphdb.Direction.BOTH; break;
		default  : throw new IllegalArgumentException("Invalid direction"); 
		}

		ArrayList<Node> curr = new ArrayList<Node>();
		ArrayList<Node> next = new ArrayList<Node>();

		curr.add(((Neo4jVertex) startVertex).getRawVertex());

		for(real_hops = 0; real_hops < k; real_hops++) {

			for (Node u : curr) {

				get_ops++;

				Iterable<Relationship> itr;
				itr = u.getRelationships(d);
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
