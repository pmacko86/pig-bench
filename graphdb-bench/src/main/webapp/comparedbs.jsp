<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<%@ page import="com.tinkerpop.bench.Bench"%>
<%@ page import="com.tinkerpop.bench.DatabaseEngine"%>
<%@ page import="com.tinkerpop.bench.Workload"%>
<%@ page import="com.tinkerpop.bench.analysis.ModelAnalysis"%>
<%@ page import="com.tinkerpop.bench.benchmark.BenchmarkMicro"%>
<%@ page import="com.tinkerpop.bench.log.SummaryLogEntry"%>
<%@ page import="com.tinkerpop.bench.log.SummaryLogReader"%>
<%@ page import="com.tinkerpop.bench.util.*"%>
<%@ page import="com.tinkerpop.bench.web.*"%>
<%@ page import="java.io.*"%>
<%@ page import="java.text.SimpleDateFormat"%>
<%@ page import="java.util.*"%>
<%@ page import="au.com.bytecode.opencsv.CSVReader"%>
<%@ page import="org.apache.commons.lang.StringEscapeUtils"%>

<%
	String jsp_title = "Compare Databases";
	String jsp_page = "comparedbs";
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
		<form id="form" name="form" method="post" action="/comparedbs.jsp">
			<h1>Compare Databases</h1>
			<p class="header">Compare the performance of selected operations across multiple databases</p>
			
			
			<!-- Database Engine / Instance Names -->
			
			<p class="header2">1) Select two or more database engine names / instance names:</p>
			
			<%
				boolean dbinst_simple = false;
				boolean dbinst_choose_many = true;
				boolean dbinst_choose_nonexistent = false;
				boolean dbinst_choose_based_on_available_datasets = false;
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
			
				boolean selectoperations_selectMultiple = true;
			%>
			<%@ include file="include/selectoperations.jsp" %>

			
			
			<!-- Jobs -->
		
			<%
				SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("[yyyy/MM/dd HH:mm:ss]");
				
			
				// A map of selected [[database engine, database instance name], operation name] pairs to a sorted set of selected jobs
				TreeMap<Pair<DatabaseEngineAndInstance, String>, SortedSet<Job>>
					selectedDatabaseInstanceAndOperationToSelectedJobsMap
					= new TreeMap<Pair<DatabaseEngineAndInstance, String>, SortedSet<Job>>();
				
				if (!selectedOperations.isEmpty()) {
					%>
					
					<div id="select_job">
						<p class="middle">3) Select relevant jobs for each operation and each database engine/instance pair:</p>
						
						<%
							for (String operationName : selectedOperations) {
								Set<Job> ojs = operationToJobsMap.get(operationName);
								if (ojs == null) continue;
								String niceOperationName = operationName;
								if (niceOperationName.startsWith("Operation")) {
									niceOperationName = niceOperationName.substring(9);
								}
								
								%>
									<p class="middle_inner"><%= niceOperationName %></p>
								<%
								
								for (DatabaseEngineAndInstance p : selectedDatabaseInstances) {
									SortedSet<Job> jobs = new TreeSet<Job>();
									for (Job j : selectedDatabaseInstanceToJobsMap.get(p)) {
										if (ojs.contains(j)) jobs.add(j);
									}
									if (jobs.isEmpty()) continue;
									
									String inputName = p.getEngine().getShortName()
											+ "-" + p.getInstanceSafe("")
											+ "-" + operationName;
									
									String[] params = WebUtils.getStringParameterValues(request, inputName);
									TreeSet<Job> selectedJobsForSelectedDatabaseInstanceAndOperation = new TreeSet<Job>();
									if (params != null) {
										for (String pm : params) {
											int jobId = Integer.parseInt(pm);
											Job job = JobList.getInstance().getFinishedJob(jobId);
											if (job == null) continue;
											if (!jobs.contains(job)) continue;
											selectedJobsForSelectedDatabaseInstanceAndOperation.add(job);
										}
									}
									selectedDatabaseInstanceAndOperationToSelectedJobsMap.put(
											new Pair<DatabaseEngineAndInstance, String>(p, operationName),
											selectedJobsForSelectedDatabaseInstanceAndOperation);
									
									if (selectedJobsForSelectedDatabaseInstanceAndOperation.isEmpty()) {
										Job lastGoodJob = null;
										for (Job job : jobs) {
											if (job.getArguments().contains("--use-stored-procedures")) continue;
											lastGoodJob = job;
										}
										if (lastGoodJob == null) {
											lastGoodJob = jobs.last();
										}
										if (lastGoodJob != null) {
											selectedJobsForSelectedDatabaseInstanceAndOperation.add(lastGoodJob);
										}
									}

									%>
										<label class="lesser">
											<%= p.getEngine().getLongName() %>
											<span class="small">
											<%= p.getInstanceSafe("&lt;default&gt;") %>
											</span>
										</label>
										<select name="<%= inputName %>" id="<%= inputName %>"
												onchange="form_submit();">
									<%
									
									int index = 0;
									for (Job job : jobs) {
										index++;
										
										String extraTags = "";
										if (selectedJobsForSelectedDatabaseInstanceAndOperation.contains(job)) {
											extraTags += " selected=\"selected\"";
										}
										
										String prefix = dateTimeFormatter.format(job.getExecutionTime()) + " ";
										
										%>
											<option value="<%= job.getId() %>"<%= extraTags %>><%= prefix + job.toString() %></option>
										<%
									}
									%>
										</select>
									<%
								}
							}
						%>
						<div class="clear"></div>
					</div>
			<%
				}
				
				
				// Display options
				
				boolean barGraphs = WebUtils.getBooleanParameter(request, "bargraphs", false);
				boolean logScale_barGraphs = WebUtils.getBooleanParameter(request, "logscale_bargraphs", false);
				boolean separateGraphForEachOperation = WebUtils.getBooleanParameter(request, "eachop", false);
				boolean boxPlots = WebUtils.getBooleanParameter(request, "boxplots", false);
				boolean logScale = WebUtils.getBooleanParameter(request, "logscale", false);
				boolean dropExtremes = WebUtils.getBooleanParameter(request, "dropextremes", false);
				boolean plotTimeVsRetrievedNodes = WebUtils.getBooleanParameter(request, "plotTimeVsRetrievedNodes", false);
				boolean plotTimeVsMaxDepth = WebUtils.getBooleanParameter(request, "plotTimeVsMaxDepth", false);
				boolean plotTimeVsRetrievedNeighborhoods = WebUtils.getBooleanParameter(request, "plotTimeVsRetrievedNeighborhoods", false);
				boolean plotTimeVsRetrievedNeighborhoodNodes = WebUtils.getBooleanParameter(request, "plotTimeVsRetrievedNeighborhoodNodes", false);
				boolean modelAnalysisLinearFits = WebUtils.getBooleanParameter(request, "modelAnalysisLinearFits", false);
				boolean modelAnalysisPredictions = WebUtils.getBooleanParameter(request, "modelAnalysisPredictions", false);
				
				String boxPlotFilter = WebUtils.getStringParameter(request, "boxplotfilter");
				if (boxPlotFilter == null) boxPlotFilter = "true";
				// XXX Further sanitize boxPlotFilter!
				boxPlotFilter = boxPlotFilter.replace(';', ' ');
				boxPlotFilter = boxPlotFilter.replace('\\', ' ');
				
				String boxPlotYValue = WebUtils.getStringParameter(request, "boxplotyvalue");
				if (boxPlotYValue == null) boxPlotYValue = "d.time";
				// XXX Further sanitize boxPlotYValue!
				boxPlotYValue = boxPlotYValue.replace(';', ' ');
				boxPlotYValue = boxPlotYValue.replace('\\', ' ');
				
				boolean benchmarksWithExtendedInfo = true;
				for (String s : selectedOperations) {
					if (!s.startsWith("OperationGetAllNeighbors")
							&& !s.startsWith("OperationGetFirstNeighbor")
							&& !s.startsWith("OperationGetRandomNeighbor")
							&& !s.startsWith("OperationGetNeighborEdgeConditional")
							&& !s.startsWith("OperationGetKFirstNeighbors")
							&& !s.startsWith("OperationGetKHopNeighbors")
							&& !s.startsWith("OperationGetKHopNeighborsEdgeConditional")
							&& !s.startsWith("OperationGetKRandomNeighbors")
							&& !s.startsWith("OperationGetShortestPath")
							&& !s.startsWith("OperationLocalClusteringCoefficient")
							) {
						benchmarksWithExtendedInfo = false;
						break;
					}
				}
				
				if (!selectedOperations.isEmpty()) {
					%>
						<div id="select_job">
							<p class="middle">4) Select display options:</p>
							
							
							<p class="middle_inner">Summary Bar Graphs</p>
							
							<label class="checkbox">
								<input class="checkbox" type="checkbox"
										name="bargraphs" id="bargraphs"
										onchange="form_submit();" <%= barGraphs ? "checked=\"checked\"" : "" %>
										value="true"/>
								Display the summary bar graph
							</label>
							
							<label class="checkbox">
								<input class="checkbox" type="checkbox"
										name="eachop" id="eachop"
										onchange="form_submit();" <%= separateGraphForEachOperation ? "checked=\"checked\"" : "" %>
										value="true"/>
								Display a bar graph and a data table for each operation 
							</label>
							
							<label class="checkbox">
								<input class="checkbox" type="checkbox"
										name="logscale_bargraphs" id="logscale_bargraphs"
										onchange="form_submit();" <%= logScale_barGraphs ? "checked=\"checked\"" : "" %>
										value="true"/>
								Use log scale on all bar graphs
							</label>
							
							
							<div style="height:10px"></div>
							<p class="middle_inner">Data Plots &ndash; Generic Options</p>
							
							<label class="checkbox">
								<input class="checkbox" type="checkbox"
										name="logscale" id="logscale"
										onchange="form_submit();" <%= logScale ? "checked=\"checked\"" : "" %>
										value="true"/>
								Use log scale
							</label>
							
							<label class="checkbox">
								<input class="checkbox" type="checkbox"
										name="dropextremes" id="dropextremes"
										onchange="form_submit();" <%= dropExtremes ? "checked=\"checked\"" : "" %>
										value="true"/>
								Drop top and bottom 5% of values
							</label>
							
							
							<div style="height:10px"></div>
							<p class="middle_inner">Data Plots &ndash; Box Plots</p>
							
							<label class="checkbox">
								<input class="checkbox" type="checkbox"
										name="boxplots" id="boxplots"
										onchange="form_submit();" <%= boxPlots ? "checked=\"checked\"" : "" %>
										value="true"/>
								Display the summary box plot
							</label>
							
							<div style="height:10px"></div>
							
							<label>
								Filter =
								<span class="small">
									Use: d.time, d.result[], etc.
								</span>
							</label>
							<input type="text" name="boxplotfilter" id="boxplotfilter"
									onchange="form_submit();"
									value="<%= StringEscapeUtils.escapeHtml(boxPlotFilter) %>"/>
							
							<label>
								Y Value =
								<span class="small">
									Use: d.time, d.result[], etc.
								</span>
							</label>
							<input type="text" name="boxplotyvalue" id="boxplotyvalue"
									onchange="form_submit();"
									value="<%= StringEscapeUtils.escapeHtml(boxPlotYValue) %>"/>
							
							
							<div style="height:10px"></div>
							<p class="middle_inner">Data Plots &ndash; Specialty Plots (selected workload types only)</p>
							
							<label class="checkbox">
								<input class="checkbox" type="checkbox"
										name="plotTimeVsRetrievedNodes" id="plotTimeVsRetrievedNodes"
										onchange="form_submit();" <%= plotTimeVsRetrievedNodes ? "checked=\"checked\"" : "" %>
										<%= !benchmarksWithExtendedInfo ? "disabled=\"disabled\"" : ""  %>
										value="true"/>
								Execution Time vs. Number of Returned Unique Nodes
							</label>
							
							<label class="checkbox">
								<input class="checkbox" type="checkbox"
										name="plotTimeVsMaxDepth" id="plotTimeVsMaxDepth"
										onchange="form_submit();" <%= plotTimeVsMaxDepth ? "checked=\"checked\"" : "" %>
										<%= !benchmarksWithExtendedInfo ? "disabled=\"disabled\"" : ""  %>
										value="true"/>
								Execution Time vs. Maximum Depth (value of K)
							</label>
							
							<label class="checkbox">
								<input class="checkbox" type="checkbox"
										name="plotTimeVsRetrievedNeighborhoods" id="plotTimeVsRetrievedNeighborhoods"
										onchange="form_submit();" <%= plotTimeVsRetrievedNeighborhoods ? "checked=\"checked\"" : "" %>
										<%= !benchmarksWithExtendedInfo ? "disabled=\"disabled\"" : ""  %>
										value="true"/>
								Execution Time vs. Number of Retrieved Neighborhoods
							</label>
							
							<label class="checkbox">
								<input class="checkbox" type="checkbox"
										name="plotTimeVsRetrievedNeighborhoodNodes" id="plotTimeVsRetrievedNeighborhoodNodes"
										onchange="form_submit();" <%= plotTimeVsRetrievedNeighborhoodNodes ? "checked=\"checked\"" : "" %>
										<%= !benchmarksWithExtendedInfo ? "disabled=\"disabled\"" : ""  %>
										value="true"/>
								Execution Time vs. Number of Retrieved Neighborhood Nodes
							</label>
							
							
							<div style="height:10px"></div>
							<p class="middle_inner">Model Analysis &ndash; "Execution Time vs. Number of Retrieved Neighborhood Nodes"</p>
							
							<label class="checkbox">
								<input class="checkbox" type="checkbox"
										name="modelAnalysisLinearFits" id="modelAnalysisLinearFits"
										onchange="form_submit();" <%= modelAnalysisLinearFits ? "checked=\"checked\"" : "" %>
										value="true"/>
								Linear Fit
							</label>
							<label class="checkbox">
								<input class="checkbox" type="checkbox"
										name="modelAnalysisPredictions" id="modelAnalysisPredictions"
										onchange="form_submit();" <%= modelAnalysisPredictions ? "checked=\"checked\"" : "" %>
										value="true"/>
								Model Prediction
							</label>
							
				
							<p class="middle"></p>
							<div class="clear"></div>
						</div>
					<%
				}
				else {
					%>
						<input type="hidden" name="bargraphs" id="bargraphs" value="<%= "" + barGraphs %>" />
						<input type="hidden" name="logscale_bargraphs" id="logscale_bargraphs" value="<%= "" + logScale_barGraphs %>" />
						<input type="hidden" name="boxplots" id="boxplots" value="<%= "" + boxPlots %>" />
						<input type="hidden" name="logscale" id="logscale" value="<%= "" + logScale %>" />
						<input type="hidden" name="dropextremes" id="dropextremes" value="<%= "" + dropExtremes %>" />
						<input type="hidden" name="plotTimeVsRetrievedNodes" id="plotTimeVsRetrievedNodes" value="<%= "" + plotTimeVsRetrievedNodes %>" />
						<input type="hidden" name="plotTimeVsMaxDepth" id="plotTimeVsMaxDepth" value="<%= "" + plotTimeVsMaxDepth %>" />
						<input type="hidden" name="plotTimeVsRetrievedNeighborhoods" id="plotTimeVsRetrievedNeighborhoods" value="<%= "" + plotTimeVsRetrievedNeighborhoods %>" />
						<input type="hidden" name="plotTimeVsRetrievedNeighborhoodNodes" id="plotTimeVsRetrievedNeighborhoodNodes" value="<%= "" + plotTimeVsRetrievedNeighborhoodNodes %>" />
						<input type="hidden" name="modelAnalysisLinearFits" id="modelAnalysisLinearFits" value="<%= "" + modelAnalysisLinearFits %>" />
						<input type="hidden" name="modelAnalysisPredictions" id="modelAnalysisPredictions" value="<%= "" + modelAnalysisPredictions %>" />
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
				
				TreeMap<String, Collection<Job>> operationsToJobs = new TreeMap<String, Collection<Job>>();
				for (String operationName : selectedOperations) {
					LinkedList<Job> currentJobs = new LinkedList<Job>();
					for (DatabaseEngineAndInstance p : selectedDatabaseInstances) {
						Collection<Job> jobs = selectedDatabaseInstanceAndOperationToSelectedJobsMap.get(
								new Pair<DatabaseEngineAndInstance, String>(p, operationName));
						currentJobs.addAll(jobs);
					}
					operationsToJobs.put(operationName, currentJobs);
				}
				
				%>
					<h2>Summary</h2>
				<%
				
				StringWriter writer = new StringWriter();
				ShowOperationRunTimes.printRunTimes(new PrintWriter(writer), operationsToJobs, "html", null);
				
				String link = "/ShowOperationRunTimes?group_by=operation";
				boolean sameDbInstance = true;
				Job firstJob = null;
				
				for (String operationName : selectedOperations) {
					String eon = StringEscapeUtils.escapeJavaScript(operationName);
					link += "&operations=" + eon;
					for (Job j : operationsToJobs.get(operationName)) {
						link += "&jobs-" + eon + "=" + j.getId();
						if (firstJob == null) {
							firstJob = j;
						}
						else if (sameDbInstance) {
							sameDbInstance = firstJob.getDbInstanceSafe().equals(j.getDbInstanceSafe());
						}
					}
				}
				
				List<Triple<String, DatabaseEngineAndInstance, Double[]>> linearFitData
					= new ArrayList<Triple<String, DatabaseEngineAndInstance, Double[]>>();
				List<Triple<String, DatabaseEngineAndInstance, Double[]>> predictionData
					= new ArrayList<Triple<String, DatabaseEngineAndInstance, Double[]>>();
				
				if (modelAnalysisLinearFits || modelAnalysisPredictions) {
					for (String operationName : selectedOperations) {
						for (Job j : operationsToJobs.get(operationName)) {
							DatabaseEngineAndInstance dbei = j.getDatabaseEngineAndInstance();
							ModelAnalysis m = ModelAnalysis.getInstance(dbei);
							Double[] p = null;
							
							p = m.getLinearFit(operationName);
							linearFitData.add(new Triple<String, DatabaseEngineAndInstance, Double[]>(operationName, dbei, p));
							
							p = m.getPrediction(operationName);
							predictionData.add(new Triple<String, DatabaseEngineAndInstance, Double[]>(operationName, dbei, p));
						}
					}
				}
				
				
				if (barGraphs) {
					ChartProperties chartProperties = new ChartProperties();
					
					chartProperties.source = link + "&format=csv";
					chartProperties.attach = "chart_all";
					chartProperties.yvalue = "d.mean";
					chartProperties.ylabel = "Execution Time (ms)";
					if (logScale_barGraphs) chartProperties.yscale = "log";
					chartProperties.group_by = "operation";
					chartProperties.group_label_function = "return d.operation.replace(/^Operation/, '')";
					chartProperties.category_label_function = "return d.dbengine" + (sameDbInstance ? "" : " + ', ' + d.dbinstance");
					
					%>
						<div class="chart_outer">
							<div class="chart chart_all" id="chart_all"></div>
							<%@ include file="include/d3barchart.jsp" %>
							<a class="chart_save" href="javascript:save_svg('chart_all', 'summary-bar-chart.svg')">Save</a>
						</div>
					<%
				}
				
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
				
				if (plotTimeVsRetrievedNodes && benchmarksWithExtendedInfo) {
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
				
				if ((plotTimeVsRetrievedNeighborhoodNodes || plotTimeVsRetrievedNeighborhoods || plotTimeVsMaxDepth) && benchmarksWithExtendedInfo) {
					ChartProperties chartProperties = new ChartProperties();
					
					chartProperties.source = link + "&show=details&format=csv";
					chartProperties.attach = "chart_all_additionalKHopNeighborsPlots_1";
					chartProperties.filter = boxPlotFilter;
					chartProperties.xvalue = "d.result[1]";
					chartProperties.yvalue = boxPlotYValue;
					if (logScale) chartProperties.yscale = "log";
					if (dropExtremes) chartProperties.dropTopBottomExtremes = true;
					chartProperties.xlabel = "Maximum Depth";
					chartProperties.ylabel = "d.time".equals(boxPlotYValue) 
							? "Execution Time (ms)" : StringEscapeUtils.escapeXml(boxPlotYValue);	// TODO Need better escape
					chartProperties.series_column = "label";
					chartProperties.series_label_function = "return d.label.replace(/^Operation/, '')";
					
					if (plotTimeVsMaxDepth) {
						%>
							<div class="chart_outer">
								<div class="chart chart_all_additionalKHopNeighborsPlots_1" id="chart_all_additionalKHopNeighborsPlots_1"></div>
								<%@ include file="include/d3scatterplot.jsp" %>
								<a class="chart_save" href="javascript:save_svg('chart_all_additionalKHopNeighborsPlots_1', 'plot.svg')">Save</a>
							</div>
						<%
					}
					
					chartProperties.attach = "chart_all_additionalKHopNeighborsPlots_2";
					chartProperties.xvalue = "d.result[2]";
					chartProperties.xlabel = "Number of Retrieved Neighborhoods"; // e.g. calls to Vertex.getVertices()
					
					if (plotTimeVsRetrievedNeighborhoods) {
						%>
							<div class="chart_outer">
								<div class="chart chart_all_additionalKHopNeighborsPlots_2" id="chart_all_additionalKHopNeighborsPlots_1"></div>
								<%@ include file="include/d3scatterplot.jsp" %>
								<a class="chart_save" href="javascript:save_svg('chart_all_additionalKHopNeighborsPlots_2', 'plot.svg')">Save</a>
							</div>
						<%
					}
					
					chartProperties.attach = "chart_all_additionalKHopNeighborsPlots_3";
					chartProperties.xvalue = "d.result[3]";
					chartProperties.xlabel = "Number of Retrieved Neighborhood Nodes";
					
					chartProperties.linear_fits.clear();
					if (modelAnalysisLinearFits) {
						for (Triple<String, DatabaseEngineAndInstance, Double[]> p : linearFitData) {
							chartProperties.linear_fits.add(p.getThird());
						}
					}
					
					chartProperties.predictions.clear();
					if (modelAnalysisPredictions) {
						for (Triple<String, DatabaseEngineAndInstance, Double[]> p : predictionData) {
							chartProperties.predictions.add(p.getThird());
						}
					}
					
					if (plotTimeVsRetrievedNeighborhoodNodes) {
						%>
							<div class="chart_outer">
								<div class="chart chart_all_additionalKHopNeighborsPlots_3" id="chart_all_additionalKHopNeighborsPlots_2"></div>
								<%@ include file="include/d3scatterplot.jsp" %>
								<a class="chart_save" href="javascript:save_svg('chart_all_additionalKHopNeighborsPlots_3', 'plot.svg')">Save</a>
							</div>
						<%
					}
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
		
		<%
			if (separateGraphForEachOperation) {
				for (String operationName : selectedOperations) {
					
					String niceOperationName = operationName;
					if (niceOperationName.startsWith("Operation")) {
						niceOperationName = niceOperationName.substring(9);
					}
					
					TreeMap<String, Collection<Job>> operationsToJobs = new TreeMap<String, Collection<Job>>();
					LinkedList<Job> currentJobs = new LinkedList<Job>();
					boolean sameDbInstance = true;
					Job firstJob = null;
					for (DatabaseEngineAndInstance p : selectedDatabaseInstances) {
						Collection<Job> jobs = selectedDatabaseInstanceAndOperationToSelectedJobsMap.get(
								new Pair<DatabaseEngineAndInstance, String>(p, operationName));
						currentJobs.addAll(jobs);
						for (Job job : jobs) {
							if (firstJob == null) {
								firstJob = job;
							}
							else if (sameDbInstance) {
								sameDbInstance = firstJob.getDbInstanceSafe().equals(job.getDbInstanceSafe());
							}
						}
					}
					operationsToJobs.put(operationName, currentJobs);
						
					%>
						<h2><%= niceOperationName %></h2>
					<%
					
					StringWriter writer = new StringWriter();
					ShowOperationRunTimes.printRunTimes(new PrintWriter(writer), operationsToJobs, "html", null);
					
					String eon = StringEscapeUtils.escapeJavaScript(operationName);
					String link = "/ShowOperationRunTimes?group_by=operation&operations=" + eon;
					for (Job j : currentJobs) {
						link += "&jobs-" + eon + "=" + j.getId();
					}
									
					ChartProperties chartProperties = new ChartProperties();
					
					chartProperties.source = link + "&format=csv";
					chartProperties.attach = "chart_" + operationName;
					chartProperties.ylabel = "Execution Time (ms)";
					if (logScale_barGraphs) chartProperties.yscale = "log";
					chartProperties.group_by = "operation";
					chartProperties.group_label_function = "return d.operation.replace(/^Operation/, '')";
					chartProperties.category_label_function = "return d.dbengine" + (sameDbInstance ? "" : " + ', ' + d.dbinstance");
					
					%>
						<div class="chart_outer"><div class="chart chart_<%= operationName %>">
						<%@ include file="include/d3barchart.jsp" %>
						</div></div>
	
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
			}
		%>
	</div>
	
	<script language="JavaScript">
		<!-- Begin
		
		scroll_sneak = new ScrollSneak(location.href);
		
		//  End -->
	</script>

<%@ include file="include/footer.jsp" %>
