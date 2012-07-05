package com.tinkerpop.bench.log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;
import java.util.Map.Entry;

import com.tinkerpop.bench.ConsoleUtils;
import com.tinkerpop.bench.LogUtils;

import edu.harvard.pass.cpl.CPL;
import edu.harvard.pass.cpl.CPLFile;

public class SummaryLogWriter {
	private static final String logDelim = LogUtils.LOG_DELIMITER;
	
	/// The summary
	LinkedHashMap<String, ArrayList<GraphRunTimes>> summarizedFiles;
	
	/// The result file paths
	Map<String, String> resultFilePaths;
	
	
	/**
	 * Create an instance of the summarizer from the result file paths
	 * 
	 * @param resultFilePaths file paths map in format ["graphName" -&gt; "path/to/result/file.csv"]
	 * @throws IOException
	 */
	public SummaryLogWriter(Map<String, String> resultFilePaths) throws IOException {
		this.resultFilePaths = resultFilePaths;
		summarizedFiles = summarizeFiles(resultFilePaths);
	}

	/*
	 * summaryFilePath = "path/to/summary/file.csv"
	 * 
	 * resultFiles = ["graphName"->"path/to/result/file.csv"]
	 */
	public void writeSummary(String summaryFilePath) throws IOException {

		writeSummaryFile(summaryFilePath, summarizedFiles);
		
		if (CPL.isAttached()) {
			CPLFile summaryFileObject = CPLFile.create(new File(summaryFilePath));
			for (Entry<String, String> fileEntry : resultFilePaths.entrySet()) {
				String path = fileEntry.getValue();
				summaryFileObject.dataFlowFrom(CPLFile.lookupOrCreate(new File(path)));
			}
		}
	}
	
	public void writeSummaryText(String summaryFilePath) throws IOException {
		
		LinkedHashMap<String, ArrayList<GraphRunTimes>> summarizedFiles;
		summarizedFiles = summarizeFiles(resultFilePaths);
		
		
		// Build the table
		
		Vector<Vector<String>> summaryTable = new Vector<Vector<String>>();

		for (Entry<String, ArrayList<GraphRunTimes>> opGraphRunTimes : summarizedFiles.entrySet()) {
			Collections.sort(opGraphRunTimes.getValue());
			Vector<String> line = new Vector<String>(opGraphRunTimes.getValue().size() * 4 + 1);
			line.add("operation");
			for (GraphRunTimes graphRunTimes : opGraphRunTimes.getValue()) {
				line.add(graphRunTimes.getGraphName() + "-mean (ms)");
				line.add(graphRunTimes.getGraphName() + "-stdev (ms)");
				line.add(graphRunTimes.getGraphName() + "-min (ms)");
				line.add(graphRunTimes.getGraphName() + "-max (ms)");
			}
			summaryTable.add(line);
			break;
		}

		for (Entry<String, ArrayList<GraphRunTimes>> opGraphRunTimes : summarizedFiles.entrySet()) {
			Collections.sort(opGraphRunTimes.getValue());
			Vector<String> line = new Vector<String>(opGraphRunTimes.getValue().size() * 4 + 1);
			line.add(opGraphRunTimes.getKey());
			for (GraphRunTimes graphRunTimes : opGraphRunTimes.getValue()) {
				line.add(String.format("%.3f", graphRunTimes.getMean() / 1000000.0));
				line.add(String.format("%.3f", graphRunTimes.getStdev() / 1000000.0));
				line.add(String.format("%.3f", graphRunTimes.getMin() / 1000000.0));
				line.add(String.format("%.3f", graphRunTimes.getMax() / 1000000.0));
			}
			summaryTable.add(line);
		}
		
		
		// Determine the column widths
		
		Vector<Integer> maxLengths = new Vector<Integer>(summaryTable.get(0).size());
		for (Vector<String> line : summaryTable) {
			for (int i = 0; i < line.size(); i++) {
				maxLengths.add(line.get(i).length());
			}
			break;
		}
		for (Vector<String> line : summaryTable) {
			for (int i = 0; i < line.size(); i++) {
				if (maxLengths.get(i) < line.get(i).length()) {
					maxLengths.set(i, line.get(i).length());
				}
			}
		}
		
		Vector<Integer> columnWidths = new Vector<Integer>(summaryTable.get(0).size());
		for (int i = 0; i < maxLengths.size(); i++) {
			columnWidths.add((maxLengths.get(i) / 4) * 4 + 4);
		}
		
		
		// Write
		
		FileWriter w = null;
		if (summaryFilePath != null) w = new FileWriter(new File(summaryFilePath));
		
		StringBuilder sb = new StringBuilder();
		for (int line_i = 0; line_i < summaryTable.size(); line_i++) {
			Vector<String> line = summaryTable.get(line_i);
			
			sb.setLength(0);
			for (int i = 0; i < line.size(); i++) {
				
				int spaces1 = maxLengths.get(i) - line.get(i).length();
				if (i == 0) spaces1 = 0;
				int spaces2 = columnWidths.get(i) - line.get(i).length() - spaces1;
				
				for (int j = 0; j < spaces1; j++) sb.append(' ');
				sb.append(line.get(i));
				for (int j = 0; j < spaces2; j++) sb.append(' ');
			}
			
			if (w == null) {
				if (line_i == 0) {
					ConsoleUtils.header(sb.toString());
				}
				else {
					System.out.println(sb.toString());
				}
			}
			else {
				w.write(sb.toString());
				w.write('\n');
			}
		}
		
		if (w == null) System.out.println();
		if (w != null) w.close();
		
		
		// Provenance
		
		if (CPL.isAttached() && w != null) {
			CPLFile summaryFileObject = CPLFile.create(new File(summaryFilePath));
			for (Entry<String, String> fileEntry : resultFilePaths.entrySet()) {
				String path = fileEntry.getValue();
				summaryFileObject.dataFlowFrom(CPLFile.lookupOrCreate(new File(path)));
			}
		}
	}

