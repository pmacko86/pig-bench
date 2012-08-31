package com.tinkerpop.bench.benchmark;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import com.tinkerpop.bench.BenchResults;
import com.tinkerpop.bench.DatabaseEngine;
import com.tinkerpop.bench.util.ConsoleUtils;


/**
 * The optimal cache settings finder
 * 
 * @author Peter Macko (pmacko@eecs.harvard.edu)
 */
public abstract class OptimalCacheSettingsFinder {
	
	public static final int DEFAULT_POPULATION_SIZE = 10;
	public static final int DEFAULT_CACHE_SIZE_GRANULARITY_MB = 4;
	
	private int populationSize;
	private int cacheSizeGranularityMB;
	
	private DatabaseEngine dbEngine;
	private int cacheSizeMB;
	private String[] args;
	
	private HashMap<CacheConfiguration, Long> scores;
	private CacheConfiguration bestConfiguration;
	private long bestTime;
	

	/**
	 * Create an instance of OptimalCacheSettingsFinder
	 * 
	 * @param dbEngine the database engine
	 * @param cacheSizeMB the total cache size
	 * @param args the command-line arguments
	 */
	public OptimalCacheSettingsFinder(DatabaseEngine dbEngine, int cacheSizeMB, String[] args) {
		
		populationSize = DEFAULT_POPULATION_SIZE;
		cacheSizeGranularityMB = DEFAULT_CACHE_SIZE_GRANULARITY_MB;
		
		this.dbEngine = dbEngine;
		this.cacheSizeMB = cacheSizeMB;
		this.args = args;
		
		if (cacheSizeMB < cacheSizeGranularityMB || cacheSizeGranularityMB <= 0
				|| (cacheSizeMB % cacheSizeGranularityMB) != 0) {
			throw new IllegalArgumentException("Invalid cache size or cache size granularity");
		}
		
		scores = new HashMap<OptimalCacheSettingsFinder.CacheConfiguration, Long>();
		bestConfiguration = null;
		bestTime = -1;
	}
	
	
	/**
	 * Run
	 * 
	 * @param args the command-line arguments
	 * @return 0 on success, otherwise an error code
	 */
	public static int run(String[] args) {
		
		
		/*
		 * Determine the database engine
		 */
		
		DatabaseEngine dbEngine = null;
		for (DatabaseEngine e : DatabaseEngine.ENGINES.values()) {
			
			String arg = "--" + e.getShortName();
			boolean found = false;
			for (String a : args) {
				if (a.equals(arg)) {
					found = true;
					break;
				}
			}
			
			if (found) {
				if (dbEngine == null) {
					dbEngine = e;
				}
				else {
					ConsoleUtils.error("Multiple databases are selected, but only one is allowed.");
					return 1;
				}
			}
		}
		
		if (dbEngine == null) {
			ConsoleUtils.error("No database is selected (please use --help for a list of options).");
			return 1;
		}
		
		
		/*
		 * Run
		 */
		
		try {
			OptimalCacheSettingsFinder p = null;
			
			if (dbEngine.getShortName().equals("neo")) {
				p = new Neo4j(dbEngine, 256, args);
			}
			else {
				throw new IllegalArgumentException("The selected database engine is not supported");
			}
			
			p.run();
		}
		catch (Throwable t) {
			ConsoleUtils.error(t.getMessage());
			t.printStackTrace(System.err);
			return 1;
		}
		
		return 0;
	}
	
	
	/**
	 * The main method
	 * 
	 * @param args the command-line arguments
	 */
	public static void main(String[] args) {
		int r = run(args);
		if (r != 0) System.exit(r);
	}

	
	/**
	 * Get the database engine
	 * 
	 * @return the database engine
	 */
	public DatabaseEngine getDatabaseEngine() {
		return dbEngine;
	}
	
	
	/**
	 * Configure the database engine for the given cache sizes
	 * 
	 * @param cacheConfiguration the cache configuration
	 * @return the configuration map
	 * @throws Exception on error
	 */
	protected abstract Map<String, String> configureDatabaseEngine(CacheConfiguration cacheConfiguration);
	
	
	/**
	 * Create one or more initial cache configurations
	 * 
	 * @return a collection of cache configurations
	 */
	protected abstract Collection<CacheConfiguration> createInitialConfigurations();
	
	
	/**
	 * Run the benchmark suite with the given additional configuration arguments
	 * 
	 * @param configuration the database engine configuration as a map of key-value pairs
	 * @return the elapsed time
	 * @throws Exception on error
	 */
	protected long runBenchmark(Map<String, String> configuration) throws Exception {
		
		String[] newArgs = new String[args.length + (configuration != null ? configuration.size() * 2 : 0)];
		
		int index = 0;
		for (index = 0; index < args.length; index++) newArgs[index] = args[index]; 
		
		if (configuration != null) {
			for (Map.Entry<String, String> e : configuration.entrySet()) {
				newArgs[index++] = "--db-config";
				newArgs[index++] = e.getKey() + "=" + e.getValue();
			}
		}
		
		int r = BenchmarkMicro.run(newArgs);	
		if (r != 0) {
			throw new Exception("The benchmark terminated with error code " + r);
		}
		
		BenchResults results = BenchmarkMicro.lastBenchmarkResults;
		if (results == null) {
			throw new Exception("The benchmark terminated without returning any results");
		}
		
		return results.getCumulativeBenchmarkTime();
	}
	
	
	/**
	 * Run the search
	 */
	public void run() {
		
		Vector<CacheConfiguration> population = new Vector<CacheConfiguration>();
		
		bestConfiguration = null;
		bestTime = -1;
		
		
		// Create the initial population
		
		Collection<CacheConfiguration> initial = createInitialConfigurations();
		if (initial == null) initial = new ArrayList<CacheConfiguration>();
		if (initial.size() > populationSize) {
			throw new IllegalStateException("The initial population is too large");
		}		
		for (CacheConfiguration cc : initial) population.add(cc);
		
		int count = (populationSize - population.size()) / 2;
		while (count --> 0) {
			population.add(population.get((int) (population.size() * Math.random())).mutate());
		}
		
		while (population.size() < populationSize) {
			int[] a = new int[population.get(0).getCacheSizes().length];
			for (int i = 0; i < a.length; i++) {
				a[i] = (int) (1000 * Math.random());
			}
			population.add(new CacheConfiguration(a));
		}
		
		
		// Random initial run
		
		ConsoleUtils.sectionHeader("Optimal Cache Settings Finder -- Cache Warming Run");
		
		try {
			int[] a = new int[population.get(0).getCacheSizes().length];
			for (int i = 0; i < a.length; i++) {
				a[i] = (int) (1000 * Math.random());
			}
			CacheConfiguration p = new CacheConfiguration(a);
			runBenchmark(configureDatabaseEngine(p));
		}
		catch (Throwable t) {
			ConsoleUtils.error(t.getMessage());
			t.printStackTrace(System.err);
		}

		
		
		// Main loop
		
		for (int generation = 1; generation <= 10; generation++) {
			
			ConsoleUtils.sectionHeader("Optimal Cache Settings Finder -- Generation " + generation);
			
			
			// Reproduce the population using Deterministic Crowding
			
			Vector<CacheConfiguration> parents = population; 
			
			if (generation == 1) {
				
				Collections.shuffle(population);
				parents = population;
				
			}
			else {
				
				Vector<CacheConfiguration> next = new Vector<CacheConfiguration>();
				Collections.shuffle(population);
				parents = population;
				
				for (int i = 0; i < population.size() - 1; i += 2) {
					
					CacheConfiguration p1 = population.get(i  );
					CacheConfiguration p2 = population.get(i+1);
					
					// Cross-over with a certain probability
					
					if (Math.random() < 0.5) {
						int point = 1 + (int) (Math.random() * (p1.getCacheSizes().length - 2));
						
						int[] a1 = new int[p1.getCacheSizes().length];
						int[] a2 = new int[p2.getCacheSizes().length];
						
						for (int j = 0; j < a1.length; j++) {
							if (j < point) {
								a1[j] = p1.getCacheSizes()[j];
								a2[j] = p2.getCacheSizes()[j];
							}
							else {
								a1[j] = p2.getCacheSizes()[j];
								a2[j] = p1.getCacheSizes()[j];
							}
						}
						
						p1 = new CacheConfiguration(a1);
						p2 = new CacheConfiguration(a2);
					}
					
					CacheConfiguration c1 = p1.mutate();
					CacheConfiguration c2 = p2.mutate();
					
					next.add(c1);
					next.add(c2);
				}
				
				population = next;
			}
			
			
			// Print the population
			
			ConsoleUtils.header("Population:");
			for (CacheConfiguration c : population) {
				ConsoleUtils.header("  " + c);
			}
			
			
			// Evaluate each element
			
			int elementIndex = 0;
			for (CacheConfiguration p : population) {
				
				ConsoleUtils.sectionHeader("Generation " + generation + ", Element " + (++elementIndex));
				
				Long score = scores.get(p);
				if (score != null) continue;
				
				try {
					score = runBenchmark(configureDatabaseEngine(p));
				}
				catch (Throwable t) {
					ConsoleUtils.error(t.getMessage());
					t.printStackTrace(System.err);
					score = new Long(-1);
				}
				
				scores.put(p, score);
				
				if (score.longValue() < bestTime || bestTime < 0) {
					bestTime = score.longValue();
					bestConfiguration = p;
				}
			}
			
			
			// Print the results
			
			System.out.println();
			ConsoleUtils.header("Results:");
			for (CacheConfiguration c : population) {
				ConsoleUtils.header("  " + c + "  -->  " + scores.get(c));
			}
			
			System.out.println();
			ConsoleUtils.header("Best result so far:");
			ConsoleUtils.header("  " + bestConfiguration + "  -->  " + bestTime);
			
			
			// Create the new population
			
			Vector<CacheConfiguration> next = new Vector<CacheConfiguration>();
			next.addAll(population);
			
			for (int i = 0; i < parents.size() - 1; i += 2) {
				
				CacheConfiguration p1 = parents.get(i  );
				CacheConfiguration p2 = parents.get(i+1);
				
				CacheConfiguration c1 = next.get(i  );
				CacheConfiguration c2 = next.get(i+1);
				
				if (c1.distance(p1) + c2.distance(p2) <= c2.distance(p1) + c1.distance(p2)) { 
					if (scores.get(c1) > scores.get(p1) - 0.001f) {
						next.set(i  , p1.clone());
					}
					if (scores.get(c2) > scores.get(p2) - 0.001f) {
						next.set(i+1, p2.clone());
					}
				}
				else {
					if (scores.get(c1) > scores.get(p2) - 0.001f) {
						next.set(i  , p2.clone());
					}
					if (scores.get(c2) > scores.get(p1) - 0.001f) {
						next.set(i+1, p1.clone());
					}
				}
			}
			
			population = next;
		}
	}
	
	
	/**
	 * A cache configuration vector
	 */
	protected class CacheConfiguration implements Cloneable {
		
