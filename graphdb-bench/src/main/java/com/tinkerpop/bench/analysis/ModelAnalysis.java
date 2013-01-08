package com.tinkerpop.bench.analysis;

import java.util.HashMap;
import java.util.SortedSet;
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
import com.tinkerpop.bench.operation.operations.OperationGetShortestPath;
import com.tinkerpop.bench.operation.operations.OperationGetVerticesUsingKeyIndex;
import com.tinkerpop.bench.operation.operations.OperationLocalClusteringCoefficient;
import com.tinkerpop.bench.operation.operations.OperationSetManyEdgeProperties;
import com.tinkerpop.bench.operation.operations.OperationSetManyVertexProperties;
import com.tinkerpop.bench.util.ConsoleUtils;
import com.tinkerpop.bench.util.MathUtils;
import com.tinkerpop.bench.util.OutputUtils;
import com.tinkerpop.bench.util.Pair;
import com.tinkerpop.bench.web.DatabaseEngineAndInstance;
import com.tinkerpop.bench.web.Job;
import com.tinkerpop.blueprints.Direction;


/**
 * Analysis using Margo's database introspection model 
 * 
 * @author Peter Macko (pmacko@eecs.harvard.edu)
 */
public class ModelAnalysis {

	/// The directions
	public static final Direction[] DIRECTIONS = new Direction[] { /*Direction.OUT, Direction.IN,*/ Direction.BOTH };
	
	
	/*
	 * Configuration
	 */

	/// Whether to use KHops operations with K=1 instead of the simpler (in theory, equivalent) primitives
	public static boolean useKHops1 = false;
	
	/// Whether to use robust linear fits
	public static boolean useRobustFits = false;
	
	/// Whether to always use the detailed log files instead of the summary files
	public static boolean alwaysUseDetailedLogs = false;
	
	
	/*
	 * Cache
	 */
	
	/// The cache
	private static ConcurrentHashMap<DatabaseEngineAndInstance, ModelAnalysis> cache =
			new ConcurrentHashMap<DatabaseEngineAndInstance, ModelAnalysis>();

	
	/*
	 * This instance
	 */
	
	/// The database and instance pair
	private DatabaseEngineAndInstance dbEI;
	
	/// The analysis context
	private AnalysisContext context;
	
	/// The map of x bounds
	private HashMap<String, Double[]> xBounds;
	
	
	/*
	 * Observations and predictions
	 */
	
	/// The primitive operation runtimes -- reads
	public Double Rv /* vertex */, Re /* edge */, Rp /* property */;
	
	/// The primitive operation runtimes -- writes
	public Double Wv /* vertex */, We /* edge */, Wp /* property */;
	
	/// The primitive operation runtimes -- read a vertex using an index on a unique property
	public Double Rvup;
	
	/// The primitive traversals -- follow the first edge
	public Double[] T             = new Double[DIRECTIONS.length];
	public Double[] T_prediction  = new Double[DIRECTIONS.length];
	
	/// The primitive traversals -- follow the first edge with label
	public Double[] Tl            = new Double[DIRECTIONS.length];
	public Double[] Tl_prediction = new Double[DIRECTIONS.length];
	
	/// The primitive traversals -- follow an edge using a property
	public Double[][] Tp            = new Double[DIRECTIONS.length][];
	public Double[][] Tp_prediction = new Double[DIRECTIONS.length][];
	
	/// Get all neighbors -- follow all first edges
	public Double[][] N             = new Double[DIRECTIONS.length][];
	public Double[][] N_prediction  = new Double[DIRECTIONS.length][];
	
	/// Get all neighbors -- follow all first edges with label
	public Double[][] Nl            = new Double[DIRECTIONS.length][];
	public Double[][] Nl_prediction = new Double[DIRECTIONS.length][];
	
	/// Get all neighbors -- follow edges using a property
	//private Double[][] Np            = new Double[DIRECTIONS.length][];
	//private Double[][] Np_prediction = new Double[DIRECTIONS.length][];
	
	/// Get all K-hop neighbors -- follow all first edges
	public Double[][][] K             = new Double[][][] { new Double[5][], new Double[5][], new Double[5][] };
	public Double[][][] K_prediction  = new Double[][][] { new Double[5][], new Double[5][], new Double[5][] };
	
