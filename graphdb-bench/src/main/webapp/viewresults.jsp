<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<%@ page import="com.tinkerpop.bench.Bench"%>
<%@ page import="com.tinkerpop.bench.DatabaseEngine"%>
<%@ page import="com.tinkerpop.bench.LogUtils"%>
<%@ page import="com.tinkerpop.bench.Workload"%>
<%@ page import="com.tinkerpop.bench.benchmark.BenchmarkMicro"%>
<%@ page import="com.tinkerpop.bench.log.OperationLogEntry"%>
<%@ page import="com.tinkerpop.bench.log.OperationLogReader"%>
<%@ page import="com.tinkerpop.bench.util.Pair"%>
<%@ page import="com.tinkerpop.bench.web.*"%>
<%@ page import="java.io.*"%>
<%@ page import="java.text.SimpleDateFormat"%>
<%@ page import="java.util.*"%>
<%@ page import="au.com.bytecode.opencsv.CSVReader"%>
<%@ page import="org.apache.commons.lang.StringEscapeUtils"%>

<%
	String jsp_title = "View Results";
	String jsp_page = "viewresults";
	String jsp_body = "onload=\"body_on_load()\" onunload=\"\"";
	boolean jsp_allow_cache = true;
%>
<%@ include file="include/header.jsp" %>
	
	<script src="/include/d3.v2.js"></script>
	
	<script language="JavaScript">
		<!-- Begin
		
		/*
		 * Toggle/check a checkbox and unselect all others in the group
		 */
		function check_radio(value)
		{
			field = document.getElementsByName('job');
			for (i = 0; i < field.length; i++) {
				if (field[i].value == value) {
					field[i].checked = true;
				}
			}
			
			job_radio_set_class_for_all();
			document.getElementById('form').submit();
		}
		
		/*
		 * Set the correct classes for all job radio inputs
		 */
		function job_radio_set_class_for_all()
		{			
			field = document.getElementsByName('job');
			for (i = 0; i < field.length; i++) {
				parent = field[i].parentNode.parentNode;
				parent.className = parent.className.replace('checked_job_', 'job_');
				parent.className = parent.className.replace('checked_job_', 'job_');
				parent.className = parent.className.replace('checked_job_', 'job_');
				if (field[i].checked) {
					parent.className = parent.className.replace('job_', 'checked_job_');
				}
			}
		}
		
		/*
		 * Handler for changing the value of a radio
		 */
		function job_radio_on_change(radio)
		{			
			job_radio_set_class_for_all();
			document.getElementById('form').submit();
		}
		
		/*
		 * Get a log file URL
		 */
		function get_log_file_url(id, warmup, format)
		{
			return "/ShowLogFile?job=" + id
				+ "&warmup=" + (warmup ? "true" : "false")
				+ "&format=" + format;
		}
		
		/*
		 * Get a log file URL
		 */
		function get_summary_log_file_url(id, format)
		{
			return "/ShowSummaryLogFile?job=" + id + "&format=" + format;
		}
		
		/*
		 * Replace the parent of the given element by the contents of the log file
		 */
		function replace_by_log_file(element, id, warmup)
		{
			var node = element.parentNode;
			var http_request = new XMLHttpRequest();
			http_request.open("GET", get_log_file_url(id, warmup, "html"), true);
			http_request.onreadystatechange = function () {
				if (http_request.readyState == 1) {
					node.innerHTML = '<p class="basic_status_running">Loading...</p>';
				}
				if (http_request.readyState == 4 && http_request.status == 200) {
					node.innerHTML = http_request.responseText;
				}
				if (http_request.readyState == 4 && http_request.status != 200) {
					node.innerHTML = '<p class="basic_status_error">Error</p>';
				}
			};
			http_request.send(null);
		}
		
		/*
		 * Handler for body load
		 */
		function body_on_load()
		{
			job_radio_set_class_for_all();
			
			e = document.getElementById("refreshed");
			if (e.value=="no") {
				e.value="yes";
			}
			else {
				e.value="no";
				//location.reload();
			}
		}
		
		//  End -->
	</script>
	
	<div class="basic_form">
		<form id="form" name="form" method="post" action="/viewresults.jsp">
			<h1>View Results</h1>
			<p class="header">Select a result log file to view</p>
			<p class="header2">1) Select a database engine name and an instance name:</p>
			
			<%
				boolean dbinst_simple = false;
				boolean dbinst_choose_many = false;
				boolean dbinst_choose_nonexistent = false;
				String dbinst_onchange = "document.getElementById('form').submit();";
			%>
			<div class="db_table_div">
				<%@ include file="include/dbinsttable.jsp" %>
			</div>		
			
			<%
				int numJobs = 0;
				TreeMap<String, Job> jobMap = new TreeMap<String, Job>();
				SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("[yyyy/MM/dd HH:mm:ss]");
				
				String[] a_selectedDatabaseInstances = WebUtils.getStringParameterValues(request, "database_engine_instance");
				HashSet<String> selectedDatabaseInstances = new HashSet<String>();
				if (a_selectedDatabaseInstances != null) {
					for (String a : a_selectedDatabaseInstances) {
						selectedDatabaseInstances.add(a);
					}
				}

				for (String s : selectedDatabaseInstances) {
					String[] p = s.split("\\|");
					if (p.length == 1 || p.length == 2) {
						for (Job job : JobList.getInstance().getFinishedJobs(p[0], p.length == 2 ? p[1] : null)) {
							String prefix = "";
							if (job.getExecutionTime() != null) {
								prefix = dateTimeFormatter.format(job.getExecutionTime()) + " ";
							}
							jobMap.put(prefix + job.toString(), job);
							numJobs++;
						}
					}
				}
				
				String[] a_selectedJobIds = WebUtils.getFileNameParameterValues(request, "job");
				TreeSet<String> selectedJobIds = new TreeSet<String>();
				if (a_selectedJobIds != null) {
					for (String a : a_selectedJobIds) {
						selectedJobIds.add(a);
					}
				}
				
				TreeMap<String, Job> selectedJobs = new TreeMap<String, Job>();
				
				if (numJobs > 0) {
					%>
					
					<div id="select_job">
						<p class="middle">2) Select a job:</p>
						<div class="job_list_div">
							<div class="header">
								<div class="header_inner">
									Jobs
								</div>
							</div>
						<%
							for (String jobString : jobMap.keySet()) {
								Job job = jobMap.get(jobString);
								String extraTags = "";
								String id = "" + job.getId();
								
								if (selectedJobIds.contains(id)) {
									selectedJobs.put(id, job);
									extraTags += " checked=\"checked\"";
								}
								
								String c = "job_neutral";
								if (job.isRunning()) {
									c = "job_running";
								}
								else if (job.getExecutionCount() > 0) {
									if (job.getLastStatus() == 0) {
										c = "job_done";
									}
									else {
										c = "job_failed";
									}
								}
								%>
									<div class="checkbox_outer">
										<div class="checkbox <%= c %>">
											<div class="checkbox_inner">
												<input class="checkbox" type="radio" name="job"
												       value="<%= id %>" <%= extraTags %>
												       onchange="job_radio_on_change(this)"/>
											</div>
											<label class="checkbox <%= c %>">
												<p class="checkbox <%= c %>"
												   onclick="check_radio('<%= id %>')">
													<%= jobString %>
												</p>
											</label>
										</div>
									</div>
								<%
							}
						%>
						</div>
		
						<div class="clear"></div>
					</div>
			<%
				}
				
				if (!selectedJobs.isEmpty()) {
					%>
						<p class="middle"></p>
					<%
				}
			%>
			
			<input type="hidden" name="refreshed" id="refreshed" value="no" />

			<div class="spacer"></div>
		</form>
		
		<%
			for (Job job : selectedJobs.values()) {
			%>
				<h2>Job Details</h2>
				<pre><%= job.toMultilineString(true) %></pre>
				
				<h3>Summary</h3>
				
				<%
					File f = job.getSummaryFile();
					if (f == null) {
						%>
							<p class="simple_status_warning">Not found.</p>
						<%
					}
					else {
						StringWriter writer = new StringWriter();
						ShowSummaryLogFile.printSummaryLogFile(new PrintWriter(writer), f, "html", null);
						
						%>
							<div class="chart_outer"><div class="chart">
										
							<script language="JavaScript">
								<!-- Begin
								
								var char_inner_width = 420;
								var bar_height = 20;
								var padding_left = 300;
								var padding_right = 100;
								var padding_top = 20;
								var padding_bottom = 0;
								
								d3.csv(get_summary_log_file_url('<%= job.getId() %>', 'csv'), function(data) {
									
									data.forEach(function(d) {
										d.label = d.operation;
										d.mean = (+d.mean) / 1000000.0;			// convert to ms
										d.stdev = (+d.stdev) / 1000000.0;		// convert to ms
									});
									
									data = data.filter(function(d) {
										return ['OperationOpenGraph', 'OperationDoGC', 'OperationShutdownGraph'].indexOf(d.operation) < 0;
									});
									
									var labels = data.map(function(d) { return d.label; });
									
									var chart = d3.select(".chart").append("svg")
												  .attr("class", "chart")
												  .attr("width", char_inner_width + padding_left + padding_right)
												  .attr("height", bar_height * data.length + padding_top + padding_bottom)
												  .append("g")
												  	.attr("transform", "translate(" + padding_left + ", " + padding_top + ")");
									
									var x = d3.scale.linear()
											  .domain([0, d3.max(data, function(d) { return d.mean; })])
											  .range([0, char_inner_width]);
									
									var y = d3.scale.ordinal()
											  .domain(labels)
											  .rangeBands([0, bar_height * data.length]);
						
						
									// The horizontal ruler (ticks)
									
									chart.selectAll("line")
										 .data(x.ticks(10))
										 .enter().append("line")
										 .attr("x1", x)
										 .attr("x2", x)
										 .attr("y1", 0)
										 .attr("y2", data.length * bar_height)
										 .style("stroke", "#ccc");
									
									chart.selectAll(".rule")
										 .data(x.ticks(10))
										 .enter().append("text")
										 .attr("class", "rule")
										 .attr("x", x)
										 .attr("y", 0)
										 .attr("dy", -3)
										 .attr("text-anchor", "middle")
										 .text(String);
									
									
									// Data and labels
											  
									chart.selectAll("rect")
										 .data(data)
										 .enter().append("rect")
										 .attr("y", function(d, i) { return i * y.rangeBand(); })
										 .attr("width", function(d, i) { return x(d.mean); })
										 .attr("height", y.rangeBand());
										 
									chart.selectAll("labels")
										 .data(data)
										 .enter().append("text")
										 .attr("x", -8)
										 .attr("y", function(d, i) { return y.rangeBand() * i + y.rangeBand() / 2; })
										 .attr("dx", 0) // padding-right
										 .attr("dy", ".35em") // vertical-align: middle
										 .attr("text-anchor", "end") // text-align: right
										 .text(function(d, i) { return d.label; });
										 
									chart.selectAll("data_values")
										 .data(data)
										 .enter().append("text")
										 .attr("x", function(d, i) { return x(d.mean); })
										 .attr("y", function(d, i) { return y.rangeBand() * i + y.rangeBand() / 2; })
										 .attr("dx", 5) // padding-right
										 .attr("dy", ".35em") // vertical-align: middle
										 .text(function(d, i) { return "" + d.mean.toFixed(2) + " ms"; });
						
						
									// Zero axis line
									
									chart.append("line")
										 .attr("y1", 0)
										 .attr("y2", bar_height * data.length + padding_top)
										 .style("stroke", "#000");
								});
								
								//  End -->
							</script>
							
							</div></div>

							<%= writer.toString() %>
						<%
					}
				%>
				
				
				<h3>Log File</h3>
				
				<%
					f = job.getLogFile();
					if (f == null) {
						%>
							<p class="simple_status_warning">Not found.</p>
						<%
					}
					else {
						%>
							<div>
								<button onclick="replace_by_log_file(this, '<%= job.getId() %>', false)">
									Show...
								</button>
							</div>
						<%
					}
				%>
				
				
				<h3>Warmup Log File</h3>
								
				<%
					f = job.getWarmupLogFile();
					if (f == null) {
						%>
							<p class="simple_status_warning">Not found.</p>
						<%
					}
					else {
						String efn = StringEscapeUtils.escapeJavaScript(f.getName());
						String dbe = job.getDbEngine();
						String dbi = job.getDbInstance();
						if (dbi == null) dbi = "";
						%>
							<div>
								<button onclick="replace_by_log_file(this, '<%= job.getId() %>', true)">
									Show...
								</button>
							</div>
						<%
					}
				%>
			<%
			}
		%>
	</div>

<%@ include file="include/footer.jsp" %>
