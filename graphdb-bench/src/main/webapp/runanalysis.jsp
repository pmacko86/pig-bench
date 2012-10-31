<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<%@ page import="com.tinkerpop.bench.Bench"%>
<%@ page import="com.tinkerpop.bench.DatabaseEngine"%>
<%@ page import="com.tinkerpop.bench.Workload"%>
<%@ page import="com.tinkerpop.bench.web.*"%>
<%@ page import="java.io.*"%>
<%@ page import="java.util.*"%>

<%
	String jsp_title = "Run Analysis";
	String jsp_page = "runanalysis";
	String jsp_body = "";
	boolean jsp_allow_cache = true;
%>
<%@ include file="include/header.jsp" %>
	
	<script language="JavaScript">
		<!-- Begin
		
		/*
		 * Validate the form
		 */
		function validate_form() {
			var c = document.getElementsByName('database_engine_instance');
			for (var i = 0; i < c.length; i++) {
				if (c[i].checked) return true;
			}
			alert('No database engine name / instance name pairs are selected.');
			return false;
		}
		
		//  End -->
	</script>
	
	<div class="stylized_form">
		<form id="form" name="form" method="post" action="/RunAnalysis" onsubmit="return validate_form()">
			<h1>Run Analysis</h1>
			<p class="header">Run an external command to perform a simple model analysis</p>
			<p class="header2">Select a database engine / instance pair:</p>
			
			<%
				boolean dbinst_simple = false;
				boolean dbinst_choose_many = false;
				boolean dbinst_choose_nonexistent = false;
				boolean dbinst_choose_based_on_available_datasets = false;
				String dbinst_onchange = null;
			%>
			<div class="db_table_div">
				<%@ include file="include/dbinsttable.jsp" %>
			</div>
			
			<div class="clear"></div>
			
			<button type="submit">Analyze</button>
		</form>
	</div>

<%@ include file="include/footer.jsp" %>
