package com.tinkerpop.bench.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import com.tinkerpop.bench.log.SummaryLogEntry;
import com.tinkerpop.bench.log.SummaryLogReader;
import com.tinkerpop.bench.operation.operations.OperationCreateKeyIndex;
import com.tinkerpop.bench.operation.operations.OperationLoadFGF;
import com.tinkerpop.bench.util.MathUtils;
import com.tinkerpop.bench.web.DatabaseEngineAndInstance;
import com.tinkerpop.bench.web.Job;
import com.tinkerpop.bench.web.JobList;

/**
 * Simple ingest analysis 
 * 
 * @author Peter Macko (pmacko@eecs.harvard.edu)
 */
public class IngestAnalysis {
	
	/// The cache
	private static ConcurrentHashMap<DatabaseEngineAndInstance, IngestAnalysis> cache =
			new ConcurrentHashMap<DatabaseEngineAndInstance, IngestAnalysis>();

	/// The map of datasets descriptions
	public static final Map<String, DatasetDescription> DATASETS;
	
	/// The database and instance pair
	private DatabaseEngineAndInstance dbEI;
	
	/// The number of all finished jobs (for cache purposes)
	private int numFinishedJobs;
	
	private Job bulkLoadJob = null;
	private double bulkLoadTime = 0;
	private double bulkLoadShutdownTime = 0;
	private String bulkLoadDataset = null;
	
	private Job incrementalLoadJob = null;
	private double incrementalLoadTime = 0;
	private double incrementalLoadShutdownTime = 0;
	private String incrementalLoadDataset = null;
	
	private double createIndexTime = 0;
	private double createIndexShutdownTime = 0;
	
