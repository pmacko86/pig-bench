package com.tinkerpop.bench;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import com.tinkerpop.bench.benchmark.Benchmark;
import com.tinkerpop.bench.log.OperationLogWriter;
import com.tinkerpop.bench.operation.Operation;
import com.tinkerpop.bench.operation.OperationDeleteGraph;
import com.tinkerpop.bench.operation.OperationDoGC;
import com.tinkerpop.bench.operation.OperationOpenGraph;
import com.tinkerpop.bench.operation.OperationShutdownGraph;
import com.tinkerpop.bench.operationFactory.OperationFactory;
import com.tinkerpop.bench.operationFactory.OperationFactoryGeneric;
import com.tinkerpop.bench.operationFactory.OperationFactoryLog;
import com.tinkerpop.blueprints.pgm.Edge;
import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.Vertex;

import edu.harvard.pass.cpl.CPL;


/**
 * Benchmark runner
 * 
 * @author Alex Averbuch (alex.averbuch@gmail.com)
 * @author Martin Neumann (m.neumann.1980@gmail.com)
 * @author Peter Macko (pmacko@eecs.harvard.edu)
 */
public class BenchRunner {
	
	/// The bench runner identification string
	public static final String ID_STRING = "BenchRunner";
	
	/// Log writer
	private OperationLogWriter logWriter = null;

	/// Graph descriptor
	private GraphDescriptor graphDescriptor = null;

	/// The benchmark to run
	private Benchmark benchmark = null;
	
	/// The number of threads
	private int numThreads;
	
	/// Shared operation factories
	private OperationFactory openFactory;
	private OperationFactory shutdownFactory;
	private OperationFactory gcFactory;
	
	/// The worker threads
	private Collection<Worker> workers;
	
	/// The operation ID of the last completed operation
	private AtomicInteger operationId;
	
	/// The benchmark semaphore - do not allow more than one benchmark at the time
	private Semaphore benchmarkSemaphore;
	
	/// The barrier for synchronizing tasks
	private CyclicBarrier barrier = null;
	
	/// The exception that terminated the last run (null = okay)
	private Exception lastException = null;
	
	
	/**
	 * Create an instance of BenchRunner
	 * 
	 * @param graphDescriptor the graph descriptor
	 * @param logFile the log file
	 * @param benchmark the benchmark to run
	 * @param threads the number of threads
	 * @throws IOException if the log writer fails
	 */
	public BenchRunner(GraphDescriptor graphDescriptor, File logFile,
			Benchmark benchmark, int numThreads) throws IOException {
		
		this.graphDescriptor = graphDescriptor;
		this.benchmark = benchmark;
		this.numThreads = numThreads;

		logWriter = LogUtils.getOperationLogWriter(logFile);

		openFactory = new OperationFactoryGeneric(OperationOpenGraph.class);
		shutdownFactory = new OperationFactoryGeneric(OperationShutdownGraph.class);
		gcFactory = new OperationFactoryGeneric(OperationDoGC.class);
		
		benchmarkSemaphore = new Semaphore(1);
	}
	
	
	/**
	 * Open the graph
	 * 
	 * @throws Exception 
	 */
	protected void openGraph() throws Exception {
		
		Operation openOperation = openFactory.next();
		openOperation.setLogWriter(logWriter);
		openOperation.initialize(graphDescriptor);
		openOperation.execute();
		logWriter.logOperation(openOperation);
	}
	
	
	/**
	 * Close the graph
	 * 
	 * @throws Exception 
	 */
	protected void closeGraph() throws Exception {
		
		Operation shutdownOperation = shutdownFactory.next();
		shutdownOperation.setLogWriter(logWriter);
		shutdownOperation.initialize(graphDescriptor);
		shutdownOperation.execute();
		logWriter.logOperation(shutdownOperation);

		// Try to Garbage Collect
		Operation gcOperation = gcFactory.next();
		gcOperation.setLogWriter(logWriter);
		gcOperation.initialize(graphDescriptor);
		gcOperation.execute();
		logWriter.logOperation(gcOperation);
	}


