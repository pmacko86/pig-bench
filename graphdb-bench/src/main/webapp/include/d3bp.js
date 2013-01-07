/*
 * D3-Based Plots
 * 
 * Author: Peter Macko <http://eecs.harvard.edu/~pmacko>
 */


/*
 * Inheritance mechanism
 * 
 * From: http://www.ruzee.com/blog/2008/12/javascript-inheritance-via-prototypes-and-closures
 */
(function(){
	CClass = function(){};
	CClass.create = function(constructor) {
		var k = this;
		c = function() {
			this._super = k;
			var pubs = constructor.apply(this, arguments), self = this;
			for (key in pubs) (function(fn, sfn) {
				self[key] = typeof fn != "function" || typeof sfn != "function" ? fn :
				function() { this._super = sfn; return fn.apply(this, arguments); };
			})(pubs[key], self[key]);
		}; 
		c.prototype = new this;
		c.prototype.constructor = c;
		c.extend = this.extend || this.create;
		return c;
	};
})();



/**
 * Namespace d3bp
 */
var d3bp = {};



/*****************************************************************************
 * Class d3bp.Data                                                           *
 *****************************************************************************/

d3bp.Data = CClass.create(function(input) {
	
	var __datasource = null;
	
	var __preprocessfunctions = [];
	var __seriesdefinitions = [];
	
	var __originaldata = null;
	var __data = null;
	
	var __seriesroot = null;
	
	
	// Case 1: URL
	if (typeof(input) == "string") {
		__datasource = input;
	}
	
	// Unknown
	else {
		throw "Invalid input type";
	}
	
	
	/**
	 * Run all the preprocess functions
	 */
	var __preprocess = function() {
		
		if (__originaldata == null) {
			throw "The data has not yet been loaded";
		}
		__data = __originaldata.filter(function() { return true; });
		
		
		// Preprocess the data
		
		for (var pi = 0; pi < __preprocessfunctions.length; pi++) {
			var p = __preprocessfunctions[pi];
			
			if (p[0] == "filter") {
				__data = __data.filter(p[1]);
			}
			else if (p[0] == "derive") {
				__data.forEach(function(d, i) {
					d[p[1]] = p[2](d, i);
				});
			}
			else if (p[0] == "foreach") {
				__data.forEach(p[1]);
			}
			else {
				throw "Invalid preprocessing function type: " + p[0];
			}
		}
		
		
		// Categorize the data into the individual data series
		
		var root = new d3bp.Series(null, null, null);
		var root_data = root.data();
		__data.forEach(function(d, i) {
			root_data.push(d);
		});
		__seriesroot = root;
		
		if (__seriesdefinitions.length > 0) {
			
			var leaves = [__seriesroot];
			
			for (var sdi = 0; sdi < __seriesdefinitions.length; sdi++) {
				var sd = __seriesdefinitions[sdi];
				var new_leaves = [];
				
				for (var li = 0; li < leaves.length; li++) {
					var leaf = leaves[li];
					var new_data = [];
					var leaf_data = leaf.data();
					
					for (var di = 0; di < leaf_data.length; di++) {
						var d = leaf_data[di];
						var v = d[sd[0]];
						if (sd[2] != undefined && sd[2] != null) {
							v = sd[2](v);
						}
						
						var found = null;
						for (var i = 0; i < new_data.length; i++) {
							if (new_data[i].value() == v) {
								found = new_data[i];
							}
						}
						if (found == null) {
							found = new d3bp.Series(leaf, v, sd[1]);
							new_data.push(found);
							new_leaves.push(found);
						}
						found.data().push(d);
					}
					
					leaf.data(new_data);
				}
				
				leaves = new_leaves;
			}
		}
	};
	
	
	/*
	 * Public
	 */
	return {
		
		
		/**
		 * Add a filter, but do not run it yet
		 *
		 * @param fn the function, taking a data element and returning true or false
		 */
		filter: function(fn) {
			__preprocessfunctions.push(["filter", fn]);
			return this;
		},
		
		
		/**
		 * Add a function to derive a new value, but do not run it yet
		 *
		 * @param property the new property name
		 * @param fn the function, taking a data element and returning a value
		 */
		derive: function(property, fn) {
			__preprocessfunctions.push(["derive", property, fn]);
			return this;
		},
		
		
		/**
		 * Add a function to run for each element, but do not run it yet
		 *
		 * @param fn the function, taking a data element
		 */
		forEach: function(fn) {
			__preprocessfunctions.push(["foreach", fn]);
			return this;
		},
		
		
		/**
		 * Add a property name, and an optional function to transform it, to serve for
		 * categorizing data elements into series/groups
		 *
		 * @param property the property
		 * @param labelfn the value label function (optional)
		 * @param transformfn the property transform function (optional)
		 */
		groupBy: function(property, labelfn, transformfn) {
			if (labelfn == undefined) {
				labelfn = function(v) { return "" + v; };
			}
			__seriesdefinitions.push([property, labelfn, transformfn]);
			return this;
		},
		
		
		/**
		 * Get the data and then run the specified callback
		 *
		 * @param fn the callback function, receiving this object
		 */
		run: function(fn) {
			var t = this;
			if (__datasource != null && typeof(__datasource) == "string") {
				d3.csv(__datasource, function(data) {
					__originaldata = data;
					__preprocess();
					fn(this);
				});
			}
			else {
				throw "Invalid data source type";
			}
		},
		
		
		/**
		 * Return the data after they have been loaded
		 *
		 * @return the loaded data
		 */
		data: function() {
			
			if (__data == null) {
				throw "The data has not yet been loaded";
			}
			
			return __data;
		},
		
		
		/**
		 * Return the series root after the data has been preprocessed
		 *
		 * @return the series root
		 */
		seriesRoot: function() {
			
			if (__seriesroot == null) {
				throw "The data has not yet been preprocessed";
			}
			
			return __seriesroot;
		}
	};
});



