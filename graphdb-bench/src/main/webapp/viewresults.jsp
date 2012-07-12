<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<%@ page import="com.tinkerpop.bench.Bench"%>
<%@ page import="com.tinkerpop.bench.Workload"%>
<%@ page import="com.tinkerpop.bench.DatabaseEngine"%>
<%@ page import="com.tinkerpop.bench.benchmark.BenchmarkMicro"%>
<%@ page import="com.tinkerpop.bench.log.OperationLogEntry"%>
<%@ page import="com.tinkerpop.bench.log.OperationLogReader"%>
<%@ page import="com.tinkerpop.bench.util.Pair"%>
<%@ page import="com.tinkerpop.bench.web.*"%>
<%@ page import="java.io.*"%>
<%@ page import="java.util.*"%>
<%@ page import="org.apache.commons.lang.StringEscapeUtils"%>

<%
	String jsp_title = "View Results";
	String jsp_page = "viewresults";
	String jsp_body = "onload=\"body_on_load()\" onunload=\"\"";
	boolean jsp_allow_cache = true;
%>
<%@ include file="include/header.jsp" %>
		
	<script language="JavaScript">
		<!-- Begin
		
		/*
		 * Toggle/check a checkbox and unselect all others in the group
		 */
		function check_radio(value)
		{
			field = document.getElementsByName('job_file');
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
			field = document.getElementsByName('job_file');
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
		 * Replace the parent of the given element by the contents of the log file
		 */
		function replace_by_log_file(element, db_name, db_instance, log_file)
		{
			var node = element.parentNode;
			var http_request = new XMLHttpRequest();
			http_request.open("GET", "/ShowLogFile?database_name=" + db_name
				+ "&database_instance=" + db_instance + "&log_file=" + escape(log_file)
				+ "&format=html", true);
			http_request.onreadystatechange = function () {
				if (http_request.readyState == 1) {
					node.innerHTML = '<p class="basic_status_running">Waiting...</p>';
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
						for (Job job : WebUtils.getFinishedJobs(p[0], p.length == 2 ? p[1] : null)) {
							jobMap.put(job.toString(), job);
							numJobs++;
						}
					}
				}
				
				String[] a_selectedJobLogFiles = WebUtils.getFileNameParameterValues(request, "job_file");
				TreeSet<String> selectedJobLogFiles = new TreeSet<String>();
				if (a_selectedJobLogFiles != null) {
					for (String a : a_selectedJobLogFiles) {
						selectedJobLogFiles.add(a);
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
								String fileName = job.getLogFile(false).getName();
								String extraTags = "";
								String escapedFileName = StringEscapeUtils.escapeJavaScript(fileName);
								
								if (selectedJobLogFiles.contains(fileName)) {
									selectedJobs.put(fileName, job);
									extraTags += " checked=\"checked\"";
								}
								
								String c = "job_neutral";
								if (job.isRunning()) {
									//TODO c = "job_running";
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
												<input class="checkbox" type="radio" name="job_file"
												       value="<%= escapedFileName %>" <%= extraTags %>
												       onchange="job_radio_on_change(this)"/>
											</div>
											<label class="checkbox <%= c %>">
												<p class="checkbox <%= c %>"
												   onclick="check_radio('<%= escapedFileName %>')">
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
						%>
							<table class="basic_table">
						<%
						BufferedReader b = new BufferedReader(new FileReader(f));
						String line;
						boolean firstLine = true;
						while ((line = b.readLine()) != null)   {
							%>
								<tr>
							<%
							String[] tokens = line.split(";");
							boolean first = true;
							for (String t : tokens) {
								String extraTags = "";
								if (!first) {
									extraTags += "class=\"numeric\"";
								}
								if (firstLine) {
									if (first) {
										t = "Operation";
									}
									else {
										t += " (ms)";
									}
									%>
										<th <%= extraTags %>><%= t %></th>
									<%
								}
								else {
									if (!first) {
										double d = Double.parseDouble(t);
										t = String.format("%.3f", d / 1000000.0);
									}
									%>
										<td <%= extraTags %>><%= t %></td>
									<%
								}
								first = false;
							}
							firstLine = false;
							%>
								</tr>
							<%
						}
						b.close();
						%>
							</table>
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
						String efn = StringEscapeUtils.escapeJavaScript(f.getName());
						String dbe = job.getDbEngine();
						String dbi = job.getDbInstance();
						if (dbi == null) dbi = "";
						%>
							<div>
								<button onclick="replace_by_log_file(this, '<%= dbe %>', '<%= dbi %>', '<%= efn %>')">
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
								<button onclick="replace_by_log_file(this, '<%= dbe %>', '<%= dbi %>', '<%= efn %>')">
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