	/**
	 * Run the benchmark
	 * 
	 * @throws Exception
	 */
	public void runBenchmark() throws Exception {
		
		if (!benchmarkSemaphore.tryAcquire()) {
			throw new IllegalStateException("Cannot execute the same benchmark "
					+ "multiple times at the same time.");
		}
		
		operationId = new AtomicInteger(-1);
		lastException = null;
		
		try {
			
			openFactory.initialize(graphDescriptor, operationId);
			shutdownFactory.initialize(graphDescriptor, operationId);
			gcFactory.initialize(graphDescriptor, operationId);
			
			
			// Open the graph
			
			if (!GlobalConfig.oneDbConnectionPerThread) openGraph();
			
			
			// Initialize the barrier concurrency primitive
			
			barrier = new CyclicBarrier(numThreads, new ResetTask());
			
			
			// Create and start the worker threads
			
			workers = new Vector<Worker>(numThreads);
			int numFactories = 0;
			
			for (int i = 0; i < numThreads; i++) {
				
				Collection<OperationFactory> factories = benchmark.createOperationFactories();
				if (i == 0) {
					numFactories = factories.size();
				}
				else {
					if (numFactories != factories.size()) {
						throw new IllegalStateException("Different worker threads have "
								+ "a different number of operation factories");
					}
				}
				
				Worker w = new Worker(i, factories);
				w.start();
				workers.add(w);
			}
			
			
			// Wait for the threads to complete
			
			for (Worker w : workers) {
				w.join();
			}
			
			
			// Finalize
			
			if (!GlobalConfig.oneDbConnectionPerThread) closeGraph();
			graphDescriptor.shutdownGraph();
			logWriter.close();
		}
		finally {
			benchmarkSemaphore.release();
		}
		
		if (lastException != null) throw lastException;
	}

	
	/**
	 * The benchmark thread
	 */
	private class Worker extends Thread {
		
		/// The worker ID
		private int id;
		
		/// The operation factories
		private Collection<OperationFactory> operationFactories = null;
		
		/// The instantiated operation factories
		private List<InstantiatedOperationFactory> instantiatedOperationFactories = null;
		
		/// The current factory
		private OperationFactory currentFactory = null;
		
		/// Whether the worker is in the initialization stage
		private boolean initializing = true;
		
		
		/**
		 * Create an instance of class Worker
		 * 
		 * @param id the worker id
		 * @param operationFactories the operation factories
		 */
		public Worker(int id, Collection<OperationFactory> operationFactories) {
			this.id = id;
			this.operationFactories = operationFactories;
			this.instantiatedOperationFactories = new ArrayList<BenchRunner.InstantiatedOperationFactory>();
		}
		
		
		/**
		 * Get the worker ID
		 * 
		 * @return the worker ID
		 */
		public int getWorkerID() {
			return id;
		}
		
		
		/**
		 * Get the current operation factory
		 * 
		 * @return the current operation factory
		 */
		public OperationFactory getCurrentOperationFactory() {
			return currentFactory;
		}
		
		
		/**
		 * Return whether the worker is initializing the operations
		 * 
		 * @return true if the worker is initializing the operations
		 */
		public boolean isInitalizingOperations() {
			return initializing;
		}