/*****************************************************************************
 * Class d3bp.Series                                                         *
 *****************************************************************************/

d3bp.Series = CClass.create(function(parent, value, valueLabelFn) {
	
	var __parent = parent;				// parent series
	
	var __value = value;				// the common property value
	var __valuelabelfn = valueLabelFn;
	var __label = null;
	
	if (valueLabelFn != undefined && valueLabelFn != null) {
		if (value != undefined && value != null) {
			__label = valueLabelFn(value);
		}
	}
	
	var __data = [];					// data or sub-series
	
	
	/*
	 * Public
	 */
	return {
		
		/**
		 * Return the data
		 * 
		 * @param the new data (optional)
		 * @return the data
		 */
		data: function(newData) {
			if (newData != undefined) {
				__data = newData;
			}
			return __data;
		},
		
		
		/**
		 * Return the common property value
		 * 
		 * @return the common property value
		 */
		value: function() {
			return __value;
		},
		
		
		/**
		 * Return the parent series (of which this series is a sub-series)
		 * 
		 * @return the parent series
		 */
		parent: function() {
			return __parent;
		},
		
		
		/**
		 * Return the series label (a printable version of the common
		 * property value that defines the series)
		 * 
		 * @return the label
		 */
		label: function() {
			return __label;
		}
	};
});



/*****************************************************************************
 * Class d3bp.Appearance                                                     *
 *****************************************************************************/

d3bp.Appearance = CClass.create(function() {
	
	return {
		chart_margin: 10,
		
		bar_width: 20,
		bar_colors: d3.scale.category10(),
		bars_margin: 10,
		
		ylabel_from_data_tick_text: 10,
		
		legend_padding_left: 10,
		legend_padding_top: 10,
		legend_bar_padding_right: 4,
		legend_bar_width: 2 * 20,
		legend_bar_height: 20,
		legend_vertical_spacing: 2,
		
		fill: "solid"
	};
});



/*****************************************************************************
 * Class d3bp.AbstractChart                                                  *
 *****************************************************************************/

d3bp.AbstractChart = CClass.create(function() {
	
	this._super();
	
	return {
	};
});



