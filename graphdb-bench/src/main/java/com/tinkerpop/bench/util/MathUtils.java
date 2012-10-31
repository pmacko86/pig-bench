package com.tinkerpop.bench.util;


/**
 * Miscellaneous math utils
 * 
 * @author Peter Macko
 */
public class MathUtils {

	
	/**
	 * Sum elements in an array
	 * 
	 * @param a the array
	 * @return the sum
	 */
	public static int sum(int[] a) {
		int t = 0; 
		for (int x : a) t += x;
		return t;
	}

	
	/**
	 * Sum elements in an array
	 * 
	 * @param a the array
	 * @return the sum
	 */
	public static long sum(long[] a) {
		long t = 0; 
		for (long x : a) t += x;
		return t;
	}

	
	/**
	 * Sum elements in an array
	 * 
	 * @param a the array
	 * @return the sum
	 */
	public static float sum(float[] a) {
		float t = 0; 
		for (float x : a) t += x;
		return t;
	}

	
	/**
	 * Sum elements in an array
	 * 
	 * @param a the array
	 * @return the sum
	 */
	public static double sum(double[] a) {
		double t = 0; 
		for (double x : a) t += x;
		return t;
	}
	
	
	/**
	 * Adjust the elements in the array, so that they sum up approximately to
	 * the given value, and so that each element is at least the specified
	 * minimum value. This method expects all numbers in the array to be
	 * non-negative, and their sum must be greater than 0.
	 * 
	 * @param a the array
	 * @param desiredTotal the desired sum
	 * @param minValue the minimum value of each element
	 * @return the adjusted array
	 */
	public static int[] adjustSumApproximate(int[] a, int desiredTotal, int minValue) {
		
		double f = desiredTotal / (double) sum(a);
		
		int[] adjusted = new int[a.length];
		for (int i = 0; i < a.length; i++) {
			adjusted[i] = (int) Math.round(a[i] * f);
			if (adjusted[i] < minValue) adjusted[i] = minValue;
		}
		
		return adjusted;
	}
	
	
	/**
	 * Adjust the elements in the array, so that they sum up approximately to
	 * the given value, and so that each element is at least the specified
	 * minimum value. This method expects all numbers in the array to be
	 * non-negative, and their sum must be greater than 0.
	 * 
	 * @param a the array
	 * @param desiredTotal the desired sum
	 * @param minValue the minimum value of each element
	 * @return the adjusted array
	 */
	public static long[] adjustSumApproximate(long[] a, long desiredTotal, long minValue) {
		
		double f = desiredTotal / (double) sum(a);
		
		long[] adjusted = new long[a.length];
		for (int i = 0; i < a.length; i++) {
			adjusted[i] = Math.round(a[i] * f);
			if (adjusted[i] < minValue) adjusted[i] = minValue;
		}
		
		return adjusted;
	}
	
	
	/**
	 * Convert an array of integers to an array of strings
	 * 
	 * @param a the array
	 * @return the array of strings
	 */
	public static String[] toStringArray(int[] a) {
		
		String[] s = new String[a.length];
		for (int i = 0; i < a.length; i++) {
			s[i] = String.valueOf(a[i]);
		}

		return s;
	}
	
	
	/**
	 * Convert an array of longs to an array of strings
	 * 
	 * @param a the array
	 * @return the array of strings
	 */
	public static String[] toStringArray(long[] a) {
		
		String[] s = new String[a.length];
		for (int i = 0; i < a.length; i++) {
			s[i] = String.valueOf(a[i]);
		}

		return s;
	}
	
	
	/**
	 * Convert an array of Strings to an array of ints
	 * 
	 * @param a the array of strings
	 * @return the array of ints
	 */
	public static int[] fromStringArray(String[] a) {
		
		int[] r = new int[a.length];
		for (int i = 0; i < a.length; i++) {
			r[i] = Integer.parseInt(a[i]);
		}

		return r;
	}
	
	
	/**
	 * Convert an array of Strings to an array of longs
	 * 
	 * @param a the array of strings
	 * @return the array of longs
	 */
	public static long[] fromStringArrayLong(String[] a) {
		
		long[] r = new long[a.length];
		for (int i = 0; i < a.length; i++) {
			r[i] = Long.parseLong(a[i]);
		}

		return r;
	}

	
	/**
	 * Log
	 * 
	 * @param x the number
	 * @param b the base
	 * @return the log
	 */
	public static double log(double x, double b) {
		return Math.log(x) / Math.log(b);
	}
	
	
	/**
	 * Round to the nearest integer
	 * 
	 * @param v the value
	 */
	public static int round(double v) {
		return (int) Math.round(v);
	}
	
	
	/**
	 * Sub array
	 * 
	 * @param a the array
	 * @param start the start offset
	 * @param end the end offset (exclusive)
	 */
	public static int[] subarray(int[] a, int start, int end) {
		int[] r = new int[end - start];
		System.arraycopy(a, start, r, 0, end - start);
		return r;
	}
	
	
	/**
	 * Sub array
	 * 
	 * @param a the array
	 * @param start the start offset
	 * @param end the end offset (exclusive)
	 */
	public static String[] subarray(String[] a, int start, int end) {
		String[] r = new String[end - start];
		System.arraycopy(a, start, r, 0, end - start);
		return r;
	}
	
	
	/**
	 * Average of Doubles, ignoring nulls
	 * 
	 * @param values the values
	 * @return the average, or null if none
	 */
	public static Double averageIgnoreNulls(Double... values) {
		double sum = 0;
		int count = 0;
		for (Double d : values) {
			if (d != null) {
				sum += d.doubleValue();
				count++;
			}
		}
		if (count == 0) return null;
		return sum / count;
	}
	
	
	/**
	 * Sum of Doubles, accounting for nulls
	 * 
	 * @param values the values
	 * @return the result, or null if at least one of the values is null
	 */
	public static Double sum(Double... values) {
		double sum = 0;
		for (Double d : values) {
			if (d != null) {
				sum += d.doubleValue();
			}
			else {
				return null;
			}
		}
		return sum;
	}
	
	
	/**
	 * Return the given array of Doubles, but only if none of them is null
	 * 
	 * @param values the values
	 * @return the values, or null if at least one of the values is null
	 */
	public static Double[] ifNeitherIsNull(Double... values) {
		for (Double d : values) {
			if (d == null) {
				return null;
			}
		}
		return values;
	}
}
