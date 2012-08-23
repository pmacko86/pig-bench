package com.tinkerpop.bench.operation.operations;

import com.tinkerpop.bench.operation.Operation;
import com.tinkerpop.gremlin.groovy.Gremlin;
import com.tinkerpop.pipes.Pipe;

/**
 * @author Alex Averbuch (alex.averbuch@gmail.com)
 */
public class OperationGremlin extends Operation {

	private String gremlinScript = null;
	@SuppressWarnings("rawtypes")
	private Pipe compiledScript = null;

	// args
	// -> 0 gremlinScript
	@Override
	protected void onInitialize(Object[] args) {
		this.gremlinScript = (String) args[0];
		this.compiledScript = Gremlin.compile(this.gremlinScript);
		// compiledScript.setStarts(graph.getVertex(1)); // FIXME necessary?
	}

	@Override
	protected void onExecute() throws Exception {
		try {
			int resultCount = 0;

			for (@SuppressWarnings("unused") Object result : compiledScript)
				resultCount++;

			setResult(Integer.toString(resultCount));
		} catch (Exception e) {
			throw e;
		}
	}

	@Override
	public boolean isUpdate() {
		return true;	// Well, actually it's maybe -- we do not know
	}
}
