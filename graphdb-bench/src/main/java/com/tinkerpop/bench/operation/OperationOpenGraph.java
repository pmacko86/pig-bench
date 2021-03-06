package com.tinkerpop.bench.operation;

import com.tinkerpop.bench.GraphDescriptor;
import com.tinkerpop.blueprints.Graph;

/**
 * @author Alex Averbuch (alex.averbuch@gmail.com)
 * @author Peter Macko (pmacko@eecs.harvard.edu)
 */
public class OperationOpenGraph extends Operation {
	
	private GraphDescriptor.OpenMode mode;
	
	@Override
	protected void onInitialize(Object[] args) {
		mode = args.length >= 1 ? (GraphDescriptor.OpenMode) args[0] : GraphDescriptor.OpenMode.DEFAULT; 
	}

	@Override
	protected void onExecute() throws Exception {
		try {
			@SuppressWarnings("unused")
			Graph graph = getGraphDescriptor().openGraph(mode);
			setResult("DONE");
		} catch (Exception e) {
			throw e;
		}
	}

}