		/**
		 * Run the worker
		 */
		@Override
		public void run() {
			boolean main = id == 0;
			
			try {
				
				// Open the graph
				
				if (GlobalConfig.oneDbConnectionPerThread) openGraph();
				

				// Instantiate operation factories
				
				int longestFactoryName = 0;
				@SuppressWarnings("unused") int totalOperations = 0;
				for (OperationFactory operationFactory : operationFactories) {
					InstantiatedOperationFactory f = new InstantiatedOperationFactory(operationFactory);
					instantiatedOperationFactories.add(f);
					longestFactoryName = Math.max(longestFactoryName, f.getFactory().getClass().getSimpleName().length());
				}
				int longestFactoryStrID = ("" + instantiatedOperationFactories.size()).length();
				
				
				// Run
				
				for (int factory_i = 0; factory_i < instantiatedOperationFactories.size(); factory_i++) {
					InstantiatedOperationFactory operationFactory = instantiatedOperationFactories.get(factory_i);
					currentFactory = operationFactory.getFactory();
					boolean hasUpdates = operationFactory.isUpdate();
					String factoryName = currentFactory.getClass().getSimpleName();
					
					
					// Initialize the operations for this factory and all subsequent factories that
					// do not perform any updates
					
					if (!operationFactory.isInitialized()) {
						initializing = true;
						
						boolean polluteCache = false;
						for (int i = factory_i; i < instantiatedOperationFactories.size(); i++) {
							InstantiatedOperationFactory f = instantiatedOperationFactories.get(i);
							
							Class<?> c = f.getFactory().getClass();
							String name = c.getSimpleName();
							if (main) {
								if (ConsoleUtils.useEscapeSequences) System.out.print("\r");
								System.out.printf("[%-" + longestFactoryName + "s %"+ longestFactoryStrID + "d]"
										+ " Initializing", name, i+1);
								System.out.flush();
							}
							
							f.initialize();
							
							for (Operation op : f.getOperations()) {
								if (!op.getClass().equals(com.tinkerpop.bench.operation.OperationDeleteGraph.class)
										&& !op.getClass().equals(com.tinkerpop.bench.operation.operations.OperationLoadGraphML.class)) {
									polluteCache = true;
								}
							}
							
							if (main) System.out.println();
							if (f.isUpdate()) break;
						}
						
						
						// Barrier + GC
						
						barrier.await();
						
						
						// Pollute cache
						
						if ((main || GlobalConfig.oneDbConnectionPerThread) && polluteCache) {
							if (main) {
								if (ConsoleUtils.useEscapeSequences) System.out.print("\r");
								System.out.printf("[%-" + longestFactoryName + "s %" + longestFactoryStrID + "s] %s",
										ID_STRING,
										"",
										"Polluting cache with a sequential scan");
								System.out.flush();
							}
							
							Graph g = graphDescriptor.getGraph();
							long numVertices = g.countVertices();
							long numEdges = g.countEdges();
							long objects = 0;
							
							for (@SuppressWarnings("unused") Vertex v : g.getVertices()) {
								objects++;
								if (main) if ((objects & 0xfff) == 0) System.gc();
								if (main) ConsoleUtils.printProgressIndicator(objects, numVertices + numEdges);
							}
							
							for (@SuppressWarnings("unused") Edge e : g.getEdges()) {
								objects++;
								if (main) if ((objects & 0xfff) == 0) System.gc();
								if (main) ConsoleUtils.printProgressIndicator(objects, numVertices + numEdges);
							}
							
							if (main) ConsoleUtils.printProgressIndicator(numVertices + numEdges, numVertices + numEdges);
							if (main) System.out.println();
						}
					}
					
					
					// Barrier + GC
					
					initializing = false;
					barrier.await();

					
					// Run the operations

					List<Operation> operations = operationFactory.getOperations();
					String lastOperationName = "";
					for (int operation_i = 0; operation_i < operations.size(); operation_i++) {
						Operation operation = operations.get(operation_i);
						
						if (operation.isUpdate() && !hasUpdates) {
							throw new IllegalStateException("Found an update operation in a factory " +
										"that claims that there are no update operations");
						}

						if (main) {
							if (!operation.getName().equals(lastOperationName)) {
								lastOperationName = operation.getName();
								if (operation_i == 0) {
									if (ConsoleUtils.useEscapeSequences) System.out.print("\r");
									System.out.printf("[%-" + longestFactoryName + "s %" + longestFactoryStrID + "d] " +
											"Running %s%s",
											factoryName,
											factory_i+1,
											operation.getName(),
											operation.isUpdate() ? " (update)" : "");
								}
								else {
									if (ConsoleUtils.useEscapeSequences) System.out.print("\r");
									System.out.printf(" %-" + longestFactoryName + "s %" + longestFactoryStrID + "s  " +
											"Running %s%s",
											" ",
											" ",
											operation.getName(),
											operation.isUpdate() ? " (update)" : "");									
								}
								System.out.flush();
							}
						}
						
						if (CPL.isAttached()) {
							if (!(operation instanceof OperationDeleteGraph)
									&& !(operation instanceof OperationDoGC)) {
								operation.getCPLObject().dataFlowFrom(graphDescriptor.getCPLObject());
							}
						}
						
						
						// Execute the operation
						
						operation.execute();
						
						
						// Finalize the operation

						logWriter.logOperation(operation);
						if (CPL.isAttached()) {
							logWriter.getCPLObject().dataFlowFrom(operation.getCPLObject());
						}
						
						if (main && operations.size() > 1) {
							ConsoleUtils.printProgressIndicator(operation_i + 1, operations.size());
						}
					}
					
					
					// Finalize the operation factory
					
					if (main) System.out.println();
				}
				
				
				// Close the graph
				
				if (GlobalConfig.oneDbConnectionPerThread) closeGraph();
			}
			catch (RuntimeException e) {
				lastException = e;
				throw e;
			}
			catch (Exception e) {
				RuntimeException r = new RuntimeException("Worker " + id + " failed", e);
				lastException = r;
				throw r;
			}
		}
	}
	
	
	/**
	 * The graph reset / cache flush task
	 */
	private class ResetTask implements Runnable {
		
