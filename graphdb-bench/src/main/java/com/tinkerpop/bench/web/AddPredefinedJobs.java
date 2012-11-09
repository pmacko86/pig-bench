package com.tinkerpop.bench.web;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * A servlet for adding predefined jobs
 * 
 * @author Peter Macko (pmacko@eecs.harvard.edu)
 */
@SuppressWarnings("serial")
public class AddPredefinedJobs extends HttpServlet {
	
	
	/**
	 * Create an instance of class AddPredefinedJobs
	 */
	public AddPredefinedJobs() {
	}

	
	/**
	 * Respond to a POST request
	 */
	@Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
		doGet(request, response);
    }
	
	
	/**
	 * Respond to a GET request
	 */
	@Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
		// Get the type
		
		String type = WebUtils.getStringParameter(request, "type");
		
		
		// Create the jobs for each database name and instance
		
		if ("load".equals(type)) {
			String[] pairs = WebUtils.getStringParameterValues(request, "database_engine_instance");
			if (pairs != null) {
				for (String p : pairs) {
					int d = p.indexOf('|');
					addLoadJobs(p.substring(0, d), computeDatasetName(p.substring(d+1)));
				}
			}
		}
		
		
		// Redirect
		
		response.sendRedirect("/index.jsp");
    }
	
	
	/**
	 * Compute the database instance name from the dataset file name
	 * 
	 * @param dataset the dataset file
	 * @return the corresponding instance name
	 */
	public static String computeInstanceName(String dataset) {
		
		if (dataset.startsWith("barabasi_") && dataset.endsWith(".fgf")) {
			
			//
			// Barabasi graphs
			//
			
			String d = dataset.substring("barabasi_".length(), dataset.length() - ".fgf".length());
			if (d.endsWith("-a") || d.endsWith("-b")) {
				d = d.substring(0, d.length() - 2);
			}
			String[] fields = d.split("_");
			
			String r = "b" + fields[0];
			for (int i = 2; i < fields.length; i++) {	// skip the number of edges argument
				r += "_" + fields[i];
			}
			
			return r;
		}
		
		if (dataset.startsWith("kron_") && dataset.endsWith(".fgf")) {
			
			//
			// Kronecker graphs
			//
			
			String d = dataset.substring("kron_".length(), dataset.length() - ".fgf".length());
			if (d.endsWith("-a") || d.endsWith("-b")) {
				d = d.substring(0, d.length() - 2);
			}
			String[] fields = d.split("_");
			
			String r = "k" + fields[0];
			for (int i = 1; i < fields.length; i++) {
				r += "_" + fields[i];
			}
			
			return r;
		}
		
		if (dataset.startsWith("amazon") && dataset.endsWith(".fgf")) {
			
			//
			// Amazon graphs
			//
			
			String d = dataset.substring(0, dataset.length() - ".fgf".length());
			if (d.endsWith("-a") || d.endsWith("-b")) {
				d = d.substring(0, d.length() - 2);
			}
			
			return d;
		}
		
		throw new IllegalArgumentException("Unsupported dataset");
	}
	
	
	/**
	 * Compute the dataset name from the instance name
	 * 
	 * @param instanceName the instance name
	 * @return the corresponding dataset file name
	 */
	public static String computeDatasetName(String instanceName) {
		
		if (instanceName.startsWith("b") && instanceName.length() > 1 && Character.isDigit(instanceName.charAt(1))) {
			
			//
			// Barabasi graphs
			//
			
			String d = instanceName.substring("b".length(), instanceName.length());
			String[] fields = d.split("_");
			
			
			// Start with the "n" argument (the number of vertices)
			
			String r = "barabasi_" + fields[0];
			
			char n_unit = fields[0].charAt(fields[0].length() - 1);
			int  n_int  = Integer.parseInt(fields[0].substring(0, fields[0].length() - 1));
			
			if (n_unit != 'k' && n_unit != 'm' && n_unit != 'g') {
				throw new IllegalArgumentException("The unit for the number of vertices needs to be \"k\", \"m\", or \"g\"");
			}
			
			
			// Compute the the number of edges
			
			char m_unit = n_unit;
			int  m_int  = n_int * 5;	/* m = 5 for our Barabasi graphs */
			
			if (m_int > 1000 && m_int % 1000 == 0 && (m_unit == 'k' || m_unit == 'm')) {
				m_int /= 1000;
				if (m_unit == 'k') m_unit = 'm';
				else if (m_unit == 'm') m_unit = 'g';
			}
			r += "_" + m_int + m_unit;
			
			
			// Copy the other fields
			
			for (int i = 1; i < fields.length; i++) {
				r += "_" + fields[i];
			}
			
			
			// Finish
			
			r += "-a.fgf";
			
			return r;
		}
		
		if (instanceName.startsWith("k") && instanceName.length() > 1 && Character.isDigit(instanceName.charAt(1))) {
			
			//
			// Kronecker graphs
			//
			
			String d = instanceName.substring("k".length(), instanceName.length());
			String[] fields = d.split("_");
			
			
			// Start with the number of vertices
			
			String r = "kron_" + fields[0];
			
			
			// Copy the other fields
			
			for (int i = 1; i < fields.length; i++) {
				r += "_" + fields[i];
			}
			
			
			// Finish
			
			r += "-a.fgf";
			
			return r;
		}
	
		if (instanceName.startsWith("amazon")) {
			return instanceName + "-a.fgf";
		}
	
		throw new IllegalArgumentException("Unsupported instance or dataset");
	}
	
	
	/**
	 * Add a database load job
	 * 
	 * @param dbEngine the database engine
	 * @param dataset the dataset file
	 */
	private static void addLoadJobs(String dbEngine, String dataset) {
		
		String instanceName = computeInstanceName(dataset);
		
		
		// Depending on the dataset...
		
		if (dataset.startsWith("barabasi_") && dataset.endsWith(".fgf")) {
		
			//
			// Barabasi graphs
			//
			
			// Parse the dataset name 
			
			String d = dataset.substring("barabasi_".length(), dataset.length() - ".fgf".length());
			
			boolean partial = false;
			if (d.endsWith("-a")) {
				partial = true;
				d = d.substring(0, d.length() - 2);
			}
			if (d.endsWith("-b")) {
				throw new IllegalArgumentException("The dataset is not bulkloadable -- please use the \"-a\", not the \"-b\" part");
			}
			String[] fields = d.split("_");
			
			
			// Get the warmup dataset name
			
			String warmupDataset = "barabasi_10k_50k";
			for (int i = 2; i < fields.length; i++) {
				warmupDataset += "_" + fields[i];
			}
			if (partial) warmupDataset += "-a";
			warmupDataset += ".fgf";
			
			
			// Create the jobs
			
			if (dataset.equals(warmupDataset)) {
				JobList.getInstance().addJob(new Job("--" + dbEngine, "--database", instanceName,
						"--ingest", dataset));
			}
			else {
				JobList.getInstance().addJob(new Job("--" + dbEngine, "--database", instanceName,
						"--ingest", dataset, "--warmup-ingest", warmupDataset));
			}
			
			if (partial) {
				
				assert dataset.endsWith("-a.fgf");
				assert warmupDataset.endsWith("-a.fgf");
				
				String dataset_b = dataset.substring(0, dataset.length() - "-a.fgf".length()) + "-b.fgf";
				String warmupDataset_b = warmupDataset.substring(0, warmupDataset.length() - "-a.fgf".length()) + "-b.fgf";
				
				if (dataset_b.equals(warmupDataset_b)) {
					JobList.getInstance().addJob(new Job("--" + dbEngine, "--database", instanceName,
							"--incr-ingest", dataset_b));
				}
				else {
					JobList.getInstance().addJob(new Job("--" + dbEngine, "--database", instanceName,
							"--incr-ingest", dataset_b, "--warmup-ingest", warmupDataset_b));
				}
			}
			
			JobList.getInstance().addJob(new Job("--" + dbEngine, "--database", instanceName,
					"--create-index"));
			
			return;
		}
		
		if (dataset.startsWith("kron_") && dataset.endsWith(".fgf")) {
		
			//
			// Kronecker graphs
			//
			
			// Parse the dataset name 
			
			String d = dataset.substring("kron_".length(), dataset.length() - ".fgf".length());
			
			boolean partial = false;
			if (d.endsWith("-a")) {
				partial = true;
				d = d.substring(0, d.length() - 2);
			}
			if (d.endsWith("-b")) {
				throw new IllegalArgumentException("The dataset is not bulkloadable -- please use the \"-a\", not the \"-b\" part");
			}
			String[] fields = d.split("_");
			
			
			// Get the warmup dataset name
			
			String warmupDataset = "kron_8k";
			for (int i = 1; i < fields.length; i++) {
				warmupDataset += "_" + fields[i];
			}
			if (partial) warmupDataset += "-a";
			warmupDataset += ".fgf";
			
			
			// Create the jobs
			
			if (dataset.equals(warmupDataset)) {
				JobList.getInstance().addJob(new Job("--" + dbEngine, "--database", instanceName,
						"--ingest", dataset));
			}
			else {
				JobList.getInstance().addJob(new Job("--" + dbEngine, "--database", instanceName,
						"--ingest", dataset, "--warmup-ingest", warmupDataset));
			}
			
			if (partial) {
				
				assert dataset.endsWith("-a.fgf");
				assert warmupDataset.endsWith("-a.fgf");
				
				String dataset_b = dataset.substring(0, dataset.length() - "-a.fgf".length()) + "-b.fgf";
				String warmupDataset_b = warmupDataset.substring(0, warmupDataset.length() - "-a.fgf".length()) + "-b.fgf";
				
				if (dataset_b.equals(warmupDataset_b)) {
					JobList.getInstance().addJob(new Job("--" + dbEngine, "--database", instanceName,
							"--incr-ingest", dataset_b));
				}
				else {
					JobList.getInstance().addJob(new Job("--" + dbEngine, "--database", instanceName,
							"--incr-ingest", dataset_b, "--warmup-ingest", warmupDataset_b));
				}
			}
			
			JobList.getInstance().addJob(new Job("--" + dbEngine, "--database", instanceName,
					"--create-index"));
			
			return;
		}
		
		if (dataset.startsWith("amazon") && dataset.endsWith(".fgf")) {
			
			//
			// Amazon graphs
			//
			
			String d = dataset.substring(0, dataset.length() - ".fgf".length());
			
			boolean partial = false;
			if (d.endsWith("-a")) {
				partial = true;
				d = d.substring(0, d.length() - 2);
			}
			if (d.endsWith("-b")) {
				throw new IllegalArgumentException("The dataset is not bulkloadable -- please use the \"-a\", not the \"-b\" part");
			}
			
			String warmupDataset = "amazon0302";
			if (partial) warmupDataset += "-a";
			warmupDataset += ".fgf";
			
			if (dataset.equals(warmupDataset)) {
				JobList.getInstance().addJob(new Job("--" + dbEngine, "--database", instanceName,
						"--ingest", dataset));
			}
			else {
				JobList.getInstance().addJob(new Job("--" + dbEngine, "--database", instanceName,
						"--ingest", dataset, "--warmup-ingest", warmupDataset));
			}
			
			if (partial) {
				
				assert dataset.endsWith("-a.fgf");
				assert warmupDataset.endsWith("-a.fgf");
				
				String dataset_b = dataset.substring(0, dataset.length() - "-a.fgf".length()) + "-b.fgf";
				String warmupDataset_b = warmupDataset.substring(0, warmupDataset.length() - "-a.fgf".length()) + "-b.fgf";
				
				if (dataset_b.equals(warmupDataset_b)) {
					JobList.getInstance().addJob(new Job("--" + dbEngine, "--database", instanceName,
							"--incr-ingest", dataset_b));
				}
				else {
					JobList.getInstance().addJob(new Job("--" + dbEngine, "--database", instanceName,
							"--incr-ingest", dataset_b, "--warmup-ingest", warmupDataset_b));
				}
			}
			
			JobList.getInstance().addJob(new Job("--" + dbEngine, "--database", instanceName,
					"--create-index", "title,salesrank,group"));
			
			return;
		}
		
		throw new IllegalArgumentException("Unsupported dataset");
	}
}
