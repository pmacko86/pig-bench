<%@ page import="com.tinkerpop.bench.Workload"%>
<%@ page import="com.tinkerpop.bench.DatabaseEngine"%>
<%@ page import="com.tinkerpop.bench.benchmark.BenchmarkMicro"%>
<%@ page import="com.tinkerpop.bench.util.NaturalStringComparator"%>
<%@ page import="com.tinkerpop.bench.util.Pair"%>
<%@ page import="com.tinkerpop.bench.web.*"%>
<%@ page import="java.util.*"%>

<%

/* 
 * VARIABLE EXPORT LIST
 */

// A sorted map [database engine name --> database engine] for all available engines
TreeMap<String, DatabaseEngine> engines = new TreeMap<String, DatabaseEngine>();
		
// A sorted set of selected database intances
TreeSet<DatabaseEngineAndInstance> selectedDatabaseInstances = new TreeSet<DatabaseEngineAndInstance>();


/*
 * Main body
 */

for (DatabaseEngine e : DatabaseEngine.ENGINES.values()) {
	engines.put(e.getLongName().toLowerCase(), e);
}

if (dbinst_simple) {
%>
	<label>Database Engine
		<span class="small">DEX, neo4j, etc.</span>
	</label>
	<select name="database_name" id="database_name">
	<%
		for (DatabaseEngine e : engines.values()) {
			%>
				<option value="<%= e.getShortName() %>"><%= e.getLongName() %></option>
			<%
		}
	%>
	</select>
	
	<label>Database Instance
		<span class="small">Name of a graph or an instance</span>
	</label>
	<input type="text" name="database_instance" id="database_instance" />
<%
}
else {
%>
		<script language="JavaScript">
		<!-- Begin
		
		/*
		 * Toggle/check all checkboxes for the given database engine
		 */
		function dbinsttable_toggle_dbengine(dbengine)
		{
			field = document.getElementsByName('database_engine_instance');

			checked_all = true;
			for (i = 0; i < field.length; i++) {
				if (field[i].value.indexOf(dbengine + "|") == 0) {
					if (!field[i].checked) {
						checked_all = false;
						break;
					}
				}
			}
			
			new_check = !checked_all;
			for (i = 0; i < field.length; i++) {
				if (field[i].value.indexOf(dbengine + "|") == 0) {
					field[i].checked = new_check;
				}
			}
			
			<% if (dbinst_onchange != null) { %>
				<%= dbinst_onchange %>;
			<% } %>
		}
		
		/*
		 * Toggle/check all checkboxes for the given database instance
		 */
		function dbinsttable_toggle_dbinstance(dbinstance)
		{
			field = document.getElementsByName('database_engine_instance');

			checked_all = true;
			for (i = 0; i < field.length; i++) {
				if (field[i].value.indexOf("|" + dbinstance) > 0) {
					if (!field[i].checked) {
						checked_all = false;
						break;
					}
				}
			}
			
			new_check = !checked_all;
			for (i = 0; i < field.length; i++) {
				if (field[i].value.indexOf("|" + dbinstance) > 0) {
					field[i].checked = new_check;
				}
			}
			
			<% if (dbinst_onchange != null) { %>
				<%= dbinst_onchange %>;
			<% } %>
		}
		
		//  End -->
	</script>
	
	<table class="db_table">
		<tr>
			<th style="min-width: 120px; text-align:left">Instance&nbsp;Name</th>
			<%
			for (DatabaseEngine e : engines.values()) {
				%>
					<th<%= dbinst_choose_many
							? " onclick=\"dbinsttable_toggle_dbengine('" + e.getShortName() + "')\"" 
							: "" %>><%= e.getLongName() %></th>
				<%
			}					
			%>
		</tr>
		<%
			// Get the available instances
			
			TreeSet<String> instanceSet = new TreeSet<String>(new NaturalStringComparator());
		
			if (dbinst_choose_based_on_available_datasets) {
				for (String __s : WebUtils.getDatasets()) {
					try {
						instanceSet.add(AddPredefinedJobs.computeInstanceName(__s));
					}
					catch (Exception e) {
						// Silently ignore (at least for now) -- the most likely reason we are here
						// is that the dataset is currently unsupported by the servlet
					}
				}
			}
			else {
				for (String __s : WebUtils.getAllDatabaseInstanceNames()) instanceSet.add(__s);
				for (Job job : JobList.getInstance().getJobs()) {
					if (job.getDbInstance() != null) instanceSet.add(job.getDbInstance());
				}
			}
			
			LinkedList<String> instances = new LinkedList<String>(instanceSet);
			if (!dbinst_choose_based_on_available_datasets) instances.addFirst("");
			if (dbinst_choose_nonexistent && !dbinst_choose_based_on_available_datasets) instances.addLast("<new>");
			
			
			// Get the selected database engine/instance paits
			
			Collection<Pair<String, String>> instancePairs = WebUtils.getDatabaseInstancePairs();
			
			String[] a_selectedDatabaseInstances = WebUtils.getStringParameterValues(request, "database_engine_instance");
			if (a_selectedDatabaseInstances != null) {
				for (String a : a_selectedDatabaseInstances) {
					String[] p = a.split("\\|");
					if (p.length == 1 || p.length == 2) {
						DatabaseEngine d = DatabaseEngine.ENGINES.get(p[0]);
						if (d == null) {
							throw new IllegalArgumentException("Unknown database engine name: " + p[0]);
						}
						selectedDatabaseInstances.add(new DatabaseEngineAndInstance(d, p.length == 2 ? p[1] : null));
					}
				}
			}
			
			
			// Render the table
			
			for (String dbi : instances) {
				%>
					<tr>
						<%
							if (dbi.equals("<new>")) {
						%>
							<th>
								<label<%= dbinst_choose_many
									? " onclick=\"dbinsttable_toggle_dbinstance('" + dbi + "')\"" 
									: "" %>>New:&nbsp;</label>
								<input type="text" name="new_database_instance" id="new_database_instance" value="" />
							</th>
						<%
							}
							else if (dbi.equals("")) {
						%>
							<th<%= dbinst_choose_many
								? " onclick=\"dbinsttable_toggle_dbinstance('" + dbi + "')\"" 
								: "" %> style="text-align:left">&lt;default&gt;</th>
						<%
							}
							else {
						%>
							<th<%= dbinst_choose_many
								? " onclick=\"dbinsttable_toggle_dbinstance('" + dbi + "')\"" 
								: "" %> style="text-align:left"><%= dbi %></th>
						<%
							}
						%>
						<%
						for (DatabaseEngine e : engines.values()) {
							String extraClass = "";
							boolean exists = false;
							DatabaseEngineAndInstance instance = new DatabaseEngineAndInstance(e, dbi);
							if (instancePairs.contains(new Pair<String, String>(e.getShortName(), dbi))) {
								extraClass += " db_table_available";
								exists = true;
							}
							%>
								<td class="db_table_checkbox">
									<label class="db_table_checkbox<%= extraClass %>">
										<%
											if (exists || dbinst_choose_nonexistent) {
												String extraTags = "";
												if (selectedDatabaseInstances.contains(instance)) {
													extraTags += " checked=\"checked\"";
												}
												if (dbinst_onchange != null) {
													extraTags += " onchange=\"" + dbinst_onchange + "\"";
												}
										%>
												<input class="db_table_checkbox"
													type="<%= dbinst_choose_many ? "checkbox" : "radio" %>"
													name="database_engine_instance" <%= extraTags %>
													value="<%= e.getShortName() + "|" + dbi %>"/>
										<%
											}
										%>
									</label>
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
