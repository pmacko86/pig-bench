package com.tinkerpop.bench.util;


/**
 * A comparator for longs
 * 
 * @author Peter Macko
 */
public interface LongComparator {

	/**
	 * Compare two longs
	 * 
	 * @param v1 the first value
	 * @param v2 he second value
	 * @return a negative number if v1 < v2, zero if v1 == v2, or a positive number if v1 > v2
	 */
	public int compare(long v1, long v2);
}
