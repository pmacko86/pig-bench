package com.tinkerpop.bench.analysis;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.tinkerpop.bench.log.OperationLogEntry;
import com.tinkerpop.bench.util.MathUtils;


/**
 * Statistics about an operation
 * 
 * @author Peter Macko (pmacko@eecs.harvard.edu)
 */
public class OperationStats implements Serializable {
	
	private static final long serialVersionUID = 3469953900075433166L;
	
	transient AnalysisContext context = null;
	private String operationName;
	private double dropExtremes = 0;

	private boolean many = false;
	
	private int numEntries = 0;
	private int numOperations = 0;
	
	private double averageRuntimeMS = 0;
	private double stdevRuntimeMS = 0;
	private double minRuntimeMS = Integer.MAX_VALUE;
	private double maxRuntimeMS = Integer.MIN_VALUE;
	
	private double[] averageResults = null;
	private double[][] linearFitsForResults = null;
	
	
	/**
	 * Create an instance of class OperationStatus
	 * 
	 * @param context the analysis context
	 * @param operationName the operation name with tags
	 * @param dropExtremes the fraction of extreme values to drop
	 */
	OperationStats(AnalysisContext context, String operationName, double dropExtremes) {
		
		this.context = context;
		this.operationName = operationName;
		this.dropExtremes = dropExtremes;
		
		many = AnalysisUtils.isManyOperation(operationName);
		int opCountArg = !many ? -1 : AnalysisUtils.getManyOperationOpCountArgumentIndex(operationName);

		
		// Get the entries and drop those with the extreme values
		
		List<OperationLogEntry> entries = context.getTailEntries(operationName);
		
		if (this.dropExtremes > 0 && entries.size() > 0) {
			
			long[] a = new long[entries.size()];
			int i = 0;
			for (OperationLogEntry t : entries) a[i++] = t.getTime();
			Arrays.sort(a);
			long min = a[(int) (dropExtremes * a.length)];
			long max = a[(int) ((1 - dropExtremes) * a.length)];
			
			ArrayList<OperationLogEntry> l = new ArrayList<OperationLogEntry>();
			for (OperationLogEntry t : entries) {
				long v = t.getTime();
				if (v >= min && v <= max) l.add(t);
			}
			entries = l;
		}
		
		
		// Compute the basic statistics: counts and averages
		
		double sumTime = 0;
		
		int[] resultCounts = null;
		double[] resultSums = null;
		
		double[] entryRuntimes = new double[entries.size()];
		double[] operationRuntimes = new double[entries.size()];
		
		for (OperationLogEntry t : entries) {
			
			// The number of included log entries
			
			numEntries++;
			
			
			// Operation count
			
			int c;
			if (many) {
				String s = t.getArgs()[opCountArg >= 0 ? opCountArg : t.getArgs().length + opCountArg];
				int opCount = Integer.parseInt(s);
				if (!s.equals("" + opCount)) throw new NumberFormatException(s);
				c = opCount;
			}
			else {
				c = 1;
			}
			
			numOperations += c;
			
			
			// Time
			
			double r = t.getTime() / (1000000.0 * c);
			if (r < minRuntimeMS) minRuntimeMS = r;
			if (r > maxRuntimeMS) maxRuntimeMS = r;
			
			entryRuntimes[numEntries - 1] = t.getTime() / 1000000.0;
			operationRuntimes[numEntries - 1] = r;
			
			sumTime += t.getTime() / 1000000.0;
		
			
			// Results
			
			String[] result = t.getResult().split(":");
			
			if (resultCounts == null) {
				resultCounts = new int[result.length];
			}
			else if (resultCounts.length < result.length) {
				int[] a = new int[result.length];
				System.arraycopy(resultCounts, 0, a, 0, resultCounts.length);
				for (int i = resultCounts.length; i < a.length; i++) a[i] = 0;
				resultCounts = a;
			}
			
			if (resultSums == null) {
				resultSums = new double[result.length];
			}
			else if (resultSums.length < result.length) {
				double[] a = new double[result.length];
				System.arraycopy(resultSums, 0, a, 0, resultSums.length);
				for (int i = resultSums.length; i < a.length; i++) a[i] = 0;
				resultSums = a;
			}
			
			for (int i = 0; i < result.length; i++) {
				resultCounts[i]++;
				resultSums[i] += Double.parseDouble(result[i]);
			}
		}
		
		if (numEntries > 0) {
			averageRuntimeMS = sumTime / numOperations;
			stdevRuntimeMS = MathUtils.stdev(operationRuntimes);
			
			averageResults = new double[resultCounts.length];
			for (int i = 0; i < resultCounts.length; i++) {
				averageResults[i] = resultSums[i] / (double) resultCounts[i];
			}
		}
		
		
		// Compute the linear fits for the results
		
		// Note: This currently assumes that all operations have exactly the same number
		//       of declared result fields.
		
		if (numEntries > 0) {
			linearFitsForResults = new double[resultCounts.length][];
			
			for (int i = 0; i < resultCounts.length; i++) {
				
				double[] x = new double[numEntries];
				int j = 0;
				for (OperationLogEntry t : entries) {
					String[] result = t.getResult().split(":");
					x[j++] = Double.parseDouble(result[i]);
				}
				
				linearFitsForResults[i] = MathUtils.linearFit(x, operationRuntimes);
			}
		}
	}
	
	
	/**
	 * Get the analysis context
	 * 
	 * @return the analysis context
	 */
	public AnalysisContext getContext() {
		return context;
	}
	
	
	/**
	 * Get the operation name
	 * 
	 * @return the operation name, including all tags
	 */
	public String getOperationName() {
		return operationName;
	}
	
	
	/**
	 * Get the number of entries of recorded operations
	 * 
	 * @return the number of entries
	 */
	public int getNumEntries() {
		return numEntries;
	}
	
	
	/**
	 * Get the number of executed operations. This is the same as the number
	 * of entries for regular operations, but larger for the "Many" operations.
	 * 
	 * @return the number of operations
	 */
	public int getNumOperations() {
		return numOperations;
	}
	
	
	/**
	 * Get the average operation runtime. This is the average operation runtime
	 * for an operation, not for a log entry. 
	 * 
	 * @return the average runtime in milliseconds
	 */
	public double getAverageOperationRuntime() {
		return averageRuntimeMS;
	}
	
	
	/**
	 * Get the minimum operation runtime. This is the minimum operation runtime
	 * for an operation, not for a log entry. 
	 * 
	 * @return the minimum runtime in milliseconds
	 */
	public double getMinOperationRuntime() {
		return minRuntimeMS;
	}
	
	
	/**
	 * Get the maximum operation runtime. This is the maximum operation runtime
	 * for an operation, not for a log entry. 
	 * 
	 * @return the maximum runtime in milliseconds
	 */
	public double getMaxOperationRuntime() {
		return maxRuntimeMS;
	}
	
	
	/**
	 * Get the standard deviation for the operation runtime. This is for the
	 * operation runtime for an operation, not for a log entry. 
	 * 
	 * @return the standard deviation of the operation runtime in milliseconds
	 */
	public double getOperationRuntimeStdev() {
		return stdevRuntimeMS;
	}
	
	
	/**
	 * Get the average values for the result fields
	 * 
	 * @return the array of average values for the result fields
	 */
	public double[] getAverageResults() {
		return averageResults;
	}
	
	
	/**
	 * Get the average value for the given result field
	 * 
	 * @param index the field index
	 * @return the average value for the given field
	 */
	public double getAverageResult(int index) {
		return averageResults[index];
	}
	
	
	/**
	 * Get the linear fit coefficients for the runtime in ms vs. the result value.
	 * Please note that the result values are not adjusted by the operation count
	 * for the "Many" operations.
	 * 
	 * @return the array of linear fit coefficients, one set for each result field
	 */
	public double[][] getLinearFitsForResults() {
		return linearFitsForResults;
	}
	
	
	/**
	 * Get the linear fit coefficients for the runtime in ms vs. the result value.
	 * Please note that the result values are not adjusted by the operation count
	 * for the "Many" operations.
	 * 
	 * @param index the field index
	 * @return the linear fit coefficients
	 */
	public double[] getLinearFitsForResult(int index) {
		return linearFitsForResults[index];
	}
}
