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
	
	this._super();
	
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
	
	this._super();
	
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
		},
		
		
		/**
		 * Process the series recursively in the breath-first sort order
		 * 
		 * @param callback the callback function
		 * @param internalNodes true to include the series tree internal nodes
		 * @return this
		 */
		processBFS: function(callback, internalNodes) {
			
			var seriesqueue = [];
			seriesqueue.push(this);
			
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
					if (!internalNodes) {
						continue;
					}
				}
				
				callback(series);
			}
		}
	};
});



/*****************************************************************************
 * Class d3bp.Appearance                                                     *
 *****************************************************************************/

d3bp.Appearance = CClass.create(function() {
	
	this._super();
	
	/*
	 * Public
	 */
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
		
		fill: "solid",
		
		
		/**
		 * Create the appearance helper
		 * 
		 * @param svg the SVG component
		 * @return a new instance of d3bp.AppearanceHelper
		 */
		createHelper: function(svg) {
			return new d3bp.AppearanceHelper(this, svg);
		}
	};
});



/*****************************************************************************
 * Class d3bp.AppearanceHelper                                               *
 *****************************************************************************/

d3bp.AppearanceHelper = CClass.create(function(appearance, svg) {
	
	this._super();
	
	var __appearance = appearance;
	var __svg = svg;
	var __defs = null;
	
	var __num_patterns = 0;			// patterns in the definition
	
	if (appearance != undefined) {
		__defs = svg.append('svg:defs');
	}
	
	
	/**
	 * Ensure that the pattern with the given index and all fill patterns with
	 * smaller indices exist in the SVG element's definitions.
	 * 
	 * @param index the pattern index
	 */
	var __ensure_pattern = function(index) {
		if (index >= __num_patterns) {
			for (var pi = __num_patterns; pi <= index; pi++) {
				
				__num_patterns++;
				var pmod = pi % 6;
				var pattern = __defs.append("svg:pattern")
				.attr("id", "pattern-" + pi)
				.attr("x", 0)
				.attr("y", 0)
				.attr("width" , pmod == 3 || pmod == 5 ? 6 : 8)
				.attr("height", pmod == 2 || pmod == 5 ? 6 : 8)
				.attr("patternUnits", "userSpaceOnUse")
				.append("g")
				.style("fill", "none")
				.style("stroke", __appearance.bar_colors(pi))
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
					default:
						throw "Internal error";
				}
			}
		}
	};
	
	
	/*
	 * Public
	 */
	return {
		
		/**
		 * Apply the style to a bar
		 * 
		 * @param bar the d3 bar component
		 * @param categoryIndex the category index
		 * @return this
		 */
		applyStyleToFilledBar: function(bar, categoryIndex) {
			
			// Solid fill
			
			if (__appearance.fill == "solid") {
				bar.style("fill", __appearance.bar_colors(categoryIndex));
				return this;
			}
			
			
			// Pattern fill
			
			if (__appearance.fill == "pattern") {
				
				__ensure_pattern(categoryIndex);
				bar.attr("style",
						 "fill:url(#pattern-" + categoryIndex + ");"
						 + "stroke:" + __appearance.bar_colors(categoryIndex));
				
				return this;
			}
			
			
			// Error
			
			throw "Invalid bar fill type: " + __appearance.fill;
		}
	};
});



/*****************************************************************************
 * Class d3bp.DataValue                                                      *
 *****************************************************************************/

