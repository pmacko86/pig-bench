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
	 * Log
	 * 
	 * @param x the number
	 * @param b the base
	 * @return the log
	 */
	public static double log(double x, double b) {
		return Math.log(x) / Math.log(b);
	}
}
