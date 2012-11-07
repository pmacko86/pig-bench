<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<%@page import="com.tinkerpop.bench.operation.operations.OperationCreateKeyIndex"%>
<%@page import="com.tinkerpop.bench.operation.operations.OperationLoadFGF"%>
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
<%@ page import="java.util.*"%>
<%@ page import="au.com.bytecode.opencsv.CSVReader"%>
<%@ page import="org.apache.commons.lang.StringEscapeUtils"%>

<%
	String jsp_title = "Analyze Ingest";
	String jsp_page = "analyzeingest";
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
		<form id="form" name="form" method="post" action="/analyzeingest.jsp">
			<h1>Analyze Ingest</h1>
			<p class="header">Analyze the ingest performance</p>
			
			
			<!-- Database Engine / Instance Names -->
			
			<p class="header2">1) Select one or more database engine names / instance names:</p>
			
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

			
			<%
				// Display options
				
				boolean smallGraphs = WebUtils.getBooleanParameter(request, "smallgraphs", false);
				boolean patternFill = WebUtils.getBooleanParameter(request, "patternfill", false);
				boolean logScale = WebUtils.getBooleanParameter(request, "logscale", false);
				boolean byInstance = WebUtils.getBooleanParameter(request, "byinstance", false);
			%>
			
			<div id="select_display_options">
				<p class="middle">2) Select display options:</p>
					
				<label class="checkbox">
					<input class="checkbox" type="checkbox"
							name="smallgraphs" id="smallgraphs"
							onchange="form_submit();" <%= smallGraphs ? "checked=\"checked\"" : "" %>
							value="true"/>
					Produce smaller graphs
				</label>
					
				<label class="checkbox">
					<input class="checkbox" type="checkbox"
							name="byinstance" id="byinstance"
							onchange="form_submit();" <%= byInstance ? "checked=\"checked\"" : "" %>
							value="true"/>
					One graph per instance (in addition to the summary graph)
				</label>
					
				<label class="checkbox">
					<input class="checkbox" type="checkbox"
							name="logscale" id="logscale"
							onchange="form_submit();" <%= logScale ? "checked=\"checked\"" : "" %>
							value="true"/>
					Use log scale
				</label>
							
				<label class="checkbox">
					<input class="checkbox" type="checkbox"
							name="patternfill" id="patternfill"
							onchange="form_submit();" <%= patternFill ? "checked=\"checked\"" : "" %>
							value="true"/>
					Use patterns instead of solid colors (enable for B/W printing)
				</label>
			</div>
			
			<input type="hidden" name="refreshed" id="refreshed" value="no" />

			<div class="spacer"></div>
			
			<% if (selectedDatabaseInstances.size() > 0) { %>
				<p class="middle"></p>
				<div class="clear"></div>
			<% } %>
		</form>
	</div>
		
	<div class="basic_form">
		<%		
			TreeSet<DatabaseEngineAndInstance> selectedDatabaseInstances_sortedByInstance
				= new TreeSet<DatabaseEngineAndInstance>(new DatabaseEngineAndInstance.ByInstance());
			selectedDatabaseInstances_sortedByInstance.addAll(selectedDatabaseInstances);
		
			if (selectedDatabaseInstances_sortedByInstance.size() > 0) {
				
				%>
					<h2>Results</h2>
				<%
				
				String link = "/ShowIngestAnalysis?format=csv";
				for (DatabaseEngineAndInstance dbei : selectedDatabaseInstances_sortedByInstance) {
					link += "&database_engine_instance=" + dbei.getEngine().getShortName() + "|" + dbei.getInstanceSafe("");
				}

				ChartProperties chartProperties = new ChartProperties();
				
				chartProperties.source = link;
				chartProperties.attach = "chart_all";
				chartProperties.stacked = true;
				chartProperties.yscale = logScale ? "log" : "linear";
				chartProperties.smallGraph = smallGraphs;
				chartProperties.patternFill = patternFill;
				chartProperties.ylabel = "Execution Time (s)";
				chartProperties.subgroup_by = "dbengine";
				chartProperties.subgroup_label_function = "return d.dbengine";
				chartProperties.group_by = "dbinstance";
				chartProperties.group_label_function = "return d.dbinstance";
				chartProperties.category_label_function = "return d.field";
				
				%>
					<div class="chart_outer">
						<div class="chart chart_all" id="chart_all"></div>
						<%@ include file="include/d3barchart.jsp" %>
						<a class="chart_save" href="javascript:save_svg('chart_all', 'ingest_analysis.svg')">Save</a>
					</div>
				<%
				
				StringWriter writer = new StringWriter();
				ShowIngestAnalysis.printIngestAnalysis(new PrintWriter(writer), selectedDatabaseInstances_sortedByInstance, "html", null);
				%>
					<%= writer.toString() %>
				<%
				
				
				if (byInstance) {

					LinkedHashMap<String, List<DatabaseEngineAndInstance>> instanceToSelectedDBEIs =
							new LinkedHashMap<String, List<DatabaseEngineAndInstance>>();
					
					for (DatabaseEngineAndInstance dbei : selectedDatabaseInstances_sortedByInstance) {
						List<DatabaseEngineAndInstance> l = instanceToSelectedDBEIs.get(dbei.getInstanceSafe("&lt;defaukt&gt;"));
						if (l == null) {
							instanceToSelectedDBEIs.put(dbei.getInstanceSafe("&lt;defaukt&gt;"),
									l = new ArrayList<DatabaseEngineAndInstance>());
						}
						l.add(dbei);
					}
					
					for (String instance : instanceToSelectedDBEIs.keySet()) {
						link = "/ShowIngestAnalysis?format=csv";
						for (DatabaseEngineAndInstance d : instanceToSelectedDBEIs.get(instance)) {
							link += "&database_engine_instance=" + d.getEngine().getShortName() + "|" + d.getInstanceSafe("");
						}
						
						chartProperties.source = link;
						chartProperties.attach = "chart_" + instance;
						chartProperties.group_label_function = "return \"\"";
						
						%>
							<h2><%= instance %></h2>
							<div class="chart_outer">
								<div class="chart chart_<%= instance %>" id="chart_<%= instance %>"></div>
								<%@ include file="include/d3barchart.jsp" %>
								<a class="chart_save" href="javascript:save_svg('chart_<%= instance %>',
									'ingest_analysis_<%= instance %>.svg')">Save</a>
							</div>
						<%
					}
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