/*****************************************************************************
 * Class d3bp.BarChart                                                       *
 *****************************************************************************/

d3bp.BarChart = d3bp.AbstractChart.extend(function() {
	
	this._super();
	
	var __data = null;
	var __valuefn = null;			// extract the value (usually y)
	var __stdevfn = null;			// extract the standard deviation
	var __labelfn = null;			// extract the label value (usually x)
	var __categoryfn = null;		// extract the category
	
	var __valuelabelfn = null;		// convert the value to string
	var __categorylabelfn = null;	// convert the category to string
	
	var __appearance = new d3bp.Appearance();
	var __valueaxislabel = null;
	
	var __stacked = false;
	var __scaletype = "linear";
	
	var __d3scale = null;
	
	var __inner_height = 400;
	var __num_ticks = 10;
	
	
	/*
	 * Compute the locations of ticks
	 */
	var __ticks = function() {
		var ticks = __d3scale.ticks(__num_ticks);
		if (__scaletype == "log" && ticks.length > __num_ticks) {
			ticks = []
			
			ticks.push(__d3scale.domain()[1]);
			while (ticks[ticks.length - 1] / 10 >= __d3scale.domain()[0]) {
				ticks.push(ticks[ticks.length - 1] / 10);
			}
		}
		return ticks;
	};
	
	
	/*
	 * Public
	 */
	return {
		
		
		/**
		 * Set or get the data
		 * 
		 * @param data the new data
		 * @return the data if no arguments are given, otherwise return this
		 */
		data: function(data) {
			if (data === undefined) return __data;
			__data = data;
			return this;
		},
		
		
		/**
		 * Set the value function or property
		 * 
		 * @param property_or_function the name of the property, or a function
		 * @param axislabel the value axis label (optional)
		 * @param strfn the to string function for data labels (optional)
		 * @return the function if no arguments are given, otherwise this
		 */
		value: function(property_or_function, axislabel, strfn) {
			if (property_or_function === undefined) return __valuefn;
			if (typeof(property_or_function) == "string") {
				__valuefn = function(d) { return d[property_or_function]; };
			}
			else if (typeof(property_or_function) == "function") {
				__valuefn = property_or_function;
			}
			else {
				throw "Invalid type of the argument";
			}
			if (axislabel === undefined) {
				__valueaxislabel = null;
			}
			else {
				__valueaxislabel = axislabel;
			}
			if (strfn === undefined) {
				__valuelabelfn = null;
			}
			else {
				__valuelabelfn = strfn;
			}
			return this;
		},
		
		
		/**
		 * Set the function or property that determines the standard deviation
		 * 
		 * @param property_or_function the name of the property, or a function
		 * @return the function if no arguments are given, otherwise this
		 */
		stdev: function(property_or_function) {
			if (property_or_function === undefined) return __stdevfn;
			if (typeof(property_or_function) == "string") {
				__stdevfn = function(d) { return d[property_or_function]; };
			}
			else if (typeof(property_or_function) == "function") {
				__stdevfn = property_or_function;
			}
			else {
				throw "Invalid type of the argument";
			}
			return this;
		},
		
		
		/**
		 * Set the function or property that determines the label of a datum
		 * 
		 * @param property_or_function the name of the property, or a function
		 * @return the function if no arguments are given, otherwise this
		 */
		label: function(property_or_function) {
			if (property_or_function === undefined) return __labelfn;
			if (typeof(property_or_function) == "string") {
				__labelfn = function(d) { return d[property_or_function]; };
			}
			else if (typeof(property_or_function) == "function") {
				__labelfn = property_or_function;
			}
			else {
				throw "Invalid type of the argument";
			}
			return this;
		},
		
		
		/**
		 * Set the function or property that determines the category, as in what appears
		 * in the legent and what determines how each bar should be colored
		 * 
		 * @param property_or_function the name of the property, or a function
		 * @param labelfn the label function to be used in the legend (optional)
		 * @return the function if no arguments are given, otherwise this
		 */
		category: function(property_or_function, labelfn) {
			if (property_or_function === undefined) return __categoryfn;
			if (typeof(property_or_function) == "string") {
				__categoryfn = function(d) { return d[property_or_function]; };
			}
			else if (typeof(property_or_function) == "function") {
				__categoryfn = property_or_function;
			}
			else {
				throw "Invalid type of the argument";
			}
			if (labelfn === undefined) {
				__categorylabelfn = function(category) { return "" + category; };
			}
			else {
				__categorylabelfn = labelfn;
			}
			return this;
		},
		
		
		/**
		 * Set or get the scale type
		 * 
		 * @param scale the new scale type
		 * @return the scale type if no arguments are given, otherwise this
		 */
		scale: function(scale) {
			if (scale === undefined) return __scaletype;
			if (scale == "linear" || scale == "log") {
				__scaletype = scale;
			}
			else {
				throw "Invalid data scale: " + __scaletype;
			}
			return this;
		},
		
		
		/**
		 * Set or return the chart appearance object
		 * 
		 * @param appearance the new appearance
		 * @return the appearance if no arguments are given, otherwise this
		 */
		appearance: function(appearance) {
			if (appearance === undefined) return __appearance;
			__appearance = appearance;
			return this;
		},
		
		
		/**
		 * Set or return the chart height object
		 * 
		 * @param innerHeight the intended inner height
		 * @param ticks the intended number of ticks
		 * @return the height and the ticks if no arguments are given, otherwise this
		 */
		height: function(innerHeight, ticks) {
			if (innerHeight === undefined) {
				var x = {};
				x.innerHeight = __inner_height;
				x.ticks = __num_ticks;
				return x;
			}
			__inner_height = innerHeight;
			__num_ticks = ticks === undefined ? 10 : ticks;
			return this;
		},
		
		
		/**
		 * Render the chart
		 * 
		 * @param id the DOM element ID
		 */
		render: function(id) {
			
			var t = this;
			var a = __appearance;
			
			if (__data == null) {
				throw "The data has not yet been specified";
			}
			
			if (__valuefn == null) {
				throw "The value function has not been specified";
			}
			
			__data.run(function() {
				
				var data = t.data();
				var num_bars = data.data().length;
				
				
				// Create the chart with the initial size estimates, but we will
				// correct them later
				
				var chart_svg = d3.select("#" + id).append("svg")
				.attr("class", "chart")
				.attr("width",  a.bar_width * num_bars)
				.attr("height", __inner_height);
				var chart = chart_svg.append("g");
				
				
				//
				// Prepare a framework for fill patterns
				//
				
				var defs = chart.append('svg:defs');
				var num_patterns = 0;
				var use_patterns = a.fill == "pattern";
				
				
				//
				// Data scale
				//
				
				var data_max = d3.max(data.data(),
										function(d) {
											var value = 1 * __valuefn(d);
											if (__stdevfn == null) {
												return value;
											}
											else {
												var stdev = 1 * __stdevfn(d);
												return value + stdev;
											}
										});
				
				var data_min_ignoring_nonpositives = d3.min(data.data(),
															function(d) {
																var value = 1 * __valuefn(d);
																if (__stdevfn == null) {
																	return value < 0 ? Infinity : value;
																}
																else {
																	var stdev = 1 * __stdevfn(d);
																	return value - stdev < 0 ? Infinity : value - stdev;
																}
															});
				
				
				if (__scaletype == "linear") {
					__d3scale = d3.scale.linear().domain([0, 1.1 * data_max]);
				}
				else if (__scaletype == "log") {
					__d3scale = d3.scale.log()
					.domain([0.9 * data_min_ignoring_nonpositives, 1.1 * data_max]);
				}
				else {
					throw "Invalid data scale: " + __scaletype;
				}
				
				__d3scale.range([__inner_height, 0]).nice();
				var ticks = __ticks(__d3scale, __scaletype, __num_ticks);
				
				/*__d3y = d3.scale.<%= chartProperties.yscale %>()
				 *		.domain([<%= "log".equals(chartProperties.yscale)
				 *		? "0.9 * d3.min(data, function(d) { "
				 *		+ "  if (d.label.indexOf('----') == 0) return 1000 * 1000 * 1000;"
				 *		+ "  if (stacked && d.mean == 0 && d.stdev == 0) return 1000 * 1000 * 1000;"
				 *		+ "  if (d.mean <= d.stdev) return 1000 * 1000 * 1000;"	// is this correct?
				 *		+ "  return d.mean - d.stdev < 0 ? 0 : d.mean - d.stdev;"
				 *		+ "})"
				 *		: "0" %>, 
				 *		1.1 * (stacked ? d3.max(subgroup_sums) : d3.max(data, function(d) { return d.mean + d.stdev; }))])*/
				
				
				//
				// Prepare for the upcoming chart adjustments
				//
				
				var min_chart_y = 0;
				var min_chart_x = 0;
				
				
				//
				// Data axis
				//
				
				var h_tick_lines = chart.selectAll("line")
				.data(ticks)
				.enter().append("line")
				.attr("x1", -a.bars_margin)
				.attr("x2", num_bars * a.bar_width + a.bars_margin)
				.attr("y1", __d3scale)
				.attr("y2", __d3scale)
				.style("stroke", "#ccc");
				
				var data_ticks_text = chart.selectAll(".rule")
				.data(ticks)
				.enter().append("text")
				.attr("class", "rule")
				.attr("x", -a.chart_margin-a.bars_margin)
				.attr("y", __d3scale)
				.attr("dx", 0)
				.attr("dy", ".35em")
				.attr("text-anchor", "end")
				.text(String);
				
				for (var ti = 0; ti < data_ticks_text[0].length; ti++) {
					var r = data_ticks_text[0][ti].getBBox();
					if (r.x < min_chart_x) {
						min_chart_x = r.x;
					}
					if (r.y < min_chart_y) {
						min_chart_y = r.y;
					}
				}
				
				chart.append("line")
				.attr("x1", -a.bars_margin)
				.attr("y1", 0)
				.attr("x2", -a.bars_margin)
				.attr("y2", __inner_height)
				.style("stroke", "#000");
				
				if (__valueaxislabel != undefined && __valueaxislabel != null) {
					var left = min_chart_x - a.ylabel_from_data_tick_text;
					var text = chart.append("text")
					.attr("x", 0)
					.attr("y", 0)
					.attr("dx", 0)
					.attr("dy", 0)
					.attr("text-anchor", "middle")
					.attr("transform", "translate(" + left + ", "
					+ (__inner_height/2)  + ") rotate(-90)")
					.text(__valueaxislabel);
					var r = text[0][0].getBoundingClientRect();
					if (left - r.width < min_chart_x) {
						min_chart_x = left - r.width;
					}
				}
				
				
				//
				// Process the series recursively
				//
				
				var seriesqueue = [];
				seriesqueue.push(data.seriesRoot());
				
				var index = 0;
				var pos = 0;
				var lastparent = null;
				var categories = [];
				
				var seriesinfo = [];
				var max_level = 0;
				
				while (seriesqueue.length > 0) {
					var series = seriesqueue.shift();
					var series_data = series.data();
					if (series_data.length == 0) {
						continue;
					}
					
					if (series_data[0] instanceof d3bp.Series) {
						for (var di = 0; di < series_data.length; di++) {
							var s = series_data[di];
							seriesqueue.push(s);
						}
						continue;
					}
					
					if (series.parent() != lastparent) {
						lastparent = series.parent();
						if (index > 0) {
							pos += a.bar_width;
						}
					}
					
					for (var di = 0; di < series_data.length; di++) {
						var d = series_data[di];
						var value = 1 * __valuefn(d);
						var stdev = __stdevfn == null ? NaN : 1 * __stdevfn(d);
						var label = __labelfn == null ? null : __labelfn(d);
						var category = __categoryfn == null ? null : __categoryfn(d);
						
						
						// Data
						
						var bar = chart.append("rect")
						.attr("x", pos)
						.attr("y", __d3scale(value))
						.attr("width", a.bar_width)
						.attr("height", __inner_height - __d3scale(value));
						
						
						// Category
						
						if (category != undefined && category != null) {
							var category_index = categories.indexOf(category);
							if (category_index < 0) {
								category_index = categories.length;
								categories.push(category);
							}
							
							if (use_patterns) {
								if (category_index >= num_patterns) {
									for (var pi = num_patterns;
											pi <= category_index; pi++) {
										
										num_patterns++;
									var pmod = pi % 6;
									var pattern = defs.append("svg:pattern")
									.attr("id", "pattern-" + pi)
									.attr("x", 0)
									.attr("y", 0)
									.attr("width" , pmod == 3 || pmod == 5 ? 6 : 8)
									.attr("height", pmod == 2 || pmod == 5 ? 6 : 8)
									.attr("patternUnits", "userSpaceOnUse")
									.append("g")
									.style("fill", "none")
									.style("stroke", a.bar_colors(pi))
									.style("stroke-width", "1")
									.style("stroke-linecap", "square");
									
									switch (pmod) {
										case 0:
											pattern.append("path")
											.attr("d", "M 0 0 L 8 8 M 0 -8 L 8 0 "
											+ "M 0 8 L 8 16");
											break;
										case 1:
											pattern.append("path")
											.attr("d", "M 8 0 L 0 8 M 8 -8 L 0 0 "
											+ "M 8 8 L 0 16");
											break;
										case 2:
											pattern.append("path")
											.attr("d", "M 0 3 L 8 3");
											break;
										case 3:
											pattern.append("path")
											.attr("d", "M 3 0 L 3 8");
											break;
										case 4:
											pattern.append("path")
											.attr("d", "M 0 0 L 8 8 M 0 -8 L 8 0 "
											+ "M 0 8 L 8 16 M 8 0 L 0 8 "
											+ "M 8 -8 L 0 0 M 8 8 L 0 16");
											break;
										case 5:
											pattern.append("path")
											.attr("d", "M 0 3 L 8 3 M 3 0 L 3 8");
											break;
									}
											}
								}
								bar.attr("style",
								"fill:url(#pattern-" + category_index + ");"
								+ "stroke:" + a.bar_colors(category_index));
							}
							else {
								bar.style("fill", a.bar_colors(category_index));
							}
						}
						
						
						// Error bars
						
						if (!isNaN(stdev) && stdev > 0) {
							var top = value + stdev;
							var bottom = value - stdev;
							if (bottom < __d3scale.domain()[0]) {
								bottom = __d3scale.domain()[0];
							}
							
							// Middle
							chart.append("line")
							.style("stroke", "#000")
							.attr("x1", pos + 0.5 * a.bar_width)
							.attr("x2", pos + 0.5 * a.bar_width)
							.attr("y1", __d3scale(top))
							.attr("y2", __d3scale(bottom));
							
							// Top
							chart.append("line")
							.style("stroke", "#000")
							.attr("x1", pos + 0.25 * a.bar_width)
							.attr("x2", pos + 0.75 * a.bar_width)
							.attr("y1", __d3scale(top))
							.attr("y2", __d3scale(top));
							
							// Bottom
							chart.append("line")
							.style("stroke", "#000")
							.attr("x1", pos + 0.25 * a.bar_width)
							.attr("x2", pos + 0.75 * a.bar_width)
							.attr("y1", __d3scale(bottom))
							.attr("y2", __d3scale(bottom));
						}
						
						
						// Datum label
						
						if (label != undefined && label != null) {
							chart.append("text")
							.attr("x", 0)
							.attr("y", 0)
							.attr("dx", 0) 			// padding-right
							.attr("dy", ".35em") 	// vertical-align: middle
							.attr("transform", "translate("
							+ (pos +  0.5 * a.bar_width) + ", "
							+ (__inner_height + a.chart_margin)
							+ ") rotate(45)")
							.text(label);
						}
						
						
						// Value label
						
						if (__valuelabelfn != undefined && __valuelabelfn != null) {
							if (!isNaN(stdev) && stdev > 0) {
								var top = value + stdev;
							}
							else {
								var top = value;
							}
							var y = __d3scale(top) - 10;
							var text = chart.append("text")
							.attr("x", 0)
							.attr("y", 0)
							.attr("dx", 0)
							.attr("dy", ".35em") // vertical-align: middle
							.attr("transform", "translate("
							+ (pos +  0.5 * a.bar_width) + ", "
							+ y + ") rotate(-90)")
							.text(__valuelabelfn(value));
							var r = text[0][0].getBoundingClientRect();
							if (y - r.height < min_chart_y) {
								min_chart_y = y - r.height;
							}
						}
						
						
						// Advance the counters
						
						var old_pos = pos;
						pos += a.bar_width;
						index++;
						
						
						// Update the series info
						
						var inv_level = 0;
						for (var s = series; s != null; s = s.parent()) {
							
							var info = null;
							for (var si = 0; si < seriesinfo.length; si++) {
								var x = seriesinfo[si];
								if (x.series == s) {
									info = x;
									break;
								}
							}
							if (info == null) {
								info = [];
								info.series = s;
								info.min_pos = 1000 * 1000 * 1000;
								info.max_pos = 0;
								seriesinfo.push(info);
							}
							
							
							// Update min and max
							
							if (info.min_pos > old_pos) {
								info.min_pos = old_pos;
							}
							
							if (info.max_pos < pos) {
								info.max_pos = pos;
							}
							
							
							// Advance the inverse level
							
							if (max_level < inv_level) {
								max_level = inv_level;
							}
							
							inv_level++;
						}
					}
				}
				
				
				//
				// Adjust the tick lines
				//
				
				var chart_inner_width = pos;
				for (var ti = 0; ti < h_tick_lines[0].length; ti++) {
					h_tick_lines[0][ti].setAttribute("x2", chart_inner_width + a.bars_margin);
				}
				
				
				//
				// Prepare for the upcoming chart adjustments
				//
				
				var max_chart_y = __inner_height;
				var max_chart_x = chart_inner_width + a.bars_margin;
				
				
				//
				// Horizontal axis
				//
				
				chart.append("line")
				.attr("x1", -a.bars_margin)
				.attr("y1", __inner_height)
				.attr("x2", chart_inner_width + a.bars_margin)
				.attr("y2", __inner_height)
				.style("stroke", "#000");
				
				
				//
				// Series labels
				//
				
				seriesqueue = [];
				seriesqueue.push(data.seriesRoot());
				
				var label_at_inv_levels = [];
				
				while (seriesqueue.length > 0) {
					var series = seriesqueue.shift();
					var series_data = series.data();
					if (series_data.length == 0) {
						continue;
					}
					
					if (series_data[0] instanceof d3bp.Series) {
						for (var di = 0; di < series_data.length; di++) {
							var s = series_data[di];
							seriesqueue.push(s);
						}
					}
					
					var info = null;
					for (var si = 0; si < seriesinfo.length; si++) {
						var x = seriesinfo[si];
						if (x.series == series) {
							info = x;
							break;
						}
					}
					if (info == null) {
						throw "Could not find series info for " + series;
					}
					
					var level = -1;
					for (var s = series; s != null; s = s.parent()) {
						level++;
					}
					
					var text = chart.append("text")
					.attr("x", 0)
					.attr("y", 0)
					.attr("dx", 0)
					.attr("dy", ".35em") // vertical-align: middle
					.attr("text-anchor", "middle")
					.attr("transform", "translate("
					+ ((info.min_pos + info.max_pos) / 2) + ", "
					+ (__inner_height + a.chart_margin)  + ") rotate(0)")
					.text(series.label());
					
					var labels_at_level = label_at_inv_levels[max_level - level];
					if (labels_at_level == undefined || labels_at_level == null) {
						labels_at_level = [];
						label_at_inv_levels[max_level - level] = labels_at_level;
					}
					
					var x = [];
					var w = text[0][0].getBoundingClientRect().width;
					x.text = text[0][0];
					x.x_pos = (info.min_pos + info.max_pos) / 2;
					x.too_long = 0.9 * (info.max_pos - info.min_pos) < w;
					labels_at_level.push(x);
				}
				
				var label_y_pos = __inner_height + a.chart_margin;
				for (var inv_level = 0; inv_level < label_at_inv_levels.length; inv_level++) {
					var labels_at_level = label_at_inv_levels[inv_level];
					
					var too_long = false;
					for (var li = 0; li < labels_at_level.length; li++) {
						if (labels_at_level[li].too_long) {
							too_long = true;
							break;
						}
					}
					
					if (too_long) {
						for (var li = 0; li < labels_at_level.length; li++) {
							var l = labels_at_level[li];
							l.text.setAttribute("text-anchor", "start");
							l.text.setAttribute("transform", "translate("
							+ (l.x_pos) + ", "
							+ (label_y_pos)  + ") rotate(45)");
						}
					}
					else {
						for (var li = 0; li < labels_at_level.length; li++) {
							var l = labels_at_level[li];
							l.text.setAttribute("text-anchor", "middle");
							l.text.setAttribute("transform", "translate("
							+ (l.x_pos) + ", "
							+ (label_y_pos)  + ") rotate(0)");
						}
					}
					
					var max_height = 0;
					for (var li = 0; li < labels_at_level.length; li++) {
						var l = labels_at_level[li];
						var h = l.text.getBoundingClientRect().height;
						if (h > max_height) {
							max_height = h;
						}
						var w = l.text.getBoundingClientRect().width;
						if (l.x_pos + w > max_chart_x) {
							max_chart_x = l.x_pos + w;
						}
					}
					
					if (max_height + label_y_pos > max_chart_y) {
						max_chart_y = max_height + label_y_pos;
					}
					
					label_y_pos += max_height + 4;
				}
				
				
				//
				// Legend
				//
				
				if (categories.length > 1) {
					var x = chart_inner_width + a.bars_margin + a.chart_margin
					+ a.legend_padding_left;
					
					for (var ci = 0; ci < categories.length; ci++) {
						var c = categories[ci];
						
						var bar = chart.append("rect")
						.attr("x", x)
						.attr("y", ci * (a.legend_bar_height + a.legend_vertical_spacing) + a.legend_padding_top)
						.attr("width", a.legend_bar_width)
						.attr("height", a.legend_bar_height);
						
						if (use_patterns) {
							bar.attr("style",
							"fill:url(#pattern-" + ci + ");"
							+ "stroke:" + a.bar_colors(ci));
						}
						else {
							bar.style("fill", a.bar_colors(ci));
						}
						
						var text = chart.append("text")
						.attr("x", x + a.legend_bar_width + a.legend_bar_padding_right)
						.attr("y", ci * (a.legend_bar_height + a.legend_vertical_spacing)
						+ a.legend_padding_top + 0.5 * a.legend_bar_height)
						.attr("dx", 0)
						.attr("dy", ".35em") // vertical-align: middle
						.text(__categorylabelfn(categories[ci]));
						
						var b = text[0][0].getBBox();
						if (b.x + b.width > max_chart_x) {
							max_chart_x = b.x + b.width;
						}
					}
				}
				
				
				//
				// Adjust the chart size
				//
				
				chart_svg[0][0].setAttribute("width" , max_chart_x - min_chart_x);
				chart_svg[0][0].setAttribute("height", max_chart_y - min_chart_y);
				chart[0][0].setAttribute("transform", "translate(" + (-min_chart_x) + ", " + (-min_chart_y) + ")");
			});
			
			return this;
		}
	};
});




/*****************************************************************************
 * Miscellaneous                                                             *
 *****************************************************************************/

/*
 * Utility: numToString3
 */
d3bp.numToString3 = function(x) {
	
	var fixed_length = 3;
	if (x > 10) fixed_length = 2;
	if (x > 100) fixed_length = 1;
	if (x > 1000) fixed_length = 0;
	
	return x.toFixed(fixed_length);
};
