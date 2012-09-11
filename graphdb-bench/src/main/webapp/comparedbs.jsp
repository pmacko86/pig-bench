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
	String jsp_title = "Compare Databases";
	String jsp_page = "comparedbs";
	String jsp_body = "onload=\"body_on_load()\" onunload=\"\"";
	boolean jsp_allow_cache = true;
%>
<%@ include file="include/header.jsp" %>
	
	<script src="/include/d3.v2.js"></script>
	<script src="/include/scroll-sneak.js"></script>
	
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
		
		//  End -->
	</script>
	
	<div class="stylized_form">
		<form id="form" name="form" method="post" action="/comparedbs.jsp">
			<h1>Compare Databases</h1>
			<p class="header">Compare the performance of selected operations across multiple databases</p>
			<p class="header2">1) Select two or more database engine names / instance names:</p>
			
			<%
				boolean dbinst_simple = false;
				boolean dbinst_choose_many = true;
				boolean dbinst_choose_nonexistent = false;
				String dbinst_onchange = "form_submit();";
			%>
			<div class="db_table_div">
				<%@ include file="include/dbinsttable.jsp" %>
			</div>
			
			<%
				int numJobs = 0;
				int numOperations = 0;
				
				SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("[yyyy/MM/dd HH:mm:ss]");
				
				// Map: job string ---> job
				TreeMap<String, Job> jobMap = new TreeMap<String, Job>();

				// Map: operation name ---> set of relevant database engine/instance pairs 
				TreeMap<String, TreeSet<String>> operationMap = new TreeMap<String, TreeSet<String>>();

				// Map: operation name ---> map of (job string ---> job)
				TreeMap<String, TreeMap<String, Job>> operationJobMap = new TreeMap<String, TreeMap<String, Job>>();
				
				
				// Get the set of all selected database engine/instance pairs
				
				String[] a_selectedDatabaseInstances = WebUtils.getStringParameterValues(request, "database_engine_instance");
				TreeMap<String, TreeMap<String, Job>> selectedDatabaseInstances
					= new TreeMap<String, TreeMap<String, Job>>(new NaturalStringComparator());
				if (a_selectedDatabaseInstances != null) {
					for (String a : a_selectedDatabaseInstances) {
						selectedDatabaseInstances.put(a, new TreeMap<String, Job>());
					}
				}
				
				
				// Get the map of all successfully completed jobs and the map of all available operations

				for (String s : selectedDatabaseInstances.keySet()) {
					String[] p = s.split("\\|");
					TreeMap<String, Job> m = selectedDatabaseInstances.get(s);
					if (p.length == 1 || p.length == 2) {
						for (Job job : JobList.getInstance().getFinishedJobs(p[0], p.length == 2 ? p[1] : null)) {
							
							if (job.getExecutionCount() < 0 || job.getLastStatus() != 0 || job.isRunning()) continue;
							File summaryFile = job.getSummaryFile();
							if (summaryFile == null) continue;
							if (job.getExecutionTime() == null) continue;
							
							
							// Jobs
							
							String prefix = "";
							prefix = dateTimeFormatter.format(job.getExecutionTime()) + " ";
							String jobStr = prefix + job.toString();
							numJobs++;
							
							jobMap.put(jobStr, job);
							m.put(jobStr, job);
							
							
							// Operation Maps
							
							SummaryLogReader reader = new SummaryLogReader(summaryFile);
							for (SummaryLogEntry e : reader) {
								String name = e.getName();
								if (name.equals("OperationOpenGraph")
										|| name.equals("OperationDoGC")
										|| name.equals("OperationShutdownGraph")) continue;
								
								TreeSet<String> dbis = operationMap.get(name);
								if (dbis == null) {
									dbis = new TreeSet<String>();
									operationMap.put(name, dbis);
								}
								
								TreeMap<String, Job> ojm = operationJobMap.get(name);
								if (ojm == null) {
									ojm = new TreeMap<String, Job>();
									operationJobMap.put(name, ojm);
								}
								
								dbis.add(s);
								ojm.put(jobStr, job);
							}
						}
					}
				}
				
				for (String n : operationMap.keySet()) {
					if (operationMap.get(n).size() == selectedDatabaseInstances.size()) numOperations++;
				}
				
				
				// Get the set of selected operations
				
				String[] a_selectedOperations = WebUtils.getStringParameterValues(request, "operations");
				TreeSet<String> selectedOperations = new TreeSet<String>();
				if (a_selectedOperations != null) {
					for (String a : a_selectedOperations) {
						if (operationMap.containsKey(a)) {
							if (operationMap.get(a).size() == selectedDatabaseInstances.size()) {
								selectedOperations.add(a);
							}
						}
					}
				}

				
				// Operations
				
				if (numOperations > 0) {
					%>
						<p class="middle">2) Select operations to compare:</p>
					<%
					for (String n : operationMap.keySet()) {
						if (operationMap.get(n).size() != selectedDatabaseInstances.size()) continue;
						
						String extraTags = "";
						if (selectedOperations.contains(n)) {
							extraTags += " checked=\"checked\"";
						}
						
						String niceName = n;
						if (niceName.startsWith("Operation")) niceName = niceName.substring(9);
						
						%>
							<label class="checkbox">
								<input class="checkbox" type="checkbox" name="operations"
										onchange="form_submit();" <%= extraTags %>
										value="<%= n %>"/>
								<%= niceName %>
							</label>
						<%
					}
				}
				
				
				// Jobs
				
				TreeMap<String, String> selectedJobIds = new TreeMap<String, String>();
				
				if (!selectedOperations.isEmpty()) {
					%>
					
					<div id="select_job">
						<p class="middle">3) Select relevant jobs for each operation and each database engine/instance pair:</p>
						
						<%
							for (String operationName : selectedOperations) {
								TreeMap<String, Job> ojm = operationJobMap.get(operationName);
								if (ojm == null) continue;
								String niceOperationName = operationName;
								if (niceOperationName.startsWith("Operation")) {
									niceOperationName = niceOperationName.substring(9);
								}
								
								%>
									<p class="middle_inner"><%= niceOperationName %></p>
								<%
								
								for (String s : selectedDatabaseInstances.keySet()) {
									String[] p = s.split("\\|");
									TreeMap<String, Job> m = selectedDatabaseInstances.get(s);
									if (m.isEmpty()) continue;
									
									String inputName = s.replace('|', '-') + "-" + operationName;
									
									String param = WebUtils.getStringParameter(request, inputName);
									if (param != null) selectedJobIds.put(inputName, param);
									
									%>
										<label class="lesser">
											<%= DatabaseEngine.ENGINES.get(p[0]).getLongName() %>
											<span class="small">
											<%= p.length == 2 ? ("".equals(p[1]) ? "&lt;default&gt;" : p[1]) : "&lt;default&gt;" %>
											</span>
										</label>
										<select name="<%= inputName %>" id="<%= inputName %>"
												onchange="form_submit();">
									<%
									
									LinkedList<String> jobStrings = new LinkedList<String>();
									for (String jobString : ojm.keySet()) {
										if (!m.containsKey(jobString)) continue;
										jobStrings.add(jobString);
									}
									
									int index = 0;
									for (String jobString : jobStrings) {
										index++;
										
										Job job = jobMap.get(jobString);
										String extraTags = "";
										String id = "" + job.getId();
										
										if (!m.containsKey(jobString)) continue;
										
										if (param == null) {
											if (index == jobStrings.size()) {
												extraTags += " selected=\"selected\"";
												selectedJobIds.put(inputName, id);
											}
										}
										else {
											if (param.equals(id)) {
												extraTags += " selected=\"selected\"";
											}
										}
										
										%>
											<option value="<%= id %>"<%= extraTags %>><%= jobString %></option>
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
				boolean separateGraphForEachOperation = WebUtils.getBooleanParameter(request, "eachop", false);
				boolean boxPlots = WebUtils.getBooleanParameter(request, "boxplots", false);
				boolean logScale = WebUtils.getBooleanParameter(request, "logscale", false);
				boolean dropExtremes = WebUtils.getBooleanParameter(request, "dropextremes", false);
				boolean plotTimeVsRetrievedNodes = WebUtils.getBooleanParameter(request, "plotTimeVsRetrievedNodes", false);
				boolean additionalKHopNeighborsPlots = WebUtils.getBooleanParameter(request, "additionalKHopNeighborsPlots", false);
				
				String boxPlotFilter = WebUtils.getStringParameter(request, "boxplotfilter");
				if (boxPlotFilter == null) boxPlotFilter = "true";
				// XXX Further sanitize boxPlotFilter!
				boxPlotFilter = boxPlotFilter.replace(';', ' ');
				
				String boxPlotYValue = WebUtils.getStringParameter(request, "boxplotyvalue");
				if (boxPlotYValue == null) boxPlotYValue = "d.time";
				// XXX Further sanitize boxPlotYValue!
				boxPlotYValue = boxPlotYValue.replace(';', ' ');
				
				boolean plotTimeVsRetrievedNodes_enabled = true;
				for (String s : selectedOperations) {
					if (!s.startsWith("OperationGetAllNeighbors")
							&& !s.startsWith("OperationGetKHopNeighbors")) {
						plotTimeVsRetrievedNodes_enabled = false;
						break;
					}
				}
				
				boolean additionalKHopNeighborsPlots_enabled = true;
				for (String s : selectedOperations) {
					if (!s.startsWith("OperationGetKHopNeighbors")) {
						additionalKHopNeighborsPlots_enabled = false;
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
								Drop top and bottom 1% of values
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
								Filter (all box plots) =
								<span class="small">
									Use: d.time, d.result[], etc.
								</span>
							</label>
							<input type="text" name="boxplotfilter" id="boxplotfilter"
									onchange="form_submit();"
									value="<%= StringEscapeUtils.escapeHtml(boxPlotFilter) %>"/>
							
							<label>
								Y Value (summary box plot) =
								<span class="small">
									Use: d.time, d.result[], etc.
								</span>
							</label>
							<input type="text" name="boxplotyvalue" id="boxplotyvalue"
									onchange="form_submit();"
									value="<%= StringEscapeUtils.escapeHtml(boxPlotYValue) %>"/>
							
							
							<div style="height:10px"></div>
							<p class="middle_inner">Data Plots &ndash; Specialty Plots</p>
							
							<label class="checkbox">
								<input class="checkbox" type="checkbox"
										name="plotTimeVsRetrievedNodes" id="plotTimeVsRetrievedNodes"
										onchange="form_submit();" <%= plotTimeVsRetrievedNodes ? "checked=\"checked\"" : "" %>
										<%= !plotTimeVsRetrievedNodes_enabled ? "disabled=\"disabled\"" : ""  %>
										value="true"/>
								Execution Time vs. Number of Returned Unique Nodes (GetAllNeighbors and GetKHopNeighbors only)
							</label>
							
							<label class="checkbox">
								<input class="checkbox" type="checkbox"
										name="additionalKHopNeighborsPlots" id="additionalKHopNeighborsPlots"
										onchange="form_submit();" <%= additionalKHopNeighborsPlots ? "checked=\"checked\"" : "" %>
										<%= !additionalKHopNeighborsPlots_enabled ? "disabled=\"disabled\"" : ""  %>
										value="true"/>
								Additional GetKHopNeighbors plots
							</label>
				
							<p class="middle"></p>
							<div class="clear"></div>
						</div>
					<%
				}
				else {
					%>
						<input type="hidden" name="bargraphs" id="bargraphs" value="<%= "" + barGraphs %>" />
						<input type="hidden" name="boxplots" id="boxplots" value="<%= "" + boxPlots %>" />
						<input type="hidden" name="logscale" id="logscale" value="<%= "" + logScale %>" />
						<input type="hidden" name="dropextremes" id="dropextremes" value="<%= "" + dropExtremes %>" />
						<input type="hidden" name="plotTimeVsRetrievedNodes" id="plotTimeVsRetrievedNodes" value="<%= "" + plotTimeVsRetrievedNodes %>" />
						<input type="hidden" name="additionalKHopNeighborsPlots" id="additionalKHopNeighborsPlots" value="<%= "" + additionalKHopNeighborsPlots %>" />
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
					for (String s : selectedDatabaseInstances.keySet()) {
						String inputName = s.replace('|', '-') + "-" + operationName;
						String s_id = selectedJobIds.get(inputName);
						if (s_id == null) continue;
						Job job = JobList.getInstance().getFinishedJob(Integer.parseInt(s_id));
						currentJobs.add(job);
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
				
				if (barGraphs) {
					ChartProperties chartProperties = new ChartProperties();
					
					chartProperties.source = link + "&format=csv";
					chartProperties.attach = "chart_all";
					chartProperties.yvalue = "d.mean";
					chartProperties.ylabel = "Execution Time (ms)";
					chartProperties.group_by = "operation";
					chartProperties.group_label_function = "return d.operation.replace(/^Operation/, '')";
					chartProperties.category_label_function = "return d.dbengine" + (sameDbInstance ? "" : " + ', ' + d.dbinstance");
					
					%>
						<div class="chart_outer"><div class="chart chart_all">
						<%@ include file="include/d3barchart.jsp" %>
						</div></div>
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
						<div class="chart_outer"><div class="chart chart_all_details">
						<%@ include file="include/d3boxplot.jsp" %>
						</div></div>
					<%
				}
				
				if (plotTimeVsRetrievedNodes && plotTimeVsRetrievedNodes_enabled) {
					ChartProperties chartProperties = new ChartProperties();
					
					chartProperties.source = link + "&show=details&format=csv";
					chartProperties.attach = "chart_all_plotTimeVsRetrievedNodes";
					chartProperties.filter = boxPlotFilter;
					chartProperties.xvalue = "d.result[0]";
					chartProperties.yvalue = "d.time";
					if (logScale) chartProperties.yscale = "log";
					if (dropExtremes) chartProperties.dropTopBottomExtremes = true;
					chartProperties.xlabel = "Number of Retrieved Unique Nodes";
					chartProperties.ylabel = "Execution Time (ms)";
					chartProperties.series_column = "label";
					chartProperties.series_label_function = "return d.label.replace(/^Operation/, '')";
					
					%>
						<div class="chart_outer"><div class="chart chart_all_plotTimeVsRetrievedNodes">
						<%@ include file="include/d3scatterplot.jsp" %>
						</div></div>
					<%
				}
				
				if (additionalKHopNeighborsPlots && additionalKHopNeighborsPlots_enabled) {
					ChartProperties chartProperties = new ChartProperties();
					
					chartProperties.source = link + "&show=details&format=csv";
					chartProperties.attach = "chart_all_additionalKHopNeighborsPlots_1";
					chartProperties.filter = boxPlotFilter;
					chartProperties.xvalue = "d.result[2]";
					chartProperties.yvalue = "d.time";
					if (logScale) chartProperties.yscale = "log";
					if (dropExtremes) chartProperties.dropTopBottomExtremes = true;
					chartProperties.xlabel = "Number of Calls to Vertex.getOutEdges()";
					chartProperties.ylabel = "Execution Time (ms)";
					chartProperties.series_column = "label";
					chartProperties.series_label_function = "return d.label.replace(/^Operation/, '')";
					
					%>
						<div class="chart_outer"><div class="chart chart_all_additionalKHopNeighborsPlots_1">
						<%@ include file="include/d3scatterplot.jsp" %>
						</div></div>
					<%
					
					chartProperties.attach = "chart_all_additionalKHopNeighborsPlots_2";
					chartProperties.xvalue = "d.result[3]";
					chartProperties.xlabel = "Number of Calls to Edge.getInVertex()";
					
					%>
						<div class="chart_outer"><div class="chart chart_all_additionalKHopNeighborsPlots_2">
						<%@ include file="include/d3scatterplot.jsp" %>
						</div></div>
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
					for (String s : selectedDatabaseInstances.keySet()) {
						String inputName = s.replace('|', '-') + "-" + operationName;
						String s_id = selectedJobIds.get(inputName);
						if (s_id == null) continue;
						Job job = JobList.getInstance().getFinishedJob(Integer.parseInt(s_id));
						currentJobs.add(job);
						if (firstJob == null) {
							firstJob = job;
						}
						else if (sameDbInstance) {
							sameDbInstance = firstJob.getDbInstanceSafe().equals(job.getDbInstanceSafe());
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
