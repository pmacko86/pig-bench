<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<%@ page import="com.tinkerpop.bench.Bench"%>
<%@ page import="com.tinkerpop.bench.DatabaseEngine"%>
<%@ page import="com.tinkerpop.bench.util.LogUtils"%>
<%@ page import="com.tinkerpop.bench.Workload"%>
<%@ page import="com.tinkerpop.bench.benchmark.BenchmarkMicro"%>
<%@ page import="com.tinkerpop.bench.log.SummaryLogEntry"%>
<%@ page import="com.tinkerpop.bench.log.SummaryLogReader"%>
<%@ page import="com.tinkerpop.bench.util.NaturalStringComparator"%>
<%@ page import="com.tinkerpop.bench.util.Pair"%>
<%@ page import="com.tinkerpop.bench.web.*"%>
<%@ page import="java.io.*"%>
<%@ page import="java.text.SimpleDateFormat"%>
<%@ page import="java.util.*"%>
<%@ page import="au.com.bytecode.opencsv.CSVReader"%>
<%@ page import="org.apache.commons.lang.StringEscapeUtils"%>

<%
	String jsp_title = "Compare Runs";
	String jsp_page = "compareruns";
	String jsp_body = "onload=\"body_on_load()\" onunload=\"\"";
	boolean jsp_allow_cache = true;
