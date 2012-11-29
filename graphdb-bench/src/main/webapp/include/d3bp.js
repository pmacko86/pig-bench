/*
 * D3-Based Plots
 */

var d3bp = {

	/*
	 * data
	 */
	Data: function(input) {
		
		this.__datasource = null;
		
		this.__preprocessfunctions = [];
		this.__seriesdefinitions = [];
		
		this.__originaldata = null;
		this.__data = null;
		
		this.__seriesroot = null;
		
		
		// Case 1: URL
		if (typeof(input) == "string") {
			this.__datasource = input;
		}
		
		// Unknown
		else {
			throw "Invalid input type";
		}
	},
	
	
	/*
	 * series
	 */
	Series: function(parent) {
		
		this.__parent = parent;		// parent series
		
		this.__value = null;		// the common property value
		this.__valuelabelfn = null;
		this.__label = null;
		
		this.__data = [];			// data or sub-series
	},
	
	
	/*
	 * appearance
	 */
	Appearance: function() {
		
		this.chart_margin = 10;
		
		this.bar_width = 20;
		this.bar_colors = d3.scale.category10();
		this.bars_margin = 10;
		
		this.ylabel_from_data_tick_text = 10;
		
		this.legend_padding_left = 10;
		this.legend_padding_top = 10;
		this.legend_bar_padding_right = 4;
		this.legend_bar_width = 2 * this.bar_width;
		this.legend_bar_height = this.bar_width;
		this.legend_vertical_spacing = 2;
		
		this.fill = "solid";
	},

	
	/*
	 * barchart
	 */
	BarChart: function() {
		
		this.__data = null;
		this.__valuefn = null;			// extract the value (usually y)
		this.__stdevfn = null;			// extract the standard deviation
		this.__labelfn = null;			// extract the label value (usually x)
		this.__categoryfn = null;		// extract the category
		
		this.__valuelabelfn = null;		// convert the value to string
		this.__categorylabelfn = null;	// convert the category to string
		
		this.__appearance = new d3bp.Appearance();
		this.__valueaxislabel = null;
		
		this.__stacked = false;
		this.__scaletype = "linear";
		
		this.__d3scale = null;
		
		this.__inner_height = 400;
		this.__num_ticks = 10;
	},
	
	
	/*
	 * Utility: __ticks
	 */
	__ticks: function(d3scale, scaleType, numTicks) {
		var ticks = d3scale.ticks(numTicks);
		if (scaleType == "log" && ticks.length > numTicks) {
			ticks = []
			
			ticks.push(d3scale.domain()[1]);
			while (ticks[ticks.length - 1] / 10 >= d3scale.domain()[0]) {
				ticks.push(ticks[ticks.length - 1] / 10);
			}
		}
		return ticks;
	},
	
	
	/*
	 * Utility: numToString3
	 */
	numToString3: function(x) {
		
		var fixed_length = 3;
		if (x > 10) fixed_length = 2;
		if (x > 100) fixed_length = 1;
		if (x > 1000) fixed_length = 0;
		
		return x.toFixed(fixed_length);
	}
};


/*****************************************************************************
 * Class d3bp.Data                                                           *
 *****************************************************************************/

/**
 * Add a filter, but do not run it yet
 *
 * @param fn the function, taking a data element and returning true or false
 */
d3bp.Data.prototype.filter = function(fn) {
	this.__preprocessfunctions.push(["filter", fn]);
	return this;
};


/**
 * Add a function to derive a new value, but do not run it yet
 *
 * @param property the new property name
 * @param fn the function, taking a data element and returning a value
 */
d3bp.Data.prototype.derive = function(property, fn) {
	this.__preprocessfunctions.push(["derive", property, fn]);
	return this;
};


/**
 * Add a function to run for each element, but do not run it yet
 *
 * @param fn the function, taking a data element
 */
d3bp.Data.prototype.forEach = function(fn) {
	this.__preprocessfunctions.push(["foreach", fn]);
	return this;
};


/**
 * Add a property name, and an optional function to transform it, to serve for
 * categorizing data elements into series/groups
 *
 * @param property the property
 * @param labelfn the value label function (optional)
 * @param transformfn the property transform function (optional)
 */
