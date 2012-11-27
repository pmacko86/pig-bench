/*
 * D3-Based Plots
 */

var d3bp = {

	/*
	 * data
	 */
	data: function(input) {
		
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
	series: function(parent) {
		
		this.__parent = parent;		// parent series
		
		this.__value = null;		// the common property value
		this.__label = null;
		
		this.__data = [];			// data or sub-series
	},
	
	
	/*
	 * appearance
	 */
	appearance: function() {
		this.chart_inner_height = 420;
		this.chart_margin = 10;
		this.num_ticks = 10;
		
		this.padding_left = 100;
		this.padding_right = 300;
		this.padding_right_without_legend = 25;
		this.padding_top = 75;
		this.padding_bottom = 150;
		
		this.bar_width = 20;
		this.bar_colors = d3.scale.category10();
		this.bars_margin = 10;
		
		this.ylabel_from_chart_margin = 40;
		
		this.legend_padding_left = 10;
		this.legend_padding_top = 10;
		this.legend_bar_padding_right = 4;
		this.legend_bar_width = 2 * this.bar_width;
		this.legend_bar_height = this.bar_width;
		this.legend_vertical_spacing = 2;
	},

	
	/*
	 * barchart
	 */
	barchart: function() {
		
		this.__data = null;
		this.__valuefn = null;
		this.__stdevfn = null;
		this.__labelfn = null;
		this.__categoryfn = null;
		this.__categorylabelfn = null;
		
		this.__appearance = new d3bp.appearance();
		this.__valuelabel = null;
		
		this.__stacked = false;
		this.__scaletype = "linear";
		
		this.__d3scale = null;
	},
	
	
	/*
	 * Utility: __ticks
	 */
	__ticks: function(d3scale, scaletype, numticks) {
		var ticks = d3scale.ticks(num_ticks);
		if (scaletype == "log" && ticks.length > numticks) {
			ticks = []
			
			ticks.push(d3scale.domain()[1]);
			while (ticks[ticks.length - 1] / 10 >= d3scale.domain()[0]) {
				ticks.push(ticks[ticks.length - 1] / 10);
			}
		}
		return ticks;
	}
};


/*****************************************************************************
 * Class d3bp.data                                                           *
 *****************************************************************************/

/**
 * Add a filter, but do not run it yet
 *
 * @param fn the function, taking a data element and returning true or false
 */
d3bp.data.prototype.filter = function(fn) {
	this.__preprocessfunctions.push(["filter", fn]);
	return this;
};


/**
 * Add a function to derive a new value, but do not run it yet
 *
 * @param property the new property name
 * @param fn the function, taking a data element and returning a value
 */
d3bp.data.prototype.derive = function(property, fn) {
	this.__preprocessfunctions.push(["derive", property, fn]);
	return this;
};


/**
 * Add a function to run for each element, but do not run it yet
 *
 * @param fn the function, taking a data element
 */
d3bp.data.prototype.foreach = function(fn) {
	this.__preprocessfunctions.push(["foreach", fn]);
	return this;
};


/**
 * Add a property name, and an optional function to transform it, to serve for
 * categorizing data elements into series/groups
 *
 * @param property the property
 * @param fn the property transform function (optional)
 */
d3bp.data.prototype.groupby = function(property, fn) {
	this.__seriesdefinitions.push([property, fn]);
	return this;
};


/**
 * Run all the preprocess functions
 */
d3bp.data.prototype.__preprocess = function() {

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
	
	var root = new d3bp.series(null);
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
					if (sd[1] != undefined && sd[1] != null) {
						v = sd[1](v);
					}
					
					var found = null;
					for (var i = 0; i < new_data.length; i++) {
						if (new_data[i].__value == v) {
							found = new_data[i];
						}
					}
					if (found == null) {
						found = new d3bp.series(leaf);
						found.__value = v;
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
d3bp.data.prototype.run = function(fn) {
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
d3bp.data.prototype.d3data = function() {
	
	if (this.__data == null) {
		throw "The data has not yet been loaded";
	}
	
	return this.__data;
};



/*****************************************************************************
 * Class d3bp.barchart                                                       *
 *****************************************************************************/

/**
 * Set or get the data
 * 
 * @param data the new data
 * @return the data if no arguments are given, otherwise return this
 */
d3bp.barchart.prototype.data = function(data) {
	if (data === undefined) return this.__data;
	this.__data = data;
	return this;
};


/**
 * Set the value function or property
 * 
 * @param property_or_function the name of the property, or a function
 * @param the value axis label (optional)
 * @return the function if no arguments are given, otherwise this
 */
d3bp.barchart.prototype.value = function(property_or_function, label) {
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
	if (label === undefined) {
		this.__valuelabel = null;
	}
	else {
		this.__valuelabel = label;
	}
	return this;
};


/**
 * Set the function or property that determines the standard deviation
 * 
 * @param property_or_function the name of the property, or a function
 * @return the function if no arguments are given, otherwise this
 */
d3bp.barchart.prototype.stdev = function(property_or_function) {
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
d3bp.barchart.prototype.label = function(property_or_function) {
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
d3bp.barchart.prototype.category = function(property_or_function, labelfn) {
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
 * Render the chart
 * 
 * @param id the DOM element ID
 */
d3bp.barchart.prototype.render = function(id) {
	
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
		var num_bars = data.d3data().length;//XXX(t.__stacked ? subgroup_ids.length : data.length);
		var chart = d3.select("#" + id).append("svg")
			.attr("class", "chart")
			.attr("width",  a.bar_width * num_bars + a.padding_left
			+ a.padding_right) //+ (categories.length > 1 ? a.padding_right : a.padding_right_without_legend))
			.attr("height", a.chart_inner_height + a.padding_top + a.padding_bottom)
			.append("g")
			.attr("transform", "translate(" + a.padding_left + ", " + a.padding_top + ")");
		
		
		//
		// Data scale
		//
		
		if (t.__scaletype == "linear") {
			t.__d3scale = d3.scale.linear()
				.domain([0, 1.1 * d3.max(data.d3data(), function(d) { return d.mean + d.stdev; })]);
			//XXX(stacked ? d3.max(subgroup_sums) : d3.max(data, function(d) { return d.mean + d.stdev; }))])
		}
		else if (t.__scale == "log") {
		}
		else {
			throw "Invalid data scale: " + t.__scaletype;
		}
		
		t.__d3scale.range([a.chart_inner_height, 0]).nice();
		var ticks = d3bp.__ticks(t.__d3scale, t.__scaletype, a.num_ticks);
		
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
		// Data axis
		//
	
		chart.selectAll("line")
			.data(ticks)
			.enter().append("line")
			.attr("x1", -a.bars_margin)
			.attr("x2", num_bars * a.bar_width + a.bars_margin)
			.attr("y1", t.__d3scale)
			.attr("y2", t.__d3scale)
			.style("stroke", "#ccc");
		
		chart.selectAll(".rule")
			.data(ticks)
			.enter().append("text")
			.attr("class", "rule")
			.attr("x", -a.chart_margin-a.bars_margin)
			.attr("y", t.__d3scale)
			.attr("dx", 0)
			.attr("dy", ".35em")
			.attr("text-anchor", "end")
			.text(String);
		
		chart.append("line")
			.attr("x1", -a.bars_margin)
			.attr("y1", 0)
			.attr("x2", -a.bars_margin)
			.attr("y2", a.chart_inner_height)
			.style("stroke", "#000");
		
		if (t.__valuelabel != undefined && t.__valuelabel != null) {
			chart.append("text")
				.attr("x", 0)
				.attr("y", 0)
				.attr("dx", 0)
				.attr("dy", 0)
				.attr("text-anchor", "middle")
				.attr("transform", "translate("
				+ (-a.bars_margin*2 - a.ylabel_from_chart_margin) + ", "
				+ (a.chart_inner_height/2)  + ") rotate(-90)")
				.text(t.__valuelabel);
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
		
		while (seriesqueue.length > 0) {
			var series = seriesqueue.shift();
			if (series.__data.length == 0) {
				continue;
			}
			
			if (series.__data[0] instanceof d3bp.series) {
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
				var value = t.__valuefn(d);
				var stdev = t.__stdevfn == null ? NaN : t.__stdevfn(d);
				var label = t.__labelfn == null ? null : t.__labelfn(d);
				var category = t.__categoryfn == null ? null : t.__categoryfn(d);
				
				
				// Data
				
				var bar = chart.append("rect")
					.attr("x", pos)
					.attr("y", t.__d3scale(value))
					.attr("width", a.bar_width)
					.attr("height", a.chart_inner_height - t.__d3scale(value));
					
					
				// Category
				
				if (category != undefined && category != null) {
					var category_index = categories.indexOf(category);
					if (category_index < 0) {
						category_index = categories.length;
						categories.push(category);
					}
					
					bar.style("fill", bar_colors(category_index));
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
							+ (a.chart_inner_height + a.chart_margin)
							+ ") rotate(45)")
						.text(label);
				}
				
				pos += bar_width;
				index++;
			}
		}
		
		
		//
		// Horizontal axis
		//
		
		chart.append("line")
			.attr("x1", -a.bars_margin)
			.attr("y1", a.chart_inner_height)
			.attr("x2", a.bar_width * num_bars + a.bars_margin)
			.attr("y2", a.chart_inner_height)
			.style("stroke", "#000");
		
		
		
		//
		// Legend
		//
		
		if (categories.length > 1) {
			var x = a.bar_width * num_bars + a.bars_margin + a.chart_margin
					+ a.legend_padding_left;
			
			for (var ci = 0; ci < categories.length; ci++) {
				var c = categories[ci];
				
				chart.append("rect")
					.attr("x", x)
					.attr("y", ci * (a.legend_bar_height + a.legend_vertical_spacing) + a.legend_padding_top)
					.attr("width", a.legend_bar_width)
					.attr("height", a.legend_bar_height)
					.style("fill", bar_colors(ci));
				
				chart.append("text")
					.attr("x", x + a.legend_bar_width + a.legend_bar_padding_right)
					.attr("y", ci * (a.legend_bar_height + a.legend_vertical_spacing)
						+ a.legend_padding_top + 0.5 * a.legend_bar_height)
					.attr("dx", 0)
					.attr("dy", ".35em") // vertical-align: middle
					.text(t.__categorylabelfn(categories[ci]));
			}
		}
	});
	
	return this;
};
