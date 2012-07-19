<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<%@ page import="com.tinkerpop.bench.Bench"%>
<%@ page import="com.tinkerpop.bench.DatabaseEngine"%>
<%@ page import="com.tinkerpop.bench.LogUtils"%>
<%@ page import="com.tinkerpop.bench.Workload"%>
<%@ page import="com.tinkerpop.bench.benchmark.BenchmarkMicro"%>
<%@ page import="com.tinkerpop.bench.log.SummaryLogEntry"%>
<%@ page import="com.tinkerpop.bench.log.SummaryLogReader"%>
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
	
	<script language="JavaScript">
		<!-- Begin
				
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
				String dbinst_onchange = "document.getElementById('form').submit();";
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
				TreeMap<String, TreeMap<String, Job>> selectedDatabaseInstances = new TreeMap<String, TreeMap<String, Job>>();
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
										onchange="document.getElementById('form').submit();" <%= extraTags %>
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
										<select name="<%= inputName %>" id="<%= inputName %>">
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
						<p class="middle"></p>
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
			if (selectedOperations.size() > 1) {
				
				TreeMap<String, Collection<Job>> operationsToJobs = new TreeMap<String, Collection<Job>>();
				for (String operationName : selectedOperations) {
					LinkedList<Job> currentJobs = new LinkedList();
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
				
				String link = "/ShowOperationRunTimes?format=csv&group_by=operation";
				
				for (String operationName : selectedOperations) {
					String eon = StringEscapeUtils.escapeJavaScript(operationName);
					link += "&operations=" + eon;
					for (Job j : operationsToJobs.get(operationName)) {
						link += "&jobs-" + eon + "=" + j.getId();
					}
				}
				
				String d3_source = link;
				String d3_attach = "chart_all";
				String d3_foreach = "";
				String d3_filter = "true";
				String d3_ylabel = "Execution Time (ms)";
				String d3_group_by = "operation";
				String d3_group_label_function = "return d.operation.replace(/^Operation/, '')";
				String d3_category_label_function = "return d.dbengine + ', ' + d.dbinstance";
				
				%>
					<div class="chart_outer"><div class="chart chart_all">
					<%@ include file="include/d3barchart.jsp" %>
					</div></div>
	
					<%= writer.toString() %>
					
					<div style="height:40px"></div>
				<%
			}
		%>
		
		<%
			for (String operationName : selectedOperations) {
				
				String niceOperationName = operationName;
				if (niceOperationName.startsWith("Operation")) {
					niceOperationName = niceOperationName.substring(9);
				}
				
				TreeMap<String, Collection<Job>> operationsToJobs = new TreeMap<String, Collection<Job>>();
				LinkedList<Job> currentJobs = new LinkedList();
				for (String s : selectedDatabaseInstances.keySet()) {
					String inputName = s.replace('|', '-') + "-" + operationName;
					String s_id = selectedJobIds.get(inputName);
					if (s_id == null) continue;
					Job job = JobList.getInstance().getFinishedJob(Integer.parseInt(s_id));
					currentJobs.add(job);
				}
				operationsToJobs.put(operationName, currentJobs);
					
				%>
					<h2><%= niceOperationName %></h2>
				<%
				
				StringWriter writer = new StringWriter();
				ShowOperationRunTimes.printRunTimes(new PrintWriter(writer), operationsToJobs, "html", null);
				
				String eon = StringEscapeUtils.escapeJavaScript(operationName);
				String link = "/ShowOperationRunTimes?format=csv&operations=" + eon;
				for (Job j : currentJobs) {
					link += "&jobs-" + eon + "=" + j.getId();
				}
				
				String d3_source = link;
				String d3_attach = "chart_" + operationName;
				String d3_foreach = "";
				String d3_filter = "true";
				String d3_ylabel = "Execution Time (ms)";
				String d3_group_by = null;
				String d3_group_label_function = null;
				String d3_category_label_function = null;
				
				%>
					<div class="chart_outer"><div class="chart chart_<%= operationName %>">
					<%@ include file="include/d3barchart.jsp" %>
					</div></div>

					<%= writer.toString() %>
					
					<div style="height:40px"></div>
				<%
			}
		%>
	</div>

<%@ include file="include/footer.jsp" %>
