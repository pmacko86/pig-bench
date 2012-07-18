package com.tinkerpop.bench.util;
import java.io.Serializable;


/**
 * A generic immutable pair
 * 
 * @author Peter Lawrey
 * @author Peter Macko
 *
 * @param <T1> the type of the first object
 * @param <T2> the type of the second object
 * @param <T3> the type of the second object
 */
public final class Triple<T1, T2, T3> implements Comparable<Triple<T1, T2, T3>>, Serializable {
	
	private static final long serialVersionUID = 1L;
	
	public final T1 first;
	public final T2 second;
	public final T3 third;

	
	/**
	 * Create an instance of class Pair
	 * 
	 * @param first the first element
	 * @param second the second element
	 * @param third the third element
	 */
	public Triple(T1 first, T2 second, T3 third) {
		this.first = first;
		this.second = second;
		this.third = third;
	}
	
	
	/**
	 * Return the first element
	 * 
	 * @return the first element
	 */
	public T1 getFirst() {
		return first;
	}
	
	
	/**
	 * Return the second element
	 * 
	 * @return the second element
	 */
	public T2 getSecond() {
		return second;
	}
	
	
	/**
	 * Return the third element
	 * 
	 * @return the third element
	 */
	public T3 getThird() {
		return third;
	}

	
	/**
	 * Compare this pair to another pair of the same type
	 * 
	 * @param o the other pair
	 * @return the result of the comparison
	 */
	@Override
	public int compareTo(Triple<T1, T2, T3> o) {
		int cmp1 = compare(first, o.first);
		int cmp2 = cmp1 == 0 ? compare(second, o.second) : cmp1;
		int cmp3 = cmp2 == 0 ? compare(third, o.third) : cmp2;
		return cmp3;
	}

	
	/**
	 * Compare two objects
	 * 
	 * @param o1 the first object
	 * @param o2 the second object
	 * @return the result of the comparison
	 */
	@SuppressWarnings("unchecked")
	private static <T> int compare(T o1, T o2) {
		return o1 == null ? o2 == null ? 0 : -1 : o2 == null ? +1
				: ((Comparable<T>) o1).compareTo(o2);
	}

	
	/**
	 * Compute the hash code
	 * 
	 * @return the hash code
	 */
	@Override
	public int hashCode() {
		return (31 * hashcode(first) + hashcode(second)) ^ hashcode(third);
	}

	
	/**
	 * Compute the hash code of the given object
	 * 
	 * @param o the object
	 * @return the hash code
	 */
	private static int hashcode(Object o) {
		return o == null ? 0 : o.hashCode();
	}

	
	/**
	 * Check whether this object is equal to the other object
	 * 
	 * @param obj the other object
	 * @return true if they are equal
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Triple<?, ?, ?>))
			return false;
		if (this == obj)
			return true;
		return equal(first, ((Triple<T1, T2, T3>) obj).first)
				&& equal(second, ((Triple<T1, T2, T3>) obj).second)
				&& equal(third, ((Triple<T1, T2, T3>) obj).third);
	}

	
	/**
	 * Check two objects for equality
	 * 
	 * @param o1 the first object
	 * @param o2 the second object
	 * @return true if they are equal
	 */
	private boolean equal(Object o1, Object o2) {
		return o1 == null ? o2 == null : (o1 == o2 || o1.equals(o2));
	}

	
	/**
	 * Return the string representation of the pair
	 * 
	 * @return the string representation
	 */
	@Override
	public String toString() {
		return "(" + first + ", " + second + ", " + third + ')';
	}
}
