package com.tinkerpop.bench.util;

import java.lang.management.CompilationMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;


/**
 * JVM monitoring and compilation utilities
 * 
 * @author Peter Macko (pmacko@gmail.com)
 */
public class JVMUtils {
	
	private static final RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
	private static final CompilationMXBean compilationBean = ManagementFactory.getCompilationMXBean();

	
	/*
	 * About JVM
	 */
	
	/**
	 * The JVM name
	 */
	public static final String JVM_NAME;
	
	/**
	 * The JVM version
	 */
	public static final String JVM_VERSION;

	
	/*
	 * JVM Features
	 */
	
	/**
	 * True if the JVM supports compilation time monitoring (using the compilation bean)
	 */
	public static final boolean haveCompilationTimeMonitoring;
	
	/**
	 * True if Compiler.compileClass() actually works and is not a no-op
	 */
	public static final boolean haveCompileClass;
	
	
	/*
	 * The class static initialization
	 */
	static {
		
		JVM_NAME = runtimeBean.getVmName();
		JVM_VERSION = runtimeBean.getVmVersion();
		
		haveCompilationTimeMonitoring = compilationBean != null && compilationBean.isCompilationTimeMonitoringSupported();
		haveCompileClass = Compiler.compileClass(JVMUtils.class);
	}
}
