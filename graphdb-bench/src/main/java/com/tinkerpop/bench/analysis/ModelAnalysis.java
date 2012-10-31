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
		
		Ko = getOperationRuntimeAsLinearFunction(OperationGetKHopNeighbors.class, "out-1", "out-2", "out-3", "out-4", "out-5");
		Ki = getOperationRuntimeAsLinearFunction(OperationGetKHopNeighbors.class, "in-1", "in-2", "in-3", "in-4", "in-5");
		Kb = getOperationRuntimeAsLinearFunction(OperationGetKHopNeighbors.class, "both-1", "both-2", "both-3", "both-4", "both-5");
		
		Ko_prediction = No;
		Ki_prediction = Ni;
		Kb_prediction = Nb;
		
		Klo = getOperationRuntimeAsLinearFunction(OperationGetKHopNeighbors.class, "out-withlabel-1", "out-withlabel-2", "out-withlabel-3", "out-withlabel-4", "out-withlabel-5");
		Kli = getOperationRuntimeAsLinearFunction(OperationGetKHopNeighbors.class, "in-withlabel-1", "in-withlabel-2", "in-withlabel-3", "in-withlabel-4", "in-withlabel-5");
		Klb = getOperationRuntimeAsLinearFunction(OperationGetKHopNeighbors.class, "both-withlabel-1", "both-withlabel-2", "both-withlabel-3", "both-withlabel-4", "both-withlabel-5");
		
		Klo_prediction = Nlo;
		Kli_prediction = Nli;
		Klb_prediction = Nlb;
		
		Kpo = getOperationRuntimeAsLinearFunction(OperationGetKHopNeighborsEdgeConditional.class, "out-1", "out-2", "out-3", "out-4", "out-5");
		Kpi = getOperationRuntimeAsLinearFunction(OperationGetKHopNeighborsEdgeConditional.class, "in-1", "in-2", "in-3", "in-4", "in-5");
		Kpb = getOperationRuntimeAsLinearFunction(OperationGetKHopNeighborsEdgeConditional.class, "both-1", "both-2", "both-3", "both-4", "both-5");
		
		Kpo_prediction = Tpo;
		Kpi_prediction = Tpi;
		Kpb_prediction = Tpb;
		
		
		/*
		 * Finish
		 */
		
		return true;
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
		
		if (value instanceof Double && predicted instanceof Double) {
			ConsoleUtils.printColumns(cw, variable + " = " + OutputUtils.format((Double) value),
					variable + "_prediction = " + model + " = " + OutputUtils.format((Double) predicted));
			return;
		}
		
		if (value instanceof Double[] && predicted instanceof Double[]) {
			ConsoleUtils.printColumns(cw, variable + " = " + OutputUtils.formatLinearCombination((Double[]) value, "n"),
					variable + "_prediction = " + model + " = " + OutputUtils.formatLinearCombination((Double[]) predicted, "n"));
			return;
		}
		
		throw new IllegalArgumentException("Illegal types for the observed value and the predicted value");
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
		
		print(CW, "To", "Re + Rv", To, To_prediction);
		print(CW, "Ti", "Re + Rv", Ti, Ti_prediction);
		print(CW, "Tb", "Re + Rv", Tb, Tb_prediction);
		System.out.println();
		
		print(CW, "Tlo", "Re + Rv", Tlo, Tlo_prediction);
		print(CW, "Tli", "Re + Rv", Tli, Tli_prediction);
		print(CW, "Tlb", "Re + Rv", Tlb, Tlb_prediction);
		System.out.println();
		
		print(CW, "Tpo", "Rp + (Re+Rv)*n", Tpo, Tpo_prediction);
		print(CW, "Tpi", "Rp + (Re+Rv)*n", Tpi, Tpi_prediction);
		print(CW, "Tpb", "Rp + (Re+Rv)*n", Tpb, Tpb_prediction);
		System.out.println();
		
		System.out.println();
		ConsoleUtils.header("Graph Operations");
		System.out.println();
		
		print(CW, "No", "0 + To*n", No, No_prediction);
		print(CW, "Ni", "0 + Ti*n", Ni, Ni_prediction);
		print(CW, "Nb", "0 + Tb*n", Nb, Nb_prediction);
		System.out.println();
		
		print(CW, "Nlo", "0 + Tlo*n", Nlo, Nlo_prediction);
		print(CW, "Nli", "0 + Tli*n", Nli, Nli_prediction);
		print(CW, "Nlb", "0 + Tlb*n", Nlb, Nlb_prediction);
		System.out.println();
		
		print(CW, "Ko", "No", Ko, Ko_prediction);
		print(CW, "Ki", "Ni", Ki, Ki_prediction);
		print(CW, "Kb", "Nb", Kb, Kb_prediction);
		System.out.println();
		
		print(CW, "Klo", "Nlo", Klo, Klo_prediction);
		print(CW, "Kli", "Nli", Kli, Kli_prediction);
		print(CW, "Klb", "Nlb", Klb, Klb_prediction);
		System.out.println();
		
		print(CW, "Kpo", "Tpo", Kpo, Kpo_prediction);
		print(CW, "Kpi", "Tpi", Kpi, Kpi_prediction);
		print(CW, "Kpb", "Tpb", Kpb, Kpb_prediction);
		System.out.println();
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
		
		if (count <= 0) throw new InternalError("Could not find any log entries for " + operationName);
		
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
