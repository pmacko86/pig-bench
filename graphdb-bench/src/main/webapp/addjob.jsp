<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<%@ page import="com.tinkerpop.bench.Bench"%>
<%@ page import="com.tinkerpop.bench.GlobalConfig"%>
<%@ page import="com.tinkerpop.bench.Workload.UpdateCategory"%>
<%@ page import="com.tinkerpop.bench.Workload"%>
<%@ page import="com.tinkerpop.bench.DatabaseEngine"%>
<%@ page import="com.tinkerpop.bench.benchmark.BenchmarkMicro"%>
<%@ page import="com.tinkerpop.bench.util.NaturalStringComparator"%>
<%@ page import="com.tinkerpop.bench.util.Pair"%>
<%@ page import="com.tinkerpop.bench.web.*"%>
<%@ page import="java.util.*"%>

<%
	String jsp_title = "Add Job";
	String jsp_page = "addjob";
	String jsp_body = "onload=\"set_div_visibility()\" onunload=\"\"";
	boolean jsp_allow_cache = true;
%>
<%@ include file="include/header.jsp" %>
	
	<script language="JavaScript">
		<!-- Begin
		
		/*
		 * Set the visibility of all configuration divs
		 */
		function set_div_visibility()
		{
			field = document.getElementsByName('workloads');
			checked = [];

			for (i = 0; i < field.length; i++) {
				if (field[i].checked) {
					checked.push(field[i].value);
				}
			}

			b_workloads = false;
			
			
			// Number of operations
			
			b_op_count = false;
			for (i = 0; i < checked.length; i++) {
				<%
					String workloadsUsingOpCount = "";
					for (Workload w : Workload.WORKLOADS.values()) {
						if (w.isUsingOpCount()) {
							if (!"".equals(workloadsUsingOpCount)) {
								workloadsUsingOpCount += ", ";
							}
							workloadsUsingOpCount += "\"" + w.getShortName() + "\"";
						}
					}
				%>
				if ([<%= workloadsUsingOpCount %>].indexOf(checked[i]) >= 0) b_op_count = true;
			}

			if (b_op_count) {
				b_workloads = true;
				document.getElementById("conf_op_count").style.display = "block";
			}
			else {
				document.getElementById("conf_op_count").style.display = "none";
			}
			
			
			// Number of k hops
			
			b_k_hops = false;
			for (i = 0; i < checked.length; i++) {
				if (["get-k"].indexOf(checked[i]) >= 0) b_k_hops = true;
				if (["get-k-label"].indexOf(checked[i]) >= 0) b_k_hops = true;
			}

			if (b_k_hops) {
				b_workloads = true;
				document.getElementById("conf_k_hops").style.display = "block";
			}
			else {
				document.getElementById("conf_k_hops").style.display = "none";
			}
			
			
			// Edge labels
			
			b_edge_labels = false;
			for (i = 0; i < checked.length; i++) {
				if (["get-label"].indexOf(checked[i]) >= 0) b_edge_labels = true;
				if (["get-k-label"].indexOf(checked[i]) >= 0) b_edge_labels = true;
			}

			if (b_edge_labels) {
				b_workloads = true;
				document.getElementById("conf_edge_labels").style.display = "block";
			}
			else {
				document.getElementById("conf_edge_labels").style.display = "none";
			}
			
			
			// Property keys
			
			b_property_keys = false;
			for (i = 0; i < checked.length; i++) {
				if (["create-index"].indexOf(checked[i]) >= 0) b_property_keys = true;
				if (["get-index"].indexOf(checked[i]) >= 0) b_property_keys = true;
				if (["get-property"].indexOf(checked[i]) >= 0) b_property_keys = true;
			}

			if (b_property_keys) {
				b_workloads = true;
				document.getElementById("conf_property_keys").style.display = "block";
			}
			else {
				document.getElementById("conf_property_keys").style.display = "none";
			}
			
			
			// Ingest
			
			b_ingest = false;
			for (i = 0; i < checked.length; i++) {
				if (["ingest"].indexOf(checked[i]) >= 0) b_ingest = true;
				if (["incr-ingest"].indexOf(checked[i]) >= 0) b_ingest = true;
			}

			if (b_ingest) {
				document.getElementById("conf_ingest").style.display = "block";
			}
			else {
				document.getElementById("conf_ingest").style.display = "none";
			}
			
			
			// Generate
			
			b_generate = false;
			for (i = 0; i < checked.length; i++) {
				if (["generate"].indexOf(checked[i]) >= 0) b_generate = true;
			}

			if (b_generate) {
				document.getElementById("conf_generate").style.display = "block";
			}
			else {
				document.getElementById("conf_generate").style.display = "none";
			}


			// Finalize

			if (b_workloads) {
				document.getElementById("conf_workloads").style.display = "block";
			}
			else {
				document.getElementById("conf_workloads").style.display = "none";
			}
		}
		
		//  End -->
	</script>

	
	<div class="stylized_form">
		<form id="form" name="form" method="post" action="/AddJob">
			<h1>Add a Job</h1>
			<p class="header">Please enter the information to specify the benchmark</p>
			
			<%
				boolean dbinst_simple = false;
				boolean dbinst_choose_many = true;
				boolean dbinst_choose_nonexistent = true;
				boolean dbinst_choose_based_on_available_datasets = false;
				String dbinst_onchange = null;
			%>	
			<%@ include file="include/dbinsttable.jsp" %>			
			
			<p class="middle">Configure the benchmark:</p>
			
			<label>Annotation
				<span class="small">A custom provenance annotation</span>
			</label>
			<input type="text" name="annotation" id="annotation" value="" />
			
			<label>Number of Threads
				<span class="small">At least 1</span>
			</label>
			<input type="text" name="tx_buffer" id="tx_buffer" value="1" />
			
			<label>Java Heap Size
				<span class="small">Default: <%= BenchmarkMicro.DEFAULT_JVM_HEAP_SIZE %></span>
			</label>
			<input type="text" name="java_heap_size" id="java_heap_size" value="<%= BenchmarkMicro.DEFAULT_JVM_HEAP_SIZE %>" />
			
			<label>Database Cache Size
				<span class="small">The sum of all cache sizes (in MB)</span>
			</label>
			<input type="text" name="db_cache_size" id="db_cache_size" value="<%=
				Bench.getProperty(Bench.DB_CACHE_SIZE, "" + GlobalConfig.databaseCacheSize) %>" />
		
			<label class="checkbox">
				<input class="checkbox" type="checkbox" name="no_provenance"
						value="true"/>
				Disable provenance
			</label>
		
			<label class="checkbox">
				<input class="checkbox" type="checkbox" name="use_stored_procedures"
						value="true"/>
				Enable the use of stored procedures (if they are available for the given database engine)
			</label>
		
			<label class="checkbox">
				<input class="checkbox" type="checkbox" name="force_blueprints"
						value="true"/>
				Use the Blueprints API even if there are specialized routines expressed in the database
				engine's native API
			</label>
		
			<label class="checkbox">
				<input class="checkbox" type="checkbox" name="no_cache_pollution"
						value="true"/>
				Disable cache pollution before running the benchmarks
			</label>
		
			<label class="checkbox">
				<input class="checkbox" type="checkbox" name="no_warmup"
						value="true"/>
				Disable the warmup run before the benchmark run
			</label>
		
			<label class="checkbox">
				<input class="checkbox" type="checkbox" name="update_directly"
						value="true"/>
				Run non-load update operations directly on the database instead of a temporary clone 
			</label>
			
			
			<p class="middle">Workloads (select one or more):</p>
			<%
				TreeMap<String, Workload> workloads = new TreeMap<String, Workload>();
				for (Workload w : Workload.WORKLOADS.values()) {
					workloads.put(w.getLongName().toLowerCase(), w);
				}
				%>
					<p class="middle_inner">Graph Loading and Generation</p>
				<%
				for (Workload w : workloads.values()) {
					if (w.getUpdateCategory() != UpdateCategory.LOAD_UPDATE) continue;
					%>
						<label class="checkbox">
							<input class="checkbox" type="checkbox" name="workloads"
									onchange="set_div_visibility()"
									value="<%= w.getShortName() %>"/>
							<%= w.getLongName() %>
						</label>
					<%
				}
				%>
					<div style="height:10px"></div>
					<p class="middle_inner">Read-Only Workloads</p>
				<%
				for (Workload w : workloads.values()) {
					if (w.getUpdateCategory() != UpdateCategory.READ_ONLY) continue;
					%>
						<label class="checkbox">
							<input class="checkbox" type="checkbox" name="workloads"
									onchange="set_div_visibility()"
									value="<%= w.getShortName() %>"/>
							<%= w.getLongName() %>
						</label>
					<%
				}
				%>
				<div style="height:10px"></div>
				<p class="middle_inner">Workloads with Temporary Updates</p>
				<%
				for (Workload w : workloads.values()) {
					if (w.getUpdateCategory() != UpdateCategory.TEMPORARY_UPDATE) continue;
					%>
						<label class="checkbox">
							<input class="checkbox" type="checkbox" name="workloads"
									onchange="set_div_visibility()"
									value="<%= w.getShortName() %>"/>
							<%= w.getLongName() %>
						</label>
					<%
				}
				%>
					<div style="height:10px"></div>
					<p class="middle_inner">Workloads with Permanent Updates</p>
				<%
				for (Workload w : workloads.values()) {
					if (w.getUpdateCategory() != UpdateCategory.PERMANENT_UPDATE) continue;
					%>
						<label class="checkbox">
							<input class="checkbox" type="checkbox" name="workloads"
									onchange="set_div_visibility()"
									value="<%= w.getShortName() %>"/>
							<%= w.getLongName() %>
						</label>
					<%
				}
			%>
			
			
			<div id="conf_ingest">
				<p class="middle">Configure the database ingest:</p>

				<label>Input file
					<span class="small">For the regular database</span>
				</label>
				<select name="ingest_file" id="ingest_file">
				<%
					TreeSet<String> datasets = new TreeSet<String>(new NaturalStringComparator());
					for (String name : WebUtils.getDatasets()) datasets.add(name);
					for (String name : datasets) {
						String extra = "";
						if (BenchmarkMicro.DEFAULT_INGEST_FILE.equals(name)) {
							extra += " selected=\"selected\"";
						}
						%>
							<option value="<%= name %>"<%= extra %>><%= name %></option>
						<%
					}
				%>
				</select>

				<label>Warmup file
					<span class="small">For the warmup database</span>
				</label>
				<select name="ingest_warmup_file" id="ingest_warmup_file">
					<option value="">&lt;same as above&gt;</option>
				<%
					for (String name : datasets) {
						String extra = "";
						%>
							<option value="<%= name %>"<%= extra %>><%= name %></option>
						<%
					}
				%>
				</select>
		
				<label class="checkbox">
					<input class="checkbox" type="checkbox" name="ingest_as_undirected"
							value="true"/>
					Ingest a directed graph as undirected by doubling-up the edges
				</label>

				<div class="clear"></div>
			</div>
			
			
			<div id="conf_generate">
				<p class="middle">Configure the graph generator:</p>

				<label>Model
					<span class="small">Such as "Barabasi"</span>
				</label>
				<select name="generate_model" id="generate_model">
					<option value="barabasi" selected="selected">Barabasi</option>
				</select>
				
				<label>N
					<span class="small">Number of new vertices</span>
				</label>
				<input type="text" name="generate_barabasi_n" id="generate_barabasi_n" value="<%= BenchmarkMicro.DEFAULT_BARABASI_N %>" />
				
				<label>M
					<span class="small">Number of incoming edges per vertex</span>
				</label>
				<input type="text" name="generate_barabasi_m" id="generate_barabasi_m" value="<%= BenchmarkMicro.DEFAULT_BARABASI_M %>" />
			</div>
			

			<p class="middle" id="conf_workloads">Configure the workloads:</p>
			
			<div id="conf_op_count">
				<label>Number of Operations
					<span class="small">At least 1</span>
				</label>
				<input type="text" name="op_count" id="op_count" value="<%= BenchmarkMicro.DEFAULT_OP_COUNT %>" />
				
				<label>Number of Warmup Operations
					<span class="small">At least 1</span>
				</label>
				<input type="text" name="warmup_op_count" id="warmup_op_count" value="<%= BenchmarkMicro.DEFAULT_OP_COUNT %>" />

				<div class="clear"></div>
			</div>
			
			<div id="conf_k_hops">
				<label>Number of K Hops
					<span class="small">A number or a range (e.g. 1:5)</span>
				</label>
				<input type="text" name="k_hops" id="k_hops" value="<%= BenchmarkMicro.DEFAULT_K_HOPS %>" />

				<div class="clear"></div>
			</div>
			
			<div id="conf_edge_labels">
				<label>Edge Labels
					<span class="small">A comma-separated list</span>
				</label>
				<input type="text" name="edge_labels" id="edge_labels" value="<%= BenchmarkMicro.DEFAULT_EDGE_LABELS %>" />

				<div class="clear"></div>
			</div>
			
			<div id="conf_property_keys">
				<%
					String defaultVertexProperties = "";
					String defaultEdgeProperties = "";
					
					for (String s : BenchmarkMicro.DEFAULT_PROPERTY_KEYS.split(",")) {
						if ("".equals(s)) continue;
						if (s.startsWith(":")) {
							if (!"".equals(defaultEdgeProperties)) defaultEdgeProperties += ", ";
							defaultEdgeProperties += s.substring(1);
						}
						else {
							if (!"".equals(defaultVertexProperties)) defaultVertexProperties += ", ";
							defaultVertexProperties += s;
						}
					}
				%>
				<label>Vertex Property Keys
					<span class="small">A comma-separated list</span>
				</label>
				<input type="text" name="vertex_property_keys" id="vertex_property_keys" value="<%= defaultVertexProperties %>" />

				<label>Edge Property Keys
					<span class="small">A comma-separated list</span>
				</label>
				<input type="text" name="edge_property_keys" id="edge_property_keys" value="<%= defaultEdgeProperties %>" />

				<div class="clear"></div>
			</div>
			

			<div class="clear"></div>
			<button type="submit">Add to the Queue</button>
			<div class="spacer"></div>
		</form>
	</div>

<%@ include file="include/footer.jsp" %>