		/// The collection of cache sizes in MB
		private int[] cacheSizes;
		
		
		/**
		 * Create an instance of CacheConfiguration
		 * 
		 * @param cacheSizes the collection of cache sizes
		 */
		public CacheConfiguration(int[] cacheSizes) {
			
			this.cacheSizes = new int[cacheSizes.length];
			for (int i = 0; i < cacheSizes.length; i++) {
				this.cacheSizes[i] = cacheSizes[i];
			}
			
			normalize();
		}
		
		
		/**
		 * Mutate
		 * 
		 * @return a mutated version
		 */
		public CacheConfiguration mutate() {
			
			int[] c = new int[cacheSizes.length];
			for (int i = 0; i < cacheSizes.length; i++) {
				c[i] = cacheSizes[i];
				
				double r = Math.random();
				double g = 0.25;
				
				if (r < g) {
					c[i] += cacheSizeGranularityMB * (int) (1 + 2 * Math.random());
				}
				else if (r < g * 2) {
					c[i] -= cacheSizeGranularityMB * (int) (1 + 2 * Math.random());
				}
			}
			
			return new CacheConfiguration(c);
		}
		
		
		/**
		 * Normalize the cache sizes
		 */
		protected void normalize() {
			
			int total = 0;
			for (int c : cacheSizes) total += c;
			if (total <= 0) throw new IllegalArgumentException();
			
			double[] d = new double[cacheSizes.length];
			for (int i = 0; i < cacheSizes.length; i++) {
				d[i] = cacheSizeMB * (cacheSizes[i] / (double) total);
			}
			
			for (int i = 0; i < cacheSizes.length; i++) {
				cacheSizes[i] = (int) (cacheSizeGranularityMB * Math.round(d[i] / cacheSizeGranularityMB));
				if (cacheSizes[i] < 0) cacheSizes[i] = 0;
			}
			
			int count = 100;
			
			do {
				total = 0;
				for (int c : cacheSizes) total += c;
				if (total <= 0) throw new IllegalArgumentException();
				
				if (total > cacheSizeMB) {
					double max = 0;
					int index = 0;
					for (int i = 0; i < cacheSizes.length; i++) {
						if (cacheSizes[i] > 0) {
							double delta = cacheSizes[i] - d[i];
							if (delta > max) {
								max = delta;
								index = i;
							}
						}
					}
					cacheSizes[index] -= cacheSizeGranularityMB;
					d[index] = cacheSizes[index];
				}
				
				if (total < cacheSizeMB) {
					double max = 0;
					int index = 0;
					for (int i = 0; i < cacheSizes.length; i++) {
						double delta = d[i] - cacheSizes[i];
						if (delta > max) {
							max = delta;
							index = i;
						}
					}
					cacheSizes[index] += cacheSizeGranularityMB;
					d[index] = cacheSizes[index];
				}
				
				if (!(count --> 0)) {
					throw new RuntimeException("Could not normalize");
				}
			}
			while (total != cacheSizeMB);
		}
		
		
		/**
		 * Get the cache sizes
		 * 
		 * @return the cache sizes
		 */
		public int[] getCacheSizes() {
			return cacheSizes;
		}
		
		
		/**
		 * Compute the distance between this object and another object
		 * 
		 * @param other the other object
		 * @return the evolutionary distance (or 0 if equal)
		 */
		public double distance(CacheConfiguration other) {
			
			double s = 0;
			for (int i = 0; i < cacheSizes.length; i++) {
				s += (cacheSizes[i] - other.cacheSizes[i]) * (cacheSizes[i] - other.cacheSizes[i]);
			}
			
			return Math.sqrt(s);
		}
		
		
		/**
		 * Create a copy of CacheConfiguration
		 * 
		 * @param num the number of 
		 */
		@Override
		public CacheConfiguration clone() {
			return new CacheConfiguration(cacheSizes);
		}
		
		
		/**
		 * Compute the hash code
		 * 
		 * @return the hash code
		 */
		@Override
		public int hashCode() {
			int h = 1;
			for (int c : cacheSizes) {
				h = (h << 4) ^ (c / cacheSizeGranularityMB); 
			}
			return h;
		}
		
		
		/**
		 * Check whether this object is equal to the other object
		 * 
		 * @param other the other object
		 * @return true if they are equal
		 */
		@Override
		public boolean equals(Object other) {
			if (other == null) return false;
			if (!(other instanceof CacheConfiguration)) return false;
			CacheConfiguration cc = (CacheConfiguration) other;
			if (cc.cacheSizes.length != cacheSizes.length) return false;
			for (int i = 0; i < cacheSizes.length; i++)
				if (cc.cacheSizes[i] != cacheSizes[i]) return false;
			return true;
		}
		
		
		/**
		 * Create a human-readable string representation of this configuration
		 * 
		 * @return a human-readable string
		 */
		@Override
		public String toString() {
			String s = "";
			for (int c : cacheSizes) {
				if (!"".equals(s)) s += " : ";
				if (c < 10 ) s += " ";
				if (c < 100) s += " ";
				s += c;
			}
			return s;
		}
	}
	
	
	/**
	 * A Neo4j-specific variant of this class
	 */
	public static class Neo4j extends OptimalCacheSettingsFinder {

