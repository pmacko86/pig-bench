<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<%@ page import="com.tinkerpop.bench.Bench"%>
<%@ page import="com.tinkerpop.bench.Workload"%>
<%@ page import="com.tinkerpop.bench.DatabaseEngine"%>
<%@ page import="java.util.TreeMap"%>

<%
	String jsp_title = "Add Job";
	String jsp_page = "addjob";
%>
<%@ include file="header.jsp" %>
	
	<div class="stylized_form">
		<form id="form" name="form" method="post" action="/AddJob">
			<h1>Add a Job</h1>
			<p class="header">Please enter the information to specify the benchmark</p>
			
			<label>Database Engine
				<span class="small">DEX, neo4j, etc.</span>
			</label>
			<select name="database_name" id="database_name">
			<%
				TreeMap<String, DatabaseEngine> engines = new TreeMap<String, DatabaseEngine>();
				for (DatabaseEngine e : DatabaseEngine.ENGINES.values()) {
					engines.put(e.getLongName().toLowerCase(), e);
				}
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
			
			<label>Number of Threads
				<span class="small">At least 1</span>
			</label>
			<input type="text" name="tx_buffer" id="tx_buffer" value="1" />
			
			<p class="middle">Workloads (select one or more):</p>
			<%
				TreeMap<String, Workload> workloads = new TreeMap<String, Workload>();
				for (Workload w : Workload.WORKLOADS.values()) {
					workloads.put(w.getLongName().toLowerCase(), w);
				}
				for (Workload w : workloads.values()) {
					%>
						<label class="checkbox">
							<input class="checkbox" type="checkbox" name="workloads" value="<%= w.getShortName() %>"/>
							<%= w.getLongName() %>
						</label>
					<%
				}
			%>
			
			<p class="middle">Configure the workloads:</p>
			
			<label>Number of Operations
				<span class="small">At least 1</span>
			</label>
			<input type="text" name="op_count" id="op_count" value="1000" />
			
			<label>Number of Warmup Operations
				<span class="small">At least 1</span>
			</label>
			<input type="text" name="warmup_op_count" id="warmup_op_count" value="1000" />
			
			<div class="clear"></div>
			<button type="submit">Add to the Queue</button>
			<div class="spacer"></div>
		</form>
	</div>

<%@ include file="footer.jsp" %>