	private synchronized LinkedHashMap<String, ArrayList<GraphRunTimes>> summarizeFiles(
			Map<String, String> resultFilePaths) {
		// summarizedResults = ["operation" -> ["graphRuntimes"]]
		LinkedHashMap<String, ArrayList<GraphRunTimes>> resultFiles = new LinkedHashMap<String, ArrayList<GraphRunTimes>>();

		// Get total time taken for each operation, for each result file
		for (Entry<String, String> fileEntry : resultFilePaths.entrySet()) {

			String graphName = fileEntry.getKey();
			String path = fileEntry.getValue();

			// Load Operations' runtimes from 1 .csv (for 1 Graph) into memory
			// fileOperationTimes= ["operation" -> "graphRuntimes"]
			LinkedHashMap<String, GraphRunTimes> fileOperationTimes = getFileOperationTimes(
					graphName, path);

			for (Entry<String, GraphRunTimes> fileOperationTimesEntry : fileOperationTimes
					.entrySet()) {
				String opType = fileOperationTimesEntry.getKey();
				GraphRunTimes opTimes = fileOperationTimesEntry.getValue();

				ArrayList<GraphRunTimes> opResults = resultFiles.get(opType);

				if (opResults == null) {
					opResults = new ArrayList<GraphRunTimes>();
					resultFiles.put(opType, opResults);
				}
				opResults.add(opTimes);
			}
		}

		return resultFiles;
	}

	private LinkedHashMap<String, GraphRunTimes> getFileOperationTimes(
			String graphName, String path) {
		// summarizedResults = ["operation" -> "graphRuntimes"]
		LinkedHashMap<String, GraphRunTimes> fileOperationTimes = new LinkedHashMap<String, GraphRunTimes>();

		OperationLogReader reader = new OperationLogReader(new File(path));

		for (OperationLogEntry opLogEntry : reader) {
			GraphRunTimes graphRunTimes = fileOperationTimes.get(opLogEntry
					.getName());

			if (graphRunTimes == null)
				graphRunTimes = new GraphRunTimes(graphName);

			fileOperationTimes.put(opLogEntry.getName(), graphRunTimes);

			fileOperationTimes.get(opLogEntry.getName()).add(
					opLogEntry.getTime());
		}

		return fileOperationTimes;
	}

