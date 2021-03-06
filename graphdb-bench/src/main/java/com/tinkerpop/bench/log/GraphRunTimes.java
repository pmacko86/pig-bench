package com.tinkerpop.bench.log;

import java.util.ArrayList;
import java.util.Arrays;


/**
 * Encapsulates the run times for one Graph & one Operation
 */
public class GraphRunTimes implements Comparable<GraphRunTimes> {
	
	private String graphName = null;
	private String operationName = null;
	private ArrayList<Long> runTimes = new ArrayList<Long>();
	
	private double mean = Double.MIN_VALUE;
	private double stdev = Double.MIN_VALUE;
	private double min = Double.MIN_VALUE;
	private double max = Double.MIN_VALUE;
	
	public GraphRunTimes(String graphName, String operationName) {
		this.graphName = graphName;
		this.operationName = operationName;
	}
	
	public GraphRunTimes(String graphName, String operationName, double mean, double stdev, double min, double max) {
		this.graphName = graphName;
		this.operationName = operationName;
		this.mean = mean;
		this.stdev = stdev;
		this.min = min;
		this.max = max;
	}

	public void add(long runTime) {
		mean = Double.MIN_VALUE;
		stdev = Double.MIN_VALUE;
		min = Double.MIN_VALUE;
		max = Double.MIN_VALUE;
		runTimes.add(runTime);
	}

	public String getGraphName() {
		return graphName;
	}

	public String getOperationName() {
		return operationName;
	}

	public double getMean() {
		return (Double.MIN_VALUE == mean) ? calcMean() : mean;
	}

	public double getStdev() {
		return (Double.MIN_VALUE == stdev) ? calcStdev() : stdev;
	}

	public double getMin() {
		return (Double.MIN_VALUE == min) ? calcMin() : min;
	}

	public double getMax() {
		return (Double.MIN_VALUE == max) ? calcMax() : max;
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
	
	public void retainTail(int tail, double tailFraction, double dropExtremes) {
		
		mean = Double.MIN_VALUE;
		stdev = Double.MIN_VALUE;
		min = Double.MIN_VALUE;
		max = Double.MIN_VALUE;

		if (tail > 0 || tailFraction > 0) {
			int n = runTimes.size();
			int keep1 = tail > 0 ? Math.max(tail, n) : n;
			int keep2 = tailFraction > 0 ? (int) Math.round(tailFraction * n) : n;
			int keep  = Math.min(keep1, keep2);
		
			if (keep > 0 && runTimes.size() > keep) {
				ArrayList<Long> a = new ArrayList<Long>();
				for (int i = runTimes.size() - keep; i < runTimes.size(); i++) {
					a.add(runTimes.get(i));
				}
				runTimes = a;
			}
		}

		if (dropExtremes > 0 && runTimes.size() > 0) {
			
			long[] a = new long[runTimes.size()];
			for (int i = 0; i < a.length; i++) a[i] = runTimes.get(i);
			Arrays.sort(a);
			long min = a[(int) (dropExtremes * a.length)];
			long max = a[(int) ((1 - dropExtremes) * a.length)];
			
			ArrayList<Long> l = new ArrayList<Long>();
			for (Long t : runTimes) {
				long v = t.longValue();
				if (v >= min && v <= max) l.add(t);
			}
			runTimes = l;
		}
	}
}
