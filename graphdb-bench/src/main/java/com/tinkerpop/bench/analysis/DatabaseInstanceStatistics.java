package com.tinkerpop.bench.analysis;

import com.tinkerpop.bench.web.DatabaseEngineAndInstance;
import com.tinkerpop.blueprints.Direction;


/**
 * Statistics about a particular database instance
 * 
 * @author Peter Macko (pmacko@eecs.harvard.edu)
 */
public class DatabaseInstanceStatistics {
	
	private DatabaseEngineAndInstance dbei;
	private DatabaseInstanceStatisticsProvider provider;
	
	long numVertices;
	long numEdges;
	
	double averageInDegree;
	double averageOutDegree;
	double averageDegree;
	
	
	/**
	 * Create an instance of class DatabaseInstanceStatistics
	 * 
	 * @param provider the statistics provider
	 */
	DatabaseInstanceStatistics(DatabaseInstanceStatisticsProvider provider) {
		this.provider = provider;
		this.dbei = provider.getDatabaseEngineAndInstance();
	}
	
	
	/**
	 * Get the statistics provider
	 * 
	 * @return the provider
	 */
	public DatabaseInstanceStatisticsProvider getProvider() {
		return provider;
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
	 * Get the number of vertices in the database
	 * 
	 * @return the number of vertices
	 */
	public long getVertexCount() {
		return numVertices;
	}
	
	
	/**
	 * Get the number of edges in the database
	 * 
	 * @return the number of edges
	 */
	public long getEdgeCount() {
		return numEdges;
	}
	
	
	/**
	 * Get the average vertex in-degree
	 * 
	 * @return the average in-degree
	 */
	public double getAverageInDegree() {
		return averageInDegree;
	}
	
	
	/**
	 * Get the average vertex out-degree
	 * 
	 * @return the average out-degree
	 */
	public double getAverageOutDegree() {
		return averageOutDegree;
	}
	
	
	/**
	 * Get the average vertex degree
	 * 
	 * @return the average degree
	 */
	public double getAverageDegree() {
		return averageDegree;
	}
	
	
	/**
	 * Get the average vertex degree specific to the supplied direction
	 * 
	 * @return the average degree for the given direction
	 */
	public double getAverageDegree(Direction direction) {
		switch (direction) {
		case IN  : return averageInDegree;
		case OUT : return averageOutDegree;
		case BOTH: return averageDegree;
		default  : throw new IllegalArgumentException();
		}
	}
}
