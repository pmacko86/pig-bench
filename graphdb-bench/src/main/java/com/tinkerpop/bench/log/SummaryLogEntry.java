package com.tinkerpop.bench.log;

import java.util.HashMap;
import java.util.Map;

public class SummaryLogEntry {

	private String name = null;
	private Map<String, GraphRunTimes> runTimes = null;

	public SummaryLogEntry(String name, Map<String, GraphRunTimes> runTimes) {
		super();
		this.name = name;
		this.runTimes = new HashMap<String, GraphRunTimes>(runTimes);
	}

	public String getName() {
		return name;
	}

	public Map<String, GraphRunTimes> getRunTimes() {
		return runTimes;
	}

	public GraphRunTimes getRunTimes(String graph) {
		return runTimes.get(graph);
	}

	public GraphRunTimes getDefaultRunTimes() {
		return runTimes.get(runTimes.keySet().iterator().next());
	}
}