d3bp.Data.prototype.groupBy = function(property, labelfn, transformfn) {
	if (labelfn == undefined) {
		labelfn = function(v) { return "" + v; };
	}
	this.__seriesdefinitions.push([property, labelfn, transformfn]);
	return this;
};


/**
 * Run all the preprocess functions
 */
d3bp.Data.prototype.__preprocess = function() {

	if (this.__originaldata == null) {
		throw "The data has not yet been loaded";
	}
	this.__data = this.__originaldata.filter(function() { return true; });

	
	// Preprocess the data
	
	for (var pi = 0; pi < this.__preprocessfunctions.length; pi++) {
		var p = this.__preprocessfunctions[pi];

		if (p[0] == "filter") {
			this.__data = this.__data.filter(p[1]);
		}
		else if (p[0] == "derive") {
			this.__data.forEach(function(d, i) {
				d[p[1]] = p[2](d, i);
			});
		}
		else if (p[0] == "foreach") {
			this.__data.forEach(p[1]);
		}
		else {
			throw "Invalid preprocessing function type: " + p[0];
		}
	}
	
	
	// Categorize the data into the individual data series
	
	var root = new d3bp.Series(null);
	this.__data.forEach(function(d, i) {
		root.__data.push(d);
	});
	this.__seriesroot = root;
	
	if (this.__seriesdefinitions.length > 0) {
		
		var leaves = [this.__seriesroot];
		
		for (var sdi = 0; sdi < this.__seriesdefinitions.length; sdi++) {
			var sd = this.__seriesdefinitions[sdi];
			var new_leaves = [];
			
			for (var li = 0; li < leaves.length; li++) {
				var leaf = leaves[li];
				var new_data = [];
				
				for (var di = 0; di < leaf.__data.length; di++) {
					var d = leaf.__data[di];
					var v = d[sd[0]];
					if (sd[2] != undefined && sd[2] != null) {
						v = sd[2](v);
					}
					
					var found = null;
					for (var i = 0; i < new_data.length; i++) {
						if (new_data[i].__value == v) {
							found = new_data[i];
						}
					}
					if (found == null) {
						found = new d3bp.Series(leaf);
						found.__value = v;
						found.__valuelabelfn = sd[1];
						found.__label = found.__valuelabelfn(v);
						new_data.push(found);
						new_leaves.push(found);
					}
					found.__data.push(d);
				}
				
				leaf.__data = new_data;
			}
			
			leaves = new_leaves;
		}
	}
};


/**
 * Get the data and then run the specified callback
 *
 * @param fn the callback function, receiving this object
 */
d3bp.Data.prototype.run = function(fn) {
	var t = this;
	if (this.__datasource != null && typeof(this.__datasource) == "string") {
		d3.csv(this.__datasource, function(data) {
			t.__originaldata = data;
			t.__preprocess();
			fn(this);
		});
	}
	else {
		throw "Invalid data source type";
	}
};


/**
 * Return the data after they have been loaded
 *
 * @return the loaded data
 */
d3bp.Data.prototype.d3data = function() {
	
	if (this.__data == null) {
		throw "The data has not yet been loaded";
	}
	
	return this.__data;
};



/*****************************************************************************
 * Class d3bp.BarChart                                                       *
 *****************************************************************************/

/**
 * Set or get the data
 * 
 * @param data the new data
 * @return the data if no arguments are given, otherwise return this
 */
d3bp.BarChart.prototype.data = function(data) {
	if (data === undefined) return this.__data;
	this.__data = data;
	return this;
};


/**
 * Set the value function or property
 * 
 * @param property_or_function the name of the property, or a function
 * @param axislabel the value axis label (optional)
 * @param strfn the to string function for data labels (optional)
 * @return the function if no arguments are given, otherwise this
 */
