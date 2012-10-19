package com.tinkerpop.bench;

/**
 * @author Peter Macko <pmacko@eecs.harvard.edu>
 */
public class GlobalConfig {

	/// The size of the database cache size in MB
	public static int databaseCacheSize = 1024;

	/// The fraction of the database cache size that should be used as the buffer pool,
	/// if the database engine requires us to make this decision
	public static double preferredBufferPoolRatio = 0.75;

	/// The size of the transaction buffer
	public static int transactionBufferSize = 100 * 1000;
	
	/// Whether to open one database connection per thread
	public static boolean oneDbConnectionPerThread = true;
	
	/// Whether databases should use stored procedures if they are available
	public static boolean useStoredProcedures = false;
	
	/// Whether we should use specialized non-Blueprints routines if they are available
	public static boolean useSpecializedProcedures = true;

	/// Whether to pollute cache before a new benchmark run
	public static boolean polluteCache = true;
}
