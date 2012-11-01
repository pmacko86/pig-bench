<%@ page import="com.tinkerpop.bench.web.ChartProperties"%>

<script language="JavaScript">
	<!-- Begin
	
	function scatterplot() {
		
		var chart_inner_height = 420;
		var chart_inner_width = 640;
		var chart_margin = 10;
		
		var padding_left = 100;
		var padding_right = 300;
		var padding_top = 50;
		var padding_bottom = 100;
		
		var band_width = 20;
		var band_colors = d3.scale.category10();
		var bands_margin = 10;
		
		var xlabel_from_chart_margin = 40;
		var ylabel_from_chart_margin = 40;
			
		var legend_padding_left = 10;
		var legend_padding_top = 10;
		var legend_band_padding_right = 4;
		var legend_band_width = 2 * band_width;
		var legend_band_height = band_width;
		var legend_vertical_spacing = 2;
		
		var boxplot_width = 0.5;
		
		var series_label_function = function(d, i) {
			<%= chartProperties.series_label_function == null
					? "return null" : chartProperties.series_label_function %>;
		};
		


		//
		// Load the data and create the chart
		//

		d3.csv('<%= chartProperties.source %>', function(data) {
		
		
			//
			// Prepare and filter the data
			//
			
			var series = new Array();
			var series_labels = new Array();
			
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
				
				d._series = <%= chartProperties.series_column == null ? "null" : "d." + chartProperties.series_column %>;
				if (d._series != null) {
					if (d._series.indexOf("----") != 0) {
						if (series.indexOf(d._series) < 0) {
							series.push(d._series);
							series_labels.push(series_label_function(d, i));
						}
					}
				}
				
				d._index = i;
				d._xvalue = <%= chartProperties.xvalue %>;
				d._yvalue = <%= chartProperties.yvalue %>;
				//if (i < 10) console.log(d);
			});
			
			data = data.filter(function(d) {
				if (d._xvalue == Infinity || isNaN(d._xvalue)) return false;
				if (d._yvalue == Infinity || isNaN(d._yvalue)) return false;
				return <%= chartProperties.filter %>;
			});
			
			
			
			//
			// Create the chart
			//
			
			var chart = d3.select(".<%= chartProperties.attach %>").append("svg")
						  .attr("class", "chart")
						  .attr("width",  chart_inner_width + padding_left + padding_right)
						  .attr("height", chart_inner_height + padding_top + padding_bottom)
						  .append("g")
						  	.attr("transform", "translate(" + padding_left + ", " + padding_top + ")");
			
			
			draw_chart(data, chart, series, series_labels);
		});
			
		
		
		//
		// Function to draw the data
		//
		
		function draw_chart(data, chart, series, series_labels) {
			
				
			//
			// Drop extreme values
			//
			
			<% if (chartProperties.dropTopBottomExtremes) { %>
							
				series.forEach(function(domain) {
					if (domain.indexOf("----") == 0) return;
				
					domain_data = data.filter(function(d) {
						return d._series == domain;
					});
					d = domain_data.map(function(d) { return d._xvalue; }).sort(d3.ascending);
					
					min = d3.quantile(d, .05);
					max = d3.quantile(d, .95);
				
					data = data.filter(function(d) {
						return d._series != domain || (d._xvalue >= min && d._xvalue <= max);
					});
				});
							
				series.forEach(function(domain) {
					if (domain.indexOf("----") == 0) return;
				
					domain_data = data.filter(function(d) {
						return d._series == domain;
					});
					d = domain_data.map(function(d) { return d._yvalue; }).sort(d3.ascending);
					
					min = d3.quantile(d, .05);
					max = d3.quantile(d, .95);
				
					data = data.filter(function(d) {
						return d._series != domain || (d._yvalue >= min && d._yvalue <= max);
					});
				});
				
			<% } %>
		
		
			
			//
			// X Scale
			//
			
			var data_xscale = "<%= chartProperties.xscale %>";
			var x = d3.scale.<%= chartProperties.xscale %>()
					  .domain([<%= "log".equals(chartProperties.yscale)
						  ? "0.9 * d3.min(data, function(d) { "
						    + "  if (d.label.indexOf('----') == 0) return 1000 * 1000 * 1000;"
							+ "  return d._xvalue;"
						  	+ "})"
						  : "0" %>, 
					  	      1.1 * d3.max(data, function(d) { return d._xvalue; })])
					  .range([0, chart_inner_width])
					  .nice();
			
			var xunit = "";
			var xunitscale = 1;
		
		
			
			//
			// Y Scale
			//
			
			var data_yscale = "<%= chartProperties.yscale %>";
			var y = d3.scale.<%= chartProperties.yscale %>()
					  .domain([<%= "log".equals(chartProperties.yscale)
						  ? "0.9 * d3.min(data, function(d) { "
						    + "  if (d.label.indexOf('----') == 0) return 1000 * 1000 * 1000;"
							+ "  return d._yvalue;"
						  	+ "})"
						  : "0" %>, 
					  	      1.1 * d3.max(data, function(d) { return d._yvalue; })])
					  .range([chart_inner_height, 0])
					  .nice();
	
	
	
			//
			// The vertical ruler (ticks) and the axis
			//
			
			var num_ticks = 10;
			var yticks = y.ticks(num_ticks);
			if (data_yscale == "log" && yticks.length > num_ticks) {
				t = new Array();
				
				t.push(y.domain()[1]);
				while (t[t.length - 1] / 10 >= y.domain()[0]) {
					t.push(t[t.length - 1] / 10);
				}
				
				yticks = t;
			}
			
			chart.selectAll(".yline")
				 .data(yticks)
				 .enter().append("line")
				 .attr("x1", 0)
				 .attr("x2", chart_inner_width)
				 .attr("y1", y)
				 .attr("y2", y)
				 .style("stroke", "#ccc");
			
			chart.selectAll(".yrule")
				 .data(yticks)
				 .enter().append("text")
				 .attr("class", "rule")
				 .attr("x", -chart_margin)
				 .attr("y", y)
				 .attr("dx", 0)
				 .attr("dy", ".35em")
				 .attr("text-anchor", "end")
				 .text(function(d) {
					return d;
				 });
			
			chart.append("line")
				 .attr("x1", 0)
				 .attr("y1", chart_inner_height)
				 .attr("x2", chart_inner_width)
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
			// The horizontal ruler (ticks) and the axis
			//
			
			num_ticks = 10;
			var xticks = x.ticks(num_ticks);
			if (data_xscale == "log" && xticks.length > num_ticks) {
				t = new Array();
				
				t.push(y.domain()[1]);
				while (t[t.length - 1] / 10 >= x.domain()[0]) {
					t.push(t[t.length - 1] / 10);
				}
				
				xticks = t;
			}
						
			<% if (chartProperties.xautounit) { %>
			
			if (data_xscale == "linear") { 
				if ((xticks[1] % 1000) == 0) {
					xunit = "thousands";
					xunitscale *= 1000;
					if (((xticks[1] / xunitscale) % 1000) == 0) {
						xunit = "millions";
						xunitscale *= 1000;
					}
				}
			}
			
			<% } %>
			
			xlabel = "<%= chartProperties.xlabel %>";
			if (xunit != "") {
				xlabel = xlabel + " (" + xunit + ")";
			}
			
			chart.selectAll(".xline")
				 .data(xticks)
				 .enter().append("line")
				 .attr("y1", 0)
				 .attr("y2", chart_inner_height)
				 .attr("x1", x)
				 .attr("x2", x)
				 .style("stroke", "#ccc");
			
			chart.selectAll(".xrule")
				 .data(xticks)
				 .enter().append("text")
				 .attr("class", "rule")
				 .attr("y", chart_inner_height+chart_margin+bands_margin)
				 .attr("x", x)
				 .attr("dy", 0)
				 .attr("dx", ".35em")
				 .attr("text-anchor", "middle")
				 .text(function(d) {
					return d / xunitscale;
				 });
			
			chart.append("line")
				 .attr("x1", 0)
				 .attr("y1", 0)
				 .attr("x2", 0)
				 .attr("y2", chart_inner_height)
				 .style("stroke", "#000");
				 
			chart.append("text")
				 .attr("x", chart_inner_width/2)
				 .attr("y", chart_inner_height + bands_margin*2 + xlabel_from_chart_margin)
				 .attr("dx", 0)
				 .attr("dy", 0)
				 .attr("text-anchor", "middle")
				 .text(xlabel);
			
			
			
			//
			// Data
			//
			
			chart.selectAll("data_points")
				 .data(data)
				 .enter().append("circle")
				 .attr("cx", function(d, i) { return x(d._xvalue); })
				 .attr("cy", function(d, i) { return y(d._yvalue); })
				 .style("fill", "none")
				 .style("stroke", function(d, i) {
						var c = d._series;
						var index = series.indexOf(c);
						return index < 0 ? "black" : band_colors(index);
					})
				 .attr("r", 4);
			
			
			//
			// Linear fits
			//
			
			var add_linear_function = function(f0, f1, series_index, dashed) {
				
				var x0 = d3.min(x.domain());
				var y0 = f0 + f1 * x0;
				
				var x1 = d3.max(x.domain());
				var y1 = f0 + f1 * x1;
				
				var ymin = d3.min(y.domain());
				if (y0 > ymax && f1 != 0) {
					x0 = (ymax - f0) / f1;
				}
				if (y0 < ymin) {
					x0 = (ymin - f0) / f1;
				}
				
				var ymax = d3.max(y.domain());
				if (y1 > ymax && f1 != 0) {
					x1 = (ymax - f0) / f1;
				}
				if (y1 < ymin && f1 != 0) {
					x1 = (ymin - f0) / f1;
				}
				
				var f;
				if (data_yscale == "linear") {
					f = chart.append("line")
						 .attr("x1", x(x0))
						 .attr("y1", y(f0 + f1 * x0))
						 .attr("x2", x(x1))
						 .attr("y2", y(f0 + f1 * x1))
						 .style("stroke", band_colors(series_index));
				}
				
				if (data_yscale == "log") {
					var im = 100.0;
					var path = "";	
					for (var i = 1; i < 2; i++) {
						var x0i = x0 + (i) * ((x1 - x0) / im);
						path = path + "M " + x(x0i) + " " + y(f0 + f1 * x0i);
					}
					for (var i = 2; i <= im; i++) {
						var x0i = x0 + (i) * ((x1 - x0) / im);
						path = path + " L " + x(x0i) + " " + y(f0 + f1 * x0i);
					}
					f = chart.append("svg:path")
						 .attr("d", path)
						 .style("fill", "none")
						 .style("stroke", band_colors(series_index));
				}
				
				if (dashed) {
					f.style("stroke-dasharray", "9,5");
				}
			}
						
			<%
				if (!chartProperties.linear_fits.isEmpty()) {
					int fit_index = -1;
					for (Double[] fit : chartProperties.linear_fits) {
						fit_index++;
						if (fit == null) continue;
						%>
							add_linear_function(<%= fit[0].doubleValue() %>,
								<%= fit.length >= 2 ? fit[1].doubleValue() : 0 %>,
								<%= fit_index %>, false);
						<%
					}
				} 
				if (!chartProperties.predictions.isEmpty()) {
					int fit_index = -1;
					for (Double[] fit : chartProperties.predictions) {
						fit_index++;
						if (fit == null) continue;
						%>
							add_linear_function(<%= fit[0].doubleValue() %>,
								<%= fit.length >= 2 ? fit[1].doubleValue() : 0 %>,
								<%= fit_index %>, true);
						<%
					}
				} 
			%>
			
						
			
			//
			// Legend
			//
			
			if (series.length > 1) {
				for (var i = 0; i < series.length; i++) {
				
					chart.append("rect")
						 .attr("x", chart_inner_width + bands_margin + chart_margin + legend_padding_left)
						 .attr("y", i * (legend_band_height + legend_vertical_spacing) + legend_padding_top)
						 .attr("width", legend_band_width)
						 .attr("height", legend_band_height)
						 .style("fill", band_colors(i));
				
					chart.append("text")
						 .attr("x", chart_inner_width + bands_margin + chart_margin
						 			+ legend_padding_left + legend_band_width + legend_band_padding_right)
						 .attr("y", (i + 0.5) * (legend_band_height + legend_vertical_spacing) + legend_padding_top)
						 .attr("dx", 0)
					 	 .attr("dy", ".35em") // vertical-align: middle
						 .text(series_labels[i]);
				}
			}
		}
	
	};
	
	this_scatterplot = new scatterplot();
	
	//  End -->
</script>