d3bp.BarChart.prototype.value = function(property_or_function, axislabel, strfn) {
	if (property_or_function === undefined) return this.__valuefn;
	if (typeof(property_or_function) == "string") {
		this.__valuefn = function(d) { return d[property_or_function]; };
	}
	else if (typeof(property_or_function) == "function") {
		this.__valuefn = property_or_function;
	}
	else {
		throw "Invalid type of the argument";
	}
	if (axislabel === undefined) {
		this.__valueaxislabel = null;
	}
	else {
		this.__valueaxislabel = axislabel;
	}
	if (strfn === undefined) {
		this.__valuelabelfn = null;
	}
	else {
		this.__valuelabelfn = strfn;
	}
	return this;
};


/**
 * Set the function or property that determines the standard deviation
 * 
 * @param property_or_function the name of the property, or a function
 * @return the function if no arguments are given, otherwise this
 */
d3bp.BarChart.prototype.stdev = function(property_or_function) {
	if (property_or_function === undefined) return this.__stdevfn;
	if (typeof(property_or_function) == "string") {
		this.__stdevfn = function(d) { return d[property_or_function]; };
	}
	else if (typeof(property_or_function) == "function") {
		this.__stdevfn = property_or_function;
	}
	else {
		throw "Invalid type of the argument";
	}
	return this;
};


/**
 * Set the function or property that determines the label of a datum
 * 
 * @param property_or_function the name of the property, or a function
 * @return the function if no arguments are given, otherwise this
 */
d3bp.BarChart.prototype.label = function(property_or_function) {
	if (property_or_function === undefined) return this.__labelfn;
	if (typeof(property_or_function) == "string") {
		this.__labelfn = function(d) { return d[property_or_function]; };
	}
	else if (typeof(property_or_function) == "function") {
		this.__labelfn = property_or_function;
	}
	else {
		throw "Invalid type of the argument";
	}
	return this;
};


/**
 * Set the function or property that determines the category, as in what appears
 * in the legent and what determines how each bar should be colored
 * 
 * @param property_or_function the name of the property, or a function
 * @param labelfn the label function to be used in the legend (optional)
 * @return the function if no arguments are given, otherwise this
 */
d3bp.BarChart.prototype.category = function(property_or_function, labelfn) {
	if (property_or_function === undefined) return this.__categoryfn;
	if (typeof(property_or_function) == "string") {
		this.__categoryfn = function(d) { return d[property_or_function]; };
	}
	else if (typeof(property_or_function) == "function") {
		this.__categoryfn = property_or_function;
	}
	else {
		throw "Invalid type of the argument";
	}
	if (labelfn === undefined) {
		this.__categorylabelfn = function(category) { return "" + category; };
	}
	else {
		this.__categorylabelfn = labelfn;
	}
	return this;
};


/**
 * Set or get the scale type
 * 
 * @param scale the new scale type
 * @return the scale type if no arguments are given, otherwise this
 */
d3bp.BarChart.prototype.scale = function(scale) {
	if (scale === undefined) return this.__scaletype;
	if (scale == "linear" || scale == "log") {
		this.__scaletype = scale;
	}
	else {
		throw "Invalid data scale: " + t.__scaletype;
	}
	return this;
};


/**
 * Set or return the chart appearance object
 * 
 * @param appearance the new appearance
 * @return the appearance if no arguments are given, otherwise this
 */
d3bp.BarChart.prototype.appearance = function(appearance) {
	if (appearance === undefined) return this.__appearance;
	this.__appearance = appearance;
	return this;
};


/**
 * Set or return the chart height object
 * 
 * @param innerHeight the intended inner height
 * @param ticks the intended number of ticks
 * @return the height and the ticks if no arguments are given, otherwise this
 */
d3bp.BarChart.prototype.height = function(innerHeight, ticks) {
	if (innerHeight === undefined) {
		var x = {};
		x.innerHeight = this.__inner_height;
		x.ticks = this.__num_ticks;
		return x;
	}
	this.__inner_height = innerHeight;
	this.__num_ticks = ticks === undefined ? 10 : ticks;
	return this;
};


/**
 * Render the chart
 * 
 * @param id the DOM element ID
 */
