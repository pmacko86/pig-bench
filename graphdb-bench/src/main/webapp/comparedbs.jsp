<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<%@page import="org.apache.commons.lang.StringUtils"%>
<%@ page import="com.tinkerpop.bench.Bench"%>
<%@ page import="com.tinkerpop.bench.DatabaseEngine"%>
<%@ page import="com.tinkerpop.bench.Workload"%>
<%@ page import="com.tinkerpop.bench.analysis.ModelAnalysis"%>
<%@ page import="com.tinkerpop.bench.benchmark.BenchmarkMicro"%>
<%@ page import="com.tinkerpop.bench.log.SummaryLogEntry"%>
<%@ page import="com.tinkerpop.bench.log.SummaryLogReader"%>
<%@ page import="com.tinkerpop.bench.util.*"%>
<%@ page import="com.tinkerpop.bench.web.*"%>
<%@ page import="com.tinkerpop.bench.web.ChartProperties.LinearFunction"%>
<%@ page import="java.io.*"%>
<%@ page import="java.text.SimpleDateFormat"%>
<%@ page import="java.util.regex.Pattern"%>
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
	<script src="/include/d3bp.js"></script>
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
			bb.append("\n<!-- Source URL: \n");
			
			var form = document.getElementById('form');
			var url = form.action;
			var url_num_arguments = 0;

			for (var i = 0; i < form.length; i++) {
				var e = form.elements[i];
				
				if (e.name == "refreshed") continue;
				if (e.type == "radio" || e.type == "checkbox") {
					if (!e.checked) continue;
				}
				
				if (url_num_arguments == 0) url += "?"; else url += "&";
				url += encodeURIComponent(e.name) + "=" + encodeURIComponent(e.value);
				url_num_arguments++;
			}
			
			bb.append(url);
			
			bb.append("\n-->\n");
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
											long jobId = Long.parseLong(pm);
											Job job = JobList.getInstance().getFinishedJobByPersistentId(jobId);
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
											<option value="<%= job.getPersistentId() %>"<%= extraTags %>><%= prefix + job.toString() %></option>
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
				
				boolean smallGraphs = WebUtils.getBooleanParameter(request, "smallgraphs", false);
				boolean convertManyOperations = WebUtils.getBooleanParameter(request, "convertmanyoperations", false);
				boolean barGraphs = WebUtils.getBooleanParameter(request, "bargraphs", false);
				boolean logScale_barGraphs = WebUtils.getBooleanParameter(request, "logscale_bargraphs", false);
				boolean patternFill_barGraphs = WebUtils.getBooleanParameter(request, "patternfill_bargraphs", false);
				boolean hideDataLabels_barGraphs = WebUtils.getBooleanParameter(request, "hidedatalabels_bargraphs", false);
				boolean separateGraphForEachOperation = WebUtils.getBooleanParameter(request, "eachop", false);
				boolean boxPlots = WebUtils.getBooleanParameter(request, "boxplots", false);
				boolean logScale = WebUtils.getBooleanParameter(request, "logscale", false);
				boolean dropExtremes = WebUtils.getBooleanParameter(request, "dropextremes", false);
				boolean plotTimeVsRetrievedNodes = WebUtils.getBooleanParameter(request, "plotTimeVsRetrievedNodes", false);
				boolean plotTimeVsMaxDepth = WebUtils.getBooleanParameter(request, "plotTimeVsMaxDepth", false);
				boolean plotTimeVsRetrievedNeighborhoods = WebUtils.getBooleanParameter(request, "plotTimeVsRetrievedNeighborhoods", false);
				boolean plotTimeVsRetrievedNeighborhoodNodes = WebUtils.getBooleanParameter(request, "plotTimeVsRetrievedNeighborhoodNodes", false);
				boolean linearFits = WebUtils.getBooleanParameter(request, "linearFits", false);
				boolean modelAnalysisPredictions = WebUtils.getBooleanParameter(request, "modelAnalysisPredictions", false);
				boolean mergeKHops = WebUtils.getBooleanParameter(request, "mergeKHops", false);
				
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
								
							<div style="height:10px"></div>
							<p class="middle_inner">Generic Options for All Plots</p>
								
							<label class="checkbox">
								<input class="checkbox" type="checkbox"
										name="smallgraphs" id="smallgraphs"
										onchange="form_submit();" <%= smallGraphs ? "checked=\"checked\"" : "" %>
										value="true"/>
								Produce smaller graphs
							</label>
								
							<label class="checkbox">
								<input class="checkbox" type="checkbox"
										name="convertmanyoperations" id="convertmanyoperations"
										onchange="form_submit();" <%= convertManyOperations ? "checked=\"checked\"" : "" %>
										value="true"/>
								Convert "Many" operations to the equivalent micro operations that they consist of
							</label>
							
							
							<div style="height:20px"></div>
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
							
							<div class="medium_spacer"></div>
							
							<label class="checkbox">
								<input class="checkbox" type="checkbox"
										name="logscale_bargraphs" id="logscale_bargraphs"
										onchange="form_submit();" <%= logScale_barGraphs ? "checked=\"checked\"" : "" %>
										value="true"/>
								Use log scale on all bar graphs
							</label>
							
							<label class="checkbox">
								<input class="checkbox" type="checkbox"
										name="patternfill_bargraphs" id="patternfill_bargraphs"
										onchange="form_submit();" <%= patternFill_barGraphs ? "checked=\"checked\"" : "" %>
										value="true"/>
								Use patterns instead of solid colors (enable for B/W printing)
							</label>
							
							<label class="checkbox">
								<input class="checkbox" type="checkbox"
										name="hidedatalabels_bargraphs" id="hidedatalabels_bargraphs"
										onchange="form_submit();" <%= hideDataLabels_barGraphs ? "checked=\"checked\"" : "" %>
										value="true"/>
								Hide data labels
							</label>
							
							
							<div style="height:20px"></div>
							<p class="middle_inner">Generic Options for Data Plots</p>
							
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
							
							
							<div style="height:20px"></div>
							<p class="middle_inner">Data Box Plots</p>
							
							<label class="checkbox">
								<input class="checkbox" type="checkbox"
										name="boxplots" id="boxplots"
										onchange="form_submit();" <%= boxPlots ? "checked=\"checked\"" : "" %>
										value="true"/>
								Display the summary box plot
							</label>
							
							
							<div style="height:20px"></div>
							<p class="middle_inner">Data Scatter Plots (selected workload types only)</p>
							
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
														
							<div class="medium_spacer"></div>
							
							<label class="checkbox">
								<input class="checkbox" type="checkbox"
										name="linearFits" id="linearFits"
										onchange="form_submit();" <%= linearFits ? "checked=\"checked\"" : "" %>
										value="true"/>
								Linear fits
							</label>
							<label class="checkbox">
								<input class="checkbox" type="checkbox"
										name="modelAnalysisPredictions" id="modelAnalysisPredictions"
										onchange="form_submit();" <%= modelAnalysisPredictions ? "checked=\"checked\"" : "" %>
										value="true"/>
								Model prediction (on the "Execution Time vs. Number of Retrieved Neighborhood Nodes" plot only)
							</label>
							<label class="checkbox">
								<input class="checkbox" type="checkbox"
										name="mergeKHops" id="mergeKHops"
										onchange="form_submit();" <%= mergeKHops ? "checked=\"checked\"" : "" %>
										value="true"/>
								Merge the k-hops plots that have different values of k, but other than that, they have the same configuration
							</label>
							
				
							<p class="middle"></p>
							<div class="clear"></div>
						</div>
					<%
				}
				else {
					%>
						<input type="hidden" name="smallgraphs" id="smallgraphs" value="<%= "" + smallGraphs %>" />
						<input type="hidden" name="convertmanyoperations" id="convertmanyoperations" value="<%= "" + convertManyOperations %>" />
						<input type="hidden" name="bargraphs" id="bargraphs" value="<%= "" + barGraphs %>" />
						<input type="hidden" name="logscale_bargraphs" id="logscale_bargraphs" value="<%= "" + logScale_barGraphs %>" />
						<input type="hidden" name="patternfill_bargraphs" id="patternfill_bargraphs" value="<%= "" + patternFill_barGraphs %>" />
						<input type="hidden" name="hidedatalabels_bargraphs" id="hidedatalabels_bargraphs" value="<%= "" + hideDataLabels_barGraphs %>" />
						<input type="hidden" name="eachop" id="eachop" value="<%= "" + separateGraphForEachOperation %>" />
						<input type="hidden" name="boxplots" id="boxplots" value="<%= "" + boxPlots %>" />
						<input type="hidden" name="logscale" id="logscale" value="<%= "" + logScale %>" />
						<input type="hidden" name="dropextremes" id="dropextremes" value="<%= "" + dropExtremes %>" />
						<input type="hidden" name="plotTimeVsRetrievedNodes" id="plotTimeVsRetrievedNodes" value="<%= "" + plotTimeVsRetrievedNodes %>" />
						<input type="hidden" name="plotTimeVsMaxDepth" id="plotTimeVsMaxDepth" value="<%= "" + plotTimeVsMaxDepth %>" />
						<input type="hidden" name="plotTimeVsRetrievedNeighborhoods" id="plotTimeVsRetrievedNeighborhoods" value="<%= "" + plotTimeVsRetrievedNeighborhoods %>" />
						<input type="hidden" name="plotTimeVsRetrievedNeighborhoodNodes" id="plotTimeVsRetrievedNeighborhoodNodes" value="<%= "" + plotTimeVsRetrievedNeighborhoodNodes %>" />
						<input type="hidden" name="linearFits" id="linearFits" value="<%= "" + linearFits %>" />
						<input type="hidden" name="modelAnalysisPredictions" id="modelAnalysisPredictions" value="<%= "" + modelAnalysisPredictions %>" />
						<input type="hidden" name="mergeKHops" id="mergeKHops" value="<%= "" + mergeKHops %>" />
					<%
				}
			%>
			
			<input type="hidden" name="refreshed" id="refreshed" value="no" />

			<div class="spacer"></div>
		</form>
	</div>
		
	<div class="basic_form">
		<%		
			TreeSet<DatabaseEngineAndInstance> selectedDatabaseInstances_sortedByInstance
				= new TreeSet<DatabaseEngineAndInstance>(new DatabaseEngineAndInstance.ByInstance());
			selectedDatabaseInstances_sortedByInstance.addAll(selectedDatabaseInstances);
		
			if (selectedOperations.size() > 0) {
				
				TreeMap<String, Collection<Job>> operationsToJobs = new TreeMap<String, Collection<Job>>();
				for (String operationName : selectedOperations) {
					LinkedList<Job> currentJobs = new LinkedList<Job>();
					for (DatabaseEngineAndInstance p : selectedDatabaseInstances_sortedByInstance) {
						Collection<Job> jobs = selectedDatabaseInstanceAndOperationToSelectedJobsMap.get(
								new Pair<DatabaseEngineAndInstance, String>(p, operationName));
						currentJobs.addAll(jobs);
					}
					operationsToJobs.put(operationName, currentJobs);
				}
				
				boolean sameOperationBaseName = true;	// i.e. without tag
				String __lastOperationName = null;
				for (String operationName : selectedOperations) {
					if (__lastOperationName != null) {
						int i1 = __lastOperationName.indexOf('-');
						int i2 = operationName.indexOf('-');
						if (i1 != i2) {
							sameOperationBaseName = false;
							break;
						}
						if (i1 > 0) {
							if (!__lastOperationName.substring(0, i1).equals(operationName.substring(0, i2))) {
								sameOperationBaseName = false;
								break;
							}
						}
					}
					__lastOperationName = operationName;
				}
				
				%>
					<h2>Summary</h2>
				<%
				
				StringWriter writer = new StringWriter();
				ShowOperationRunTimes.printRunTimesSummary(new PrintWriter(writer), operationsToJobs,
						"html", null, null, convertManyOperations);
				
				String link = "/ShowOperationRunTimes?group_by=operation&convert_many_operations=" + convertManyOperations;
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
				
				List<Triple<String, DatabaseEngineAndInstance, Collection<LinearFunction>>> predictionData
					= new ArrayList<Triple<String, DatabaseEngineAndInstance, Collection<LinearFunction>>>();
				
				if (modelAnalysisPredictions) {
					for (String operationName : selectedOperations) {
						for (Job j : operationsToJobs.get(operationName)) {
							DatabaseEngineAndInstance dbei = j.getDatabaseEngineAndInstance();
							ModelAnalysis m = ModelAnalysis.getInstance(dbei);
							
							Collection<LinearFunction> cf = null;
							Collection<LinearFunction> cm = null;
							Double[] p = null;
													
							if (operationName.equals("OperationGetShortestPath")) {
								for (int k = 0; k < ModelAnalysis.SP_MAX_DEPTH; k++) {
									Double[] bounds = m.SP_x_bounds[k];
									if (bounds == null) {
										continue;
									}
									
									p = m.SP_prediction[k];
									if (p != null) {
										if (cm == null) cm = new ArrayList<LinearFunction>();
										cm.add(new LinearFunction(p, bounds[0], bounds[1]));
									}
								}
								predictionData.add(new Triple<String, DatabaseEngineAndInstance, Collection<LinearFunction>>
									(operationName, dbei, cm));
							}
							else {
								
								p = m.getPrediction(operationName);
								if (p != null) {
									cm = new ArrayList<LinearFunction>();
									cm.add(new LinearFunction(p));
								}
								predictionData.add(new Triple<String, DatabaseEngineAndInstance, Collection<LinearFunction>>
									(operationName, dbei, cm));
							}
						}
					}
				}
				
				String additonalScatterPlotRequestFlags = "";
				
				if (mergeKHops) {
					StringBuilder sb = new StringBuilder();
					HashSet<String> seenNames = new HashSet<String>();
					
					for (String operationName : selectedOperations) {
						if (!operationName.contains("GetKHop")) continue;
						
						String[] s = operationName.split("-");
						List<String> n = new ArrayList<String>();
						
						for (int i = 0; i < s.length; i++) {
							if (s[i].length() == 1 && Character.isDigit(s[i].charAt(0))) {
								s[i] = "[0-9]";
							}
							else {
								n.add(s[i]);
								// TODO Need to quote the pattern -- too bad Pattern.quote() does not work.
								// s[i] = Pattern.quote(s[i]);
							}
						}
						
						String name = StringUtils.join(n, "-");
						if (seenNames.add(name)) {
							sb.append("&custom_series=");
							sb.append(StringEscapeUtils.escapeCsv(name + "=^" + StringUtils.join(s, "-") + "$"));
						}
					}
					
					additonalScatterPlotRequestFlags += sb.toString();
				}
				
				
				if (barGraphs) {
					ChartProperties chartProperties = new ChartProperties();
					
					chartProperties.source = link + "&format=csv";
					chartProperties.attach = "chart_all";
					chartProperties.smallGraph = smallGraphs;
					chartProperties.patternFill = patternFill_barGraphs;
					chartProperties.hideDataLabels = hideDataLabels_barGraphs;
					chartProperties.yvalue = "d.mean";
					chartProperties.ylabel = "Execution Time (ms)";
					if (logScale_barGraphs) chartProperties.yscale = "log";
					chartProperties.group_by = "operation";
					chartProperties.group_label_function = "return d.operation.replace(/^Operation/, '')";
					if (sameOperationBaseName && selectedOperations.size() > 1) {
						chartProperties.group_label_function += ".replace(/^[^-]*-/, '')";
					}
					if (!sameDbInstance) {
						chartProperties.subgroup_by = "dbinstance";
						chartProperties.subgroup_label_function = "return d.dbinstance";
					}
					chartProperties.category_label_function = "return d.dbengine";
					
					String operationLabelFunction = "return v.replace(/^Operation/, '')";
					if (sameOperationBaseName && selectedOperations.size() > 1) {
						operationLabelFunction += ".replace(/^[^-]*-/, '')";
					}
					
					%>
						<div class="chart_outer">
							<div class="chart chart_all" id="chart_all"></div>
							<script language="JavaScript">
								<!-- Begin
								
								var data = new d3bp.Data("<%= link + "&format=csv" %>");
								data.filter(function(d) { return d.label.indexOf("----") != 0; })
									.forEach(function(d) { d.mean = d.mean / 1000000.0; })
									.forEach(function(d) { d.stdev = d.stdev / 1000000.0; })
									.groupBy("operation", function(v) { <%= operationLabelFunction %>; })
									.groupBy("dbinstance");
								
								var chart = new d3bp.BarChart();
								chart.data(data)
									 .value("mean", "Execution Time (ms)",
									 	<%= hideDataLabels_barGraphs %> ? null : d3bp.numToString3)
									 .stdev("stdev")
									 .category("dbengine")
									 .scale(<%= logScale_barGraphs %> ? "log" : "linear");
								if (<%= smallGraphs %>) {
									var x = chart.height();
									chart.height(x.innerHeight / 2, x.ticks / 2);
								}
								if (<%= patternFill_barGraphs %>) {
									chart.appearance().fill = "pattern";
								}
								chart.render("chart_all");
								
								//  End -->
							</script>
							<a class="chart_save" href="javascript:save_svg('chart_all', 'summary-bar-chart.svg')">Save</a>
						</div>
					<%
				}
				
				if (boxPlots) {
					ChartProperties chartProperties = new ChartProperties();
					
					chartProperties.source = link + "&show=details&format=csv";
					chartProperties.attach = "chart_all_details";
					chartProperties.smallGraph = smallGraphs;
					chartProperties.filter = boxPlotFilter;
					chartProperties.yvalue = boxPlotYValue;
					if (logScale) chartProperties.yscale = "log";
					if (dropExtremes) chartProperties.dropTopBottomExtremes = true;
					chartProperties.ylabel = "d.time".equals(boxPlotYValue) 
							? "Execution Time (ms)" : StringEscapeUtils.escapeXml(boxPlotYValue);	// TODO Need better escape
					chartProperties.group_by = "operation";
					chartProperties.group_label_function = "return d.operation.replace(/^Operation/, '')";
					if (sameOperationBaseName && selectedOperations.size() > 1) {
						chartProperties.group_label_function += ".replace(/^[^-]*-/, '')";
					}
					if (!sameDbInstance) {
						chartProperties.subgroup_by = "dbinstance";
						chartProperties.subgroup_label_function = "return d.dbinstance";
					}
					chartProperties.category_label_function = "return d.dbengine";
					
					%>
						<div class="chart_outer">
							<div class="chart chart_all_details" id="chart_all_details"></div>
							<%@ include file="include/d3boxplot.jsp" %>
							<a class="chart_save" href="javascript:save_svg('chart_all_details', 'plot.svg')">Save</a>
						</div>
					<%
				}
				
				if (benchmarksWithExtendedInfo) {
					
					ChartProperties chartProperties = new ChartProperties();
					
					chartProperties.source = link + "&show=details&format=csv" + additonalScatterPlotRequestFlags;
					chartProperties.attach = "chart_all_plotTimeVsRetrievedNodes";
					chartProperties.smallGraph = smallGraphs;
					chartProperties.filter = boxPlotFilter;
					chartProperties.xvalue = "d.result[0]";
					chartProperties.yvalue = boxPlotYValue;
					if (logScale) chartProperties.yscale = "log";
					if (dropExtremes) chartProperties.dropTopBottomExtremes = true;
					chartProperties.linear_fits = linearFits;
					chartProperties.xlabel = "Number of Retrieved or Returned Unique Nodes";
					chartProperties.ylabel = "d.time".equals(boxPlotYValue) 
							? "Execution Time (ms)" : StringEscapeUtils.escapeXml(boxPlotYValue);	// TODO Need better escape
					chartProperties.series_column = "label";
					chartProperties.series_label_function = "return d.label.replace(/^Operation/, '')";
					if (sameOperationBaseName && selectedOperations.size() > 1) {
						chartProperties.series_label_function += ".replace(/^[^-]*-/, '')";
					}
			
					if (plotTimeVsRetrievedNodes) {
						%>
							<div class="chart_outer">
								<div class="chart chart_all_plotTimeVsRetrievedNodes" id="chart_all_plotTimeVsRetrievedNodes"></div>
								<%@ include file="include/d3scatterplot.jsp" %>
								<a class="chart_save" href="javascript:save_svg('chart_all_plotTimeVsRetrievedNodes', 'plot.svg')">Save</a>
							</div>
						<%
					}
					
					chartProperties.attach = "chart_all_additionalKHopNeighborsPlots_1";
					chartProperties.xvalue = "d.result[1]";
					chartProperties.xlabel = "Maximum Depth";
					
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
								<div class="chart chart_all_additionalKHopNeighborsPlots_2" id="chart_all_additionalKHopNeighborsPlots_2"></div>
								<%@ include file="include/d3scatterplot.jsp" %>
								<a class="chart_save" href="javascript:save_svg('chart_all_additionalKHopNeighborsPlots_2', 'plot.svg')">Save</a>
							</div>
						<%
					}
					
					chartProperties.attach = "chart_all_additionalKHopNeighborsPlots_3";
					chartProperties.xvalue = "d.result[3]";
					chartProperties.xlabel = "Number of Retrieved Neighborhood Nodes";
					
					chartProperties.predictions.clear();
					if (modelAnalysisPredictions) {
						for (Triple<String, DatabaseEngineAndInstance, Collection<LinearFunction>> p : predictionData) {
							chartProperties.predictions.add(p.getThird());
						}
					}
					
					if (plotTimeVsRetrievedNeighborhoodNodes) {
						%>
							<div class="chart_outer">
								<div class="chart chart_all_additionalKHopNeighborsPlots_3" id="chart_all_additionalKHopNeighborsPlots_3"></div>
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
					for (DatabaseEngineAndInstance p : selectedDatabaseInstances_sortedByInstance) {
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
					
					%>
						<div class="chart_outer">
							<div class="chart" id="chart_<%= operationName %>"></div>
							<script language="JavaScript">
								<!-- Begin
								
								var data = new d3bp.Data("<%= link + "&format=csv" %>");
								data.filter(function(d) { return d.label.indexOf("----") != 0; })
									.forEach(function(d) { d.mean = d.mean / 1000000.0; })
									.forEach(function(d) { d.stdev = d.stdev / 1000000.0; })
									.groupBy("operation", function(v) { return ""; })
									.groupBy("dbinstance");
								
								var chart = new d3bp.BarChart();
								chart.data(data)
									 .value("mean", "Execution Time (ms)",
									 	<%= hideDataLabels_barGraphs %> ? null : d3bp.numToString3)
									 .stdev("stdev")
									 .category("dbengine")
									 .scale(<%= logScale_barGraphs %> ? "log" : "linear");
								if (<%= smallGraphs %>) {
									var x = chart.height();
									chart.height(x.innerHeight / 2, x.ticks / 2);
								}
								if (<%= patternFill_barGraphs %>) {
									chart.appearance().fill = "pattern";
								}
								chart.render("chart_<%= operationName %>");
								
								//  End -->
							</script>
							<a class="chart_save" href="javascript:save_svg('chart_<%= operationName %>', '<%= operationName %>.svg')">Save</a>
						</div>
	
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
