package com.tinkerpop.bench.analysis;


/**
 * A prediction for an operation
 * 
 * @author Peter Macko (pmacko@eecs.harvard.edu)
 */
public class Prediction {

	private OperationModel model;
	private String tag;
	private String description;
	
	private double averageRuntime;
	
	
	/**
	 * Create an instance of class Prediction
	 * 
	 * @param model the operation model
	 * @param tag the operation tag(s)
	 * @param description the model description (use if multiple predictions exist for the same operation)
	 * @param averageRuntime the predicted average runtime
	 */
	public Prediction(OperationModel model, String tag, String description, double averageRuntime) {
		this.model = model;
		this.tag = tag;
		this.description = description;
		this.averageRuntime = averageRuntime;
	}
	
	
	/**
	 * Get the operation model
	 * 
	 * @return the model
	 */
	public final OperationModel getModel() {
		return model;
	}
	
	
	/**
	 * Get the operation tag(s)
	 * 
	 * @return the operation tag(s)
	 */
	public final String getTag() {
		return tag;
	}
	
	
	/**
	 * Get the prediction description
	 * 
	 * @return the description, or null if not available
	 */
	public final String getDescription() {
		return description;
	}
	
	
	/**
	 * Get the predicted average runtime
	 * 
	 * @return the predicted average runtime in ms
	 */
	public final double getPredictedAverageRuntime() {
		return averageRuntime;
	}
}
