package com.tinkerpop.bench.operation.operations;

import java.util.ArrayList;

import com.tinkerpop.bench.operation.Operation;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.gremlin.groovy.Gremlin;
import com.tinkerpop.pipes.Pipe;

/**
 * @author Alex Averbuch (alex.averbuch@gmail.com)
 */
public class OperationGremlin extends Operation {

	private String gremlinScript = null;
	private Pipe<Vertex, Vertex> compiledScript = null;

	// args
	// -> 0 gremlinScript
	@SuppressWarnings("unchecked")
	@Override
	protected void onInitialize(Object[] args) {
		this.gremlinScript = (String) args[0];
		this.compiledScript = Gremlin.compile(this.gremlinScript);
		if (args.length >= 2) {
			ArrayList<Vertex> a = new ArrayList<Vertex>();
			a.add(getGraph().getVertex(args[1]));
			compiledScript.setStarts(a);
		}
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
