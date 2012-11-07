package com.tinkerpop.bench.web;

import java.util.Collection;
import java.util.Vector;


/**
 * A collection of properties shared by different implementations of charts
 * 
 * @author Peter Macko (pmacko@eecs.harvard.edu)
 */
public class ChartProperties {
	
	/*
	 * Data source
	 */
	
	public String source = null;
	public String xvalue = "null";
	public String yvalue = "d.time";
	
	
	/*
	 * Data transformation
	 */
	
	public String foreach = "";
	
	
	/*
	 * Data filters
	 */
	
	public String filter = "true";
	public boolean dropTopBottomExtremes = false;
	
	
	/*
	 * HTML hooks
	 */
	
	public String attach = null;
	
	
	/*
	 * Chart appearance 
	 */
	
	public String xscale = "linear";
	public String xlabel = "";
	public boolean xautounit = true;
	
	public String yscale = "linear";
	public String ylabel = "";
	
	public boolean smallGraph = false;
	public boolean stacked = false;
	public boolean patternFill = false;
	
	
	/*
	 * Simple series
	 */
	
	public String series_column = null;
	public String series_label_function = null;
	
	
	/*
	 * Group by
	 */
	
	public String group_by = null;
	public String group_label_function = null;
	
	public String subgroup_by = null;
	public String subgroup_label_function = null;
	
	public String category_label_function = null;
	
	
	/*
	 * Linear fits and models
	 */
	
	public boolean linear_fits = false;
	public Vector<Collection<LinearFunction>> predictions = new Vector<Collection<LinearFunction>>();
	
	
	/*
	 * A linear function
	 */
	public static class LinearFunction {
		
		public Double[] coefficients;
		public Double xmin;
		public Double xmax;
		
		public LinearFunction(Double[] coefficients, Double xmin, Double xmax) {
			this.coefficients = coefficients;
			this.xmin = xmin;
			this.xmax = xmax;
		}
		
		public LinearFunction(Double[] coefficients) {
			this.coefficients = coefficients;
			this.xmin = null;
			this.xmax = null;
		}
	}
}
