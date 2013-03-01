package com.tinkerpop.bench.analysis;

import com.tinkerpop.bench.web.DatabaseEngineAndInstance;


/**
 * Provider for statistics about a particular database instance
 * 
 * @author Peter Macko (pmacko@eecs.harvard.edu)
 */
public class HardcodedStatisticsProvider extends DatabaseInstanceStatisticsProvider {
	
	
	/**
	 * Create an instance of class HardcodedStatisticsProvider
	 * 
	 * @param dbei the database engine and instance
	 */
	protected HardcodedStatisticsProvider(DatabaseEngineAndInstance dbei) {
		super(dbei);
	}

	
	/**
	 * Get the statistics
	 * 
	 * @return the statistics
	 */
	public DatabaseInstanceStatistics getStatistics() {
		
		final String instanceName = getDatabaseEngineAndInstance().getInstanceSafe();

		
		//
		// Barabasi graphs
		//
		
		if (instanceName.startsWith("b") && instanceName.length() > 1 && Character.isDigit(instanceName.charAt(1))) {
			
			String d = instanceName.substring("b".length(), instanceName.length());
			String[] fields = d.split("_");
			
			
			// Get the "n" argument (the number of vertices)
			
			char n_unit = fields[0].charAt(fields[0].length() - 1);
			int  n_int  = Integer.parseInt(fields[0].substring(0, fields[0].length() - 1));
			
			
			// Finish
			
			DatabaseInstanceStatistics stat = new DatabaseInstanceStatistics(this);
			
			stat.averageDegree = 10;
			stat.averageInDegree = 5;
			stat.averageOutDegree = 5;
			
			stat.numVertices = applyUnit(n_int, n_unit);
			stat.numEdges = stat.numVertices * 5;
			
			return stat;
		}
		
		
		//
		// Kronecker graphs
		//
		
		if (instanceName.startsWith("k") && instanceName.length() > 1 && Character.isDigit(instanceName.charAt(1))) {
			
			String d = instanceName.substring("k".length(), instanceName.length());
			String[] fields = d.split("_");
			
			
			// Create the statistics object
			
			DatabaseInstanceStatistics stat = new DatabaseInstanceStatistics(this);
			
			
			// Depending on the first argument...
			
			if (fields[0].equals("1k")) {
				
				stat.averageDegree = 5.231527093596059;
				stat.averageInDegree = 2.6157635467980294;
				stat.averageOutDegree = 2.6157635467980294;
				
				stat.numVertices = 1015;
				stat.numEdges = 2655;
			}
			
			else if (fields[0].equals("8k")) {
				
				stat.averageDegree = 6.912979711561965;
				stat.averageInDegree = 3.4564898557809824;
				stat.averageOutDegree = 3.4564898557809824;
				
				stat.numVertices = 8182;
				stat.numEdges = 28281;
			}
			
			else if (fields[0].equals("1m")) {
				
				stat.averageDegree = 13.455010848055695;
				stat.averageInDegree = 6.727505424027847;
				stat.averageOutDegree = 6.727505424027847;
				
				stat.numVertices = 1048575;
				stat.numEdges = 7054294;
			}
			
			else if (fields[0].equals("2m")) {
				
				stat.averageDegree = 14.800498962402344;
				stat.averageInDegree = 7.400249481201172;
				stat.averageOutDegree = 7.400249481201172;
				
				stat.numVertices = 2097152;
				stat.numEdges = 15519448;
			}
			
			else {
				throw new IllegalArgumentException("Unsupported database instance");
			}
			
			return stat;
		}
	
		
		//
		// Amazon co-purchasing graph
		//
		
		if (instanceName.startsWith("amazon")) {
			
			// Create the statistics object
			
			DatabaseInstanceStatistics stat = new DatabaseInstanceStatistics(this);
			
			
			// Depending on the instance name...
			
			if (instanceName.equals("amazon0302")) {
				
				stat.averageDegree = 9.42254998836371;
				stat.averageInDegree = 4.711274994181855;
				stat.averageOutDegree = 4.711274994181855;
				
				stat.numVertices = 262111;
				stat.numEdges = 1234877;
			}
			
			else if (instanceName.equals("amazon0312")) {
				
				stat.averageDegree = 15.973168765768216;
				stat.averageInDegree = 7.986584382884108;
				stat.averageOutDegree = 7.986584382884108;
				
				stat.numVertices = 400727;
				stat.numEdges = 3200440;
			}
			
			else {
				throw new IllegalArgumentException("Unsupported database instance");
			}
			
			return stat;
		}
	
		throw new IllegalArgumentException("Unsupported database instance");
	}
	
	
	/**
	 * Convert a given number with the given unit to just a number
	 * 
	 * @param num the number
	 * @param unit the unit
	 * @return the converted number
	 */
	private static long applyUnit(long num, char unit) {
		
		switch (unit) {
		case '\0':
		case 'b':
		case 'B':
			return num;
		case 'k':
		case 'K':
			return num * 1000l;
		case 'm':
		case 'M':
			return num * 1000l * 1000l;
		case 'g':
		case 'G':
			return num * 1000l * 1000l * 1000l;
		case 't':
		case 'T':
			return num * 1000l * 1000l * 1000l * 1000l;
		default :
			throw new IllegalArgumentException("Illegal unit: " + unit);
		}
	}
}