		/**
		 * Create an instance of the class
		 */
		public ResetTask() {
		}
		
		
		/**
		 * Run the task
		 */
		@Override
		public void run() {
			
			// Get the current operation factory
			
			OperationFactory currentFactory = null;
			boolean initalizing = false;
			for (Worker w : workers) {
				if (w.getWorkerID() == 0) {
					currentFactory = w.getCurrentOperationFactory();
					initalizing = w.isInitalizingOperations();
					break;
				}
			}
			if (currentFactory == null) {
				throw new IllegalStateException("Could not find the current "
						+ "operation factory from worker 0");
			}
			
			
			// Check consistency across the different threads
			
			for (Worker w : workers) {
				if (!currentFactory.getClass().equals(w.getCurrentOperationFactory().getClass())) {
					throw new IllegalStateException("Inconsistent operation factories "
							+ "between worker threads 0 and " + w.getWorkerID());
				}
				if (initalizing != w.isInitalizingOperations()) {
					throw new IllegalStateException("Inconsistent worker stages "
							+ "between worker threads 0 and " + w.getWorkerID());
				}
			}
			
			
			// Run the garbage collector
			
			if (!(currentFactory instanceof OperationFactoryLog)) {
				try {
					Operation gcOperation = gcFactory.next();
					gcOperation.setLogWriter(logWriter);
					gcOperation.initialize(graphDescriptor);
					gcOperation.execute();
					logWriter.logOperation(gcOperation);
				}
				catch (RuntimeException e) {
					throw e;
				}
				catch (Exception e) {
					throw new RuntimeException("Garbage collection failed", e);
				}
			}
		}
	}
	
	
	/**
	 * A collection of instantiated operations from a single factory
	 */
	private class InstantiatedOperationFactory {
		
		private OperationFactory factory;
		private ArrayList<Operation> operations;
		private boolean update;
		private boolean initialized;
		
		
		/**
		 * Create an instance of class InstantiatedOperationFactory
		 * 
		 * @param factory the factory
		 */
		public InstantiatedOperationFactory(OperationFactory factory) {
			this.factory = factory;
			this.update = factory.isUpdate();
			this.initialized = false;
			this.operations = new ArrayList<Operation>();
		}
		
		
		/**
		 * Get the factory
		 * 
		 * @return the original factory
		 */
		public OperationFactory getFactory() {
			return factory;
		}
		
		
		/**
		 * Return true if any of the operations can perform an update
		 * 
		 * @return true if any operation can perform an update
		 */
		public boolean isUpdate() {
			return update;
		}
		
		
		/**
		 * Initialize the factory and all operations
		 */
		public void initialize() {
			
			if (initialized) return;
			
			
			// Initialize the operation factory and instantiate the operations

			factory.initialize(graphDescriptor, operationId);
			for (Operation operation : factory) {
				operations.add(operation);
				if (operation.isUpdate() && !update) {
					throw new IllegalStateException("Found an update operation in a factory " +
								"that claims that there are no update operations");
				}
			}
			
			
			// Initialize all operations
			
			for (Operation operation : operations) {
				operation.setLogWriter(logWriter);
				operation.initialize(graphDescriptor);
			}
			initialized = true;
		}
		
		
		/**
		 * Return true if the operations are initialized
		 * 
		 * @return true if the operations are initialized
		 */
		public boolean isInitialized() {
			return initialized;
		}
		
		
		/**
		 * Return a collection of operations
		 * 
		 * @return a collection of operations
		 */
		public List<Operation> getOperations() {
			if (!initialized) throw new IllegalStateException("Not initialized");
			return operations;
		}
	}
}
