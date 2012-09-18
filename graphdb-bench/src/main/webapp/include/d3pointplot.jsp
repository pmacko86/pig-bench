<%@ page import="com.tinkerpop.bench.web.ChartProperties"%>

<script language="JavaScript">
	<!-- Begin
	
	// XXX Group by does not work
	
	plot = function() {
		
		var chart_inner_height = 420;
		var chart_margin = 10;
		
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
		
		d3.csv('<%= chartProperties.source %>', function(data) {
			
			data.forEach(function(d) {
				<%= chartProperties.foreach %>;
				d.time = (+d.time) / 1000000.0;			// convert to ms
			});
			
			data = data.filter(function(d) {
				return <%= chartProperties.filter %>;
			});
			
			var labels = data.map(function(d) { return d.label; });
			
			var y = d3.scale.linear()
					  .domain([0, 1.1 * d3.max(data, function(d) { return d.time; })])
					  .range([chart_inner_height, 0]);
			
			var x = d3.scale.ordinal()
					  .domain(labels);
			x.rangeBands([0, band_width * x.domain().length]);
			
			var chart = d3.select(".<%= chartProperties.attach %>").append("svg")
						  .attr("class", "chart")
						  .attr("width",  band_width * x.domain().length + padding_left + padding_right)
						  .attr("height", chart_inner_height + padding_top + padding_bottom)
						  .append("g")
						  	.attr("transform", "translate(" + padding_left + ", " + padding_top + ")");
	
	
			// The vertical ruler (ticks) and the axis
			
			chart.selectAll("line")
				 .data(y.ticks(10))
				 .enter().append("line")
				 .attr("x1", -bands_margin)
				 .attr("x2", x.domain().length * band_width + bands_margin)
				 .attr("y1", y)
				 .attr("y2", y)
				 .style("stroke", "#ccc");
			
			chart.selectAll(".rule")
				 .data(y.ticks(10))
				 .enter().append("text")
				 .attr("class", "rule")
				 .attr("x", -chart_margin-bands_margin)
				 .attr("y", y)
				 .attr("dx", 0)
				 .attr("dy", ".35em")
				 .attr("text-anchor", "end")
				 .text(String);
			
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
						var categories = [];
						
						var __last_group_column = "";
						var __last_group_length = -1;
						
						var group_label_function = function(d, i) {
							<%= chartProperties.group_label_function %>;
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
							__last_group_length++;
							
							if (g != "" && g.indexOf("----") != 0) {
								var c = category_label_function(d, i);
								if (categories.indexOf(c) < 0) categories.push(c);
							}
						});
						
						if (__last_group_length > 0) {
							group_columns.push(__last_group_column)
							group_lengths.push(__last_group_length)
						}
						
						
						// Group labels
						
						for (var i = 0; i < group_columns.length; i++) {
							if (group_columns[i] == "" || group_columns[i].indexOf("----") == 0) continue;
							var p = group_offsets[i] + 0.5 * group_lengths[i] - 0.5;
							
							chart.append("text")
							 .attr("x", 0)
							 .attr("y", 0)
							 .attr("dx", 0)
							 .attr("dy", ".35em") // vertical-align: middle
							 .attr("transform", "translate("
							 	+ (x.rangeBand() * p + x.rangeBand() / 2) + ", "
							 	+ (chart_inner_height + chart_margin)  + ") rotate(45)")
							 .text(group_label_function(data[group_offsets[i]], group_offsets[i]));
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
			
			
			// Data and labels
					  
			chart.selectAll("bands_points")
				 .data(data)
				 .enter().append("circle")
				 .attr("cx", function(d, i) { return 0.5 * x.rangeBand() + x(d.label); })
				 .attr("cy", function(d, i) { return y(d.time); })
				 .style("fill", "none")
				 .style("stroke", "#000")
				<% if (chartProperties.group_by != null) { %>
					.style("fill", function(d, i) {
						var c = category_label_function(d, i);
						var index = categories.indexOf(c);
						return index < 0 ? "black" : band_colors(index);
					})
				<% } %>
				 .attr("r", x.rangeBand() / 4);
				 
			x.domain().forEach(function(d, i) {
			
				if (d.indexOf("----") == 0) return;
			
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
	
	
			// Zero axis line
			
			chart.append("line")
				 .attr("x1", -bands_margin)
				 .attr("y1", chart_inner_height)
				 .attr("x2", band_width * x.domain().length + bands_margin)
				 .attr("y2", chart_inner_height)
				 .style("stroke", "#000");
		});
	
	};
	
	plot();
	
	//  End -->
</script>
