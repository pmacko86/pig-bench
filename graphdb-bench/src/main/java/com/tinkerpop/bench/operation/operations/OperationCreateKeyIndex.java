package com.tinkerpop.bench.operation.operations;

import com.tinkerpop.bench.operation.Operation;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.KeyIndexableGraph;
import com.tinkerpop.blueprints.Vertex;

public class OperationCreateKeyIndex extends Operation {

	private boolean vertexType;
	private String key;
	
	@Override
	protected void onInitialize(Object[] args) {
		
		if (args.length == 0) {
			key = null; vertexType = false;
			return;
		}
		
		if (args.length != 2) 
			throw new IllegalArgumentException("Invalid arguments - usage: { type, key }");
		
		String type = ((String) args[0]).toLowerCase();
		if (type.equals("vertex")) vertexType = true;
		else if (type.equals("edge")) vertexType = false;
		else throw new IllegalArgumentException("Invalid type - must be \"vertex\" or \"edge\"");
		
		key = (String) args[1];
	}
	
	@Override
	protected void onExecute() throws Exception {
		
		Graph g = getGraph();
		if (!(g instanceof KeyIndexableGraph)) {
			throw new UnsupportedOperationException("The graph is not a KeyIndexableGraph");
		}
		
		((KeyIndexableGraph) g).createKeyIndex(key, vertexType ? Vertex.class : Edge.class);
		
		setResult("DONE");
	}

	@Override
	public boolean isUpdate() {
		return true;
	}
}