d3bp.BarChart.prototype.render = function(id) {
	
	var t = this;
	var a = this.__appearance;
	
	if (this.__data == null) {
		throw "The data has not yet been specified";
	}
	
	if (this.__valuefn == null) {
		throw "The value function has not been specified";
	}
	
	this.__data.run(function() {
		
		var data = t.data();
		var num_bars = data.d3data().length;
		
		
		// Create the chart with the initial size estimates, but we will
		// correct them later
		
		var chart_svg = d3.select("#" + id).append("svg")
			.attr("class", "chart")
			.attr("width",  a.bar_width * num_bars)
			.attr("height", t.__inner_height);
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
		
		var data_max = d3.max(data.d3data(),
			function(d) {
				var value = 1 * t.__valuefn(d);
				if (t.__stdevfn == null) {
					return value;
				}
				else {
					var stdev = 1 * t.__stdevfn(d);
					return value + stdev;
				}
			});
		
		var data_min_ignoring_nonpositives = d3.min(data.d3data(),
			function(d) {
				var value = 1 * t.__valuefn(d);
				if (t.__stdevfn == null) {
					return value < 0 ? Infinity : value;
				}
				else {
					var stdev = 1 * t.__stdevfn(d);
					return value - stdev < 0 ? Infinity : value - stdev;
				}
			});
		
		
		if (t.__scaletype == "linear") {
			t.__d3scale = d3.scale.linear().domain([0, 1.1 * data_max]);
		}
		else if (t.__scaletype == "log") {
			t.__d3scale = d3.scale.log()
				.domain([0.9 * data_min_ignoring_nonpositives, 1.1 * data_max]);
		}
		else {
			throw "Invalid data scale: " + t.__scaletype;
		}
		
		t.__d3scale.range([t.__inner_height, 0]).nice();
		var ticks = d3bp.__ticks(t.__d3scale, t.__scaletype, t.__num_ticks);
		
		/*this.__d3y = d3.scale.<%= chartProperties.yscale %>()
		.domain([<%= "log".equals(chartProperties.yscale)
		? "0.9 * d3.min(data, function(d) { "
		+ "  if (d.label.indexOf('----') == 0) return 1000 * 1000 * 1000;"
		+ "  if (stacked && d.mean == 0 && d.stdev == 0) return 1000 * 1000 * 1000;"
		+ "  if (d.mean <= d.stdev) return 1000 * 1000 * 1000;"	// is this correct?
		+ "  return d.mean - d.stdev < 0 ? 0 : d.mean - d.stdev;"
		+ "})"
		: "0" %>, 
		1.1 * (stacked ? d3.max(subgroup_sums) : d3.max(data, function(d) { return d.mean + d.stdev; }))])*/
		
		
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
			.attr("y1", t.__d3scale)
			.attr("y2", t.__d3scale)
			.style("stroke", "#ccc");
		
		var data_ticks_text = chart.selectAll(".rule")
			.data(ticks)
			.enter().append("text")
			.attr("class", "rule")
			.attr("x", -a.chart_margin-a.bars_margin)
			.attr("y", t.__d3scale)
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
			.attr("y2", t.__inner_height)
			.style("stroke", "#000");
		
		if (t.__valueaxislabel != undefined && t.__valueaxislabel != null) {
			var left = min_chart_x - a.ylabel_from_data_tick_text;
			var text = chart.append("text")
				.attr("x", 0)
				.attr("y", 0)
				.attr("dx", 0)
				.attr("dy", 0)
				.attr("text-anchor", "middle")
				.attr("transform", "translate(" + left + ", "
					+ (t.__inner_height/2)  + ") rotate(-90)")
				.text(t.__valueaxislabel);
			var r = text[0][0].getBoundingClientRect();
			if (left - r.width < min_chart_x) {
				min_chart_x = left - r.width;
			}
		}
		
		
		//
		// Process the series recursively
		//
		
		var seriesqueue = [];
		seriesqueue.push(data.__seriesroot);
		
		var index = 0;
		var pos = 0;
		var lastparent = null;
		var categories = [];
		
		var seriesinfo = [];
		var max_level = 0;
		
		while (seriesqueue.length > 0) {
			var series = seriesqueue.shift();
			if (series.__data.length == 0) {
				continue;
			}
			
			if (series.__data[0] instanceof d3bp.Series) {
				for (var di = 0; di < series.__data.length; di++) {
					var s = series.__data[di];
					seriesqueue.push(s);
				}
				continue;
			}
			
			if (series.__parent != lastparent) {
				lastparent = series.__parent;
				if (index > 0) {
					pos += a.bar_width;
				}
			}
			
			for (var di = 0; di < series.__data.length; di++) {
				var d = series.__data[di];
				var value = 1 * t.__valuefn(d);
				var stdev = t.__stdevfn == null ? NaN : 1 * t.__stdevfn(d);
				var label = t.__labelfn == null ? null : t.__labelfn(d);
				var category = t.__categoryfn == null ? null : t.__categoryfn(d);
				
				
				// Data
				
				var bar = chart.append("rect")
					.attr("x", pos)
					.attr("y", t.__d3scale(value))
					.attr("width", a.bar_width)
					.attr("height", t.__inner_height - t.__d3scale(value));
					
					
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
					if (bottom < t.__d3scale.domain()[0]) {
						bottom = t.__d3scale.domain()[0];
					}
					
					// Middle
					chart.append("line")
						.style("stroke", "#000")
						.attr("x1", pos + 0.5 * a.bar_width)
						.attr("x2", pos + 0.5 * a.bar_width)
						.attr("y1", t.__d3scale(top))
						.attr("y2", t.__d3scale(bottom));
					
					// Top
					chart.append("line")
						.style("stroke", "#000")
						.attr("x1", pos + 0.25 * a.bar_width)
						.attr("x2", pos + 0.75 * a.bar_width)
						.attr("y1", t.__d3scale(top))
						.attr("y2", t.__d3scale(top));
					
					// Bottom
					chart.append("line")
						.style("stroke", "#000")
						.attr("x1", pos + 0.25 * a.bar_width)
						.attr("x2", pos + 0.75 * a.bar_width)
						.attr("y1", t.__d3scale(bottom))
						.attr("y2", t.__d3scale(bottom));
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
							+ (t.__inner_height + a.chart_margin)
							+ ") rotate(45)")
						.text(label);
				}
				
				
				// Value label
				
				if (t.__valuelabelfn != undefined && t.__valuelabelfn != null) {
					if (!isNaN(stdev) && stdev > 0) {
						var top = value + stdev;
					}
					else {
						var top = value;
					}
					var y = t.__d3scale(top) - 10;
					var text = chart.append("text")
						.attr("x", 0)
						.attr("y", 0)
						.attr("dx", 0)
						.attr("dy", ".35em") // vertical-align: middle
						.attr("transform", "translate("
							+ (pos +  0.5 * a.bar_width) + ", "
							+ y + ") rotate(-90)")
						.text(t.__valuelabelfn(value));
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
				for (var s = series; s != null; s = s.__parent) {
				
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
		
		var max_chart_y = t.__inner_height;
		var max_chart_x = chart_inner_width + a.bars_margin;
		
		
		//
		// Horizontal axis
		//
		
		chart.append("line")
			.attr("x1", -a.bars_margin)
			.attr("y1", t.__inner_height)
			.attr("x2", chart_inner_width + a.bars_margin)
			.attr("y2", t.__inner_height)
			.style("stroke", "#000");
		
		
		//
		// Series labels
		//
		
		seriesqueue = [];
		seriesqueue.push(data.__seriesroot);
		
		var label_at_inv_levels = [];
		
		while (seriesqueue.length > 0) {
			var series = seriesqueue.shift();
			if (series.__data.length == 0) {
				continue;
			}
			
			if (series.__data[0] instanceof d3bp.Series) {
				for (var di = 0; di < series.__data.length; di++) {
					var s = series.__data[di];
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
			
			var level = -1;
			for (var s = series; s != null; s = s.__parent) {
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
					+ (t.__inner_height + a.chart_margin)  + ") rotate(0)")
				.text(series.__label);
			
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
		
		var label_y_pos = t.__inner_height + a.chart_margin;
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
					.text(t.__categorylabelfn(categories[ci]));
				
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
};
