<%@ page import="com.tinkerpop.bench.web.ChartProperties"%>

<script language="JavaScript">
	<!-- Begin
	
	function class_boxplot() {
		this.quantile25 = 0;
		this.quantile50 = 0;
		this.quantile75 = 0;
		this.whisker_high = 0;
		this.whisker_low = 0;
		this.outliers = [];
		this.category = null;
	}
	
	// Computes the interquartile range
	// Inspired by: http://mbostock.github.com/d3/ex/box.html
	function iqr(d, q, k) {
		var q1 = q.quantile25,
			q3 = q.quantile75,
			iqr = (q3 - q1) * k,
			i = -1,
			j = d.length;
		while (d[++i] < q1 - iqr);
		while (d[--j] > q3 + iqr);
		return [i, j];
	}
	
	function boxplot() {
		
		var chart_inner_height = 420;
		var chart_margin = 10;
		var num_ticks = 10;
		
		var padding_left = 100;
		var padding_right = 300;
		var padding_top = 50;
		var padding_bottom = 150;
		
		var band_width = 20;
		var band_colors = d3.scale.category10();
		var bands_margin = 10;
		
		var ylabel_from_chart_margin = 40;
			
		var legend_padding_left = 10;
		var legend_padding_top = 10;
		var legend_band_padding_right = 4;
		var legend_band_width = 2 * band_width;
		var legend_band_height = band_width;
		var legend_vertical_spacing = 2;
		
		var boxplot_width = 0.5;
				
		
		<% if (chartProperties.smallGraph) { %>
			chart_inner_height /= 2;
			padding_right /= 2;
			padding_bottom /= 2;
			num_ticks /= 2;
			legend_padding_left = 0;
			legend_band_width /= 1.5;
		<% } %>
		
		

		<% if (chartProperties.group_by != null) { %>
			
			var group_label_function = function(d, i) {
				<%= chartProperties.group_label_function %>;
			};
			
			var subgroup_label_function = function(d, i) {
				<%= chartProperties.subgroup_label_function == null
						? "return null" : chartProperties.subgroup_label_function %>;
			};
			
			var category_label_function = function(d, i) {
				<%= chartProperties.category_label_function %>;
			};

		<% } else { %>
			
			var group_label_function = function(d, i) {
				return null;
			};
			
			var subgroup_label_function = function(d, i) {
				return null;
			};
			
			var category_label_function = function(d, i) {
				return null;
			};

		<% } %>



		//
		// Load the data and create the chart
		//

		d3.csv('<%= chartProperties.source %>', function(data) {
		
		
			//
			// Prepare and filter the data
			//
			
			data.forEach(function(d, i) {
			
				<%= chartProperties.foreach %>;
				d.time = (+d.time) / 1000000.0;			// convert to ms
				
				tokens = d.result.split(":");
				result_tokens = new Array();
				for (var t = 0; t < tokens.length; t++) {
					var token = tokens[t].split("=");
					if (token.length > 1) {
						result_tokens[token[0]] = parseFloat(token[1]);
						result_tokens[t] = parseFloat(tokens[t]);
					}
					else {
						result_tokens[t] = parseFloat(tokens[t]);
					}
				}
				d.result_string = d.result;
				d.result = result_tokens;
				
				d._index = i;
				d._value = <%= chartProperties.yvalue %>;
				//if (i < 10) console.log(d);
			});
			
			data = data.filter(function(d) {
				if (d._value == Infinity || isNaN(d._value)) return false;
				return <%= chartProperties.filter %>;
			});
	
	
		
			//
			// X Scale
			//
			
			var labels = data.map(function(d) { return d.label; });
			
			var x = d3.scale.ordinal()
					  .domain(labels);
			x.rangeBands([0, band_width * x.domain().length]);
			
			
			
			//
			// Create the chart
			//
			
			var chart = d3.select(".<%= chartProperties.attach %>").append("svg")
						  .attr("class", "chart")
						  .attr("width",  band_width * x.domain().length + padding_left + padding_right)
						  .attr("height", chart_inner_height + padding_top + padding_bottom)
						  .append("g")
						  	.attr("transform", "translate(" + padding_left + ", " + padding_top + ")");
			
			
			draw_chart(data, chart, x);
		});
			
		
		
		//
		// Function to draw the data
		//
		
		function draw_chart(data, chart, x) {
		
			//
			// Set up fill patterns
			//
			
			// TODO
			//   http://www.carto.net/svg/samples/patterns.shtml
			//   http://bl.ocks.org/1178682
			
				
			//
			// Drop extreme values
			//
			
			<% if (chartProperties.dropTopBottomExtremes) { %>
							
				x.domain().forEach(function(domain) {
					if (domain.indexOf("----") == 0) return;
				
					domain_data = data.filter(function(d) {
						return d.label == domain;
					});
					d = domain_data.map(function(d) { return d._value; }).sort(d3.ascending);
					
					min = d3.quantile(d, .05);
					max = d3.quantile(d, .95);
				
					data = data.filter(function(d) {
						return d.label != domain || (d._value >= min && d._value <= max);
					});
				});
				
			<% } %>
			
			
				
			//
			// Determine the quantiles for each element in the domain
			//
			
			var boxplots = new Array();
							
			x.domain().forEach(function(domain) {
				if (domain.indexOf("----") == 0) return;
			
				domain_data = data.filter(function(d) {
					return d.label == domain;
				});
				d = domain_data.map(function(d) { return d._value; }).sort(d3.ascending);
				
				var q = new class_boxplot();
				
				q.quantile25 = d3.quantile(d, .25);
				q.quantile50 = d3.quantile(d, .50);
				q.quantile75 = d3.quantile(d, .75);
				
				w = iqr(d, q, 1.5);
				q.whisker_low = d[w[0]];
				q.whisker_high = d[w[1]];
				
				for (var i = 0; i < w[0]; i++) q.outliers.push(d[i]);
				for (var i = w[1] + 1; i < d.length; i++) q.outliers.push(d[i]);
				
				q.category = category_label_function(domain_data[0], domain_data[0]._index);
				boxplots[domain] = q;
			});
		
		
			
			//
			// Y Scale
			//
			
			var data_scale = "<%= chartProperties.yscale %>";
			var y = d3.scale.<%= chartProperties.yscale %>()
					  .domain([<%= "log".equals(chartProperties.yscale)
						  ? "0.9 * d3.min(data, function(d) { "
						    + "  if (d.label.indexOf('----') == 0) return 1000 * 1000 * 1000;"
							+ "  return d._value;"
						  	+ "})"
						  : "0" %>, 
					  	      1.1 * d3.max(data, function(d) { return d._value; })])
					  .range([chart_inner_height, 0])
					  .nice();
	
	
	
			//
			// The vertical ruler (ticks) and the axis
			//
			
			var ticks = y.ticks(num_ticks);
			if (data_scale == "log" && ticks.length > num_ticks) {
				t = new Array();
				
				/*for (var i = 0; i < ticks.length; i += Math.floor(ticks.length / num_ticks)) {
					t.push(ticks[i]);
				}*/
				
				t.push(y.domain()[1]);
				while (t[t.length - 1] / 10 >= y.domain()[0]) {
					t.push(t[t.length - 1] / 10);
				}
				
				ticks = t;
			}
			
			chart.selectAll("line")
				 .data(ticks)
				 .enter().append("line")
				 .attr("x1", -bands_margin)
				 .attr("x2", x.domain().length * band_width + bands_margin)
				 .attr("y1", y)
				 .attr("y2", y)
				 .style("stroke", "#ccc");
			
			chart.selectAll(".rule")
				 .data(ticks)
				 .enter().append("text")
				 .attr("class", "rule")
				 .attr("x", -chart_margin-bands_margin)
				 .attr("y", y)
				 .attr("dx", 0)
				 .attr("dy", ".35em")
				 .attr("text-anchor", "end")
				 .text(function(d) {
					return d;
				 });
			
			chart.append("line")
				 .attr("x1", -bands_margin)
				 .attr("y1", 0)
				 .attr("x2", -bands_margin)
				 .attr("y2", chart_inner_height)
				 .style("stroke", "#000");
				 
			chart.append("text")
				 .attr("x", 0)
				 .attr("y", 0)
				 .attr("dx", 0)
				 .attr("dy", 0)
				 .attr("text-anchor", "middle")
				 .attr("transform", "translate("
				 	+ (-bands_margin*2 - ylabel_from_chart_margin) + ", "
				 	+ (chart_inner_height/2)  + ") rotate(-90)")
				 .text("<%= chartProperties.ylabel %>");
			
			
			
			//
			// Group labels and legend
			//
			
			<%
				// Group labels and legend
				
				// TODO Bars within a group need different colors + we need a legend
				
				if (chartProperties.group_label_function == null) {
					chartProperties.group_label_function = "return d." + chartProperties.group_by;
				}
				
				if (chartProperties.group_by != null) {
					%>
					
						// Get the groups and the categories 
						
						var group_lengths = [];
						var group_columns = [];
						var group_offsets = [];
						var group_labels = [];
						
						var subgroup_lengths = [];
						var subgroup_columns = [];
						var subgroup_offsets = [];
						
						var categories = [];
						
						var __last_group_column = "";
						var __last_group_label = "";
						var __last_group_length = -1;
						
						data.forEach(function(d, i) {
						
							var g = d.<%= chartProperties.group_by %>;
							if (g != __last_group_column) {
								if (__last_group_length > 0) {
									group_columns.push(__last_group_column);
									group_lengths.push(__last_group_length);
									group_labels.push(group_label_function(data[i-1], i-1));
								}
								if (__last_group_length == -1) {
									group_offsets.push(0);
								}
								else {
									group_offsets.push(group_offsets[group_offsets.length-1] + __last_group_length);
								}
								__last_group_column = g;
								__last_group_length = 0;
							}
							if (__last_group_label != d.label) __last_group_length++;
							
							if (g != "" && g.indexOf("----") != 0) {
								var c = category_label_function(d, i);
								if (categories.indexOf(c) < 0) categories.push(c);
							}
							
							__last_group_label = d.label;
						});
						
						if (__last_group_length > 0) {
							group_columns.push(__last_group_column)
							group_lengths.push(__last_group_length)
							group_labels.push(
								group_label_function(data[data.length-1], data.length-1));
						}
					
					
						<% if (chartProperties.subgroup_by != null) { %>
						
							var __last_subgroup_column = "";
							var __last_subgroup_length = -1;
							var __last_subgroup_label = "";
							var __last_subgroup_column_major = "";
							
							data.forEach(function(d, i) {
							
								var g = d.<%= chartProperties.subgroup_by %>;
								if (g != __last_subgroup_column || d.<%= chartProperties.group_by %> != __last_subgroup_column_major) {
									if (__last_subgroup_length > 0) {
										subgroup_columns.push(__last_subgroup_column)
										subgroup_lengths.push(__last_subgroup_length)
									}
									if (__last_subgroup_length == -1) {
										subgroup_offsets.push(0);
									}
									else {
										subgroup_offsets.push(subgroup_offsets[subgroup_offsets.length-1] + __last_subgroup_length);
									}
									__last_subgroup_column = g;
									__last_subgroup_length = 0;
									__last_subgroup_column_major = d.<%= chartProperties.group_by %>;
								}
								if (__last_subgroup_label != d.label) __last_subgroup_length++;
								
								__last_subgroup_label = d.label;
							});
							
							if (__last_subgroup_length > 0) {
								subgroup_columns.push(__last_subgroup_column)
								subgroup_lengths.push(__last_subgroup_length)
							}
						<% } %>
						
							
						var __longest_subgroup_name = "";
						
						<% if (chartProperties.subgroup_by != null) { %>
						
							// Subgroup labels
							
							for (var i = 0; i < subgroup_columns.length; i++) {
								if (subgroup_columns[i] == "" || subgroup_columns[i].indexOf("----") == 0) continue;
								var p = subgroup_offsets[i] + 0.5 * subgroup_lengths[i] - 0.5;
								var t = subgroup_label_function(data[subgroup_offsets[i]], subgroup_offsets[i]);
								if (t.length > __longest_subgroup_name.length) __longest_subgroup_name = t;
								//console.log("" + p + " --> " + t);
								
								chart.append("text")
								 .attr("x", 0)
								 .attr("y", 0)
								 .attr("dx", 0)
								 .attr("dy", ".35em") // vertical-align: middle
								 .attr("transform", "translate("
								 	+ (x.rangeBand() * p + x.rangeBand() / 2) + ", "
								 	+ (chart_inner_height + chart_margin)  + ") rotate(45)")
								 .text(t);
							}
						<% } %>
						
						var __dy = 0;
						
						if (__longest_subgroup_name != "") {
							__dy = __longest_subgroup_name.length * 5 + 10;
						}
						
						
						// Group labels
						
						for (var i = 0; i < group_columns.length; i++) {
							if (group_columns[i] == "" || group_columns[i].indexOf("----") == 0) continue;
							var p = group_offsets[i] + 0.5 * group_lengths[i] - 0.5;
							
							if (__longest_subgroup_name != "" && __last_subgroup_length >= 6) {
								chart.append("text")
								 .attr("x", 0)
								 .attr("y", 0)
								 .attr("dx", 0)
								 .attr("dy", ".35em") // vertical-align: middle
								 .attr("text-anchor", "middle")
								 .attr("transform", "translate("
								 	+ (x.rangeBand() * p + x.rangeBand() / 2) + ", "
								 	+ (chart_inner_height + chart_margin + __dy)  + ") rotate(0)")
								 .text(group_labels[i]);
							}
							else {
								chart.append("text")
								 .attr("x", 0)
								 .attr("y", 0)
								 .attr("dx", 0)
								 .attr("dy", ".35em") // vertical-align: middle
								 .attr("transform", "translate("
								 	+ (x.rangeBand() * p + x.rangeBand() / 2) + ", "
								 	+ (chart_inner_height + chart_margin + __dy)  + ") rotate(45)")
								 .text(group_labels[i]);
							}	 
						}
						
						
						// Lines separating the variuos groups
						
						for (var i = 0; i < group_columns.length; i++) {
							if (group_columns[i] == "" || group_columns[i].indexOf("----") == 0) continue;
							var p = group_offsets[i] + 1 * group_lengths[i] + 0.5;
							
							chart.append("line")
								 .attr("x1", x.rangeBand() * p)
								 .attr("x2", x.rangeBand() * p)
								 .attr("y1", 0)
								 .attr("y2", chart_inner_height)
								 .style("stroke", "#ccc");
						}
						
						
						// Legend
						
						for (var i = 0; i < categories.length; i++) {
						
							chart.append("rect")
								 .attr("x", band_width * x.domain().length + bands_margin + chart_margin + legend_padding_left)
								 .attr("y", i * (legend_band_height + legend_vertical_spacing) + legend_padding_top)
								 .attr("width", legend_band_width)
								 .attr("height", legend_band_height)
								 .style("fill", band_colors(i));
						
							chart.append("text")
								 .attr("x", band_width * x.domain().length + bands_margin + chart_margin
								 			+ legend_padding_left + legend_band_width + legend_band_padding_right)
								 .attr("y", (i + 0.5) * (legend_band_height + legend_vertical_spacing) + legend_padding_top)
								 .attr("dx", 0)
							 	 .attr("dy", ".35em") // vertical-align: middle
								 .text(categories[i]);
						}
					<%
				}
			%>
			
			
			
			//
			// Data and labels
			//
				 
			x.domain().forEach(function(d, i) {
			
				if (d.indexOf("----") == 0) return;

				<% if (chartProperties.group_by != null) { %>
					var stroke = band_colors(categories.indexOf(boxplots[d].category));
				<% } else { %>
					var stroke = "#000";
				<% } %>
				
				chart.append("rect")
				 .attr("x", 0.5 * (1 - boxplot_width) * x.rangeBand() + x(d))
				 .attr("y", y(boxplots[d].quantile75))
				 .attr("width", boxplot_width * x.rangeBand())
				 .attr("height", y(boxplots[d].quantile25) - y(boxplots[d].quantile75))
				 .style("fill", "none")
				 .style("stroke", stroke);
				
				chart.append("line")
				 .attr("x1", 0.5 * (1 - boxplot_width) * x.rangeBand() + x(d))
				 .attr("x2", (1 - 0.5 * (1 - boxplot_width)) * x.rangeBand() + x(d))
				 .attr("y1", y(boxplots[d].quantile50))
				 .attr("y2", y(boxplots[d].quantile50))
				 .style("stroke", stroke);
				
				chart.append("line")
				 .attr("x1", 0.5 * x.rangeBand() + x(d))
				 .attr("x2", 0.5 * x.rangeBand() + x(d))
				 .attr("y1", y(boxplots[d].quantile25))
				 .attr("y2", y(boxplots[d].whisker_low))
				 .style("stroke", stroke);
				
				chart.append("line")
				 .attr("x1", 0.5 * x.rangeBand() + x(d))
				 .attr("x2", 0.5 * x.rangeBand() + x(d))
				 .attr("y1", y(boxplots[d].whisker_high))
				 .attr("y2", y(boxplots[d].quantile75))
				 .style("stroke", stroke);
				
				chart.append("line")
				 .attr("x1", 0.5 * (1 - boxplot_width) * x.rangeBand() + x(d))
				 .attr("x2", (1 - 0.5 * (1 - boxplot_width)) * x.rangeBand() + x(d))
				 .attr("y1", y(boxplots[d].whisker_low))
				 .attr("y2", y(boxplots[d].whisker_low))
				 .style("stroke", stroke);
				
				chart.append("line")
				 .attr("x1", 0.5 * (1 - boxplot_width) * x.rangeBand() + x(d))
				 .attr("x2", (1 - 0.5 * (1 - boxplot_width)) * x.rangeBand() + x(d))
				 .attr("y1", y(boxplots[d].whisker_high))
				 .attr("y2", y(boxplots[d].whisker_high))
				 .style("stroke", stroke);
				 
				boxplots[d].outliers.forEach(function(outlier) {

					chart.append("circle")
					 .attr("cx", 0.5 * x.rangeBand() + x(d))
					 .attr("cy", y(outlier))
					 .attr("r", 0.125 * x.rangeBand())
					 .style("stroke", stroke)
					 .style("fill", "none");
				});
			
				<%
					if (chartProperties.group_by == null) {
						%>
							chart.append("text")
							 .attr("x", 0)
							 .attr("y", 0)
							 .attr("dx", 0) // padding-right
							 .attr("dy", ".35em") // vertical-align: middle
							 .attr("transform", "translate("
							 	+ (x.rangeBand() * i + x.rangeBand() / 2) + ", "
							 	+ (chart_inner_height + chart_margin)  + ") rotate(45)")
							 .text(d);
						<%
					}
				%>
			});
	
	
			//
			// Zero axis line
			//
			
			chart.append("line")
				 .attr("x1", -bands_margin)
				 .attr("y1", chart_inner_height)
				 .attr("x2", band_width * x.domain().length + bands_margin)
				 .attr("y2", chart_inner_height)
				 .style("stroke", "#000");
			
			
			
			// TODO
	
			function redraw() {
			
				/*data.forEach(function(d, i) {
					d.time += 0.1;
					d._value += 0.1;
				});*/
		 
				chart.selectAll("rect")
					.transition()
					.duration(100)
					.remove();
		 
				chart.selectAll("line")
					.transition()
					.duration(100)
					.remove();
		 
				chart.selectAll("text")
					.transition()
					.duration(100)
					.remove();
		 
				chart.selectAll("circle")
					.transition()
					.duration(200)
					.remove();
					
				draw_chart(data, chart, x);
			}
		}
	
	};
	
	this_boxplot = new boxplot();
	
	//  End -->
</script>
