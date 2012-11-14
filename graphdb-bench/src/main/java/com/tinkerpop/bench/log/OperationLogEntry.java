package com.tinkerpop.bench.log;

public class OperationLogEntry {

	private int opId = -1;
	private String name = null;
	private String type = null;
	private String args = null;
	private String[] args_parsed = null;
	private long time = -1;
	private String result = null;
	private long memory = -1;
	private long gcCount = -1;
	private long gcTimeMS = -1;

	public OperationLogEntry(int opId, String name, String type, String args,
			long time, String result, long memory, long gcCount, long gcTimeMS)  {
		this.opId = opId;
		this.name = name;
		this.type = type;
		this.args = args;
		this.time = time;
		this.result = result;
		this.memory = memory;
		this.gcCount = gcCount;
		this.gcTimeMS = gcTimeMS;
	}

	public int getOpId() {
		return opId;
	}

	public String getName() {
		return name;
	}

	public String getType() {
		return type;
	}

	public String getArgsString() {
		return args;
	}

	public String[] getArgs() {
		if (args_parsed != null) return args_parsed;
		args_parsed = extractArgs(args);
		return args_parsed;
	}

	public long getTime() {
		return time;
	}

	public String getResult() {
		return result;
	}

	public long getMemory() {
		return memory;
	}

	public long getGCCount() {
		return gcCount;
	}

	public long getGCTimeMS() {
		return gcTimeMS;
	}

	private static String[] extractArgs(String argsStr) {
		return argsStr.replaceAll("[\\[\\]]", "").split(", ");
	}
}
