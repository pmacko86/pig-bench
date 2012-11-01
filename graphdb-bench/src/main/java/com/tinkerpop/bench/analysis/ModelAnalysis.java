package com.tinkerpop.bench.analysis;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import com.tinkerpop.bench.log.OperationLogEntry;
import com.tinkerpop.bench.log.OperationLogReader;
import com.tinkerpop.bench.log.SummaryLogEntry;
import com.tinkerpop.bench.log.SummaryLogReader;
import com.tinkerpop.bench.operation.Operation;
import com.tinkerpop.bench.operation.operations.OperationAddManyEdges;
import com.tinkerpop.bench.operation.operations.OperationAddManyVertices;
import com.tinkerpop.bench.operation.operations.OperationGetAllNeighbors;
import com.tinkerpop.bench.operation.operations.OperationGetFirstNeighbor;
import com.tinkerpop.bench.operation.operations.OperationGetKHopNeighbors;
import com.tinkerpop.bench.operation.operations.OperationGetKHopNeighborsEdgeConditional;
import com.tinkerpop.bench.operation.operations.OperationGetManyEdgeProperties;
import com.tinkerpop.bench.operation.operations.OperationGetManyEdges;
import com.tinkerpop.bench.operation.operations.OperationGetManyVertexProperties;
import com.tinkerpop.bench.operation.operations.OperationGetManyVertices;
import com.tinkerpop.bench.operation.operations.OperationGetNeighborEdgeConditional;
import com.tinkerpop.bench.operation.operations.OperationSetManyEdgeProperties;
import com.tinkerpop.bench.operation.operations.OperationSetManyVertexProperties;
import com.tinkerpop.bench.util.ConsoleUtils;
import com.tinkerpop.bench.util.MathUtils;
import com.tinkerpop.bench.util.OutputUtils;
import com.tinkerpop.bench.web.DatabaseEngineAndInstance;
import com.tinkerpop.bench.web.Job;
import com.tinkerpop.bench.web.JobList;
import com.tinkerpop.blueprints.Direction;


/**
 * A context for database introspection 
 * 
 * @author Peter Macko (pmacko@eecs.harvard.edu)
 */
public class ModelAnalysis {
	
	/// The directions
	public static final Direction[] DIRECTIONS = new Direction[] { Direction.OUT, Direction.IN, Direction.BOTH };
	
	/// The cache
	private static ConcurrentHashMap<DatabaseEngineAndInstance, ModelAnalysis> cache =
			new ConcurrentHashMap<DatabaseEngineAndInstance, ModelAnalysis>();

	/// The database and instance pair
	private DatabaseEngineAndInstance dbEI;
	
	/// The number of all finished jobs (for cache purposes)
	private int numFinishedJobs;
	
	/// The list of relevant successfully finished jobs
	private List<Job> finishedJobs;
	
	/// The map of operations to finished jobs
	private HashMap<String, SortedSet<Job>> operationToJobs;
	
	/// The primitive operation runtimes -- reads
	public Double Rv /* vertex */, Re /* edge */, Rp /* property */;
	
	/// The primitive operation runtimes -- writes
	public Double Wv /* vertex */, We /* edge */, Wp /* property */;
	
	/// The primitive traversals -- follow the first edge
	public Double[] T             = new Double[3];
	public Double[] T_prediction  = new Double[3];
	
	/// The primitive traversals -- follow the first edge with label
	public Double[] Tl            = new Double[3];
	public Double[] Tl_prediction = new Double[3];
	
	/// The primitive traversals -- follow an edge using a property
	public Double[][] Tp            = new Double[3][];
	public Double[][] Tp_prediction = new Double[3][];
	
	/// Get all neighbors -- follow all first edges
	public Double[][] N             = new Double[3][];
	public Double[][] N_prediction  = new Double[3][];
	
	/// Get all neighbors -- follow all first edges with label
	public Double[][] Nl            = new Double[3][];
	public Double[][] Nl_prediction = new Double[3][];
	
	/// Get all neighbors -- follow edges using a property
	//private Double[][] Np            = new Double[3][];
	//private Double[][] Np_prediction = new Double[3][];
	
	/// Get all K-hop neighbors -- follow all first edges
	public Double[][] K             = new Double[3][];
	public Double[][] K_prediction  = new Double[3][];
	
	/// Get all K-hop neighbors -- follow all first edges with label
	public Double[][] Kl            = new Double[3][];
	public Double[][] Kl_prediction = new Double[3][];
	