	// summarizedResults = ["operation" -> ["graphRunTime"]]
	private void writeSummaryFile(String summaryFilePath,
			LinkedHashMap<String, ArrayList<GraphRunTimes>> summarizedResults)
			throws IOException {

		File summaryFile = new File(summaryFilePath);
		(new File(summaryFile.getParent())).mkdirs();

		BufferedWriter bufferedLogWriter = new BufferedWriter(new FileWriter(
				new File(summaryFilePath)));

		// write .csv column headers
		bufferedLogWriter.write("operation");
		bufferedLogWriter.write(logDelim);

		for (Entry<String, ArrayList<GraphRunTimes>> opGraphRunTimes : summarizedResults
				.entrySet()) {
			Collections.sort(opGraphRunTimes.getValue());
			for (GraphRunTimes graphRunTimes : opGraphRunTimes.getValue()) {
				bufferedLogWriter.write(graphRunTimes.getGraphName() + "-mean");
				bufferedLogWriter.write(logDelim);
				bufferedLogWriter
						.write(graphRunTimes.getGraphName() + "-stdev");
				bufferedLogWriter.write(logDelim);
				bufferedLogWriter.write(graphRunTimes.getGraphName() + "-min");
				bufferedLogWriter.write(logDelim);
				bufferedLogWriter.write(graphRunTimes.getGraphName() + "-max");
				bufferedLogWriter.write(logDelim);
			}
			break;
		}

		bufferedLogWriter.newLine();

		// write .csv column data
		for (Entry<String, ArrayList<GraphRunTimes>> opGraphRunTimes : summarizedResults
				.entrySet()) {
			bufferedLogWriter.write(opGraphRunTimes.getKey());
			bufferedLogWriter.write(logDelim);

			Collections.sort(opGraphRunTimes.getValue());
			for (GraphRunTimes graphRunTimes : opGraphRunTimes.getValue()) {
				bufferedLogWriter.write(graphRunTimes.getMean().toString());
				bufferedLogWriter.write(logDelim);
				bufferedLogWriter.write(graphRunTimes.getStdev().toString());
				bufferedLogWriter.write(logDelim);
				bufferedLogWriter.write(graphRunTimes.getMin().toString());
				bufferedLogWriter.write(logDelim);
				bufferedLogWriter.write(graphRunTimes.getMax().toString());
				bufferedLogWriter.write(logDelim);
			}

			bufferedLogWriter.newLine();
		}

		bufferedLogWriter.flush();
		bufferedLogWriter.close();
	}

	// Encapsulates the run times for one Graph & one Operation
	public class GraphRunTimes implements Comparable<GraphRunTimes> {
		private String graphName = null;
		private ArrayList<Long> runTimes = new ArrayList<Long>();
		private Double mean = null;
		private Double stdev = null;
		private Double min = null;
		private Double max = null;

		public GraphRunTimes(String graphName) {
			this.graphName = graphName;
		}

		public void add(long runTime) {
			mean = null;
			stdev = null;
			min = null;
			max = null;
			runTimes.add(runTime);
		}

		public String getGraphName() {
			return graphName;
		}

		public Double getMean() {
			return (null == mean) ? calcMean() : mean;
		}

		public Double getStdev() {
			return (null == stdev) ? calcStdev() : stdev;
		}

		public Double getMin() {
			return (null == min) ? calcMin() : min;
		}

		public Double getMax() {
			return (null == max) ? calcMax() : max;
		}

		private double calcMean() {
			double runTimesSum = 0;
			for (Long runTime : runTimes)
				runTimesSum += runTime;

			return (mean = runTimesSum / (double) runTimes.size());
		}

		private double calcStdev() {
			mean = getMean();

			double diffFromMeanSum = 0;
			for (Long runTime : runTimes)
				diffFromMeanSum += Math.pow(runTime - mean, 2);

			return (stdev = Math.sqrt(diffFromMeanSum
					/ (double) runTimes.size()));
		}

		private double calcMin() {
			calcMinMax();
			return min;
		}

		private double calcMax() {
			calcMinMax();
			return max;
		}

		private void calcMinMax() {
			min = Double.MAX_VALUE;
			max = -1d;

			for (Long runTime : runTimes) {
				min = (runTime < min) ? runTime : min;
				max = (runTime > max) ? runTime : max;
			}
		}

		@Override
		public int compareTo(GraphRunTimes otherGraphName) {
			return this.graphName.compareTo(otherGraphName.getGraphName());
		}
	}

}
