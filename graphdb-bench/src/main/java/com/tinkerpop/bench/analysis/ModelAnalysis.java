package com.tinkerpop.bench.analysis;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

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


/**
 * A context for database introspection 
 * 
 * @author Peter Macko (pmacko@eecs.harvard.edu)
 */
public class ModelAnalysis {

	/// The database and instance pair
	private DatabaseEngineAndInstance dbEI;
	
	/// The number of all finished jobs (for cache purposes)
	private int numFinishedJobs;
	
	/// The list of relevant successfully finished jobs
	private List<Job> finishedJobs;
	
	/// The map of operations to finished jobs
	private HashMap<String, SortedSet<Job>> operationToJobs;
	
	/// The primitive operation runtimes -- reads
	private Double Rv /* vertex */, Re /* edge */, Rp /* property */;
	
	/// The primitive operation runtimes -- writes
	private Double Wv /* vertex */, We /* edge */, Wp /* property */;
	
	/// The primitive traversals -- follow the first edge
	private Double To /* out */, Ti /* in */, Tb /* both */;
	private Double To_prediction /* out */, Ti_prediction /* in */, Tb_prediction /* both */;
	
	/// The primitive traversals -- follow the first edge with label
	private Double Tlo /* out */, Tli /* in */, Tlb /* both */;
	private Double Tlo_prediction /* out */, Tli_prediction /* in */, Tlb_prediction /* both */;
	
	/// The primitive traversals -- follow an edge using a property
	private Double[] Tpo /* out */, Tpi /* in */, Tpb /* both */;
	private Double[] Tpo_prediction /* out */, Tpi_prediction /* in */, Tpb_prediction /* both */;
	
	/// Get all neighbors -- follow all first edges
	private Double[] No /* out */, Ni /* in */, Nb /* both */;
	private Double[] No_prediction /* out */, Ni_prediction /* in */, Nb_prediction /* both */;
	
	/// Get all neighbors -- follow all first edges with label
	private Double[] Nlo /* out */, Nli /* in */, Nlb /* both */;
	private Double[] Nlo_prediction /* out */, Nli_prediction /* in */, Nlb_prediction /* both */;
	
	/// Get all neighbors -- follow edges using a property
	//private Double[] No /* out */, Ni /* in */, Nb /* both */;
	//private Double[] No_prediction /* out */, Ni_prediction /* in */, Nb_prediction /* both */;
	
	/// Get all K-hop neighbors -- follow all first edges
	private Double[] Ko /* out */, Ki /* in */, Kb /* both */;
	private Double[] Ko_prediction /* out */, Ki_prediction /* in */, Kb_prediction /* both */;
	
	/// Get all K-hop neighbors -- follow all first edges with label
	private Double[] Klo /* out */, Kli /* in */, Klb /* both */;
	private Double[] Klo_prediction /* out */, Kli_prediction /* in */, Klb_prediction /* both */;
	
