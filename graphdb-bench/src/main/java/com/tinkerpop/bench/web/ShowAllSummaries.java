package com.tinkerpop.bench.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.tinkerpop.bench.DatabaseEngine;
import com.tinkerpop.bench.analysis.AnalysisContext;
import com.tinkerpop.bench.analysis.AnalysisUtils;
import com.tinkerpop.bench.analysis.IngestAnalysis;
import com.tinkerpop.bench.log.SummaryLogEntry;
import com.tinkerpop.bench.log.SummaryLogReader;
import com.tinkerpop.bench.util.Pair;


/**
 * A servlet for showing all summary data 
 * 
 * @author Peter Macko (pmacko@eecs.harvard.edu)
 */
@SuppressWarnings("serial")
public class ShowAllSummaries extends HttpServlet {
	
	/**
	 * Create an instance of class ShowAllSummaries
	 */
	public ShowAllSummaries() {
	}

	
	/**
	 * Respond to a POST request
	 */
	@Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
		doGet(request, response);
    }
	
	
	/**
	 * Respond to a GET request
	 */
	@Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
		// Get all database engine/instance pairs
		
		Collection<Pair<String, String>> pairs = WebUtils.getDatabaseInstancePairs();
		SortedSet<DatabaseEngineAndInstance> dbeis = new TreeSet<DatabaseEngineAndInstance>();
		
		if (pairs != null) {
			for (Pair<String, String> p : pairs) {
				dbeis.add(new DatabaseEngineAndInstance(DatabaseEngine.ENGINES.get(p.getFirst()), p.getSecond()));
			}
		}
		
		
		// Now for each DBEI, get the map of all operation names to latest jobs
		
		SortedMap<DatabaseEngineAndInstance, Map<String, SortedSet<Job>>> dbeiOperationMap
			= new TreeMap<DatabaseEngineAndInstance, Map<String, SortedSet<Job>>>();
		SortedSet<String> operationNames = new TreeSet<String>();
		
		for (DatabaseEngineAndInstance dbei : dbeis) {
			AnalysisContext m = AnalysisContext.getInstance(dbei);
			Map<String, SortedSet<Job>> map = m.getJobsForAllOperationsWithTags();
			dbeiOperationMap.put(dbei, map);
			operationNames.addAll(map.keySet());
		}
		
		
		// Short case?
		
		boolean shortCase = WebUtils.getBooleanParameter(request, "short", false);
		if (shortCase) {
			operationNames.clear();
			operationNames.add("OperationGetManyVertices");
			operationNames.add("OperationGetManyEdges");
			operationNames.add("OperationGetManyEdgeProperties-time");
			operationNames.add("OperationGetManyVertexProperties-age");
			operationNames.add("OperationGetManyVertexProperties-name");
			operationNames.add("OperationGetFirstNeighbor-both");
			operationNames.add("OperationGetFirstNeighbor-in");
			operationNames.add("OperationGetFirstNeighbor-out");
			operationNames.add("OperationGetKHopNeighbors-both-1");
			operationNames.add("OperationGetKHopNeighbors-both-2");
			operationNames.add("OperationGetKHopNeighbors-both-3");
			operationNames.add("OperationGetKHopNeighbors-both-4");
			operationNames.add("OperationGetKHopNeighbors-both-5");
			operationNames.add("OperationGetShortestPath");
			operationNames.add("OperationLocalClusteringCoefficient");
			operationNames.add("OperationAddManyVertices");
			operationNames.add("OperationAddManyEdges");
			operationNames.add("OperationSetManyEdgeProperties");
			operationNames.add("OperationSetManyVertexProperties");
			operationNames.add("OperationGetVerticesUsingKeyIndex-_original_id");
		}
		
		
		// Start the response
		
        response.setContentType("text/plain");
        response.setStatus(HttpServletResponse.SC_OK);

        
        // Print the header
        
        String delimiter = "\t";
        PrintWriter writer = response.getWriter();
        
        writer.print("dbname");
        writer.print(delimiter);
        writer.print("dbinstance");
        writer.print(delimiter);
        writer.print("edgelabels");
        
        for (String s : operationNames) {
        	boolean many = AnalysisUtils.isManyOperation(s);
            writer.print(delimiter);
        	writer.print(many ? AnalysisUtils.convertNameOfManyOperation(s) : s);
        }
        
        writer.print(delimiter);
        writer.print("BulkIngest");
        writer.print(delimiter);
        writer.print("BulkIngestShutdown");
        writer.print(delimiter);
        writer.print("IncIngest");
        writer.print(delimiter);
        writer.print("IncIngestShutdown");
        writer.print(delimiter);
        writer.print("IndexCreate");
        writer.print(delimiter);
        writer.print("IndexShutdown");
        
        writer.println();
        
        
        // Print the data
        
        for (Map.Entry<DatabaseEngineAndInstance, Map<String, SortedSet<Job>>> data : dbeiOperationMap.entrySet()) {
            
        	DatabaseEngineAndInstance dbei = data.getKey();
        	String dbi_simple = dbei.getInstanceSafe("default");
        	String dbi_el = "none";
        	if (dbi_simple.endsWith("_1el") || dbi_simple.endsWith("_2el")) {
        		dbi_el = dbi_simple.substring(dbi_simple.length() - 3);
        		dbi_simple = dbi_simple.substring(0, dbi_simple.length() - 4);
        	}
        	
            writer.print(dbei.getEngine().getLongName());
            writer.print(delimiter);
            writer.print(dbi_simple);
            writer.print(delimiter);
            writer.print(dbi_el);
            
            Map<String, SortedSet<Job>> operationMap = data.getValue();
            for (String s : operationNames) {
            	try {
	            	SortedSet<Job> jobs = operationMap.get(s);
	            	if (jobs == null || jobs.isEmpty()) {
	                    writer.print(delimiter);
	                    writer.print("none");
	                    continue;
	            	}
	            	Job job = jobs.last();
	            	
	            	SummaryLogEntry entry = SummaryLogReader.getEntryForOperation(job.getSummaryFile(), s);
	            	if (AnalysisUtils.isManyOperation(s)) entry = AnalysisUtils.convertLogEntryForManyOperation(entry, job);
	            	
	                writer.print(delimiter);
	            	writer.print(entry.getDefaultRunTimes().getMean() / 1000000.0);
            	}
            	catch (Exception e) {
            		writer.println();
            		writer.println();
            		writer.println("Error: An exception occurred while processing " + s + " of " + dbei);
            		writer.println();
            		e.printStackTrace(writer);
                    writer.flush();
                    response.flushBuffer();
                    if (e instanceof RuntimeException)
                    	throw (RuntimeException) e;
                    else
                    	throw new RuntimeException(e);
            	}
            }
            
            IngestAnalysis ia = IngestAnalysis.getInstance(dbei);
            
            writer.print(delimiter);
            writer.print(ia.getBulkLoadTime());
            writer.print(delimiter);
            writer.print(ia.getBulkLoadShutdownTime());
            writer.print(delimiter);
            writer.print(ia.getIncrementalLoadTime());
            writer.print(delimiter);
            writer.print(ia.getIncrementalLoadShutdownTime());
            writer.print(delimiter);
            writer.print(ia.getCreateIndexTime());
            writer.print(delimiter);
            writer.print(ia.getCreateIndexShutdownTime());
            
            writer.println();
            writer.flush();
            response.flushBuffer();
        }
	}
}