d3bp.DataValue = CClass.create(function() {
	
	this._super();
	
	var __valuefn = null;			// extract the value
	var __valueaxislabel = null;	// the axis label
	var __valuelabelfn = null;		// convert the value to string


	/*
	 * Public
	 */
	return {
		
		
		/**
		 * Set the value function or property, the axis label, and the to-string
		 * function
		 * 
		 * @param property_or_function the name of the property, or a function
		 * @param axislabel the value axis label (optional)
		 * @param strfn the to string function for data labels (optional)
		 * @return this
		 */
		set: function(property_or_function, axislabel, strfn) {
			
			if (property_or_function === undefined) {
				throw "No arguments";
			}
			
			this.extractFunction(property_or_function);
			
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
		 * Set or get the value extract function
		 * 
		 * @param property_or_function the name of the property, or a function
		 * @return the function if no arguments are given, otherwise this
		 */
		extractFunction: function(property_or_function) {
			if (property_or_function === undefined) {
				return __valuefn;
			}
			if (typeof(property_or_function) == "string") {
				__valuefn = function(d) {
					return d[property_or_function];
				};
			}
			else if (typeof(property_or_function) == "function") {
				__valuefn = property_or_function;
			}
			else {
				throw "Invalid type of the argument";
			}
			return this;
		},
		
		
		/**
		 * Set or get the value axis label
		 * 
		 * @param axislabel the value axis label
		 * @return the axis label if no arguments are given, otherwise this
		 */
		axisLabel: function(axislabel) {
			if (axislabel === undefined) {
				return __valueaxislabel;
			}
			__valueaxislabel = axislabel
			return this;
		},
		
		
		/**
		 * Set or get the value to-string function
		 * 
		 * @param strfn the to string function for data labels
		 * @return the axis label if no arguments are given, otherwise this
		 */
		toStringFunction: function(strfn) {
			if (strfn === undefined) {
				return __valuelabelfn;
			}
			__valuelabelfn = strfn
			return this;
		},
		
		
		/**
		 * Extract the value from the datum
		 * 
		 * @param d the datum
		 * @return the extracted value
		 */
		extract: function(d) {
			return __valuefn(d);
		},
		
		
		/**
		 * Convert the value to string
		 * 
		 * @param value the value
		 * @return the string
		 */
		toString: function(value) {
			if (__valuelabelfn == undefined || __valuelabelfn == null) {
				return "" + value;
			}
			else {
				return __valuelabelfn(value);
			}
		}
	};
});



/*****************************************************************************
 * Class d3bp.Scale                                                          *
 *****************************************************************************/

d3bp.Scale = CClass.create(function() {
	
	this._super();
	
	var __scaletype = "linear";
	
	
	/*
	 * Public
	 */
	return {
		
		
		/**
		 * Get or set the scale type
		 * 
		 * @param type the scale type (linear or log)
		 * @return the type if no arguments are given, otherwise return this
		 */
		type: function(type) {
			if (type === undefined) {
				return __scaletype;
			}
			if (type == "linear" || type == "log") {
				__scaletype = type;
			}
			else {
				throw "Invalid scale type: " + type;
			}
			return this;
		},
		
		
		/**
		 * Instantiate
		 * 
		 * @return a new instance of d3bp.InstantiatedScale
		 */
		instantiate: function() {
			return new d3bp.InstantiatedScale(this);
		}
	};
});



/*****************************************************************************
 * Class d3bp.InstantiatedScale                                              *
 *****************************************************************************/

d3bp.InstantiatedScale = CClass.create(function(scale) {
	
	this._super();
	
	var __scale = scale;
	var __scaletype = scale.type();
	
	var __d3scale = null;
	if (__scaletype == "linear") {
		__d3scale = d3.scale.linear();
	}
	else if (__scaletype == "log") {
		__d3scale = d3.scale.log();
	}
	else {
		throw "Unsupported scale type: " + __scaletype;
	}
	
	
	/*
	 * Public
	 */
	return {
		
		
		/**
		 * Get the scale type
		 * 
		 * @return the scale type
		 */
		type: function() {
			return __scaletype;
		},
		
		
		/**
		 * Get the underlying d3 scale or convert a domain value to the
		 * corresponding range value
		 * 
		 * @param value the domain value to convert (optional)
		 * @return the d3 scale if no arguments are given, or the converted value
		 */
		d3scale: function(value) {
			if (value === undefined) {
				return __d3scale;
			}
			else {
				return __d3scale(value);
			}
		},
		
		
		/**
		 * Get or set the domain
		 * 
		 * @param domain the new d3 domain
		 * @return this, or the current domain if no arguments are given
		 */
		domain: function(domain) {
			if (domain === undefined) {
				return __d3scale.domain();
			}
			__d3scale.domain(domain);
			return this;
		},
		
		
		/**
		 * Set the domain from the numerical data
		 * 
		 * @param data the data (an instance of d3bp.Data)
		 * @param value the value wrapper (an instance of d3bp.DataValue)
		 * @param stdev the stdev wrapper (optional)
		 * @param startAtZero whether to start the domain at zero (if applicable)
		 * @param ignoreNegatives true to ignore the negative values
		 * @return this
		 */
		domainFromData: function(data, value, stdev, startAtZero, ignoreNegatives) {
			
			var have_stdev = false;
			if (stdev != undefined && stdev != null) {
				have_stdev = stdev.extractFunction() != null;
			}
			
			var ignoreZero = __scaletype == "log";
			
			var data_max = d3.max(data.data(),
								  function(d) {
									  var v = 1 * value.extract(d);
									  if (!have_stdev) {
										  return v;
									  }
									  else {
										  var s = 1 * stdev.extract(d);
										  return v + s;
									  }
								  });
			
			var data_min = d3.min(data.data(),
								  function(d) {
									  var v = 1 * value.extract(d);
									  if (!have_stdev) {
										  if (ignoreNegatives && v < 0) {
											  return Infinity;
										  }
										  if (ignoreZero && v == 0) {
											  return Infinity;
										  }
										  return v;
									  }
									  else {
										  var s = 1 * stdev.extract(d);
										  if (ignoreNegatives && v - s < 0) {
											  return Infinity;
										  }
										  if (ignoreZero && v - s == 0) {
											  return Infinity;
										  }
										  return v - s;
									  }
								  });
			
			if (__scaletype == "log") {
				__d3scale.domain([0.9 * data_min, 1.1 * data_max]);
			}
			else {
				__d3scale.domain([startAtZero ? 0 : data_min, 1.1 * data_max]);
			}
			
			return this;
		},
		
		
		/**
		 * Get or set the range
		 * 
		 * @param range the new d3 range (will be automatically adjusted)
		 * @return this, or the current range if no arguments are given
		 */
		range: function(range) {
			if (range === undefined) {
				return __d3scale.range();
			}
			__d3scale.range(range).nice();
			return this;
		},
		
		
		/**
		 * Compute the locations of ticks
		 * 
		 * @param num_ticks the desired number of ticks (might be adjusted)
		 * @return the ticks
		 */
		ticks: function(num_ticks) {
			var ticks = __d3scale.ticks(num_ticks);
			if (__scaletype == "log" && ticks.length > num_ticks) {
				ticks = []
				
				ticks.push(__d3scale.domain()[1]);
				while (ticks[ticks.length - 1] / 10 >= __d3scale.domain()[0]) {
					ticks.push(ticks[ticks.length - 1] / 10);
				}
			}
			return ticks;
		}
	};
});



/*****************************************************************************
 * Class d3bp.BoundingBox                                                    *
 *****************************************************************************/

d3bp.BoundingBox = CClass.create(function() {
	
	this._super();
	
	
	/*
	 * Public
	 */
	return {
		
		x1: Infinity,
		y1: Infinity,
		x2: -Infinity,
		y2: -Infinity,
		
		
		/**
		 * Return the box width
		 * 
		 * @return the width
		 */
		width: function() {
			return this.x2 - this.x1;
		},
		
		
		/**
		 * Return the box height
		 * 
		 * @return the height
		 */
		height: function() {
			return this.y2 - this.y1;
		},
		
		
		/**
		 * Update from an instance of another bounding box
		 * 
		 * @return bbox the other bounding box
		 * @return this
		 */
		updateFromBoundingBox: function(bbox) {
			
			// Is it d3bp.BoundingBox?
			
			if (bbox.x1 != undefined && bbox.x2 != undefined
				&& bbox.y1 != undefined && bbox.y2 != undefined) {
				
				if (bbox.x1 < this.x1) {
					this.x1 = bbox.x1;
				}
				
				if (bbox.y1 < this.y1) {
					this.y1 = bbox.y1;
				}
				
				if (bbox.x2 > this.x2) {
					this.x2 = bbox.x2;
				}
				
				if (bbox.y2 > this.y2) {
					this.y2 = bbox.y2;
				}
				
				return this;
			}
			
			
			// Is it SVGRect?
			
			if (bbox.x != undefined && bbox.width != undefined
				&& bbox.y != undefined && bbox.height != undefined) {
				
				if (bbox.x < this.x1) {
					this.x1 = bbox.x;
				}
				
				if (bbox.y < this.y1) {
					this.y1 = bbox.y;
				}
				
				if (bbox.x + bbox.width > this.x2) {
					this.x2 = bbox.x + bbox.width;
				}
				
				if (bbox.y + bbox.height > this.y2) {
					this.y2 = bbox.y + bbox.height;
				}
				
				return this;
			}
			
			
			// Is it ClientRect?
			
			if (bbox.left != undefined && bbox.right != undefined
				&& bbox.top != undefined && bbox.bottom != undefined) {
				
				if (bbox.left < this.x1) {
					this.x1 = bbox.left;
				}
				
				if (bbox.top < this.y1) {
					this.y1 = bbox.top;
				}
				
				if (bbox.right > this.x2) {
					this.x2 = bbox.right;
				}
				
				if (bbox.bottom > this.y2) {
					this.y2 = bbox.bottom;
				}
				
				return this;
			}
			
			
			// Something else?
			
			throw "Invalid or unrecognized bounding box type";
		},
		
		
		/**
		 * Update the bounding box using BBox of an *untranslated* d3 component
		 *
		 * @param component the d3 component
		 * @return this
		 */
		updateFromUntranslatedD3: function(component) {
			
			for (var ti = 0; ti < component[0].length; ti++) {
				var r = component[0][ti].getBBox();
				if (r.x < this.x1) {
					this.x1 = r.x;
				}
				if (r.y < this.y1) {
					this.y1 = r.y;
				}
				if (r.x + r.width > this.x2) {
					this.x2 = r.x + r.width;
				}
				if (r.y + r.height > this.y2) {
					this.y2 = r.y + r.height;
				}
			}
			
			return this;
		},
		
		
		/**
		 * Update the bounding box using BBox of a translated d3 component
		 *
		 * @param component the d3 component
		 * @param x the X coordinate
		 * @param y the Y coordinate
		 * @return this
		 */
		updateFromTranslatedD3: function(component, x, y) {
			
			if (x === undefined) {
				 x = 0;
			}
			if (y === undefined) {
				y = 0;
			}
			
			for (var ti = 0; ti < component[0].length; ti++) {
				var r = component[0][ti].getBoundingClientRect();
				if (x - r.width < this.x1) {
					this.x1 = x - r.width;
				}
				if (y - r.height < this.y1) {
					this.y1 = y - r.height;
				}
				if (x + r.width > this.x2) {
					this.x2 = x + r.width;
				}
				if (y + r.height > this.y2) {
					this.y2 = y + r.height;
				}
			}
			
			return this;
		}
	};
});



/*****************************************************************************
 * Class d3bp.InstantiatedChart                                              *
 *****************************************************************************/

d3bp.InstantiatedChart = CClass.create(function(chart, anchorId) {
	
	this._super();
	
	var __chart = chart;
	var __bbox  = new d3bp.BoundingBox();
	
	var __anchor_id = anchorId;
	var __svg_root = null;
	var __container = null;
	
	var __appearanceHelper = null;
	
	if (chart != undefined) {
		__svg_root = d3.select("#" + __anchor_id).append("svg");
		__container = __svg_root.append("g");
		__appearanceHelper = __chart.appearance().createHelper(__container);
	}
	
	
	/*
	 * Public
	 */
	return {
		
		
		/**
		 * Get the chart
		 * 
		 * @return the chart
		 */
		chart: function() {
			return __chart;
		},
		
		
		/**
		 * Get the bounding box
		 * 
		 * @return the bounding box
		 */
		boundingBox: function() {
			return __bbox;
		},
		
		
		/**
		 * Get the SVG root element
		 * 
		 * @return the SVG root element
		 */
		svgRootElement: function() {
			return __svg_root;
		},
		
		
		/**
		 * Get the SVG content container
		 * 
		 * @return the container
		 */
		container: function() {
			return __container;
		},
		
		
		/**
		 * Get the corresponding instance of the d3bp.AppearanceHelper
		 * 
		 * @return the appearance helper
		 */
		appearanceHelper: function() {
			return __appearanceHelper;
		}
	};
});



/*****************************************************************************
 * Class d3bp.AbstractChartElement                                           *
 *****************************************************************************/

d3bp.AbstractChartElement = CClass.create(function(instantiatedChart) {
	
	this._super();
	
	var __instantiatedChart = instantiatedChart;
	var __bbox  = new d3bp.BoundingBox();
	
	
	/*
	 * Public
	 */
	return {
		
		
		/**
		 * Get the parent chart
		 * 
		 * @return the parent chart
		 */
		chart: function() {
			return __instantiatedChart.chart();
		},
		
		
		/**
		 * Get the instantiated parent chart
		 * 
		 * @return the instantiated parent chart
		 */
		instantiatedChart: function() {
			return __instantiatedChart;
		},
		
		
		/**
		 * Get the bounding box
		 * 
		 * @return the bounding box
		 */
		boundingBox: function() {
			return __bbox;
		}
	};
});



/*****************************************************************************
 * Class d3bp.Legend                                                         *
 *****************************************************************************/

d3bp.Legend = d3bp.AbstractChartElement.extend(function(chart) {
	
	this._super(chart);
	
	
	/*
	 * Public
	 */
	return {
		
		
		/**
		 * Render the legend
		 * 
		 * @param categories a list of data values describing their categories
		 * @param categoryWrapper the corresponding instance of d3bp.DataValue
		 * @return this
		 */
		render: function(categories, categoryWrapper) {
			
			if (categories.length == 0) {
				return this;
			}
			
			var a = this.chart().appearance();
			var boundingBox = this.boundingBox();
			var appearanceHelper = this.instantiatedChart().appearanceHelper();
			var chart_inner_width = this.instantiatedChart().innerWidth();
			
			var x = chart_inner_width + a.bars_margin + a.chart_margin + a.legend_padding_left;
			
			var container = this.instantiatedChart().container().append("g");
			
			for (var ci = 0; ci < categories.length; ci++) {
				var c = categories[ci];
				
				var bar = container.append("rect")
				.attr("x", x)
				.attr("y", ci * (a.legend_bar_height + a.legend_vertical_spacing) + a.legend_padding_top)
				.attr("width", a.legend_bar_width)
				.attr("height", a.legend_bar_height);
				
				appearanceHelper.applyStyleToFilledBar(bar, ci);
				boundingBox.updateFromUntranslatedD3(bar);
				
				var text = container.append("text")
				.attr("x", x + a.legend_bar_width + a.legend_bar_padding_right)
				.attr("y", ci * (a.legend_bar_height + a.legend_vertical_spacing)
				+ a.legend_padding_top + 0.5 * a.legend_bar_height)
				.attr("dx", 0)
				.attr("dy", ".35em") // vertical-align: middle
				.text(categoryWrapper.toString(categories[ci]));
				
				boundingBox.updateFromUntranslatedD3(text);
			}
			
			return this;
		}
	};
});



/*****************************************************************************
 * Class d3bp.AbstractChart                                                  *
 *****************************************************************************/

d3bp.AbstractChart = CClass.create(function() {
	
	this._super();
	
	var __data = null;							// the data
	var __appearance = new d3bp.Appearance();	// the appearance specs
	
	
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
			if (data === undefined) {
				return __data;
			}
			__data = data;
			return this;
		},
		
		
		/**
		 * Set or return the chart appearance object
		 * 
		 * @param appearance the new appearance
		 * @return the appearance if no arguments are given, otherwise this
		 */
		appearance: function(appearance) {
			if (appearance === undefined) {
				return __appearance;
			}
			__appearance = appearance;
			return this;
		}
	};
});