	/// Get all K-hop neighbors -- follow edges using a property
	private Double[] Kpo /* out */, Kpi /* in */, Kpb /* both */;
	private Double[] Kpo_prediction /* out */, Kpi_prediction /* in */, Kpb_prediction /* both */;
	
	
	/**
	 * Create an instance of the class
	 * 
	 * @param dbei the database engine and instance
	 */
	public ModelAnalysis(DatabaseEngineAndInstance dbEI) {
		
		this.dbEI = dbEI;
		this.numFinishedJobs = -1;
		
		recompute();
	}
	
	
	/**
	 * Recompute if necessary
	 * 
	 * @return true if it was recomputed
	 */
	public boolean recompute() {
		
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
			
			if (job.getExecutionCount() < 0 || job.getLastStatus() != 0 || job.isRunning()) continue;
			File summaryFile = job.getSummaryFile();
			if (summaryFile == null) continue;
			if (job.getExecutionTime() == null) continue;
			
			
			// Jobs
			
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
		
		To = getAverageOperationRuntime(OperationGetFirstNeighbor.class, "out");
		Ti = getAverageOperationRuntime(OperationGetFirstNeighbor.class, "in");
		Tb = getAverageOperationRuntime(OperationGetFirstNeighbor.class, "both");
		
		To_prediction = MathUtils.sum(Re, Rv);
		Ti_prediction = MathUtils.sum(Re, Rv);
		Tb_prediction = MathUtils.sum(Re, Rv);
		
		Tlo = getAverageOperationRuntime(OperationGetFirstNeighbor.class, "out-withlabel");
		Tli = getAverageOperationRuntime(OperationGetFirstNeighbor.class, "in-withlabel");
		Tlb = getAverageOperationRuntime(OperationGetFirstNeighbor.class, "both-withlabel");
		
		Tlo_prediction = MathUtils.sum(Re, Rv);
		Tli_prediction = MathUtils.sum(Re, Rv);
		Tlb_prediction = MathUtils.sum(Re, Rv);
		
		Tpo = getOperationRuntimeAsLinearFunction(OperationGetNeighborEdgeConditional.class, "out");
		Tpi = getOperationRuntimeAsLinearFunction(OperationGetNeighborEdgeConditional.class, "in");
		Tpb = getOperationRuntimeAsLinearFunction(OperationGetNeighborEdgeConditional.class, "both");
		
		Tpo_prediction = MathUtils.ifNeitherIsNull(MathUtils.sum(Re, Rv), Rp);
		Tpi_prediction = MathUtils.ifNeitherIsNull(MathUtils.sum(Re, Rv), Rp);
		Tpb_prediction = MathUtils.ifNeitherIsNull(MathUtils.sum(Re, Rv), Rp);
		
		
		/*
		 * Pull out the get all neighbors operations
		 */
		
		No = getOperationRuntimeAsLinearFunction(OperationGetAllNeighbors.class, "out");
		Ni = getOperationRuntimeAsLinearFunction(OperationGetAllNeighbors.class, "in");
		Nb = getOperationRuntimeAsLinearFunction(OperationGetAllNeighbors.class, "both");
		
		No_prediction = MathUtils.ifNeitherIsNull(new Double(0), To);
		Ni_prediction = MathUtils.ifNeitherIsNull(new Double(0), Ti);
		Nb_prediction = MathUtils.ifNeitherIsNull(new Double(0), Tb);
		
		Nlo = getOperationRuntimeAsLinearFunction(OperationGetAllNeighbors.class, "out-withlabel");
		Nli = getOperationRuntimeAsLinearFunction(OperationGetAllNeighbors.class, "in-withlabel");
		Nlb = getOperationRuntimeAsLinearFunction(OperationGetAllNeighbors.class, "both-withlabel");
		
		Nlo_prediction = MathUtils.ifNeitherIsNull(new Double(0), Tlo);
		Nli_prediction = MathUtils.ifNeitherIsNull(new Double(0), Tli);
		Nlb_prediction = MathUtils.ifNeitherIsNull(new Double(0), Tlb);
		
		
		/*
		 * Pull out the get all K-hop neighbors operations
		 */
		
		Ko = getOperationRuntimeAsLinearFunction(OperationGetKHopNeighbors.class, "out-3");
		Ki = getOperationRuntimeAsLinearFunction(OperationGetKHopNeighbors.class, "in-3");
		Kb = getOperationRuntimeAsLinearFunction(OperationGetKHopNeighbors.class, "both-3");
		
		Ko_prediction = MathUtils.ifNeitherIsNull(new Double(0), To);
		Ki_prediction = MathUtils.ifNeitherIsNull(new Double(0), Ti);
		Kb_prediction = MathUtils.ifNeitherIsNull(new Double(0), Tb);
		
		Klo = getOperationRuntimeAsLinearFunction(OperationGetKHopNeighbors.class, "out-3");
		Kli = getOperationRuntimeAsLinearFunction(OperationGetKHopNeighbors.class, "in-3");
		Klb = getOperationRuntimeAsLinearFunction(OperationGetKHopNeighbors.class, "both-3");
		
		Klo_prediction = MathUtils.ifNeitherIsNull(new Double(0), Tlo);
		Kli_prediction = MathUtils.ifNeitherIsNull(new Double(0), Tli);
		Klb_prediction = MathUtils.ifNeitherIsNull(new Double(0), Tlb);
		
		Kpo = getOperationRuntimeAsLinearFunction(OperationGetKHopNeighborsEdgeConditional.class, "out-3");
		Kpi = getOperationRuntimeAsLinearFunction(OperationGetKHopNeighborsEdgeConditional.class, "in-3");
		Kpb = getOperationRuntimeAsLinearFunction(OperationGetKHopNeighborsEdgeConditional.class, "both-3");
		
		Kpo_prediction = Tpo;
		Kpi_prediction = Tpi;
		Kpb_prediction = Tpb;
		
		
		/*
		 * Finish
		 */
		
		return true;
	}
	
	
	/**
	 * Print the analysis
	 */
	public void printAnalysis() {
		
		int CW = 35;
		
		
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
		
		ConsoleUtils.printColumns(CW, "To = " + OutputUtils.format(To), "To_prediction = " + OutputUtils.format(To_prediction));
		ConsoleUtils.printColumns(CW, "Ti = " + OutputUtils.format(Ti), "Ti_prediction = " + OutputUtils.format(Ti_prediction));
		ConsoleUtils.printColumns(CW, "Tb = " + OutputUtils.format(Tb), "Tb_prediction = " + OutputUtils.format(Tb_prediction));
		System.out.println();
		
		ConsoleUtils.printColumns(CW, "Tlo = " + OutputUtils.format(Tlo), "Tlo_prediction = " + OutputUtils.format(Tlo_prediction));
		ConsoleUtils.printColumns(CW, "Tli = " + OutputUtils.format(Tli), "Tli_prediction = " + OutputUtils.format(Tli_prediction));
		ConsoleUtils.printColumns(CW, "Tlb = " + OutputUtils.format(Tlb), "Tlb_prediction = " + OutputUtils.format(Tlb_prediction));
		System.out.println();
		
		ConsoleUtils.printColumns(CW, "Tpo = " + OutputUtils.formatLinearCombination(Tpo, "n"), "Tpo_prediction = " + OutputUtils.formatLinearCombination(Tpo_prediction, "n"));
		ConsoleUtils.printColumns(CW, "Tpi = " + OutputUtils.formatLinearCombination(Tpi, "n"), "Tpi_prediction = " + OutputUtils.formatLinearCombination(Tpi_prediction, "n"));
		ConsoleUtils.printColumns(CW, "Tpb = " + OutputUtils.formatLinearCombination(Tpb, "n"), "Tpb_prediction = " + OutputUtils.formatLinearCombination(Tpb_prediction, "n"));
		System.out.println();
		
		System.out.println();
		ConsoleUtils.header("Graph Operations");
		System.out.println();
		
		ConsoleUtils.printColumns(CW, "No = " + OutputUtils.formatLinearCombination(No, "n"), "No_prediction = " + OutputUtils.formatLinearCombination(No_prediction, "n"));
		ConsoleUtils.printColumns(CW, "Ni = " + OutputUtils.formatLinearCombination(Ni, "n"), "Ni_prediction = " + OutputUtils.formatLinearCombination(Ni_prediction, "n"));
		ConsoleUtils.printColumns(CW, "Nb = " + OutputUtils.formatLinearCombination(Nb, "n"), "Nb_prediction = " + OutputUtils.formatLinearCombination(Nb_prediction, "n"));
		System.out.println();
		
		ConsoleUtils.printColumns(CW, "Nlo = " + OutputUtils.formatLinearCombination(Nlo, "n"), "Nlo_prediction = " + OutputUtils.formatLinearCombination(Nlo_prediction, "n"));
		ConsoleUtils.printColumns(CW, "Nli = " + OutputUtils.formatLinearCombination(Nli, "n"), "Nli_prediction = " + OutputUtils.formatLinearCombination(Nli_prediction, "n"));
		ConsoleUtils.printColumns(CW, "Nlb = " + OutputUtils.formatLinearCombination(Nlb, "n"), "Nlb_prediction = " + OutputUtils.formatLinearCombination(Nlb_prediction, "n"));
		System.out.println();
		
		ConsoleUtils.printColumns(CW, "Ko = " + OutputUtils.formatLinearCombination(Ko, "n"), "Ko_prediction = " + OutputUtils.formatLinearCombination(Ko_prediction, "n"));
		ConsoleUtils.printColumns(CW, "Ki = " + OutputUtils.formatLinearCombination(Ki, "n"), "Ki_prediction = " + OutputUtils.formatLinearCombination(Ki_prediction, "n"));
		ConsoleUtils.printColumns(CW, "Kb = " + OutputUtils.formatLinearCombination(Kb, "n"), "Kb_prediction = " + OutputUtils.formatLinearCombination(Kb_prediction, "n"));
		System.out.println();
		
		ConsoleUtils.printColumns(CW, "Klo = " + OutputUtils.formatLinearCombination(Klo, "n"), "Klo_prediction = " + OutputUtils.formatLinearCombination(Klo_prediction, "n"));
		ConsoleUtils.printColumns(CW, "Kli = " + OutputUtils.formatLinearCombination(Kli, "n"), "Kli_prediction = " + OutputUtils.formatLinearCombination(Kli_prediction, "n"));
		ConsoleUtils.printColumns(CW, "Klb = " + OutputUtils.formatLinearCombination(Klb, "n"), "Klb_prediction = " + OutputUtils.formatLinearCombination(Klb_prediction, "n"));
		System.out.println();
		
		ConsoleUtils.printColumns(CW, "Kpo = " + OutputUtils.formatLinearCombination(Kpo, "n"), "Kpo_prediction = " + OutputUtils.formatLinearCombination(Kpo_prediction, "n"));
		ConsoleUtils.printColumns(CW, "Kpi = " + OutputUtils.formatLinearCombination(Kpi, "n"), "Kpi_prediction = " + OutputUtils.formatLinearCombination(Kpi_prediction, "n"));
		ConsoleUtils.printColumns(CW, "Kpb = " + OutputUtils.formatLinearCombination(Kpb, "n"), "Kpb_prediction = " + OutputUtils.formatLinearCombination(Kpb_prediction, "n"));
		System.out.println();
	}
	
	
	/**
	 * Get a runtime of an operation from the latest job, taking a simple average
	 * if there is more than one within the job. If the operation is of the "Many"
	 * type, then return the average runtime of the simpler operation that gets
	 * repeated.
	 * 
	 * @param operation the operation type
	 * @return the runtime in ms, or null if not found
	 */
	private Double getAverageOperationRuntime(Class<? extends Operation> operation) {
		return getAverageOperationRuntime(operation.getSimpleName(), null);
	}
	
	
	/**
	 * Get a runtime of an operation from the latest job, taking a simple average
	 * if there is more than one within the job. If the operation is of the "Many"
	 * type, then return the average runtime of the simpler operation that gets
	 * repeated.
	 * 
	 * @param operation the operation type
	 * @param tag the tag
	 * @return the runtime in ms, or null if not found
	 */
	private Double getAverageOperationRuntime(Class<? extends Operation> operation, String tag) {
		return getAverageOperationRuntime(operation.getSimpleName(), tag);
	}
	
	
	/**
	 * Get a runtime of an operation from the latest job, taking a simple average
	 * if there is more than one within the job. If the operation is of the "Many"
	 * type, then return the average runtime of the simpler operation that gets
	 * repeated.
	 * 
	 * @param operationName the operation name
	 * @param tag the tag
	 * @return the runtime in ms, or null if not found
	 */
	private Double getAverageOperationRuntime(String operationName, String tag) {
		
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
		
		String operationNameFilter = tag == null ? operationName : operationName + "-" + tag; 
		OperationLogReader reader = tag == null
				? new OperationLogReader(job.getLogFile())
				: new OperationLogReader(job.getLogFile(), operationNameFilter);
		
		double time_ms = 0;
		int count = 0;
		
		for (OperationLogEntry e : reader) {
			
			if (tag == null) {
				if (!e.getName().equals(operationName)
						&& !e.getName().startsWith(operationName + "-")) continue;
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
		
		if (count <= 0) throw new InternalError("Could not find any log entries for " + operationName);
		
		return time_ms / count;
	}
	
	
	/**
	 * Get a runtime of a job as a linear function of one of the operation results
	 * 
	 * @param operation the operation type
	 * @param tag the tag
	 * @return the runtime in ms as a function described by [intercept, slope], or null if not found
	 */
	private Double[] getOperationRuntimeAsLinearFunction(Class<? extends Operation> operation, String tag) {
		return getOperationRuntimeAsLinearFunction(operation.getSimpleName(), tag);
	}
	
	
	/**
	 * Get a runtime of a job as a linear function of one of the operation results
	 * 
	 * @param operationName the operation name
	 * @param tag the tag
	 * @return the runtime in ms as a function described by [intercept, slope], or null if not found
	 */
	private Double[] getOperationRuntimeAsLinearFunction(String operationName, String tag) {
		
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
		
		String operationNameFilter = tag == null ? operationName : operationName + "-" + tag; 
		OperationLogReader reader = tag == null
				? new OperationLogReader(job.getLogFile())
				: new OperationLogReader(job.getLogFile(), operationNameFilter);
		
		Vector<Double> y = new Vector<Double>();
		Vector<Double> x = new Vector<Double>();
		@SuppressWarnings("unused")
		double sumx = 0, sumy = 0, sumx2 = 0; int n = 0;

		for (OperationLogEntry e : reader) {

			if (tag == null) {
				if (!e.getName().equals(operationName)
						&& !e.getName().startsWith(operationName + "-")) continue;
			}

			double xv = Double.parseDouble(e.getResult().split(":")[xArg]);
			double yv = e.getTime() / 1000000.0;

			sumx  += xv;
			sumx2 += xv * xv;
			sumy  += yv;
			n++;

			x.add(xv);
			y.add(yv);
 		}

		if (n <= 0) throw new InternalError("Could not find any log entries for " + operationName);

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
		
		if (xxbar == 0) {
			// Might need to revisit this -- it might be beneficial to keep the
			// slope, even if there is only one unique value of x
			beta1 = 0;
			beta0 = getAverageOperationRuntime(operationName, tag);
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
