package com.tinkerpop.bench.analysis;

import java.util.HashMap;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;

import com.tinkerpop.bench.log.GraphRunTimes;
import com.tinkerpop.bench.log.OperationLogEntry;
import com.tinkerpop.bench.log.OperationLogReader;
import com.tinkerpop.bench.log.SummaryLogEntry;
import com.tinkerpop.bench.util.MathUtils;
import com.tinkerpop.bench.web.Job;


/**
 * Miscellaneous utilities for analysis
 *  
 * @author Peter Macko
 */
public class AnalysisUtils {
	
	
	/**
	 * Determine if this is a "Many" operation. "Many" operations are implemented as a tight
	 * loop around a statistically significant number of micro operations.
	 * 
	 * @param operationName the operation name
	 * @return true if it is a "Many" operation
	 */
	public static boolean isManyOperation(String operationName) {
		return operationName.startsWith("OperationGetMany")
				|| operationName.startsWith("OperationAddMany")
				|| operationName.startsWith("OperationSetMany");
	}
	
	
	/**
	 * Return the argument index that corresponds to the number of micro operations executed
	 * by this is a "Many" operation.
	 * 
	 * @param operationName the operation name
	 * @return the argument number
	 * @throws IllegalArgumentException if this is not a "Many" operation, or if it is currently unsupported
	 */
	public static int getManyOperationOpCountArgumentIndex(String operationName) {
		
		String s = operationName;
		if (s.indexOf('-') > 0) s = s.substring(0, s.indexOf('-'));
		
		if (s.equals("OperationGetManyVertices"          )) return 0;
		if (s.equals("OperationGetManyEdges"             )) return 0;
		if (s.equals("OperationGetManyVertexProperties"  )) return 1;
		if (s.equals("OperationGetManyEdgeProperties"    )) return 1;
		if (s.equals("OperationGetManyVertexPropertySets")) return -1;
		if (s.equals("OperationGetManyEdgePropertySets"  )) return -1;
		if (s.equals("OperationAddManyVertices"          )) return 0;
		if (s.equals("OperationAddManyEdges"             )) return 0;
		if (s.equals("OperationSetManyVertexProperties"  )) return 1;
		if (s.equals("OperationSetManyEdgeProperties"    )) return 1;
		
		throw new IllegalArgumentException("Invalid or unsupported \"Many\" operation " + operationName);
	}
	
	
	/**
	 * Convert a name of a "Many" operation to the name of the corresponding micro operation
	 * 
	 * @param operationName the name of the "Many" operation
	 * @return the name of the corresponding micro operation
	 */
	public static String convertNameOfManyOperation(String operationName) {
		
		String s = operationName;
		
		s = s.replaceFirst("OperationGetMany", "OperationGet");
		s = s.replaceFirst("OperationAddMany", "OperationAdd");
		s = s.replaceFirst("OperationSetMany", "OperationSet");
		
		s = s.replaceFirst("Edges$"     , "Edge"     );
		s = s.replaceFirst("Vertices$"  , "Vertex"   );
		s = s.replaceFirst("Properties$", "Property" );
		s = s.replaceFirst("Sets$"      , "Set"      );
		
		s = s.replaceFirst("Edges-"     , "Edge-"    );
		s = s.replaceFirst("Vertices-"  , "Vertex-"  );
		s = s.replaceFirst("Properties-", "Property-");
		s = s.replaceFirst("Sets-"      , "Set-"     );
		
		return s;
	}
	
	
	/**
	 * Convert a log entry corresponding to a "Many" operation to an equivalent entry that approximates
	 * an average micro operation inside the "Many" operation. Please note that this is very approximate,
	 * and not all fields can be properly converted. 
	 * 
	 * @param entry the log entry
	 * @return a new log entry that corresponds to one of the micro operations
	 * @throws IllegalArgumentException if this is not a "Many" operation, or if it is currently unsupported
	 */
	public static OperationLogEntry convertLogEntryForManyOperation(OperationLogEntry entry) {
		
		int opCountArg = getManyOperationOpCountArgumentIndex(entry.getName());
		
		String s_opCount = entry.getArgs()[opCountArg];
		int opCount = Integer.parseInt(s_opCount);
		if (opCount <= 0) throw new IllegalArgumentException("Invalid operation count");
		
		return new OperationLogEntry(
				entry.getOpId(),
				convertNameOfManyOperation(entry.getName()),
				entry.getType(),
				entry.getArgsString(),
				Math.round(entry.getTime() / (double) opCount),
				entry.getResult(),
				entry.getMemory(),
				Math.round(Math.ceil(entry.getGCCount () / (double) opCount)),
				Math.round(Math.ceil(entry.getGCTimeMS() / (double) opCount))
				);
	}
	
	
	/**
	 * Convert a log entry corresponding to a "Many" operation to an equivalent entry that approximates
	 * an average micro operation inside the "Many" operation. Please note that this is very approximate,
	 * and not all fields can be properly converted. Since the operation count is not available in the
	 * log entry, the method actually recomputes it from the log file. 
	 * 
	 * @param entry the log entry
	 * @param job the job that this entry is from
	 * @return a new log entry that corresponds to one of the micro operations
	 * @throws IllegalArgumentException if this is not a "Many" operation, or if it is currently unsupported
	 */
	public static SummaryLogEntry convertLogEntryForManyOperation(SummaryLogEntry entry, Job job) {
		
		int opCountArg = getManyOperationOpCountArgumentIndex(entry.getName());
		
		GraphRunTimes runTimes = entry.getDefaultRunTimes();
		if (entry.getRunTimes().size() != 1) {
			throw new UnsupportedOperationException("Not supported if the size of entry.getRunTimes() is not 1");
		}
		
		
		// Reread the actual log file
		
		double time = 0;
		int count = 0;
		double min = 0;
		double max = 0;
		
		Vector<Double> averageRunTimes = new Vector<Double>();
		
		OperationLogReader reader = new OperationLogReader(job.getLogFile(), entry.getName());
		for (OperationLogEntry e : reader) {
			String s = e.getArgs()[opCountArg >= 0 ? opCountArg : e.getArgs().length + opCountArg];
			int opCount;
			try {
				opCount = Integer.parseInt(s);
			}
			catch (NumberFormatException ex) {
				throw new RuntimeException("Cannot parse argument " + opCountArg + " of: " + StringUtils.join(e.getArgs(), ", "), ex);
			}
			
			double t = e.getTime() / (double) opCount;
			if (count == 0 || t < min) min = t;
			if (count == 0 || t > max) max = t;
			averageRunTimes.add(t);
			
			count += opCount;
			time += e.getTime();
		}
		
		
		// Finish
		
		String operationName = convertNameOfManyOperation(entry.getName());
		double stdev = MathUtils.stdev(MathUtils.downgradeArray(averageRunTimes.toArray(new Double[0])));
		
		GraphRunTimes newRunTimes = new GraphRunTimes(runTimes.getGraphName(),
				operationName, time / count, stdev, min, max);
		
		HashMap<String, GraphRunTimes> m = new HashMap<String, GraphRunTimes>();
		m.put(newRunTimes.getGraphName(), newRunTimes);
		
		return new SummaryLogEntry(operationName, m);
	}
}