%>
<%@ include file="include/header.jsp" %>
	
	<script src="/include/d3.v2.js"></script>
	<script src="/include/scroll-sneak.js"></script>
	<script src="/include/BlobBuilder.min.js"></script>
	<script src="/include/FileSaver.min.js"></script>
	
	<script language="JavaScript">
		<!-- Begin
		
		var scroll_sneak;
				
		/*
		 * Handler for body load
		 */
		function body_on_load()
		{
			e = document.getElementById("refreshed");
			if (e.value=="no") {
				e.value="yes";
			}
			else {
				e.value="no";
				//location.reload();
			}
		}
		
		/*
		 * Submit the form
		 */
		function form_submit() {
			scroll_sneak.sneak();
			document.getElementById('form').submit();
			return true;
		}
				
		/*
		 * Replace the parent of the given element by the contents of the page at the given URL
		 */
		function replace_by_page(element, url)
		{
			var node = element.parentNode;
			var http_request = new XMLHttpRequest();
			http_request.open("GET", url, true);
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
		 * Save the contents of an element to file
		 */
		function save_svg(element, file)
		{
			var bb = new BlobBuilder;
			bb.append(document.getElementById(element).innerHTML);
			saveAs(bb.getBlob("image/svg+xml;charset=utf-8"), file);
		}
		
		
		//  End -->
	</script>
	
	<div class="stylized_form">
		<form id="form" name="form" method="post" action="/compareruns.jsp">
			<h1>Compare Runs</h1>
			<p class="header">Compare the performance of multiple runs of the same operation on the same database</p>
			
			
			<!-- Database Engine / Instance Names -->
			
			<p class="header2">1) Select a combination of a database engine name and an instance name:</p>
			
			<%
				boolean dbinst_simple = false;
				boolean dbinst_choose_many = false;
				boolean dbinst_choose_nonexistent = false;
				String dbinst_onchange = "form_submit();";
			%>
			<div class="db_table_div">
				<%@ include file="include/dbinsttable.jsp" %>
			</div>
			
			
			
			<!-- Operations -->
			
			<%
				// Operations
				
				if (!selectedDatabaseInstances.isEmpty()) {
					%>
						<p class="middle">2) Select operations to compare:</p>
					<%
				}
			
				boolean selectoperations_selectMultiple = false;
			%>
			<%@ include file="include/selectoperations.jsp" %>

			
			
			<!-- Jobs -->
		
			<%
				SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("[yyyy/MM/dd HH:mm:ss]");
				
			
				
				// Jobs
				
				TreeSet<String> selectedJobIds = new TreeSet<String>();
				
				if (!selectedOperations.isEmpty()) {
					%>
					
					<div id="select_job">
						<p class="middle">3) Select relevant jobs for each operation and each database engine/instance pair:</p>
						
						<%
							for (String operationName : selectedOperations) {
								Set<Job> ojs = operationToJobsMap.get(operationName);
								if (ojs == null) continue;
								
								for (DatabaseEngineAndInstance p : selectedDatabaseInstances) {
									Set<Job> jobs = new TreeSet<Job>();
									for (Job j : selectedDatabaseInstanceToJobsMap.get(p)) {
										if (ojs.contains(j)) jobs.add(j);
									}
									if (jobs.isEmpty()) continue;
									
									String inputName = p.getEngine().getShortName()
											+ "-" + p.getInstanceSafe("")
											+ "-" + operationName;
									
									String[] params = WebUtils.getStringParameterValues(request, "run");
									if (params != null) {
										for (String pm : params) selectedJobIds.add(pm);
									}
									
									int index = 0;
									for (Job job : jobs) {
										index++;
										
										String extraTags = "";
										String id = "" + job.getId();
										
										if (selectedJobIds.contains(id)) {
											extraTags += " checked=\"checked\"";
										}
										
										String prefix = dateTimeFormatter.format(job.getExecutionTime()) + " ";
										
										%>
											<label class="checkbox">
												<input class="checkbox" type="checkbox" name="run"
														onchange="form_submit();" <%= extraTags %>
														value="<%= id %>"/>
												<%= prefix + job.toString() %>
											</label>
										<%
									}
								}
							}
						%>
						<div class="clear"></div>
					</div>
			<%
				}
			%>
			
			<input type="hidden" name="refreshed" id="refreshed" value="no" />

			<div class="spacer"></div>
		</form>
	</div>
		
	<div class="basic_form">
		<%		
			if (selectedOperations.size() > 0) {
				
				LinkedList<Job> currentJobs = new LinkedList<Job>();
				for (String s_id : selectedJobIds) {
					Job job = JobList.getInstance().getFinishedJob(Integer.parseInt(s_id));
					currentJobs.add(job);
				}
				TreeMap<String, Collection<Job>> operationsToJobs = new TreeMap<String, Collection<Job>>();
				operationsToJobs.put(selectedOperations.iterator().next(), currentJobs);

				%>
					<h2>Summary</h2>
				<%
				
				StringWriter writer = new StringWriter();
				ShowOperationRunTimes.printRunTimesSummary(new PrintWriter(writer), operationsToJobs, "html", null, "run");
				
				String link = "/ShowOperationRunTimes?group_by=run";
				boolean sameDbInstance = true;
				Job firstJob = null;
				
				for (String operationName : selectedOperations) {
					String eon = StringEscapeUtils.escapeJavaScript(operationName);
					link += "&operations=" + eon;
					for (Job j : currentJobs) {
						link += "&jobs-" + eon + "=" + j.getId();
						if (firstJob == null) {
							firstJob = j;
						}
						else if (sameDbInstance) {
							sameDbInstance = firstJob.getDbInstanceSafe().equals(j.getDbInstanceSafe());
						}
					}
				}
				
				boolean boxPlots = true;
				String boxPlotFilter = "true";
				String boxPlotYValue = "d.time";
				boolean logScale = false;
				boolean dropExtremes = true;
				
				if (boxPlots) {
					ChartProperties chartProperties = new ChartProperties();
					
					chartProperties.source = link + "&show=details&format=csv";
					chartProperties.attach = "chart_all_details";
					chartProperties.filter = boxPlotFilter;
					chartProperties.yvalue = boxPlotYValue;
					if (logScale) chartProperties.yscale = "log";
					if (dropExtremes) chartProperties.dropTopBottomExtremes = true;
					chartProperties.ylabel = "d.time".equals(boxPlotYValue) 
							? "Execution Time (ms)" : StringEscapeUtils.escapeXml(boxPlotYValue);	// TODO Need better escape
					chartProperties.group_by = "operation";
					chartProperties.group_label_function = "return d.operation.replace(/^Operation/, '')";
					chartProperties.category_label_function = "return d.dbengine" + (sameDbInstance ? "" : " + ', ' + d.dbinstance");
					
					%>
						<div class="chart_outer">
							<div class="chart chart_all_details" id="chart_all_details"></div>
							<%@ include file="include/d3boxplot.jsp" %>
							<a class="chart_save" href="javascript:save_svg('chart_all_details', 'plot.svg')">Save</a>
						</div>
					<%
				}
				
				boolean plotTimeVsRetrievedNodes_enabled = true;
				boolean plotTimeVsRetrievedNodes = true;

				if (plotTimeVsRetrievedNodes && plotTimeVsRetrievedNodes_enabled) {
					ChartProperties chartProperties = new ChartProperties();
					
					chartProperties.source = link + "&show=details&format=csv";
					chartProperties.attach = "chart_all_plotTimeVsRetrievedNodes";
					chartProperties.filter = boxPlotFilter;
					chartProperties.xvalue = "d.result[0]";
					chartProperties.yvalue = boxPlotYValue;
					if (logScale) chartProperties.yscale = "log";
					if (dropExtremes) chartProperties.dropTopBottomExtremes = true;
					chartProperties.xlabel = "Number of Retrieved Unique Nodes";
					chartProperties.ylabel = "d.time".equals(boxPlotYValue) 
							? "Execution Time (ms)" : StringEscapeUtils.escapeXml(boxPlotYValue);	// TODO Need better escape
					chartProperties.series_column = "label";
					chartProperties.series_label_function = "return d.label.replace(/^Operation/, '')";
					
					%>
						<div class="chart_outer">
							<div class="chart chart_all_plotTimeVsRetrievedNodes" id="chart_all_plotTimeVsRetrievedNodes"></div>
							<%@ include file="include/d3scatterplot.jsp" %>
							<a class="chart_save" href="javascript:save_svg('chart_all_plotTimeVsRetrievedNodes', 'plot.svg')">Save</a>
						</div>
					<%
				}
				
				boolean additionalKHopNeighborsPlots_enabled = true;
				boolean additionalKHopNeighborsPlots = true;
				
				if (additionalKHopNeighborsPlots && additionalKHopNeighborsPlots_enabled) {
					ChartProperties chartProperties = new ChartProperties();
					
					chartProperties.source = link + "&show=details&format=csv";
					chartProperties.attach = "chart_all_additionalKHopNeighborsPlots_1";
					chartProperties.filter = boxPlotFilter;
					chartProperties.xvalue = "d.result[2]";
					chartProperties.yvalue = boxPlotYValue;
					if (logScale) chartProperties.yscale = "log";
					if (dropExtremes) chartProperties.dropTopBottomExtremes = true;
					chartProperties.xlabel = "Number of Retrieved Neighborhoods"; // e.g. calls to Vertex.getVertices()
					chartProperties.ylabel = "d.time".equals(boxPlotYValue) 
							? "Execution Time (ms)" : StringEscapeUtils.escapeXml(boxPlotYValue);	// TODO Need better escape
					chartProperties.series_column = "label";
					chartProperties.series_label_function = "return d.label.replace(/^Operation/, '')";
					
					%>
						<div class="chart_outer">
							<div class="chart chart_all_additionalKHopNeighborsPlots_1" id="chart_all_additionalKHopNeighborsPlots_1"></div>
							<%@ include file="include/d3scatterplot.jsp" %>
							<a class="chart_save" href="javascript:save_svg('chart_all_additionalKHopNeighborsPlots_1', 'plot.svg')">Save</a>
						</div>
					<%
					
					chartProperties.attach = "chart_all_additionalKHopNeighborsPlots_2";
					chartProperties.xvalue = "d.result[3]";
					chartProperties.xlabel = "Number of Retrieved Neighborhood Nodes";
					
					%>
						<div class="chart_outer">
							<div class="chart chart_all_additionalKHopNeighborsPlots_2" id="chart_all_additionalKHopNeighborsPlots_2"></div>
							<%@ include file="include/d3scatterplot.jsp" %>
							<a class="chart_save" href="javascript:save_svg('chart_all_additionalKHopNeighborsPlots_2', 'plot.svg')">Save</a>
						</div>
					<%
				}
				
				%>
					<%= writer.toString() %>
					
					<div style="height:10px"></div>
					
					<div>
						<button onclick="replace_by_page(this, '<%= StringEscapeUtils.escapeJavaScript(link + "&show=details&format=html") %>')">
							Show Details...
						</button>
					</div>
					
					<div style="height:40px"></div>
				<%
			}
		%>
	</div>

	<script language="JavaScript">
		<!-- Begin
		
		scroll_sneak = new ScrollSneak(location.href);
		
		//  End -->
	</script>

<%@ include file="include/footer.jsp" %>
