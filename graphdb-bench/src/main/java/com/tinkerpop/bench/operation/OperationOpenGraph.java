package com.tinkerpop.bench.operation;

import com.tinkerpop.bench.cache.Cache;
import com.tinkerpop.blueprints.Graph;

/**
 * @author Alex Averbuch (alex.averbuch@gmail.com)
 */
public class OperationOpenGraph extends Operation {

	@Override
	protected void onInitialize(Object[] args) {
	}

	@Override
	protected void onExecute() throws Exception {
		try {
			Graph graph = getGraphDescriptor().openGraph();
			Cache.getInstance(graph);
			setResult("DONE");
		} catch (Exception e) {
			throw e;
		}
	}

}
