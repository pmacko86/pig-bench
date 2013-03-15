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
	 * Average elements in an array
	 * 
	 * @param a the array
	 * @return the average
	 */
	public static double average(double[] a) {
		double t = 0; 
		for (double x : a) t += x;
		return t / a.length;
	}

	
	/**
	 * Find the minimum element in an array
	 * 
	 * @param a the array
	 * @return the minimum element
	 */
	public static double min(double[] a) {
		double t = a[0];
		for (double x : a) if (x < t) t = x;
		return t;
	}
	
	/**
	 * Find the maximum element in an array
	 * 
	 * @param a the array
	 * @return the maximum element
	 */
	public static double max(double[] a) {
		double t = a[0];
		for (double x : a) if (x > t) t = x;
		return t;
	}
	
	
	/**
	 * Compute standard deviation
	 * 
	 * @param a the array
	 * @return the standard deviation
	 */
	public static double stdev(double[] a) {
		double mean = average(a);
		double s = 0;
		for (double x : a) s += sqr(x - mean);
		return Math.sqrt(s / a.length);
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
	 * Square
	 * 
	 * @param v the value
	 */
	public static int sqr(int v) {
		return v*v;
	}
	
	
	/**
	 * Square
	 * 
	 * @param v the value
	 */
	public static long sqr(long v) {
		return v*v;
	}
	
	
	/**
	 * Square
	 * 
	 * @param v the value
	 */
	public static float sqr(float v) {
		return v*v;
	}
	
	
	/**
	 * Square
	 * 
	 * @param v the value
	 */
	public static double sqr(double v) {
		return v*v;
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
	 * Product of Doubles, accounting for nulls
	 * 
	 * @param values the values
	 * @return the result, or null if at least one of the values is null
	 */
	public static Double product(Double... values) {
		double r = 1;
		for (Double d : values) {
			if (d != null) {
				r *= d.doubleValue();
			}
			else {
				return null;
			}
		}
		return r;
	}
	
	
	/**
	 * Multiply an array by a scalar value, accounting for nulls
	 * 
	 * @param array the array
	 * @param scalar the scalar
	 * @return the result, or null if at least one of the values is null
	 */
	public static Double[] multiplyElementwise(Double[] array, Double scalar) {
		if (array == null) return null;
		Double[] r = new Double[array.length];
		if (scalar == null) return null;
		for (int i = 0; i < array.length; i++) {
			if (array[i] == null) return null;
			r[i] = scalar.doubleValue() * array[i].doubleValue();
		}
		return r;
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
	
	
	/**
	 * Return the given array of Doubles
	 * 
	 * @param values the values
	 * @return the values
	 */
	public static Double[] upgradeArray(double... values) {
		Double[] r = new Double[values.length];
		for (int i = 0; i < values.length; i++) {
			r[i] = values[i];
		}
		return r;
	}
	
	
	/**
	 * Return the given array of doubles
	 * 
	 * @param values the values
	 * @return the values
	 */
	public static double[] downgradeArray(Double... values) {
		double[] r = new double[values.length];
		for (int i = 0; i < values.length; i++) {
			r[i] = values[i].doubleValue();
		}
		return r;
	}
	
	
	/**
	 * Linear fit
	 * 
	 * @param x the array of x's
	 * @param y the array of y's
	 * @return the linear fit in the form [intercept, slope]
	 */
	public static double[] linearFit(double[] x, double[] y) {
		
		// Linear regression algorithm from:
		//     http://introcs.cs.princeton.edu/java/97data/LinearRegression.java.html

		int n = x.length;
		if (n != y.length) {
			throw new IllegalArgumentException("The x and y arrays need to be of the same size");
		}
		
		double sumx = 0, sumy = 0;
		for (int i = 0; i < n; i++) {
			sumx  += x[i];
			sumy  += y[i];
		}
		
		double xbar = sumx / n;
		double ybar = sumy / n;

		double xxbar = 0.0, xybar = 0.0;
		for (int i = 0; i < n; i++) {
			xxbar += (x[i] - xbar) * (x[i] - xbar);
			xybar += (x[i] - xbar) * (y[i] - ybar);
		}
		double beta1 = xybar / xxbar;
		double beta0 = ybar - beta1 * xbar;
		
		if (xxbar == 0) {
			beta0 = average(y);
			beta1 = 0;
		}

		return new double[] { beta0, beta1 };
	}
	
	
	/**
	 * Mean absolute square error of a linear fit
	 * 
	 * @param x the array of x's
	 * @param y the array of y's
	 * @param fit the linear fit in the form [intercept, slope]
	 * @return the MAE
	 */
	public static double meanAbsoluteError(double[] x, double[] y, double[] fit) {

		int n = x.length;
		if (n != y.length) {
			throw new IllegalArgumentException("The x and y arrays need to be of the same size");
		}
		
		double s = 0;
		for (int i = 0; i < n; i++) {
			double p = fit[1]*x[i] + fit[0];
			s += Math.abs(p - y[i]);
		}
		
		return s / n;
	}
	
	
	/**
	 * Median square error of a linear fit
	 * 
	 * @param x the array of x's
	 * @param y the array of y's
	 * @param fit the linear fit in the form [intercept, slope]
	 * @return the MedSE
	 */
	public static double medianSquareError(double[] x, double[] y, double[] fit) {

		int n = x.length;
		if (n != y.length) {
			throw new IllegalArgumentException("The x and y arrays need to be of the same size");
		}
		
		@SuppressWarnings("rawtypes")
		Triple[] data = new Triple[n];
		for (int i = 0; i < n; i++) {
			data[i] = new Triple<Double, Double, Integer>(x[i], y[i], i);
		}
		java.util.Arrays.sort(data);
		
		@SuppressWarnings("unchecked")
		Triple<Double, Double, Integer> median = data[n / 2];
		
		double p = fit[1]*median.getFirst().doubleValue() + fit[0];
		return sqr(p - median.getSecond().doubleValue());
	}
	
	
	/**
	 * Robust linear fit
	 * 
	 * @param x the array of x's
	 * @param y the array of y's
	 * @return the linear fit in the form [intercept, slope]
	 */
	public static double[] robustLinearFit(double[] x, double[] y) {
		
		if (x.length < 50) {
			throw new IllegalArgumentException("The number of samples is too small");
		}
		
		double bestError = Double.MAX_VALUE;
		double[] best = null;
		
		int[] samples = new int[x.length / 5];
		double[] sx = new double[samples.length];
		double[] sy = new double[samples.length];
		
		for (int pass = 0; pass < 20; pass++) {
			
			for (int i = 0; i < samples.length; i++) {
				int r;
				while (true) {
					r = (int) (Math.random() * x.length);
					boolean ok = true;
					for (int j = 0; j < i; j++) {
						if (samples[j] == r) {
							ok = false;
							break;
						}
					}
					if (ok) break;
				}
				samples[i] = r;
				sx[i] = x[i];
				sy[i] = y[i];
			}
			
			double[] fit = linearFit(sx, sy);
			double error = meanAbsoluteError(x, y, fit);
			
			if (error < bestError) {
				best = fit;
				bestError = error;
			}
		}
		
		return best;
	}
	
	
	/**
	 * Evaluate a polynomial
	 * 
	 * @param coefficients the coefficients of the polynomial in the form c[0] + c[1] * x + c[2] * x^2 + ...
	 * @param x the value of x
	 * @return the result
	 */
	public static double evaluatePolynomial(double[] coefficients, double x) {
		
		double r = 0;
		for (int i = coefficients.length - 1; i >= 0; i--) {
			r = (r * x) + coefficients[i];
		}
		
		return r;
	}
	
	
	/**
	 * Return the quantile
	 * 
	 * @param x the array of values
	 * @param q the quantile
	 * @return the quantile
	 */
	public static double quantile(double[] x, double q) {

		int n = x.length;
		double[] a = new double[n];
		System.arraycopy(x, 0, a, 0, n);
		java.util.Arrays.sort(a);
		
		int i = (int) Math.round(q * n);
		if (i >= n) i = n-1; 
		return a[i];
	}
	
	
	/**
	 * Filter pairwise based on the first array
	 * 
	 * @param a the primary array of values
	 * @param b the secondary array of values
	 * @param min the minimum value (inclusive)
	 * @param max the maximum value (inclusive)
	 * @return the new pair
	 */
	public static Pair<double[], double[]> filter(double[] a, double[] b, double min, double max) {

		int n = a.length;
		if (n != b.length) {
			throw new IllegalArgumentException("The a and b arrays need to be of the same size");
		}

		int count = 0;
		for (int i = 0; i < n; i++) {
			if (a[i] >= min && a[i] <= max) count++;
		}
		
		double[] a2 = new double[count];
		double[] b2 = new double[count];
		
		int j = 0;
		for (int i = 0; i < n; i++) {
			if (a[i] >= min && a[i] <= max) {
				a2[j] = a[i];
				b2[j] = b[i];
				j++;
			}
		}
		
		assert j == count;
		return new Pair<double[], double[]>(a2, b2);
	}
}