	/// Get all K-hop neighbors -- follow all first edges with label
	public Double[][][] Kl            = new Double[][][] { new Double[5][], new Double[5][], new Double[5][] };
	public Double[][][] Kl_prediction = new Double[][][] { new Double[5][], new Double[5][], new Double[5][] };
	
	/// Get all K-hop neighbors -- follow edges using a property
	public Double[][][] Kp            = new Double[][][] { new Double[5][], new Double[5][], new Double[5][] };
	public Double[][][] Kp_prediction = new Double[][][] { new Double[5][], new Double[5][], new Double[5][] };
	
	/// Local clustering coefficient
	public Double[] CC;
	public Double[] CC_prediction;
	
	/// Shortest path
	public static final int SP_MAX_DEPTH = 10;
	public Double[][] SP            = new Double[SP_MAX_DEPTH][];
	public Double[][] SP_prediction = new Double[SP_MAX_DEPTH][];
	public Double[][] SP_x_bounds   = new Double[SP_MAX_DEPTH][];
	
	
	/**
	 * Create an instance of the class
	 * 
	 * @param dbei the database engine and instance
	 */
	protected ModelAnalysis(DatabaseEngineAndInstance dbEI) {
		
		this.dbEI = dbEI;
		this.context = null;
		
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
	 * Get the analysis context
	 * 
	 * @return the context
	 */
	public AnalysisContext getContext() {
		return context;
	}
	
	
	/**
	 * Recompute if necessary
	 * 
	 * @return true if it was recomputed
	 */
	public boolean update() {
		
		if (context == null) {
			context = AnalysisContext.getInstance(dbEI);
		}
		else {
			if (!context.update()) return false;
		}

		
		/*
		 * Initialize
		 */
		
		xBounds = new HashMap<String, Double[]>();
		
		
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
		
		Rvup = getAverageOperationRuntime(OperationGetVerticesUsingKeyIndex.class, "_original_id");
		
		
		/*
		 * Pull out properties
		 */
		
		Double averageDegree = getAverageResultValue("OperationGetAllNeighbors", new String[] { "both" });

		
		/*
		 * Pull out the primitive traversals
		 */
		
		for (Direction d : DIRECTIONS) {
			String t  = OutputUtils.toTag(d);
			int index = translateDirection(d);
			
			T [index] = getAverageOperationRuntime(OperationGetFirstNeighbor.class, t);
			Tl[index] = getAverageOperationRuntime(OperationGetFirstNeighbor.class, t + "-withlabel");
			Tp[index] = getOperationRuntimeAsLinearFunction(OperationGetNeighborEdgeConditional.class, t);
			
			if (useKHops1) {
				Tp[index] = getOperationRuntimeAsLinearFunction(OperationGetKHopNeighborsEdgeConditional.class, t + "-" + 1);
			}
			
			T_prediction [index] = MathUtils.product(MathUtils.sum(Re, Rv), averageDegree);
			Tl_prediction[index] = MathUtils.product(MathUtils.sum(Re, Rv), averageDegree);
			Tp_prediction[index] = MathUtils.multiplyElementwise(MathUtils.ifNeitherIsNull(MathUtils.sum(Re, Rv), Rp), averageDegree);
		}
		
		
		/*
		 * Pull out the get all neighbors operations
		 */
		
		for (Direction d : DIRECTIONS) {
			String t  = OutputUtils.toTag(d);
			int index = translateDirection(d);
		
			N [index] = getOperationRuntimeAsLinearFunction(OperationGetAllNeighbors.class, t);
			Nl[index] = getOperationRuntimeAsLinearFunction(OperationGetAllNeighbors.class, t + "-withlabel");
			
			if (useKHops1) {
				N [index] = getOperationRuntimeAsLinearFunction(OperationGetKHopNeighbors.class, t + "-" + 1);
				Nl[index] = getOperationRuntimeAsLinearFunction(OperationGetKHopNeighbors.class, t + "-withlabel-" + 1);
				
				if (N[index] != null) {
					Double[] D = N[index];
					if (D[1] < 0) {
						N [index] = MathUtils.multiplyElementwise(getOperationRuntimeAsLinearFunction(OperationGetKHopNeighbors.class, t + "-" + 2), (double) 0.5);
					}
				}
				
				if (Nl[index] != null) {
					Double[] D = Nl[index];
					if (D[1] < 0) {
						Nl[index] = MathUtils.multiplyElementwise(getOperationRuntimeAsLinearFunction(OperationGetKHopNeighbors.class, t + "-withlabel-" + 2), (double) 0.5);
					}
				}
			}
			
			N_prediction [index] = MathUtils.ifNeitherIsNull(new Double(0), MathUtils.sum(Re, Rv));
			Nl_prediction[index] = MathUtils.ifNeitherIsNull(new Double(0), MathUtils.sum(Re, Rv));
		}
		
		
		/*
		 * Pull out the get all K-hop neighbors operations
		 */
		
		for (Direction d : DIRECTIONS) {
			String t  = OutputUtils.toTag(d);
			int index = translateDirection(d);

			for (int k = 1; k <= 5; k++) {
				K [index][k-1] = getOperationRuntimeAsLinearFunction(OperationGetKHopNeighbors.class, t + "-" + k);
				Kl[index][k-1] = getOperationRuntimeAsLinearFunction(OperationGetKHopNeighbors.class, t + "-withlabel-" + k);
				Kp[index][k-1] = getOperationRuntimeAsLinearFunction(OperationGetKHopNeighborsEdgeConditional.class, t + "-" + k);
				
				K_prediction [index][k-1] = N [index];
				Kl_prediction[index][k-1] = Nl[index];
				Kp_prediction[index][k-1] = Tp[index];
				
				/*K_prediction [index][k-1] = MathUtils.multiplyElementwise(N [index], (double) k);
				Kl_prediction[index][k-1] = MathUtils.multiplyElementwise(Nl[index], (double) k);
				Kp_prediction[index][k-1] = MathUtils.multiplyElementwise(Tp[index], (double) k);*/
			}
		}
		
		
		/*
		 * Algorithms
		 */
		
		CC = getOperationRuntimeAsLinearFunction(OperationLocalClusteringCoefficient.class);
		CC_prediction = K_prediction[translateDirection(Direction.BOTH)][2-1];
		
		for (int k = 0; k < SP_MAX_DEPTH; k++) {
			SP[k] = getOperationRuntimeAsLinearFunctionWithResultFilter(OperationGetShortestPath.class, null, k);
			SP_x_bounds[k] = getXBounds(OperationGetShortestPath.class, null, k);
			assert SP[k] == null || SP_x_bounds[k] != null;
			
			int index = translateDirection(Direction.BOTH);
			if (k == 0) {
				SP_prediction[k] = MathUtils.ifNeitherIsNull(Rv, (double) 0);
			}
			else {
				SP_prediction[k] = N[index];
			}
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
		int number = -1;
		
		for (String t : tags) {		
			if (t.equals("")) continue;
			
			if (t.equals("withlabel")) withlabel = true;
			if (t.equals("out"      )) direction = Direction.OUT;
			if (t.equals("in"       )) direction = Direction.IN;
			if (t.equals("both"     )) direction = Direction.BOTH;
			
			boolean isNumeric = true;
			for (int i = 0; i < t.length(); i++) {
				if (!Character.isDigit(t.charAt(i))) {
					isNumeric = false;
					break;
				}
			}
			if (isNumeric) {
				number = Integer.parseInt(t);
			}
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
				return prediction ? Kl_prediction[translateDirection(direction)][number-1] : Kl[translateDirection(direction)][number-1];
			}
			else {
				return prediction ? K_prediction[translateDirection(direction)][number-1] : K[translateDirection(direction)][number-1];
			}
		}
		
		if (OperationGetKHopNeighborsEdgeConditional.class.getSimpleName().equals(operationName)) {
			if (direction == null) throw new IllegalArgumentException("No direction");
			if (withlabel) {
				return prediction ? Kp_prediction[translateDirection(direction)][number-1] : Kp[translateDirection(direction)][number-1];
			}
			else {
				return prediction ? Kp_prediction[translateDirection(direction)][number-1] : Kp[translateDirection(direction)][number-1];
			}
		}
		
		if (OperationLocalClusteringCoefficient.class.getSimpleName().equals(operationName)) {
			return prediction ? CC_prediction : CC;
		}
		
		if (OperationGetShortestPath.class.getSimpleName().equals(operationName)) {
			if (number < 0) throw new IllegalArgumentException("The path length needs to be specified");
			return prediction ? SP_prediction[number] : SP[number];
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
		
		int CW = 32;
		
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
		
		for (int k = 1; k <= 5; k++) {
			for (Direction d : DIRECTIONS) {
				String t  = OutputUtils.toTag(d);
				int index = translateDirection(d);
				print(CW, "K[k="+k+"]" + t.charAt(0), "N" + t.charAt(0) + " * k", K[index][k-1], K_prediction[index][k-1]);
			}
			for (Direction d : DIRECTIONS) {
				String t  = OutputUtils.toTag(d);
				int index = translateDirection(d);
				print(CW, "K[k="+k+"]l" + t.charAt(0), "Nl" + t.charAt(0) + " * k", Kl[index][k-1], Kl_prediction[index][k-1]);
			}
			for (Direction d : DIRECTIONS) {
				String t  = OutputUtils.toTag(d);
				int index = translateDirection(d);
				print(CW, "K[k="+k+"]p" + t.charAt(0), "Tp" + t.charAt(0) + " * k", Kp[index][k-1], Kp_prediction[index][k-1]);
			}
			System.out.println();
		}
		
		System.out.println();
		ConsoleUtils.header("Algorithms");
		System.out.println();
		
		print(CW, "CC", "K[k=2]b", CC, CC_prediction);
		System.out.println();
		
		for (int k = 1; k < SP_MAX_DEPTH; k++) {
			print(CW, "SP[k="+k+"]", "K[k="+k+"]b", SP[k], SP_prediction[k]);
		}
	}
	
	
	/**
	 * Translate a Direction to an index
	 * 
	 * @param d the direction
	 * @return an index
	 */
	public static int translateDirection(Direction d) {
		
		for (int i = 0; i < DIRECTIONS.length; i++) {
			if (DIRECTIONS[i].equals(d)) return i;
		}
		
		throw new IllegalArgumentException();
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
		
		boolean many = AnalysisUtils.isManyOperation(operationName);
		int opCountArg = !many ? -1 : AnalysisUtils.getManyOperationOpCountArgumentIndex(operationName);
		
		
		// Find the correct job
		
		SortedSet<Job> operationJobs = null;		
		if (tags.length == 0) {
			operationJobs = context.operationWithTagsToJobs.get(operationName);
		}
		else if (tags.length == 1) {
			operationJobs = context.operationWithTagsToJobs.get(operationName + "-" + tags[0]);
		}
		else {
			throw new UnsupportedOperationException();
		}

		if (operationJobs == null || operationJobs.isEmpty()) return null;
		Job job = operationJobs.last();
		
		
		// Get the average operation run time
		
		if (alwaysUseDetailedLogs || tags.length > 1) {
			
			// Read the log file
			
			OperationLogReader reader = new OperationLogReader(job.getLogFile());
			Vector<Double> times_ms = new Vector<Double>();
			Vector<Integer> counts = new Vector<Integer>();
			
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
				
				int count;
				if (many) {
					String s = e.getArgs()[opCountArg >= 0 ? opCountArg : e.getArgs().length + opCountArg];
					int opCount = Integer.parseInt(s);
					if (!s.equals("" + opCount)) throw new NumberFormatException(s);
					count = opCount;
				}
				else {
					count = 1;
				}
				
				counts.add(count);
				times_ms.add(e.getTime() / 1000000.0);
			}
			
			assert counts.size() == times_ms.size();
			if (counts.isEmpty()) return null;
			
			
	    	// Compute the mean from the last 25%
	    	
	    	int l = counts.size();
	    	int from = (3 * l) / 4; 
	    	if (from >= l) from = 0;
	    	
	    	double time = 0; int count = 0;
	    	for (int i = from; i < l; i++) {
	    		time += times_ms.get(i).doubleValue();
	    		count += counts.get(i).intValue();
	    	}
			
			return time / count;
		}
		else {
			
			// Read the log file
			
			String name = operationName;
			if (tags.length == 1) name += "-" + tags[0];
			SummaryLogEntry entry = SummaryLogReader.getEntryForOperation(job.getSummaryFile(), name);
			if (entry == null) return null;
			
			if (many) {
				entry = AnalysisUtils.convertLogEntryForManyOperation(entry, job);
			}
			
			return entry.getDefaultRunTimes().getMean() / 1000000.0;
		}
	}
	
	
	/**
	 * Get a runtime of a job as a linear function of one of the operation results
	 * 
	 * @param operation the operation type
	 * @param tag the tag
	 * @return the runtime in ms as a function described by [intercept, slope], or null if not found
	 */
	private Double[] getOperationRuntimeAsLinearFunction(Class<? extends Operation> operation, String... tags) {
		return getOperationRuntimeAsLinearFunction(operation.getSimpleName(), tags, null);
	}
	
	
	/**
	 * Get a runtime of a job as a linear function of one of the operation results
	 * 
	 * @param operation the operation type
	 * @param resultFilter the result filter
	 * @return the runtime in ms as a function described by [intercept, slope], or null if not found
	 */
	private Double[] getOperationRuntimeAsLinearFunctionWithResultFilter(
			Class<? extends Operation> operation, Integer... resultFilter) {
		return getOperationRuntimeAsLinearFunction(operation.getSimpleName(), new String[0], resultFilter);
	}
	
	
	/**
	 * Get a runtime of a job as a linear function of one of the operation results
	 * 
	 * @param operationName the operation name
	 * @param tags the tags
	 * @param resultFilter the result filter
	 * @return the runtime in ms as a function described by [intercept, slope], or null if not found
	 */
	private Double[] getOperationRuntimeAsLinearFunction(String operationName, String[] tags, Integer[] resultFilter) {
	
		int xArg = -1;
		if (operationName.equals("OperationGetAllNeighbors"                )) xArg = 3;
		if (operationName.equals("OperationGetNeighborEdgeConditional"     )) xArg = 3;
		if (operationName.equals("OperationGetKHopNeighbors"               )) xArg = 3;
		if (operationName.equals("OperationGetKHopNeighborsEdgeConditional")) xArg = 3;
		if (operationName.equals("OperationLocalClusteringCoefficient"     )) xArg = 3;
		if (operationName.equals("OperationGetShortestPath"                )) xArg = 3;
		if (xArg < 0) {
			throw new IllegalArgumentException("Unsupported operation " + operationName);
		}
		
		
		// Find the correct job
		
		SortedSet<Job> operationJobs = null;		
		if (tags.length == 0) {
			operationJobs = context.operationWithTagsToJobs.get(operationName);
		}
		else if (tags.length == 1) {
			operationJobs = context.operationWithTagsToJobs.get(operationName + "-" + tags[0]);
		}
		else {
			throw new UnsupportedOperationException();
		}

		if (operationJobs == null || operationJobs.isEmpty()) return null;
		Job job = operationJobs.last();
		
		
		// Read the log file
		
		OperationLogReader reader = new OperationLogReader(job.getLogFile());
		
		Vector<Double> y = new Vector<Double>();
		Vector<Double> x = new Vector<Double>();

		for (OperationLogEntry e : reader) {
			
			
			// Filter by tags (if specified)

			if (tags == null || tags.length == 0) {
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
			
			
			// Filter by the result values
			
			String[] result = e.getResult().split(":");
			if (resultFilter != null && resultFilter.length > 0) {
				boolean ok = true;
				for (int i = 0; i < resultFilter.length; i++) {
					if (resultFilter[i] != null) {
						if (result.length <= i) {
							ok = false;
							break;
						}
						if (Integer.parseInt(result[i]) != resultFilter[i].intValue()) {
							ok = false;
							break;
						}
					}
				}
				if (!ok) continue;
			}
			
			
			// Get the data points

			int    xv = Integer.parseInt(result[xArg]);
			double yv = e.getTime() / 1000000.0;

			x.add((double) xv);
			y.add(yv);
 		}

		if (x.size() <= 0) return null;
		
		
		// Drop extreme 5% values
		
		double[] ax = MathUtils.downgradeArray(x.toArray(new Double[0]));
		double[] ay = MathUtils.downgradeArray(y.toArray(new Double[0]));
		double  min = MathUtils.quantile(ay, 0.05);
		double  max = MathUtils.quantile(ay, 0.95);
        
		Pair<double[], double[]> filtered = MathUtils.filter(ay, ax, min, max);
		ay = filtered.getFirst();
		ax = filtered.getSecond();
		
		
		// Find the bounds and store them for later
		
		String key = operationName;
		if (tags != null) for (String t : tags) key += "-" + t;
		if (resultFilter != null) for (Integer n : resultFilter) key += ":" + n;
		xBounds.put(key, new Double[] { MathUtils.min(ax), MathUtils.max(ax) });
		
		
		// Finish
		
		return MathUtils.upgradeArray(
				useRobustFits && ax.length >= 50 ? MathUtils.robustLinearFit(ax, ay) : MathUtils.linearFit(ax, ay));
	}
	
	
	/**
	 * Get an average result value
	 * 
	 * @param operationName the operation name
	 * @param tags the tags
	 * @return the average result value, or null if not found
	 */
	private Double getAverageResultValue(String operationName, String[] tags) {
	
		int xArg = -1;
		if (operationName.equals("OperationGetAllNeighbors"                )) xArg = 3;
		if (operationName.equals("OperationGetNeighborEdgeConditional"     )) xArg = 3;
		if (operationName.equals("OperationGetKHopNeighbors"               )) xArg = 3;
		if (operationName.equals("OperationGetKHopNeighborsEdgeConditional")) xArg = 3;
		if (operationName.equals("OperationLocalClusteringCoefficient"     )) xArg = 3;
		if (operationName.equals("OperationGetShortestPath"                )) xArg = 3;
		if (xArg < 0) {
			throw new IllegalArgumentException("Unsupported operation " + operationName);
		}
		
		
		// Find the correct job
		
		SortedSet<Job> operationJobs = null;		
		if (tags.length == 0) {
			operationJobs = context.operationWithTagsToJobs.get(operationName);
		}
		else if (tags.length == 1) {
			operationJobs = context.operationWithTagsToJobs.get(operationName + "-" + tags[0]);
		}
		else {
			throw new UnsupportedOperationException();
		}

		if (operationJobs == null || operationJobs.isEmpty()) return null;
		Job job = operationJobs.last();
		
		
		// Read the log file
		
		OperationLogReader reader = new OperationLogReader(job.getLogFile());
		
		Vector<Double> y = new Vector<Double>();
		Vector<Double> x = new Vector<Double>();

		for (OperationLogEntry e : reader) {
			
			
			// Filter by tags (if specified)

			if (tags == null || tags.length == 0) {
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
			
			
			// Get the result values
			
			String[] result = e.getResult().split(":");
			
			
			// Get the data points

			int    xv = Integer.parseInt(result[xArg]);
			double yv = e.getTime() / 1000000.0;

			x.add((double) xv);
			y.add(yv);
 		}

		if (x.size() <= 0) return null;
		
		
		// Drop extreme 5% values
		
		double[] ax = MathUtils.downgradeArray(x.toArray(new Double[0]));
		double[] ay = MathUtils.downgradeArray(y.toArray(new Double[0]));
		double  min = MathUtils.quantile(ay, 0.05);
		double  max = MathUtils.quantile(ay, 0.95);
        
		Pair<double[], double[]> filtered = MathUtils.filter(ay, ax, min, max);
		ay = filtered.getFirst();
		ax = filtered.getSecond();
		
		
		// Finish
		
		return MathUtils.average(ax);
	}
	
	
	/**
	 * Get the x bounds computed by a call to getOperationRuntimeAsLinearFunction()
	 * 
	 * @param operation the operation type
	 * @param resultFilter the result filter
	 * @return the x bounds, or null if not found
	 */
	private Double[] getXBounds(Class<? extends Operation> operation, Integer... resultFilter) {
		return getXBounds(operation.getSimpleName(), new String[0], resultFilter);
	}
	
	
	/**
	 * Get the x bounds computed by a call to getOperationRuntimeAsLinearFunction()
	 * 
	 * @param operationName the operation name
	 * @param tags the tags
	 * @param resultFilter the result filter
	 * @return the x bounds, or null if not found
	 */
	private Double[] getXBounds(String operationName, String[] tags, Integer[] resultFilter) {
		String key = operationName;
		if (tags != null) for (String t : tags) key += "-" + t;
		if (resultFilter != null) for (Integer n : resultFilter) key += ":" + n;
		return xBounds.get(key);
	}
}
