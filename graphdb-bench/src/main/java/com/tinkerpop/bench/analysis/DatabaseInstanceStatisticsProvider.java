package com.tinkerpop.bench.analysis;

import com.tinkerpop.bench.web.DatabaseEngineAndInstance;


/**
 * Provider for statistics about a particular database instance
 * 
 * @author Peter Macko (pmacko@eecs.harvard.edu)
 */
public abstract class DatabaseInstanceStatisticsProvider {
	
	private DatabaseEngineAndInstance dbei;
	
	
	/**
	 * Create an instance of class DatabaseInstanceStatisticsProvider
	 * 
	 * @param dbei the database engine and instance
	 */
	protected DatabaseInstanceStatisticsProvider(DatabaseEngineAndInstance dbei) {
		this.dbei = dbei;
	}
	
	
	/**
	 * Get the database engine and the instance
	 * 
	 * @return the database engine and the instance
	 */
	public DatabaseEngineAndInstance getDatabaseEngineAndInstance() {
		return dbei;
	}

	
	/**
	 * Get the statistics
	 * 
	 * @return the statistics
	 */
	public abstract DatabaseInstanceStatistics getStatistics();
	
	
	/**
	 * Get the statistics provider for the given database engine and instance pair
	 * 
	 * @param dbei the database engine and instance
	 * @return the statistics provider
	 */
	public static DatabaseInstanceStatisticsProvider getProviderFor(DatabaseEngineAndInstance dbei) {
		
		final String instanceName = dbei.getInstanceSafe();

		
		//
		// Barabasi graphs
		//
		
		if (instanceName.startsWith("b") && instanceName.length() > 1 && Character.isDigit(instanceName.charAt(1))) {
			return new HardcodedStatisticsProvider(dbei);
		}
		
		
		//
		// Kronecker graphs
		//
		
		if (instanceName.startsWith("k") && instanceName.length() > 1 && Character.isDigit(instanceName.charAt(1))) {
			return new HardcodedStatisticsProvider(dbei);
		}
	
		
		//
		// Amazon co-purchasing graph
		//
		
		if (instanceName.startsWith("amazon")) {
			return new HardcodedStatisticsProvider(dbei);
		}
	
		
		throw new IllegalArgumentException("There is no suitable statistics provider for \"" + instanceName + "\"");
	}
	
	
	/**
	 * Get the statistics for the given database engine and instance pair
	 * 
	 * @param dbei the database engine and instance
	 * @return the statistics
	 */
	public static DatabaseInstanceStatistics getStatisticsFor(DatabaseEngineAndInstance dbei) {
		DatabaseInstanceStatisticsProvider provider = getProviderFor(dbei);
		return provider == null ? null : provider.getStatistics();
	}
}