	/// Get all K-hop neighbors -- follow edges using a property
	public Double[][] Kp            = new Double[3][];
	public Double[][] Kp_prediction = new Double[3][];
	
	
	/**
	 * Create an instance of the class
	 * 
	 * @param dbei the database engine and instance
	 */
	protected ModelAnalysis(DatabaseEngineAndInstance dbEI) {
		
		this.dbEI = dbEI;
		this.numFinishedJobs = -1;
		
		update();
	}
	
	
	/**
	 * Get an instance of ModelAnalysis for a particular database engine and instance pair
	 * 
	 * @param dbEI the database engine and instance pair
	 */
	public static ModelAnalysis getInstance(DatabaseEngineAndInstance dbEI) {
		
		synchronized (cache) {
			ModelAnalysis m = cache.get(dbEI);
			if (m == null) {
				m = new ModelAnalysis(dbEI);
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
		
		List<Job> jobs = JobList.getInstance().getFinishedJobs(dbEI);
		if (jobs.size() == numFinishedJobs) return false;
		
		
		/*
		 * Compute the generic job maps
		 */
	
		finishedJobs = new ArrayList<Job>();
		operationToJobs = new HashMap<String, SortedSet<Job>>();
		numFinishedJobs = jobs.size();
		
		for (Job job : jobs) {
			numFinishedJobs++;
			
			
			// Exclude failed or unfinished jobs
			
			if (job.getExecutionCount() < 0 || job.getLastStatus() != 0 || job.isRunning()) continue;
			File summaryFile = job.getSummaryFile();
			if (summaryFile == null) continue;
			if (job.getExecutionTime() == null) continue;
			
			
			// Exclude jobs with some command-line arguments
			
			if (job.getArguments().contains("--use-stored-procedures")) continue;
			
			
			// We found a relevant job!
			
			finishedJobs.add(job);
			
			
			// Operation Maps
			
			SummaryLogReader reader = new SummaryLogReader(summaryFile);
			for (SummaryLogEntry e : reader) {
				String name = e.getName();
				if (name.equals("OperationOpenGraph")
						|| name.equals("OperationDoGC")
						|| name.equals("OperationShutdownGraph")) continue;
				
				int tagStart = name.indexOf('-');
				if (tagStart > 0) name = name.substring(0, tagStart);
				
				SortedSet<Job> ojs = operationToJobs.get(name);
				if (ojs == null) {
					ojs = new TreeSet<Job>();
					operationToJobs.put(name, ojs);
				}
				ojs.add(job);
			}
		}
		
		
		/*
		 * Pull out the primitive operations
		 */
		
		Rv = getAverageOperationRuntime(OperationGetManyVertices.class);
		Re = getAverageOperationRuntime(OperationGetManyEdges.class);
		Rp = MathUtils.averageIgnoreNulls(getAverageOperationRuntime(OperationGetManyVertexProperties.class),
				getAverageOperationRuntime(OperationGetManyEdgeProperties.class));
		
		Wv = getAverageOperationRuntime(OperationAddManyVertices.class);
		We = getAverageOperationRuntime(OperationAddManyEdges.class);
		Wp = MathUtils.averageIgnoreNulls(getAverageOperationRuntime(OperationSetManyVertexProperties.class),
				getAverageOperationRuntime(OperationSetManyEdgeProperties.class));
		
		
		/*
		 * Pull out the primitive traversals
		 */
		
		for (Direction d : DIRECTIONS) {
			String t  = OutputUtils.toTag(d);
			int index = translateDirection(d);
			
			T [index] = getAverageOperationRuntime(OperationGetFirstNeighbor.class, t);
			Tl[index] = getAverageOperationRuntime(OperationGetFirstNeighbor.class, t + "-withlabel");
			Tp[index] = getOperationRuntimeAsLinearFunction(OperationGetNeighborEdgeConditional.class, t);
			
			T_prediction [index] = MathUtils.sum(Re, Rv);
			Tl_prediction[index] = MathUtils.sum(Re, Rv);
			Tp_prediction[index] = MathUtils.ifNeitherIsNull(MathUtils.sum(Re, Rv), Rp);
		}
		
		
		/*
		 * Pull out the get all neighbors operations
		 */
		
		for (Direction d : DIRECTIONS) {
			String t  = OutputUtils.toTag(d);
			int index = translateDirection(d);
		
			N [index] = getOperationRuntimeAsLinearFunction(OperationGetAllNeighbors.class, t);
			Nl[index] = getOperationRuntimeAsLinearFunction(OperationGetAllNeighbors.class, t + "-withlabel");
			
			N_prediction [index] = MathUtils.ifNeitherIsNull(new Double(0), T[index]);
			Nl_prediction[index] = MathUtils.ifNeitherIsNull(new Double(0), Tl[index]);
		}
		
		
		/*
		 * Pull out the get all K-hop neighbors operations
		 */
		
		for (Direction d : DIRECTIONS) {
			String t  = OutputUtils.toTag(d);
			int index = translateDirection(d);
			
			String[] tags = new String[4];
			for (int i = 1; i <= tags.length; i++) tags[i-1] = t + "-" + i;
			
			String[] tagsWithLabel = new String[tags.length];
			for (int i = 1; i <= tags.length; i++) tagsWithLabel[i-1] = t + "-withlabel-" + i;
			
			K [index] = getOperationRuntimeAsLinearFunction(OperationGetKHopNeighbors.class, tags);
			Kl[index] = getOperationRuntimeAsLinearFunction(OperationGetKHopNeighbors.class, tagsWithLabel);
			Kp[index] = getOperationRuntimeAsLinearFunction(OperationGetKHopNeighborsEdgeConditional.class, tags);
			
			K_prediction [index] = N [index];
			Kl_prediction[index] = Nl[index];
			Kp_prediction[index] = Tp[index];
		}
		
		
		/*
		 * Finish
		 */
		
		return true;
	}
	
	
	/**
	 * Get prediction for the operation
	 * 
	 * @param operationNameWithTags the operation name with all of its tags
	 * @return the prediction as a linear function, or null
	 */
	public Double[] getPrediction(String operationNameWithTags) {
		return getPredictionOrFit(operationNameWithTags, true);
	}
	
	
	/**
	 * Get the fit for the operation
	 * 
	 * @param operationNameWithTags the operation name with all of its tags
	 * @return the fit as a linear function, or null
	 */
	public Double[] getLinearFit(String operationNameWithTags) {
		return getPredictionOrFit(operationNameWithTags, false);
	}
	
	
	/**
	 * Get prediction or the fit for the operation
	 * 
	 * @param operationNameWithTags the operation name with all of its tags
	 * @param prediction true for a prediction
	 * @return the prediction as a linear function, or null
	 */
	public Double[] getPredictionOrFit(String operationNameWithTags, boolean prediction) {
		
		String operationName = operationNameWithTags;
		int tagStart = operationName.indexOf('-');
		if (tagStart > 0) operationName = operationName.substring(0, tagStart);
		
		
		// Parse the tags
		
		String[] tags;
		if (tagStart > 0) {
			tags = operationNameWithTags.substring(tagStart + 1).split("-");
		}
		else {
			tags = new String[0];
		}
		
		boolean withlabel = false;
		Direction direction = null;
		
		for (String t : tags) {			
			if (t.equals("withlabel")) withlabel = true;
			if (t.equals("out"      )) direction = Direction.OUT;
			if (t.equals("in"       )) direction = Direction.IN;
			if (t.equals("both"     )) direction = Direction.BOTH;
		}
		
		
		// Get the prediction, if available
		
		if (OperationGetNeighborEdgeConditional.class.getSimpleName().equals(operationName)) {
			if (direction == null) throw new IllegalArgumentException("No direction");
			if (withlabel) {
				return prediction ? Tp_prediction[translateDirection(direction)] : Tp[translateDirection(direction)];
			}
			else {
				return prediction ? Tp_prediction[translateDirection(direction)] : Tp[translateDirection(direction)];
			}
		}
		
		if (OperationGetAllNeighbors.class.getSimpleName().equals(operationName)) {
			if (direction == null) throw new IllegalArgumentException("No direction");
			if (withlabel) {
				return prediction ? Nl_prediction[translateDirection(direction)] : Nl[translateDirection(direction)];
			}
			else {
				return prediction ? N_prediction[translateDirection(direction)] : N[translateDirection(direction)];
			}
		}
		
		if (OperationGetKHopNeighbors.class.getSimpleName().equals(operationName)) {
			if (direction == null) throw new IllegalArgumentException("No direction");
			if (withlabel) {
				return prediction ? Kl_prediction[translateDirection(direction)] : Kl[translateDirection(direction)];
			}
			else {
				return prediction ? K_prediction[translateDirection(direction)] : K[translateDirection(direction)];
			}
		}
		
		if (OperationGetKHopNeighborsEdgeConditional.class.getSimpleName().equals(operationName)) {
			if (direction == null) throw new IllegalArgumentException("No direction");
			if (withlabel) {
				return prediction ? Kp_prediction[translateDirection(direction)] : Kp[translateDirection(direction)];
			}
			else {
				return prediction ? Kp_prediction[translateDirection(direction)] : Kp[translateDirection(direction)];
			}
		}
		
		return null;
	}
	
	
	/**
	 * Print two columns
	 * 
	 * @param cw the column width
	 * @param variable the variable name
	 * @param model the model string
	 * @param value the actual value
	 * @param predicted the predicted value
	 */
	private void print(int cw, String variable, String model, Object value, Object predicted) {
		
		String s_value = null;
		if (value == null) {
			s_value = null;
		}
		else if (value instanceof Double) {
			s_value = OutputUtils.format((Double) value);
		}
		else if (value instanceof Double[]) {
			s_value = OutputUtils.formatLinearCombination((Double[]) value, "n");
		}
		else {
			throw new IllegalArgumentException("Illegal type for the observed value");
		}
		
		String s_predicted = null;
		if (predicted == null) {
			s_predicted = null;
		}
		else if (predicted instanceof Double) {
			s_predicted = OutputUtils.format((Double) predicted);
		}
		else if (predicted instanceof Double[]) {
			s_predicted = OutputUtils.formatLinearCombination((Double[]) predicted, "n");
		}
		else {
			throw new IllegalArgumentException("Illegal type for the predicted value");
		}
		
		ConsoleUtils.printColumns(cw, variable + " = " + s_value,
				variable + "_prediction = " + model + " = " + s_predicted);
	}
	
	
	/**
	 * Print the analysis
	 */
	public void printAnalysis() {
		
		int CW = 30;
		
		ConsoleUtils.sectionHeader("Model Analysis");
		
		System.out.println("All values are in ms unless stated otherwise.");
		System.out.println();
		
		System.out.println();
		ConsoleUtils.header("Primitive Operations");
		System.out.println();
		
		ConsoleUtils.printColumns(CW, "Rv = " + OutputUtils.format(Rv), "Wv = " + OutputUtils.format(Wv));
		ConsoleUtils.printColumns(CW, "Re = " + OutputUtils.format(Re), "We = " + OutputUtils.format(We));
		ConsoleUtils.printColumns(CW, "Rp = " + OutputUtils.format(Rp), "Wp = " + OutputUtils.format(Wp));
		System.out.println();
		
		System.out.println();
		ConsoleUtils.header("Micro Operations");
		System.out.println();
		
		for (Direction d : DIRECTIONS) {
			String t  = OutputUtils.toTag(d);
			int index = translateDirection(d);
			print(CW, "T" + t.charAt(0), "Re + Rv", T[index], T_prediction[index]);
		}
		System.out.println();
		
		for (Direction d : DIRECTIONS) {
			String t  = OutputUtils.toTag(d);
			int index = translateDirection(d);
			print(CW, "Tl" + t.charAt(0), "Re + Rv", Tl[index], Tl_prediction[index]);
		}
		System.out.println();
		
		for (Direction d : DIRECTIONS) {
			String t  = OutputUtils.toTag(d);
			int index = translateDirection(d);
			print(CW, "Tp" + t.charAt(0), "Rp + (Re+Rv)*n", Tp[index], Tp_prediction[index]);
		}
		System.out.println();
		
		System.out.println();
		ConsoleUtils.header("Graph Operations");
		System.out.println();
		
		for (Direction d : DIRECTIONS) {
			String t  = OutputUtils.toTag(d);
			int index = translateDirection(d);
			print(CW, "N" + t.charAt(0), "0 + T"+t.charAt(0)+"*n", N[index], N_prediction[index]);
		}
		System.out.println();
		
		for (Direction d : DIRECTIONS) {
			String t  = OutputUtils.toTag(d);
			int index = translateDirection(d);
			print(CW, "Nl" + t.charAt(0), "0 + Tl"+t.charAt(0)+"*n", Nl[index], Nl_prediction[index]);
		}
		System.out.println();
		
		for (Direction d : DIRECTIONS) {
			String t  = OutputUtils.toTag(d);
			int index = translateDirection(d);
			print(CW, "K" + t.charAt(0), "N" + t.charAt(0), K[index], K_prediction[index]);
		}
		System.out.println();
		
		for (Direction d : DIRECTIONS) {
			String t  = OutputUtils.toTag(d);
			int index = translateDirection(d);
			print(CW, "Kl" + t.charAt(0), "Nl" + t.charAt(0), Kl[index], Kl_prediction[index]);
		}
		System.out.println();
		
		for (Direction d : DIRECTIONS) {
			String t  = OutputUtils.toTag(d);
			int index = translateDirection(d);
			print(CW, "Kp" + t.charAt(0), "Tp" + t.charAt(0), Kp[index], Kp_prediction[index]);
		}
		System.out.println();
	}
	
	
	/**
	 * Translate a Direction to an index
	 * 
	 * @param d the direction
	 * @return an index
	 */
	public static int translateDirection(Direction d) {
		
		switch (d) {
		case OUT : return 0;
		case IN  : return 1;
		case BOTH: return 2;
		default  : throw new IllegalArgumentException();
		}
	}
	
	
	/**
	 * Get a runtime of an operation from the latest job, taking a simple average
	 * if there is more than one within the job. If the operation is of the "Many"
	 * type, then return the average runtime of the simpler operation that gets
	 * repeated.
	 * 
	 * @param operation the operation type
	 * @param tags the tags
	 * @return the runtime in ms, or null if not found
	 */
	private Double getAverageOperationRuntime(Class<? extends Operation> operation, String... tags) {
		return getAverageOperationRuntime(operation.getSimpleName(), tags);
	}
	
	
	/**
	 * Get a runtime of an operation from the latest job, taking a simple average
	 * if there is more than one within the job. If the operation is of the "Many"
	 * type, then return the average runtime of the simpler operation that gets
	 * repeated.
	 * 
	 * @param operationName the operation name
	 * @param tags the tags
	 * @return the runtime in ms, or null if not found
	 */
	private Double getAverageOperationRuntime(String operationName, String... tags) {
		
		boolean many = operationName.contains("GetMany")
				|| operationName.contains("AddMany")
				|| operationName.contains("SetMany");
		
		int opCountArg = -1;
		if (many) {
			if (operationName.equals("OperationGetManyVertices"          )) opCountArg = 0;
			if (operationName.equals("OperationGetManyEdges"             )) opCountArg = 0;
			if (operationName.equals("OperationGetManyVertexProperties"  )) opCountArg = 1;
			if (operationName.equals("OperationGetManyEdgeProperties"    )) opCountArg = 1;
			if (operationName.equals("OperationGetManyVertexPropertySets")) opCountArg = 1;
			if (operationName.equals("OperationGetManyEdgePropertySets"  )) opCountArg = 1;
			if (operationName.equals("OperationAddManyVertices"          )) opCountArg = 0;
			if (operationName.equals("OperationAddManyEdges"             )) opCountArg = 0;
			if (operationName.equals("OperationSetManyVertexProperties"  )) opCountArg = 1;
			if (operationName.equals("OperationSetManyEdgeProperties"    )) opCountArg = 1;
			if (opCountArg < 0) {
				throw new IllegalArgumentException("Unsupported \"GetMany\" operation " + operationName);
			}
		}
		
		
		// Find the correct job
		
		SortedSet<Job> operationJobs = operationToJobs.get(operationName);
		if (operationJobs == null || operationJobs.isEmpty()) return null;
		Job job = operationJobs.last();
		
		
		// Read the log file
		
		OperationLogReader reader = new OperationLogReader(job.getLogFile());
		
		double time_ms = 0;
		int count = 0;
		
		for (OperationLogEntry e : reader) {
			
			if (tags.length == 0) {
				if (!e.getName().equals(operationName)
						&& !e.getName().startsWith(operationName + "-")) continue;
			}
			else {
				boolean ok = false;
				for (String t : tags) {
					if (e.getName().equals(operationName + "-" + t)) { ok = true; break; }
				}
				if (!ok) continue;
			}
			
			if (many) {
				String s = e.getArgs()[opCountArg];
				int opCount = Integer.parseInt(s);
				if (!s.equals("" + opCount)) throw new NumberFormatException(s);
				count += opCount;
			}
			else {
				count++;
			}
			
			time_ms += e.getTime() / 1000000.0;
		}
		
		if (count <= 0) return null;
		
		return time_ms / count;
	}
	
	
	/**
	 * Get a runtime of a job as a linear function of one of the operation results
	 * 
	 * @param operation the operation type
	 * @param tags the tags
	 * @return the runtime in ms as a function described by [intercept, slope], or null if not found
	 */
	private Double[] getOperationRuntimeAsLinearFunction(Class<? extends Operation> operation, String... tags) {
		return getOperationRuntimeAsLinearFunction(operation.getSimpleName(), tags);
	}
	
	
	/**
	 * Get a runtime of a job as a linear function of one of the operation results
	 * 
	 * @param operationName the operation name
	 * @param tags the tags
	 * @return the runtime in ms as a function described by [intercept, slope], or null if not found
	 */
	private Double[] getOperationRuntimeAsLinearFunction(String operationName, String... tags) {
		
		// Linear regression algorithm from:
		//     http://introcs.cs.princeton.edu/java/97data/LinearRegression.java.html
		
	
		int xArg = -1;
		if (operationName.equals("OperationGetAllNeighbors"                )) xArg = 3;
		if (operationName.equals("OperationGetNeighborEdgeConditional"     )) xArg = 3;
		if (operationName.equals("OperationGetKHopNeighbors"               )) xArg = 3;
		if (operationName.equals("OperationGetKHopNeighborsEdgeConditional")) xArg = 3;
		if (xArg < 0) {
			throw new IllegalArgumentException("Unsupported operation " + operationName);
		}
		
		
		// Find the correct job
		
		SortedSet<Job> operationJobs = operationToJobs.get(operationName);
		if (operationJobs == null || operationJobs.isEmpty()) return null;
		Job job = operationJobs.last();
		
		
		// Read the log file
		
		OperationLogReader reader = new OperationLogReader(job.getLogFile());
		
		Vector<Double > y = new Vector<Double >();
		Vector<Integer> x = new Vector<Integer>();
		@SuppressWarnings("unused")
		double sumx = 0, sumy = 0, sumx2 = 0; int n = 0;
		boolean samex = true; int lastx = 0;

		for (OperationLogEntry e : reader) {

			if (tags.length == 0) {
				if (!e.getName().equals(operationName)
						&& !e.getName().startsWith(operationName + "-")) continue;
			}
			else {
				boolean ok = false;
				for (String t : tags) {
					if (e.getName().equals(operationName + "-" + t)) { ok = true; break; }
				}
				if (!ok) continue;
			}

			int    xv = Integer.parseInt(e.getResult().split(":")[xArg]);
			double yv = e.getTime() / 1000000.0;
			
			if (n == 0) {
				lastx = xv;
			}
			else {
				if (lastx != xv) samex = false;
				lastx = xv;
			}

			sumx  += xv;
			sumx2 += xv * xv;
			sumy  += yv;
			n++;

			x.add(xv);
			y.add(yv);
 		}

		if (n <= 0) return null;

		double xbar = sumx / n;
		double ybar = sumy / n;

		@SuppressWarnings("unused")
		double xxbar = 0.0, yybar = 0.0, xybar = 0.0;
		for (int i = 0; i < n; i++) {
			xxbar += (x.get(i) - xbar) * (x.get(i) - xbar);
			yybar += (y.get(i) - ybar) * (y.get(i) - ybar);
			xybar += (x.get(i) - xbar) * (y.get(i) - ybar);
		}
		double beta1 = xybar / xxbar;
		double beta0 = ybar - beta1 * xbar;
		
		if (samex) {
			// Might need to revisit this -- it might be beneficial to keep the
			// slope, even if there is only one unique value of x
			beta0 = getAverageOperationRuntime(operationName, tags);
			beta1 = 0;
		}
		
		/*// print results
		System.out.println("y   = " + beta1 + " * x + " + beta0);
		// analyze results
		int df = n - 2;
		double rss = 0.0;      // residual sum of squares
		double ssr = 0.0;      // regression sum of squares
		for (int i = 0; i < n; i++) {
			double fit = beta1*x.get(i) + beta0;
			rss += (fit - y.get(i)) * (fit - y.get(i));
			ssr += (fit - ybar) * (fit - ybar);
		}
		double R2    = ssr / yybar;
		double svar  = rss / df;
		double svar1 = svar / xxbar;
		double svar0 = svar/n + xbar*xbar*svar1;
		System.out.println("R^2                 = " + R2);
		System.out.println("std error of beta_1 = " + Math.sqrt(svar1));
		System.out.println("std error of beta_0 = " + Math.sqrt(svar0));
		svar0 = svar * sumx2 / (n * xxbar);
		System.out.println("std error of beta_0 = " + Math.sqrt(svar0));

		System.out.println("SSTO = " + yybar);
		System.out.println("SSE  = " + rss);
		System.out.println("SSR  = " + ssr);*/
        
		return new Double[] { beta0, beta1 };
	}
}
