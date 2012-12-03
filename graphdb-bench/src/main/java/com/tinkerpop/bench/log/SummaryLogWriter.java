package com.tinkerpop.bench.log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;
import java.util.Map.Entry;

import au.com.bytecode.opencsv.CSVWriter;

import com.tinkerpop.bench.util.ConsoleUtils;
import com.tinkerpop.bench.util.LogUtils;

import edu.harvard.pass.cpl.CPL;
import edu.harvard.pass.cpl.CPLFile;

public class SummaryLogWriter {
	private final char logDelim = LogUtils.LOG_DELIMITER.charAt(0);
	
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
			GraphRunTimes graphRunTimes = fileOperationTimes.get(opLogEntry.getName());

			if (graphRunTimes == null)
				graphRunTimes = new GraphRunTimes(graphName, opLogEntry.getName());

			fileOperationTimes.put(opLogEntry.getName(), graphRunTimes);

			fileOperationTimes.get(opLogEntry.getName()).add(
					opLogEntry.getTime());
		}
		
		for (Entry<String, GraphRunTimes> e : fileOperationTimes.entrySet()) {
			e.getValue().retainTail(10000, 0.25, 0.05);	// XXX hard-coded values
		}

		return fileOperationTimes;
	}

	// summarizedResults = ["operation" -> ["graphRunTime"]]
	private void writeSummaryFile(String summaryFilePath,
			LinkedHashMap<String, ArrayList<GraphRunTimes>> summarizedResults)
			throws IOException {
		
		int numDatasets = 0;
		for (Entry<String, ArrayList<GraphRunTimes>> opGraphRunTimes : summarizedResults.entrySet()) {
			for (@SuppressWarnings("unused") GraphRunTimes graphRunTimes : opGraphRunTimes.getValue()) {
				numDatasets++;
			}
			break;
		}

		File summaryFile = new File(summaryFilePath);
		(new File(summaryFile.getParent())).mkdirs();

		CSVWriter writer = new CSVWriter(new FileWriter(summaryFilePath), logDelim);
		String[] buffer = new String[1 + 4 * numDatasets];

		
		// Write .csv column headers
		
		int index = 0;
		buffer[index++] = "operation";

		for (Entry<String, ArrayList<GraphRunTimes>> opGraphRunTimes : summarizedResults.entrySet()) {
			Collections.sort(opGraphRunTimes.getValue());
			for (GraphRunTimes graphRunTimes : opGraphRunTimes.getValue()) {
				buffer[index++] = graphRunTimes.getGraphName() + "-mean";
				buffer[index++] = graphRunTimes.getGraphName() + "-stdev";
				buffer[index++] = graphRunTimes.getGraphName() + "-min";
				buffer[index++] = graphRunTimes.getGraphName() + "-max";
			}
			break;
		}

		writer.writeNext(buffer);

		
		// Write .csv column data
		
		for (Entry<String, ArrayList<GraphRunTimes>> opGraphRunTimes : summarizedResults.entrySet()) {
			
			index = 0;
			buffer[index++] = opGraphRunTimes.getKey();

			Collections.sort(opGraphRunTimes.getValue());
			for (GraphRunTimes graphRunTimes : opGraphRunTimes.getValue()) {
				buffer[index++] = Double.toString(graphRunTimes.getMean());
				buffer[index++] = Double.toString(graphRunTimes.getStdev());
				buffer[index++] = Double.toString(graphRunTimes.getMin());
				buffer[index++] = Double.toString(graphRunTimes.getMax());
			}

			writer.writeNext(buffer);
		}

		writer.close();
	}
}
