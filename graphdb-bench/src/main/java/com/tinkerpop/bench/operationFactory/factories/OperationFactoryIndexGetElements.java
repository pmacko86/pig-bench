package com.tinkerpop.bench.operationFactory.factories;

import java.util.ArrayList;
import java.util.Arrays;

import com.tinkerpop.bench.operation.operations.OperationIndexGetElements;
import com.tinkerpop.bench.operationFactory.OperationArgs;
import com.tinkerpop.bench.operationFactory.OperationFactoryBase;
import com.tinkerpop.bench.util.StatisticsHelper;
import com.tinkerpop.blueprints.Vertex;

/**
 * @author Alex Averbuch (alex.averbuch@gmail.com)
 */
public class OperationFactoryIndexGetElements extends OperationFactoryBase {

	private String propertyKey = null;
	private int opCount = 0;
	private int lookupPerOp = 0;
	private String tag = null;
	private ArrayList<Object> vertexSamples = null;

	public OperationFactoryIndexGetElements(int opCount, String propertyKey,
			int lookupPerOp) {
		this(opCount, propertyKey, lookupPerOp, "");
	}

	public OperationFactoryIndexGetElements(int opCount, String propertyKey,
			int lookupPerOp, String tag) {
		this.opCount = opCount;
		this.propertyKey = propertyKey;
		this.lookupPerOp = lookupPerOp;
		this.tag = tag;
	}

	@Override
	public void onInitialize() {
		vertexSamples = new ArrayList<Object>(Arrays.asList(StatisticsHelper
				.getRandomVertexIds(getGraph(), opCount * lookupPerOp)));
	}

	@Override
	public boolean hasNext() {
		return vertexSamples.isEmpty() == false;
	}

	@Override
	protected OperationArgs onCreateOperation() throws Exception {

		ArrayList<String> propertyKeys = new ArrayList<String>();
		ArrayList<String> propertyValues = new ArrayList<String>();

		for (int i = 0; i < lookupPerOp; i++) {

			Object startVertexId = vertexSamples.remove(0);
			Vertex startVertex = getGraph().getVertex(startVertexId);

			if (startVertex == null)
				throw new Exception(String.format("Vertex ID[%d] not found!",
						startVertexId));

			propertyKeys.add(propertyKey);
			propertyValues.add(startVertex.getProperty(propertyKey).toString());
		}

		String tagLine = ("".equals(tag)) ? Integer.toString(lookupPerOp) : tag
				+ "-" + Integer.toString(lookupPerOp);

		// args
		// -> 0 property key
		// -> 1 property value
		String[] args = new String[] {
				propertyKeys.toString().replaceAll("[\\[ \\]]", ""),
				propertyValues.toString().replaceAll("[\\[ \\]]", "") };

		return new OperationArgs(args, OperationIndexGetElements.class, tagLine);
	}

	@Override
	public boolean isUpdate() {
		return false;
	}
}