	private double bulkLoadTimePrediction = 0;
	private double incrementalLoadTimePrediction = 0;
	
	
	static {
		TreeMap<String, DatasetDescription> m = new TreeMap<String, DatasetDescription>();
		DatasetDescription d;
		
		// XXX This should not be hard-coded
		
		d = new DatasetDescription("kron_1k_1el-a" ,     913,     2323,       0); m.put(d.getName(), d);
		d = new DatasetDescription("kron_1k_1el-b" ,     102,      332,     266); m.put(d.getName(), d);
		d = new DatasetDescription("kron_1k_2el-a" ,     913,     2323,       0); m.put(d.getName(), d);
		d = new DatasetDescription("kron_1k_2el-b" ,     102,      332,     266); m.put(d.getName(), d);
		
		d = new DatasetDescription("kron_8k_1el-a" ,    7363,    24362,       0); m.put(d.getName(), d);
		d = new DatasetDescription("kron_8k_1el-b" ,     819,     3919,    2684); m.put(d.getName(), d);
		d = new DatasetDescription("kron_8k_2el-a" ,    7363,    24362,       0); m.put(d.getName(), d);
		d = new DatasetDescription("kron_8k_2el-b" ,     819,     3919,    2684); m.put(d.getName(), d);
		
		d = new DatasetDescription("kron_1m_1el-a" ,  943717,  5868033,       0); m.put(d.getName(), d);
		d = new DatasetDescription("kron_1m_1el-b" ,  104858,  1186261,  608288); m.put(d.getName(), d);
		d = new DatasetDescription("kron_1m_2el-a" ,  943717,  5868033,       0); m.put(d.getName(), d);
		d = new DatasetDescription("kron_1m_2el-b" ,  104858,  1186261,  608288); m.put(d.getName(), d);
		
		d = new DatasetDescription("kron_2m_1el-a" , 1887436, 12867612,       0); m.put(d.getName(), d);
		d = new DatasetDescription("kron_2m_1el-b" ,  209716,  2651836, 1288514); m.put(d.getName(), d);
		d = new DatasetDescription("kron_2m_2el-a" , 1887436, 12867612,       0); m.put(d.getName(), d);
		d = new DatasetDescription("kron_2m_2el-b" ,  209716,  2651836, 1288514); m.put(d.getName(), d);
		
		
		d = new DatasetDescription("barabasi_1k_5k_1el-a"  ,     900,     4495,       0); m.put(d.getName(), d);
		d = new DatasetDescription("barabasi_1k_5k_1el-b"  ,     100,      500,     378); m.put(d.getName(), d);
		d = new DatasetDescription("barabasi_1k_5k_2el-a"  ,     900,     4486,       0); m.put(d.getName(), d);
		d = new DatasetDescription("barabasi_1k_5k_2el-b"  ,     100,      509,     380); m.put(d.getName(), d);
		
		d = new DatasetDescription("barabasi_1m_5m_1el-a"  ,  900000,  4499995,       0); m.put(d.getName(), d);
		d = new DatasetDescription("barabasi_1m_5m_1el-b"  ,  100000,   500000,  357886); m.put(d.getName(), d);
		d = new DatasetDescription("barabasi_1m_5m_2el-a"  ,  900000,  4499760,       0); m.put(d.getName(), d);
		d = new DatasetDescription("barabasi_1m_5m_2el-b"  ,  100000,   500235,  358083); m.put(d.getName(), d);
		
		d = new DatasetDescription("barabasi_2m_10m_1el-a" , 1800000,  8999995,       0); m.put(d.getName(), d);
		d = new DatasetDescription("barabasi_2m_10m_1el-b" ,  200000,  1000000,  715848); m.put(d.getName(), d);
		d = new DatasetDescription("barabasi_2m_10m_2el-a" , 1800000,  8999775,       0); m.put(d.getName(), d);
		d = new DatasetDescription("barabasi_2m_10m_2el-b" ,  200000,  1000220,  715954); m.put(d.getName(), d);
		
		d = new DatasetDescription("barabasi_10m_50m_1el-a", 9000000, 44999995,       0); m.put(d.getName(), d);
		d = new DatasetDescription("barabasi_10m_50m_1el-b", 1000000,  5000000, 3577880); m.put(d.getName(), d);
		d = new DatasetDescription("barabasi_10m_50m_2el-a", 9000000, 44998470,       0); m.put(d.getName(), d);
		d = new DatasetDescription("barabasi_10m_50m_2el-b", 1000000,  5001525, 3580726); m.put(d.getName(), d);

		
		DATASETS = Collections.unmodifiableMap(m);
	}

	
	/**
	 * Create an instance of the class
	 * 
	 * @param dbei the database engine and instance
	 */
	protected IngestAnalysis(DatabaseEngineAndInstance dbEI) {
		
		this.dbEI = dbEI;
		this.numFinishedJobs = -1;
		
		update();
	}
	
	
	/**
	 * Get an instance of IngestAnalysis for a particular database engine and instance pair
	 * 
	 * @param dbEI the database engine and instance pair
	 */
	public static IngestAnalysis getInstance(DatabaseEngineAndInstance dbEI) {
		
		synchronized (cache) {
			IngestAnalysis m = cache.get(dbEI);
			if (m == null) {
				m = new IngestAnalysis(dbEI);
				cache.put(dbEI, m);
			}
			else {
				m.update();
			}
			return m;
		}
	}
	
	
	/**
	 * Recompute if necessary
	 * 
	 * @return true if it was recomputed
	 */
	public boolean update() {
		
		List<Job> finishedJobs = JobList.getInstance().getFinishedJobs(dbEI);
		if (finishedJobs.size() == numFinishedJobs) return false;
		numFinishedJobs = finishedJobs.size();
		
		
		/*
		 * Initialize
		 */
	
		ModelAnalysis m = ModelAnalysis.getInstance(dbEI);
		
		bulkLoadJob = null;
		bulkLoadTime = 0;
		bulkLoadShutdownTime = 0;
		bulkLoadDataset = null;
		
		incrementalLoadJob = null;
		incrementalLoadTime = 0;
		incrementalLoadShutdownTime = 0;
		incrementalLoadDataset = null;
		
		createIndexTime = 0;
		createIndexShutdownTime = 0;
		
		bulkLoadTimePrediction = 0;
		incrementalLoadTimePrediction = 0;
		
		
		/*
		 * Find the load FGF operations
		 */
		
		ArrayList<Job> jobs = new ArrayList<Job>(m.getJobs(OperationLoadFGF.class));
		Collections.reverse(jobs);
		
		for (Job j : jobs) {
			SummaryLogReader reader = new SummaryLogReader(j.getSummaryFile());
			for (SummaryLogEntry e : reader) {
				if (e.getName().startsWith("OperationLoadFGF")) {
					String[] tags = e.getName().split("-");
					if (tags.length < 3) {
						throw new RuntimeException("Error: Invalid number of operation tags in \"" + e.getName()
								+ "\" for \"" + dbEI + "\"");
					}
					
					boolean thisBulkLoad = tags[1].equals("bulkload");
					boolean thisIncremental = tags[1].equals("incremental");
					if (!thisBulkLoad && !thisIncremental) {
						throw new RuntimeException("Error: Invalid operation tags in \"" + e.getName()
								+ "\" for \"" + dbEI + "\"");
					}
					
					String dataset = tags[2];
					if (tags.length >= 4 && (tags[3].equals("a") || tags[3].equals("b"))) {
						dataset += "-" + tags[3];
					}
					
					if (thisBulkLoad && bulkLoadJob == null) {
						bulkLoadJob = j;
						bulkLoadTime = e.getDefaultRunTimes().getMean() / 1000000.0;	// to ms
						bulkLoadDataset = dataset;
					}
					
					if (thisIncremental && incrementalLoadJob == null) {
						incrementalLoadJob = j;
						incrementalLoadTime = e.getDefaultRunTimes().getMean() / 1000000.0;	// to ms
						incrementalLoadDataset = dataset;
					}
				}
				
				if (e.getName().equals("OperationShutdownGraph")) {
					if (j == bulkLoadJob)
						bulkLoadShutdownTime = e.getDefaultRunTimes().getMean() / 1000000.0;	// to ms
					if (j == incrementalLoadJob)
						incrementalLoadShutdownTime = e.getDefaultRunTimes().getMean() / 1000000.0;	// to ms
				}
			}
			if (bulkLoadJob != null && incrementalLoadJob != null) break; 
		}
		
		
		/*
		 * Find the create index job
		 */
		
		SortedSet<Job> createIndexJobs = m.getJobs(OperationCreateKeyIndex.class);
		
		Job createIndexJob = createIndexJobs == null || createIndexJobs.isEmpty() ? null : createIndexJobs.last();
		if (createIndexJob != null) {
			SummaryLogReader reader = new SummaryLogReader(createIndexJob.getSummaryFile());
			for (SummaryLogEntry e : reader) {
				if (e.getName().startsWith("OperationCreateKeyIndex")) {
					createIndexTime += e.getDefaultRunTimes().getMean() / 1000000.0;	// to ms
				}
				if (e.getName().equals("OperationShutdownGraph")) {
					createIndexShutdownTime += e.getDefaultRunTimes().getMean() / 1000000.0;	// to ms
				}
			}
		}
		
		
		/*
		 * Make the predictions
		 */
		
		DatasetDescription dataset;
		
		dataset = DATASETS.get(bulkLoadDataset);
		if (dataset != null && MathUtils.ifNeitherIsNull(m.Wv, m.We, m.Wp, m.Rvup) != null) {
			bulkLoadTimePrediction  = 0;
			bulkLoadTimePrediction += dataset.getNumVertices()         * m.Wv.doubleValue();
			bulkLoadTimePrediction += dataset.getNumEdges()            * m.We.doubleValue();
			bulkLoadTimePrediction += dataset.getNumVertices()         * m.Wp.doubleValue() * 2;		// XXX Hard-coded
			bulkLoadTimePrediction += dataset.getNumEdges()            * m.Wp.doubleValue() * 1;		// XXX Hard-coded
			bulkLoadTimePrediction += dataset.getNumExternalVertices() * m.Rvup.doubleValue();
		}
		
		dataset = DATASETS.get(incrementalLoadDataset);
		if (dataset != null && MathUtils.ifNeitherIsNull(m.Wv, m.We, m.Wp, m.Rvup) != null) {
			incrementalLoadTimePrediction  = 0;
			incrementalLoadTimePrediction += dataset.getNumVertices()         * m.Wv.doubleValue();
			incrementalLoadTimePrediction += dataset.getNumEdges()            * m.We.doubleValue();
			incrementalLoadTimePrediction += dataset.getNumVertices()         * m.Wp.doubleValue() * 2;		// XXX Hard-coded
			incrementalLoadTimePrediction += dataset.getNumEdges()            * m.Wp.doubleValue() * 1;		// XXX Hard-coded
			incrementalLoadTimePrediction += dataset.getNumExternalVertices() * m.Rvup.doubleValue();
		}
		
		
		/*
		 * Finish
		 */
		
		return true;
	}
	
	
	/**
	 * Get the bulk load time
	 * 
	 * @return the time in ms, or 0 if none
	 */
	public double getBulkLoadTime() {
		return bulkLoadTime;
	}
	
	
	/**
	 * Get the shutdown time after the bulk load
	 * 
	 * @return the time in ms, or 0 if none
	 */
	public double getBulkLoadShutdownTime() {
		return bulkLoadShutdownTime;
	}
	
	
	/**
	 * Get the incremental load time
	 * 
	 * @return the time in ms, or 0 if none
	 */
	public double getIncrementalLoadTime() {
		return incrementalLoadTime;
	}
	
	
	/**
	 * Get the shutdown time after the incremental load
	 * 
	 * @return the time in ms, or 0 if none
	 */
	public double getIncrementalLoadShutdownTime() {
		return incrementalLoadShutdownTime;
	}
	
	
	/**
	 * Get the index creation time
	 * 
	 * @return the time in ms, or 0 if none
	 */
	public double getCreateIndexTime() {
		return createIndexTime;
	}
	
	
	/**
	 * Get the shutdown time after the index creation
	 * 
	 * @return the time in ms, or 0 if none
	 */
	public double getCreateIndexShutdownTime() {
		return createIndexShutdownTime;
	}
	
	
	/**
	 * Get the total ingest time, including all shutdown times
	 * 
	 * @return the time in ms, or 0 if none
	 */
	public double getTotalTime() {
		double r = 0;
		r += bulkLoadTime + bulkLoadShutdownTime;
		r += incrementalLoadTime + incrementalLoadShutdownTime;
		r += createIndexTime + createIndexShutdownTime;
		return r;
	}
	
	
	/**
	 * Get the bulk load time prediction
	 * 
	 * @return the time in ms, or 0 if none
	 */
	public double getBulkLoadTimePrediction() {
		return bulkLoadTimePrediction;
	}
	
	
	/**
	 * Get the incremental load time prediction
	 * 
	 * @return the time in ms, or 0 if none
	 */
	public double getIncrementalLoadTimePrediction() {
		return incrementalLoadTimePrediction;
	}
	
	
	/**
	 * A dataset description
	 */
	public static class DatasetDescription {
		
		private String name;
		private int numVertices;
		private int numEdges;
		private int numExternalVertices;
		
		
		/**
		 * Create an instance of class DatasetDescription
		 * 
		 * @param name the dataset name
		 * @param numVertices the total number of vertices
		 * @param numEdges the total number of edges
		 * @param numExternalVertices the total number of externally referenced vertices
		 */
		public DatasetDescription(String name, int numVertices, int numEdges, int numExternalVertices) {
			this.name = name;
			this.numVertices = numVertices;
			this.numEdges = numEdges;
			this.numExternalVertices = numExternalVertices;
		}


		/**
		 * Return the dataset name
		 * 
		 * @return the dataset name
		 */
		public String getName() {
			return name;
		}


		/**
		 * Return the total number of vertices
		 * 
		 * @return the total number of vertices
		 */
		public int getNumVertices() {
			return numVertices;
		}


		/**
		 * Return the total number of edges
		 * 
		 * @return the total number of edges
		 */
		public int getNumEdges() {
			return numEdges;
		}


		/**
		 * Return the total number of externally referenced vertices
		 * 
		 * @return the total number of externally referenced vertices
		 */
		public int getNumExternalVertices() {
			return numExternalVertices;
		}
	}
}
