<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<%@ page import="au.com.bytecode.opencsv.CSVReader"%>
<%@ page import="com.tinkerpop.bench.Bench"%>
<%@ page import="com.tinkerpop.bench.DatabaseEngine"%>
<%@ page import="com.tinkerpop.bench.Workload"%>
<%@ page import="com.tinkerpop.bench.analysis.*"%>
<%@ page import="com.tinkerpop.bench.benchmark.BenchmarkMicro"%>
<%@ page import="com.tinkerpop.bench.log.SummaryLogEntry"%>
<%@ page import="com.tinkerpop.bench.log.SummaryLogReader"%>
<%@ page import="com.tinkerpop.bench.util.*"%>
<%@ page import="com.tinkerpop.bench.web.*"%>
<%@ page import="com.tinkerpop.bench.web.ChartProperties.LinearFunction"%>
<%@ page import="java.awt.Color"%>
<%@ page import="java.io.*"%>
<%@ page import="java.text.SimpleDateFormat"%>
<%@ page import="java.util.regex.Pattern"%>
<%@ page import="java.util.*"%>
<%@ page import="org.apache.commons.lang.StringEscapeUtils"%>
<%@ page import="org.apache.commons.lang.StringUtils"%>

<%
	String jsp_title = "Compare Efficiency";
	String jsp_page = "compareefficiency";
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
				
		//  End -->
	</script>
	
	<div class="stylized_form">
		<form id="form" name="form" method="post" action="/compareefficiency.jsp">
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
								String niceOperationName = operationName;
								if (niceOperationName.startsWith("Operation")) {
									niceOperationName = niceOperationName.substring(9);
								}

								
								%>
									<p class="middle_inner"><%= niceOperationName %></p>
								<%
								
								for (DatabaseEngineAndInstance p : selectedDatabaseInstances) {
									AnalysisContext ac = AnalysisContext.getInstance(p);
									
									String untransformedOperationName = operationName;
									Map<DatabaseEngineAndInstance, String> tm = operationNameTransforms.get(operationName);
									if (tm != null && tm.containsKey(p)) untransformedOperationName = tm.get(p);
									
									SortedSet<Job> jobs = ac.getJobsWithTag(untransformedOperationName);
									if (jobs == null || jobs.isEmpty()) continue;
									
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
			
			boolean sameDatabaseEngine = true;
			DatabaseEngine lastEngine = null;
			
			for (DatabaseEngineAndInstance p : selectedDatabaseInstances) {
				if (lastEngine == null) {
					lastEngine = p.getEngine();
				}
				else {
					if (lastEngine != p.getEngine()) {
						sameDatabaseEngine = false;
					}
				}
			}
		
			if (selectedOperations.size() > 0) {
				
				TreeMap<String, TreeMap<DatabaseEngineAndInstance, Job>> operationsToJobMaps
					= new TreeMap<String, TreeMap<DatabaseEngineAndInstance, Job>>();
				for (String operationName : selectedOperations) {
					TreeMap<DatabaseEngineAndInstance, Job> currentJobs = new TreeMap<DatabaseEngineAndInstance, Job>();
					for (DatabaseEngineAndInstance p : selectedDatabaseInstances_sortedByInstance) {
						SortedSet<Job> jobs = selectedDatabaseInstanceAndOperationToSelectedJobsMap.get(
								new Pair<DatabaseEngineAndInstance, String>(p, operationName));
						currentJobs.put(p, jobs.first());
					}
					operationsToJobMaps.put(operationName, currentJobs);
				}
				
				%>
					<h2>Summary</h2>
					
					<table class="basic_table">
						<tr>
							<th>Operation Name</th>
							<%
							
							for (DatabaseEngineAndInstance dbei : selectedDatabaseInstances_sortedByInstance) {
								%>
									<th><%= sameDatabaseEngine ? dbei.getInstanceSafe("&lt;defaukt&gt;") : dbei.toString() %></th>
								<%
							}
							
							%>
						</tr>
						<%
						
						for (Map.Entry<String, TreeMap<DatabaseEngineAndInstance, Job>> map : operationsToJobMaps.entrySet()) {
						
							String operationName = map.getKey();
							boolean many = AnalysisUtils.isManyOperation(operationName);
							
							String newOperationName = AnalysisUtils.convertNameOfManyOperation(operationName);
							
							String niceOperationName = newOperationName;
							if (niceOperationName.startsWith("Operation")) {
								niceOperationName = niceOperationName.substring(9);
							}
							
							%>
								<tr>
									<td><%= niceOperationName %></td>
							<%
							
							for (Map.Entry<DatabaseEngineAndInstance, Job> e : map.getValue().entrySet()) {
								AnalysisContext context = AnalysisContext.getInstance(e.getKey());
								Job job = e.getValue();
								
								SummaryLogEntry entry = SummaryLogReader.getEntryForOperation(job.getSummaryFile(), operationName);
								if (entry == null) {
									%>
										<td class="na_right">&mdash;</td>
									<%
									continue;
								}
								if (many) entry = AnalysisUtils.convertLogEntryForManyOperation(entry, job);
								
								double runtime = entry.getDefaultRunTimes().getMean() / 1000000.0;
								if (runtime <= 0) {
									%>
										<td class="na_right">&mdash;</td>
									<%
									continue;
								}
								
								OperationModel model = context.getModelFor(operationName);
								List<Prediction> l = model == null ? null : model.predictFromName(operationName);
								if (l == null || l.isEmpty()) {
									%>
										<td class="numeric"><%= String.format("%.3f", runtime) %> ms</td>
									<%
									continue;
								}
								
								// Get the last prediction
								Prediction prediction = l.get(l.size() - 1);
								double predictedRuntime = prediction.getPredictedAverageRuntime();
								if (predictedRuntime <= 0) {
									%>
										<td class="na_right">INV_PREDICTION</td>
									<%
									continue;
								}
								
								double score = predictedRuntime / runtime;
								
								double x;
								if (score <= 1) {
									x = score * score * score;
								}
								else {
									x = 1 + (score - 1) * (score - 1) * (score - 1);
								}
								
								double H = Math.min(x * 0.35, 0.65);
								double S = 1.0;
								double B = 0.85;
								
								Color color = Color.getHSBColor((float)H, (float)S, (float)B);
								String strColor = "rgb(" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + ")";
								String tooltip = "Runtime: " + String.format("%.3f", runtime) + " ms\n";
								tooltip += "Predicted: " + String.format("%.3f", predictedRuntime) + " ms";
								
								%>
									<td class="numeric" style="background: <%= strColor %>; color: white"
										title="<%= tooltip %>">
										<%= Math.round(score * 100) %>%
									</td>
								<%
							}
							
							%>
								</tr>
							<%
						}
						
						%>
					</table>
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
