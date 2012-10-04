package com.tinkerpop.bench;

/**
 * @author Peter Macko <pmacko@eecs.harvard.edu>
 */
public class GlobalConfig {

	/// The size of the database buffer pool in MB
	public static int databaseBufferPoolSize = 256;

	/// The size of the transaction buffer
	public static int transactionBufferSize = 100 * 1000;
	
	/// Whether to open one database connection per thread
	public static boolean oneDbConnectionPerThread = true;
	
	/// Whether databases should use stored procedures if they are available
	public static boolean useStoredProcedures = false;
	
	/// Whether we should use specialized non-Blueprints routines if they are availavle
	public static boolean useSpecializedProcedures = false;

	/// Whether to pollute cache before a new benchmark run
	public static boolean polluteCache = true;
}
