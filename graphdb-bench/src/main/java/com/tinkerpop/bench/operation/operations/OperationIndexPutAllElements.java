package com.tinkerpop.bench.operation.operations;

import com.tinkerpop.bench.operation.Operation;
import com.tinkerpop.bench.util.GraphUtils;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Index;
import com.tinkerpop.blueprints.IndexableGraph;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.extensions.util.ClosingIterator;


/**
 * @author Alex Averbuch (alex.averbuch@gmail.com)
 */
public class OperationIndexPutAllElements extends Operation {

	private String propertyKey = null;
	private final int TRANSACTION_BUFFER = 1000;

	// args
	// -> 0 property key
	@Override
	protected void onInitialize(Object[] args) {
		this.propertyKey = (String) args[0];
	}

	@Override
	protected void onExecute() throws Exception {
		try {
			if ((getGraph() instanceof IndexableGraph) == false)
				throw new Exception("Graph is not IndexableGraph");

			int elementCount = (getGraph() instanceof TransactionalGraph) ? indexElementsTransactional()
					: indexElements();

			setResult(elementCount);
		} catch (Exception e) {
			throw e;
		}
	}

	private int indexElements() {
		IndexableGraph indexableGraph = ((IndexableGraph) getGraph());

		Index<Vertex> vIndex = indexableGraph.getIndex(GraphUtils.INDEX_VERTICES,
				Vertex.class);

		Index<Edge> eIndex = indexableGraph.getIndex(GraphUtils.INDEX_EDGES, Edge.class);

		int elementCount = 0;

		for (Vertex v : getGraph().getVertices()) {
			vIndex.put(propertyKey, v.getProperty(propertyKey), v);
			elementCount++;
		}

		for (Edge e : getGraph().getEdges()) {
			eIndex.put(propertyKey, e.getProperty(propertyKey), e);
			elementCount++;
		}

		return elementCount;
	}

	private int indexElementsTransactional() {
		Index<Vertex> vIndex = ((IndexableGraph) getGraph()).getIndex(
				GraphUtils.INDEX_VERTICES, Vertex.class);

		Index<Edge> eIndex = ((IndexableGraph) getGraph()).getIndex(
				GraphUtils.INDEX_EDGES, Edge.class);

		int elementCount = 0;

		TransactionalGraph transactionalGraph = (TransactionalGraph) getGraph();
		
		
		// The transactions in a TransactionalGraph are started automatically

		for (Vertex v : new ClosingIterator<Vertex>(getGraph().getVertices())) {
			vIndex.put(propertyKey, v.getProperty(propertyKey), v);
			elementCount++;

			if (elementCount % TRANSACTION_BUFFER == 0) {
				transactionalGraph.commit();
			}
		}

		for (Edge e : new ClosingIterator<Edge>(getGraph().getEdges())) {
			eIndex.put(propertyKey, e.getProperty(propertyKey), e);
			elementCount++;

			if (elementCount % TRANSACTION_BUFFER == 0) {
				transactionalGraph.commit();
			}
		}

		transactionalGraph.commit();

		return elementCount;
	}

	@Override
	public boolean isUpdate() {
		return true;
	}
}