/*****************************************************************************
 * Class d3bp.BarChart                                                       *
 *****************************************************************************/

d3bp.BarChart = d3bp.AbstractChart.extend(function() {
	
	this._super();
	
	var __value = new d3bp.DataValue();			// the data value (usually y)
	var __stdev = new d3bp.DataValue();			// the standard deviation
	var __label = new d3bp.DataValue();			// the label value (usually x)
	var __category = new d3bp.DataValue();		// the category
	
	var __scale = new d3bp.Scale();				// the scale specs
	
	var __stacked = false;
	
	var __inner_height = 400;
	var __num_ticks = 10;
	
	
	/*
	 * Public
	 */
	return {
		
		/**
		 * Set the value function or property, the axis label, and the to-string
		 * function (usually for the Y axis)
		 * 
		 * @param property_or_function the name of the property, or a function
		 * @param axislabel the value axis label (optional)
		 * @param strfn the to string function for data labels (optional)
		 * @return this, or the data wrapper if no arguments are given
		 */
		value: function(property_or_function, axislabel, strfn) {
			if (property_or_function === undefined) {
				return __value;
			}
			__value.set(property_or_function, axislabel, strfn);
			return this;
		},
		
		
		/**
		 * Set the standard deviation function or property
		 * 
		 * @param property_or_function the name of the property, or a function
		 * @return this, or the data wrapper if no arguments are given
		 */
		stdev: function(property_or_function) {
			if (property_or_function === undefined) {
				return __stdev;
			}
			__stdev.set(property_or_function, null, null);
			return this;
		},
		
		
		/**
		 * Set the label function or property (usually for the X axis)
		 * 
		 * @param property_or_function the name of the property, or a function
		 * @return this, or the data wrapper if no arguments are given
		 */
		label: function(property_or_function) {
			if (property_or_function === undefined) {
				return __label;
			}
			__label.set(property_or_function, null, null);
			return this;
		},
		
		
		/**
		 * Set the function or property that determines the category, as in what
		 * appears in the legend and what determines how each bar should be
		 * colored
		 * 
		 * @param property_or_function the name of the property, or a function
		 * @param labelfn the label function to be used in the legend (optional)
		 * @return this, or the data wrapper if no arguments are given
		 */
		category: function(property_or_function, labelfn) {
			if (property_or_function === undefined) {
				return __category;
			}
			__category.set(property_or_function, null, labelfn);
			return this;
		},
		
		
		/**
		 * Set or get the scale type
		 * 
		 * @param type the new scale type
		 * @return the scale wrapper if no arguments are given, otherwise this
		 */
		scale: function(type) {
			if (type === undefined) {
				return __scale;
			}
			__scale.type(type);
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
			
			if (this.data() == null) {
				throw "The data has not yet been specified";
			}
			
			if (__value.extractFunction() == null) {
				throw "The value extract function has not been specified";
			}
			
			var instantiatedChart = d3bp.InstantiatedChart.extend(function() {
				
				this._super(t, id);
				
				var chart_inner_width = null;
				
				
				/**
				 * Public
				 */
				return {
					
					/**
					 * Get the inner width of the chart
					 * 
					 * @return the inner width
					 */
					innerWidth: function() {
						return chart_inner_width;
					},
					
					
					/**
					 * Get the inner height of the chart
					 * 
					 * @return the inner width
					 */
					innerHeight: function() {
						return __inner_height;
					},
					
					
					/**
					 * Render
					 */
					render: function() {
						
						var chart_svg = this.svgRootElement();
						var chart = this.container();
						
						var data = t.data();
						var num_bars = data.data().length;
						var boundingBox = this.boundingBox();
						
						var a = t.appearance();
						var appearanceHelper = this.appearanceHelper();
						
						
						//
						// Further initialization
						//
						
						chart_svg.attr("class", "chart");
						
						
						//
						// Data scale
						//
						
						var scale = __scale.instantiate()
						.domainFromData(data, __value, __stdev, true, true)
						.range([__inner_height, 0]);
						var ticks = scale.ticks(__num_ticks);
						
						
						//
						// Data axis
						//
						
						var h_tick_lines = chart.selectAll("line")
						.data(ticks)
						.enter().append("line")
						.attr("x1", -a.bars_margin)
						.attr("x2", num_bars * a.bar_width + a.bars_margin)
						.attr("y1", scale.d3scale())
						.attr("y2", scale.d3scale())
						.style("stroke", "#ccc");
						
						var data_ticks_text = chart.selectAll(".rule")
						.data(ticks)
						.enter().append("text")
						.attr("class", "rule")
						.attr("x", -a.chart_margin-a.bars_margin)
						.attr("y", scale.d3scale())
						.attr("dx", 0)
						.attr("dy", ".35em")
						.attr("text-anchor", "end")
						.text(String);
						
						boundingBox.updateFromUntranslatedD3(data_ticks_text);
						
						chart.append("line")
						.attr("x1", -a.bars_margin)
						.attr("y1", 0)
						.attr("x2", -a.bars_margin)
						.attr("y2", __inner_height)
						.style("stroke", "#000");
						
						if (__value.axisLabel() != undefined && __value.axisLabel() != null) {
							var left = boundingBox.x1 - a.ylabel_from_data_tick_text;
							var text = chart.append("text")
							.attr("x", 0)
							.attr("y", 0)
							.attr("dx", 0)
							.attr("dy", 0)
							.attr("text-anchor", "middle")
							.attr("transform", "translate(" + left + ", "
							+ (__inner_height/2)  + ") rotate(-90)")
							.text(__value.axisLabel());
							boundingBox.updateFromTranslatedD3(text, left, __inner_height/2);
						}
						
						
						//
						// Process the series recursively
						//
						
						var index = 0;
						var pos = 0;
						var lastparent = null;
						var categories = [];
						
						var seriesinfo = [];
						var max_level = 0;
						
						data.seriesRoot().processBFS(function(series) {
							
							var series_data = series.data();
							if (series_data.length == 0) {
								return;
							}
							
							if (series.parent() != lastparent) {
								lastparent = series.parent();
								if (index > 0) {
									pos += a.bar_width;
								}
							}
							
							for (var di = 0; di < series_data.length; di++) {
								var d = series_data[di];
								var value = 1 * __value.extract(d);
								var stdev = __stdev.extractFunction() == null ? NaN : 1 * __stdev.extract(d);
								var label = __label.extractFunction() == null ? null : __label.extract(d);
								var category = __category.extractFunction() == null ? null : __category.extract(d);
								
								
								// Data
								
								var bar = chart.append("rect")
								.attr("x", pos)
								.attr("y", scale.d3scale(value))
								.attr("width", a.bar_width)
								.attr("height", __inner_height - scale.d3scale(value));
								
								
								// Category
								
								if (category != undefined && category != null) {
									var category_index = categories.indexOf(category);
									if (category_index < 0) {
										category_index = categories.length;
										categories.push(category);
									}
									
									appearanceHelper.applyStyleToFilledBar(bar, category_index);
								}
								
								
								// Error bars
								
								if (!isNaN(stdev) && stdev > 0) {
									var top = value + stdev;
									var bottom = value - stdev;
									if (bottom < scale.d3scale().domain()[0]) {
										bottom = scale.d3scale().domain()[0];
									}
									
									// Middle
									chart.append("line")
									.style("stroke", "#000")
									.attr("x1", pos + 0.5 * a.bar_width)
									.attr("x2", pos + 0.5 * a.bar_width)
									.attr("y1", scale.d3scale(top))
									.attr("y2", scale.d3scale(bottom));
									
									// Top
									chart.append("line")
									.style("stroke", "#000")
									.attr("x1", pos + 0.25 * a.bar_width)
									.attr("x2", pos + 0.75 * a.bar_width)
									.attr("y1", scale.d3scale(top))
									.attr("y2", scale.d3scale(top));
									
									// Bottom
									chart.append("line")
									.style("stroke", "#000")
									.attr("x1", pos + 0.25 * a.bar_width)
									.attr("x2", pos + 0.75 * a.bar_width)
									.attr("y1", scale.d3scale(bottom))
									.attr("y2", scale.d3scale(bottom));
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
								
								if (__value.toStringFunction() != undefined && __value.toStringFunction != null) {
									if (!isNaN(stdev) && stdev > 0) {
										var top = value + stdev;
									}
									else {
										var top = value;
									}
									var x = pos +  0.5 * a.bar_width;
									var y = scale.d3scale(top) - 10;
									var text = chart.append("text")
									.attr("x", 0)
									.attr("y", 0)
									.attr("dx", 0)
									.attr("dy", ".35em") // vertical-align: middle
									.attr("transform", "translate(" + x + ", " + y + ") rotate(-90)")
									.text(__value.toString(value));
									boundingBox.updateFromTranslatedD3(text, x, y);
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
										info.min_pos = Infinity;
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
						});
						
						
						//
						// Adjust the tick lines
						//
						
						chart_inner_width = pos;
						for (var ti = 0; ti < h_tick_lines[0].length; ti++) {
							h_tick_lines[0][ti].setAttribute("x2", chart_inner_width + a.bars_margin);
						}
						
						
						//
						// Prepare for the upcoming chart adjustments
						//
						
						if (__inner_height > boundingBox.y2) {
							boundingBox.y2 = __inner_height;
						}
						if (chart_inner_width + a.bars_margin > boundingBox.x2) {
							boundingBox.x2 = chart_inner_width + a.bars_margin;
						}
						
						
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
						
						var label_at_inv_levels = [];
						
						data.seriesRoot().processBFS(function(series) {
							
							var series_data = series.data();
							if (series_data.length == 0) {
								return;
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
							
						}, true /* include the internal series tree nodes */);
					
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
								if (l.x_pos + w > boundingBox.x2) {
									boundingBox.x2 = l.x_pos + w;
								}
							}
							
							if (max_height + label_y_pos > boundingBox.y2) {
								boundingBox.y2 = max_height + label_y_pos;
							}
							
							label_y_pos += max_height + 4;
						}
						
						
						//
						// Legend
						//
						
						if (categories.length > 1) {
							var legend = new d3bp.Legend(this);
							legend.render(categories, __category);
							boundingBox.updateFromBoundingBox(legend.boundingBox());
						}
						
						
						//
						// Adjust the chart size
						//
						
						chart_svg[0][0].setAttribute("width" , boundingBox.width());
						chart_svg[0][0].setAttribute("height", boundingBox.height());
						chart[0][0].setAttribute("transform",
												 "translate(" + (-boundingBox.x1) + ", " + (-boundingBox.y1) + ")");
					}
				};
			});
			
			this.data().run(function() { (new instantiatedChart()).render(); });
			
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
