package com.tinkerpop.bench.web;

import java.util.Comparator;

import com.tinkerpop.bench.DatabaseEngine;
import com.tinkerpop.bench.util.NaturalStringComparator;


/**
 * A database engine and instance pair
 * 
 * @author Peter Macko
 */
public class DatabaseEngineAndInstance implements Comparable<DatabaseEngineAndInstance> {

	private static Comparator<String> instanceComparator = new NaturalStringComparator();
	
	private DatabaseEngine engine; 
	private String instance;
	
	
	/**
	 * Create an instance of class DatabaseEngineAndInstance
	 * 
	 * @param engine the database engine
	 * @param instance the database instance name or null for default
	 */
	public DatabaseEngineAndInstance(DatabaseEngine engine, String instance) {
		
		this.engine = engine;
		
		if (instance == null) {
			this.instance = instance;
		}
		else {
			this.instance = "".equals(instance) ? null : instance;
		}
	}


	/**
	 * Get the database engine
	 * 
	 * @return the engine
	 */
	public DatabaseEngine getEngine() {
		return engine;
	}


	/**
	 * Get the database instance name
	 * 
	 * @return the instance, or null if none
	 */
	public String getInstance() {
		return instance;
	}


	/**
	 * Get the database instance name
	 * 
	 * @return the instance, or an empty string if none
	 */
	public String getInstanceSafe() {
		return instance == null ? "" : instance;
	}


	/**
	 * Get the database instance name
	 * 
	 * @param defaultInstance the default instance name 
	 * @return the instance, or the specified default string if none
	 */
	public String getInstanceSafe(String defaultInstance) {
		return instance == null ? defaultInstance : instance;
	}
	
	
	/**
	 * Check this object with another one for equality
	 * 
	 * @param other the other object
	 * @return true if they are equal
	 */
	@Override
	public boolean equals(Object other) {
		
		if (!(other instanceof DatabaseEngineAndInstance)) return false;
		DatabaseEngineAndInstance d = (DatabaseEngineAndInstance) other;
		
		if (!d.engine.equals(engine)) return false;
		
		if (d.instance == instance) return true;
		if (d.instance == null || instance == null) return false;
		return d.instance.equals(instance);
	}
	
	
	/**
	 * Compute the hash code
	 * 
	 * @return the hash code
	 */
	@Override
	public int hashCode() {
		return 31 * engine.hashCode() + (instance == null ? 0 : instance.hashCode());
	}
	
	
	/**
	 * Compare this object with another one
	 * 
	 * @param other the other object
	 * @return the result of the comparison
	 */
	@Override
	public int compareTo(DatabaseEngineAndInstance other) {
		
		int r = engine.compareTo(other.engine);
		if (r != 0) return r;
		
		if (instance == null && other.instance != null) return -1;
		if (instance != null && other.instance == null) return  1;
		if (instance == null && other.instance == null) return  0;
		
		return instanceComparator.compare(instance, other.instance);
	}

	
	/**
	 * Return a string representation of this object
	 * 
	 * @return the string representation
	 */
	@Override
	public String toString() {
		return engine.toString() + ", " + getInstanceSafe("<default>");
	}
	
	
	/**
	 * Comparator -- first by engine, then by instance
	 */
	public static class ByEngine implements Comparator<DatabaseEngineAndInstance> {

		/**
		 * Create an instance of the comparator
		 */
		public ByEngine() {
			//
		}
		
		/**
		 * Compare two instances of DatabaseEngineAndInstance
		 * 
		 * @param o1 the first object
		 * @param o2 the second object
		 * @return the result of the comparison
		 */
		@Override
		public int compare(DatabaseEngineAndInstance o1, DatabaseEngineAndInstance o2) {
			
			int r = o1.engine.compareTo(o2.engine);
			if (r != 0) return r;
			
			if (o1.instance == null && o2.instance != null) return -1;
			if (o1.instance != null && o2.instance == null) return  1;
			if (o1.instance == null && o2.instance == null) return  0;
			
			return instanceComparator.compare(o1.instance, o2.instance);
		}
	}
	
	
	/**
	 * Comparator -- first by instance, then by engine
	 */
	public static class ByInstance implements Comparator<DatabaseEngineAndInstance> {

		/**
		 * Create an instance of the comparator
		 */
		public ByInstance() {
			//
		}
		
		/**
		 * Compare two instances of DatabaseEngineAndInstance
		 * 
		 * @param o1 the first object
		 * @param o2 the second object
		 * @return the result of the comparison
		 */
		@Override
		public int compare(DatabaseEngineAndInstance o1, DatabaseEngineAndInstance o2) {
			
			if (o1.instance == null && o2.instance != null) return -1;
			if (o1.instance != null && o2.instance == null) return  1;
			if (o1.instance == null && o2.instance == null) return  0;
			
			int r = instanceComparator.compare(o1.instance, o2.instance);
			if (r != 0) return r;
			
			return o1.engine.compareTo(o2.engine);
		}
	}
}
