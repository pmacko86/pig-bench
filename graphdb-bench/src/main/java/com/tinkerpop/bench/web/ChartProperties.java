package com.tinkerpop.bench.web;

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
	public String category_label_function = null;
	
	
	/*
	 * Linear fits
	 */
	
	public Vector<Double[]> linear_fits = new Vector<Double[]>();
	public Vector<Double[]> predictions = new Vector<Double[]>();
}
