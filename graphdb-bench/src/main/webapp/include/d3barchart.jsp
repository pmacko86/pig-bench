<%@ page import="com.tinkerpop.bench.web.ChartProperties"%>

<script language="JavaScript">
	<!-- Begin
	
	var chart_inner_height = 420;
	var chart_margin = 10;
	var num_ticks = 10;
	
	var padding_left = 100;
	var padding_right = 300;
	var padding_top = 75;
	var padding_bottom = 150;
	
	var bar_width = 20;
	var bar_colors = d3.scale.category10();
	var bars_margin = 10;
	
	var ylabel_from_chart_margin = 40;
		
	var legend_padding_left = 10;
	var legend_padding_top = 10;
	var legend_bar_padding_right = 4;
	var legend_bar_width = 2 * bar_width;
	var legend_bar_height = bar_width;
	var legend_vertical_spacing = 2;
		
	<% if (chartProperties.smallGraph) { %>
		chart_inner_height /= 2;
		padding_right /= 2;
		padding_bottom /= 2;
		num_ticks /= 2;
		legend_padding_left = 0;
		legend_bar_width /= 1.5;
	<% } %>


	d3.csv('<%= chartProperties.source %>', function(data) {
		
		data.forEach(function(d, i) {
			<%= chartProperties.foreach %>;
			d.mean = (+d.mean) / 1000000.0;			// convert to ms
			d.stdev = (+d.stdev) / 1000000.0;		// convert to ms
			d.min = (+d.min) / 1000000.0;			// convert to ms
			d.max = (+d.max) / 1000000.0;			// convert to ms
			d._index = i;
		});
		
		data = data.filter(function(d) {
			return <%= chartProperties.filter %>;
		});
		
		var labels = data.map(function(d) { return d.label; });
	
		var stacked = <%= chartProperties.stacked %>;
		
		
		
		<%
			// Group by
			
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
					var subgroup_labels = [];
					var subgroup_ids = [];
					var subgroup_sums = [];
					
					var categories = [];
					
					var __last_group_column = "";
					var __last_group_length = -1;
					var __last_subgroup_column = "";					
					var __last_subgroup_sum = 0;
					
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
					
					data.forEach(function(d, i) {
					
						var g = d.<%= chartProperties.group_by %>;
						if (g != __last_group_column) {
							if (__last_group_length > 0) {
								group_columns.push(__last_group_column)
								group_lengths.push(__last_group_length)
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
						
						if (stacked) {
							var s = d.<%= chartProperties.subgroup_by == null ? chartProperties.group_by : chartProperties.subgroup_by %>;
							if (s != __last_subgroup_column) {
								__last_group_length++;
								__last_subgroup_column = s;
							}
						}
						else {
							__last_group_length++;
						}
						
						if (g != "" && g.indexOf("----") != 0) {
							var c = category_label_function(d, i);
							if (categories.indexOf(c) < 0) categories.push(c);
						}
					});
							
					if (__last_group_length > 0) {
						group_columns.push(__last_group_column)
						group_lengths.push(__last_group_length)
						group_labels.push(
								group_label_function(data[data.length-1], data.length-1));
					}
					
					<% if (chartProperties.subgroup_by != null) { %>
					
						__last_subgroup_column = "";
						var __last_subgroup_length = -1;
						var __last_subgroup_column_major = "";
						var __id = 0;
						
						data.forEach(function(d, i) {
						
							var g = d.<%= chartProperties.subgroup_by %>;
							if (g != __last_subgroup_column || d.<%= chartProperties.group_by %> != __last_subgroup_column_major) {
								if (__last_subgroup_length > 0) {
									subgroup_columns.push(__last_subgroup_column)
									subgroup_lengths.push(stacked ? 1 : __last_subgroup_length)
									subgroup_labels.push(subgroup_label_function(data[i-1], i-1));
									subgroup_ids.push(__id++);
									subgroup_sums.push(__last_subgroup_sum);
								}
								if (__last_subgroup_length == -1) {
									subgroup_offsets.push(0);
								}
								else {
									subgroup_offsets.push(subgroup_offsets[subgroup_offsets.length-1] + (stacked ? 1 : __last_subgroup_length));
								}
								__last_subgroup_column = g;
								__last_subgroup_length = 0;
								__last_subgroup_column_major = d.<%= chartProperties.group_by %>;
								__last_subgroup_sum = 0;
							}
							
							if (d.<%= chartProperties.group_by %> != __last_subgroup_column_major) {
								subgroup_ids.push(":-- " + d.<%= chartProperties.group_by %>);
							}
							
							__last_subgroup_length++;
							__last_subgroup_sum += d.mean;
							d._subgroup_id = __id;
						});
						
						if (__last_subgroup_length > 0) {
							subgroup_columns.push(__last_subgroup_column)
							subgroup_lengths.push(stacked ? 1 : __last_subgroup_length)
							subgroup_labels.push(
									subgroup_label_function(data[data.length-1], data.length-1));
							subgroup_ids.push(__id++);
							subgroup_sums.push(__last_subgroup_sum);
						}
					<% } %>
					<%
			}
		%>
		
		
		
		var num_bars = (stacked ? subgroup_ids.length : data.length);
		var chart = d3.select(".<%= chartProperties.attach %>").append("svg")
					  .attr("class", "chart")
					  .attr("width",  bar_width * num_bars + padding_left + padding_right)
					  .attr("height", chart_inner_height + padding_top + padding_bottom)
					  .append("g")
					  	.attr("transform", "translate(" + padding_left + ", " + padding_top + ")");
				
		//
		// Set up fill patterns
		//
		
		var defs = chart.append('svg:defs');
		var num_patterns = 0;

		for (var pattern_index = 0;
			pattern_index < d3.max([2<% if (chartProperties.group_by != null) { %>, categories.length<% } %>]);
			pattern_index++) {
			num_patterns++;
			
			var pmod = pattern_index % 6;
			var pattern = defs.append("svg:pattern")
				    .attr("id", "pattern-" + pattern_index)
				    .attr("x", 0)
				    .attr("y", 0)
				    .attr("width" , pmod == 3 || pmod == 5 ? 6 : 8)
				    .attr("height", pmod == 2 || pmod == 5 ? 6 : 8)
				    .attr("patternUnits", "userSpaceOnUse")
				    .append("g")
				    .style("fill", "none")
				    .style("stroke", bar_colors(pattern_index))
				    .style("stroke-width", "1")
				    .style("stroke-linecap", "square");
		
			switch (pmod) {
			case 0:
				pattern.append("path")
				    .attr("d", "M 0 0 L 8 8 M 0 -8 L 8 0 M 0 8 L 8 16");
				break;		
			case 1:
				pattern.append("path")
				    .attr("d", "M 8 0 L 0 8 M 8 -8 L 0 0 M 8 8 L 0 16");
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
				    .attr("d", "M 0 0 L 8 8 M 0 -8 L 8 0 M 0 8 L 8 16 M 8 0 L 0 8 M 8 -8 L 0 0 M 8 8 L 0 16");
				break;			
			case 5:
				pattern.append("path")
				    .attr("d", "M 0 3 L 8 3 M 3 0 L 3 8");
				break;
			}
		}
		
		
		//
		// X Scale
		//
		
		var x = d3.scale.ordinal()
				  .domain(stacked ? subgroup_ids : labels)
				  .rangeBands([0, bar_width * num_bars]);
			
		//
		// Y Scale
		//
		
		var data_scale = "<%= chartProperties.yscale %>";
		var y = d3.scale.<%= chartProperties.yscale %>()
				  .domain([<%= "log".equals(chartProperties.yscale)
					  ? "0.9 * d3.min(data, function(d) { "
					    + "  if (d.label.indexOf('----') == 0) return 1000 * 1000 * 1000;"
						+ "  return d.mean - d.stdev < 0 ? 0 : d.mean - d.stdev;"
					  	+ "})"
					  : "0" %>, 
				  	      1.1 * (stacked ? d3.max(subgroup_sums) : d3.max(data, function(d) { return d.mean + d.stdev; }))])
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


		// The vertical ruler (ticks) and the axis
		
		chart.selectAll("line")
			 .data(ticks)
			 .enter().append("line")
			 .attr("x1", -bars_margin)
			 .attr("x2", num_bars * bar_width + bars_margin)
			 .attr("y1", y)
			 .attr("y2", y)
			 .style("stroke", "#ccc");
		
		chart.selectAll(".rule")
			 .data(ticks)
			 .enter().append("text")
			 .attr("class", "rule")
			 .attr("x", -chart_margin-bars_margin)
			 .attr("y", y)
			 .attr("dx", 0)
			 .attr("dy", ".35em")
			 .attr("text-anchor", "end")
			 .text(String);
		
		chart.append("line")
			 .attr("x1", -bars_margin)
			 .attr("y1", 0)
			 .attr("x2", -bars_margin)
			 .attr("y2", chart_inner_height)
			 .style("stroke", "#000");
			 
		chart.append("text")
			 .attr("x", 0)
			 .attr("y", 0)
			 .attr("dx", 0)
			 .attr("dy", 0)
			 .attr("text-anchor", "middle")
			 .attr("transform", "translate("
			 	+ (-bars_margin*2 - ylabel_from_chart_margin) + ", "
			 	+ (chart_inner_height/2)  + ") rotate(-90)")
			 .text("<%= chartProperties.ylabel %>");
					
		
		
		<%
			// Group labels and legend
			
			if (chartProperties.group_by != null) {
				%>
						
					var __longest_subgroup_name = "";
					
					<% if (chartProperties.subgroup_by != null) { %>
					
						// Subgroup labels
						
						for (var i = 0; i < subgroup_columns.length; i++) {
							if (subgroup_columns[i] == "" || subgroup_columns[i].indexOf("----") == 0) continue;
							var p = subgroup_offsets[i] + 0.5 * subgroup_lengths[i] - 0.5;
							var t = subgroup_labels[i];
							if (t.length > __longest_subgroup_name.length) __longest_subgroup_name = t;
							//console.log("" + p + " ==> " + t);
							
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
					
					
					// Legend
					
					for (var j = 0; j < categories.length; j++) {
						var i = stacked ? categories.length - j - 1 : j;
					
						chart.append("rect")
							 .attr("x", bar_width * num_bars + bars_margin + chart_margin + legend_padding_left)
							 .attr("y", j * (legend_bar_height + legend_vertical_spacing) + legend_padding_top)
							 .attr("width", legend_bar_width)
							 .attr("height", legend_bar_height)
							<% if (!chartProperties.patternFill) { %>
								.style("fill", bar_colors(i));
							<% } else { %>
								.attr("style", "fill:url(#pattern-" + (i % num_patterns) + ")"
											+ ";stroke:" + bar_colors(i));
							<% } %>
					
						chart.append("text")
							 .attr("x", bar_width * num_bars + bars_margin + chart_margin
							 			+ legend_padding_left + legend_bar_width + legend_bar_padding_right)
							 .attr("y", (j + 0.5) * (legend_bar_height + legend_vertical_spacing) + legend_padding_top)
							 .attr("dx", 0)
						 	 .attr("dy", ".35em") // vertical-align: middle
							 .text(categories[i]);
					}
				<%
			}
		%>
		
		
		// Data and labels
				  
		if (stacked) {
		
			for (var subgroup_index = 0; subgroup_index < subgroup_columns.length; subgroup_index++) {
				if (subgroup_columns[subgroup_index] == "" || subgroup_columns[subgroup_index].indexOf("----") == 0) continue;
				
				var filtered = data.filter(function(d) {
					return d._subgroup_id == subgroup_ids[subgroup_index];
				});
				
				if (filtered.length <= 0) continue;
				
				var p = subgroup_offsets[i];
				
				var bottom = chart_inner_height;
				var total_so_far = 0;
					
				<% if (chartProperties.group_by != null) { %>
				var g = filtered[0].<%= chartProperties.group_by %>;
				if (g == "" || g.indexOf("----") == 0) continue;
				<% } %>
				
				for (var i = 0; i < filtered.length; i++) {
					d = filtered[i];
					var category = category_label_function(d, d._index);
					var category_index = categories.indexOf(category);
					
					var top = y(d.mean + total_so_far);
					var top_adjusted = top;
					
					<% if (chartProperties.patternFill) { %>
						top_adjusted = top+1;
					<% } %>
					
					chart.append("rect")
						 .attr("x", subgroup_index * x.rangeBand())
						 .attr("y", (i == filtered.length-1 ? top : top_adjusted))
						<% if (chartProperties.group_by != null) { %>
							<% if (!chartProperties.patternFill) { %>
								.style("fill", category_index < 0 ? "black" : bar_colors(category_index))
							<% } else { %>
								.attr("style", "fill:" + (category_index < 0
									? "black"
									: "url(#pattern-" + (category_index % num_patterns) + ")")
								+ ";stroke:" + (category_index < 0
									? "none"
									: bar_colors(category_index)))
							<% } %>
						<% } %>
						 .attr("width", x.rangeBand())
						 .attr("height", bottom - (i == filtered.length-1 ? top : top_adjusted));
					
					bottom = top;
					total_so_far += d.mean;
				}
				
								
				var fixed_length = 3;
				if (total_so_far > 10) fixed_length = 2;
				if (total_so_far > 100) fixed_length = 1;
				if (total_so_far > 1000) fixed_length = 0;
					 
				chart.append("text")
				 .attr("x", 0)
				 .attr("y", 0)
				 .attr("dx", 0)
				 .attr("dy", ".35em") // vertical-align: middle
				 .attr("transform", "translate("
				 	+ (x.rangeBand() * subgroup_index + x.rangeBand() / 2) + ", "
				 	+ (top - 10)  + ") rotate(-90)")
				 .text("" + total_so_far.toFixed(fixed_length) + " s");
			}
		
		}
		else {
			
			chart.selectAll("bars_rect")
				 .data(data)
				 .enter().append("rect")
				 .attr("x", function(d, i) { return i * x.rangeBand(); })
				 .attr("y", function(d, i) { return y(d.mean); })
				<% if (chartProperties.group_by != null) { %>
					<% if (chartProperties.group_by != null) { %>
						<% if (!chartProperties.patternFill) { %>
							.style("fill", function(d, i) {
								var c = category_label_function(d, i);
								var category_index = categories.indexOf(c);
								return category_index < 0 ? "black" : bar_colors(category_index);
							})
						<% } else { %>
							.attr("style",  function(d, i) {
								var c = category_label_function(d, i);
								var category_index = categories.indexOf(c);
								return "fill:" + (category_index < 0
									? "black"
									: "url(#pattern-" + (category_index % num_patterns) + ")")
								+ ";stroke:" + (category_index < 0
									? "none"
									: bar_colors(category_index));
							})
						<% } %>
					<% } %>
				<% } %>
				 .attr("width", x.rangeBand())
				 .attr("height", function(d, i) { return chart_inner_height - y(d.mean); });
					  
			chart.selectAll("error_bars_middle")
				 .data(data)
				 .enter().append("line")
				 .style("stroke", function(d, i) { return isNaN(d.stdev) || d.stdev == 0 ? "none" : "#000" })
				 .attr("x1", function(d, i) { return (i + 0.5) * x.rangeBand(); })
				 .attr("x2", function(d, i) { return (i + 0.5) * x.rangeBand(); })
				 .attr("y1", function(d, i) { return y(d.mean + d.stdev); })
				 .attr("y2", function(d, i) { return y(d.mean - d.stdev < 0 ? 0 : d.mean - d.stdev); });
					  
			chart.selectAll("error_bars_top")
				 .data(data)
				 .enter().append("line")
				 .style("stroke", function(d, i) { return isNaN(d.stdev) || d.stdev == 0 ? "none" : "#000" })
				 .attr("x1", function(d, i) { return (i + 0.25) * x.rangeBand(); })
				 .attr("x2", function(d, i) { return (i + 0.75) * x.rangeBand(); })
				 .attr("y1", function(d, i) { return y(d.mean + d.stdev); })
				 .attr("y2", function(d, i) { return y(d.mean + d.stdev); });
					  
			chart.selectAll("error_bars_bottom")
				 .data(data)
				 .enter().append("line")
				 .style("stroke", function(d, i) { return isNaN(d.stdev) || d.stdev == 0 ? "none" : "#000" })
				 .attr("x1", function(d, i) { return (i + 0.25) * x.rangeBand(); })
				 .attr("x2", function(d, i) { return (i + 0.75) * x.rangeBand(); })
				 .attr("y1", function(d, i) { return y(d.mean - d.stdev < 0 ? 0 : d.mean - d.stdev); })
				 .attr("y2", function(d, i) { return y(d.mean - d.stdev < 0 ? 0 : d.mean - d.stdev); });
	
				 
			data.forEach(function(d, i) {
			
				if (d.label.indexOf("----") == 0) return;
			
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
							 .text(d.label);
						<%
					}
				%>
				
				var fixed_length = 3;
				if (d.mean > 10) fixed_length = 2;
				if (d.mean > 100) fixed_length = 1;
				if (d.mean > 1000) fixed_length = 0;
					 
				chart.append("text")
				 .attr("x", 0)
				 .attr("y", 0)
				 .attr("dx", 0)
				 .attr("dy", ".35em") // vertical-align: middle
				 .attr("transform", "translate("
				 	+ (x.rangeBand() * i + x.rangeBand() / 2) + ", "
				 	+ (y(d.mean + d.stdev) - 10)  + ") rotate(-90)")
				 .text("" + d.mean.toFixed(fixed_length) + " ms");
			});
		}


		// Zero axis line
		
		chart.append("line")
			 .attr("x1", -bars_margin)
			 .attr("y1", chart_inner_height)
			 .attr("x2", bar_width * num_bars + bars_margin)
			 .attr("y2", chart_inner_height)
			 .style("stroke", "#000");
	});
	
	//  End -->
</script>