		/**
		 * Create an instance of OptimalCacheSettingsFinder
		 * 
		 * @param dbEngine the database engine
		 * @param cacheSizeMB the total cache size
		 * @param args the command-line arguments
		 */
		public Neo4j(DatabaseEngine dbEngine, int cacheSizeMB, String[] args) {
			super(dbEngine, cacheSizeMB, args);
		}
		
		
		/**
		 * Configure the database engine for the given cache sizes
		 * 
		 * @param cacheConfiguration the cache configuration
		 * @return the configuration map
		 * @throws Exception on error
		 */
		@Override
		protected Map<String, String> configureDatabaseEngine(CacheConfiguration cacheConfiguration) {
			
			int[] c = cacheConfiguration.getCacheSizes();
			if (c.length != 5) throw new IllegalArgumentException();
			
			Map<String, String> m = new HashMap<String, String>();
			m.put("neostore.nodestore.db.mapped_memory"            , "" + c[0] + "M");
			m.put("neostore.relationshipstore.db.mapped_memory"    , "" + c[1] + "M");
			m.put("neostore.propertystore.db.mapped_memory"        , "" + c[2] + "M");
			m.put("neostore.propertystore.db.strings.mapped_memory", "" + c[3] + "M");
			m.put("neostore.propertystore.db.arrays.mapped_memory" , "" + c[4] + "M");
			
			return m;
		}
		
		
		/**
		 * Create one or more initial cache configurations
		 * 
		 * @return a collection of cache configurations
		 */
		@Override
		protected Collection<CacheConfiguration> createInitialConfigurations() {
			
			ArrayList<CacheConfiguration> a = new ArrayList<CacheConfiguration>();
			
			// http://docs.neo4j.org/chunked/stable/configuration-io-examples.html
			a.add(new CacheConfiguration(new int[] { 25, 90, 90, 130, 130 }));			
			a.add(new CacheConfiguration(new int[] { 15, 285, 100, 100, 0 }));
			
			a.add(new CacheConfiguration(new int[] { 20, 100, 10, 10, 0 }));
			
			return a;
		}
	}
}
