package com.tinkerpop.bench.analysis;

import java.lang.reflect.Constructor;
import java.util.List;

import com.tinkerpop.bench.operation.Operation;


/**
 * An abstract model for a single operation
 * 
 * @author Peter Macko (pmacko@eecs.harvard.edu)
 */
public abstract class OperationModel {
	
	private static final String[] OPERATION_PACKAGES = {
		null,
		"com.tinkerpop.bench.operation",
		"com.tinkerpop.bench.operation.operations"
	};
	
	private AnalysisContext context;
	private Class<? extends Operation> operationClass;

	
	/**
	 * Create an instance of class OperationModel
	 * 
	 * @param context the analysis context
	 * @param operationClass the operation class
	 */
	public OperationModel(AnalysisContext context, Class<? extends Operation> operationClass) {
		this.context = context;
		this.operationClass = operationClass;
	}
	
	
	/**
	 * Get the analysis context
	 * 
	 * @return the analysis context
	 */
	public final AnalysisContext getContext() {
		return context;
	}
	
	
	/**
	 * Get the operation class
	 * 
	 * @return the operation class
	 */
	public final Class<? extends Operation> getOperationClass() {
		return operationClass;
	}
	
	
	/**
	 * Create prediction(s) based on the specified operation tags
	 * in the specified context
	 * 
	 * @param tag the operation tag(s)
	 * @return a collection of predictions
	 */
	public abstract List<Prediction> predictFromTag(String tag);
	
	
	/**
	 * Create prediction(s) based on the specified operation name
	 * in the specified context
	 * 
	 * @param operationName the operation name
	 * @return a collection of predictions
	 */
	public List<Prediction> predictFromName(String operationName) {
		int k = operationName.indexOf('-');
		return predictFromTag(k < 0 ? "" : operationName.substring(k + 1));
	}
	
	
	/**
	 * Get the model for the given operation type
	 * 
	 * @param context the analysis context
	 * @param operationClass the operation class
	 * @return the model, or null if unavailable
	 */
	public static OperationModel getModelFor(AnalysisContext context, Class<? extends Operation> operationClass) {
		
		Class<?>[] subclasses = operationClass.getDeclaredClasses();
		for (Class<?> c : subclasses) {
			if (c.getSimpleName().equals("Model") && OperationModel.class.isAssignableFrom(c)) {
				try {
					Constructor<?> constructor = c.getConstructor(AnalysisContext.class);
					return (OperationModel) constructor.newInstance(context);
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}
		
		return null;
	}
	
	
	/**
	 * Get the model for the given operation type
	 * 
	 * @param context the analysis context
	 * @param operationName the operation name (the tag will be ignored if present)
	 * @return the model, or null if unavailable
	 */
	@SuppressWarnings("unchecked")
	public static OperationModel getModelFor(AnalysisContext context, String operationName) {
		
		if (operationName.contains("-")) {
			operationName = operationName.substring(0, operationName.indexOf('-'));
		}
		
		for (String p : OPERATION_PACKAGES) {
			try {
				Class<?> operationClass = Class.forName(p == null ? operationName : p + "." + operationName);
				if (Operation.class.isAssignableFrom(operationClass)) {
					return getModelFor(context, (Class<? extends Operation>) operationClass);
				}
			} catch (ClassNotFoundException e) {
				continue;
			}
		}
		
		return null;
	}
}
