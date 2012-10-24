<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<%@ page import="com.tinkerpop.bench.Bench"%>
<%@ page import="com.tinkerpop.bench.DatabaseEngine"%>
<%@ page import="com.tinkerpop.bench.util.LogUtils"%>
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
	String jsp_title = "Download Results";
	String jsp_page = "downloadresults";
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
		<form id="form" name="form" method="post" action="/DownloadResults" onsubmit="return validate_form()">
			<h1>Download Results</h1>
			<p class="header">Download the results as an archive of .csv files</p>
			<p class="header2">1) Select the database engine names / instance names:</p>
			
			<%
				boolean dbinst_simple = false;
				boolean dbinst_choose_many = true;
				boolean dbinst_choose_nonexistent = false;
				boolean dbinst_choose_based_on_available_datasets = false;
				String dbinst_onchange = null;
			%>
			<div class="db_table_div">
				<%@ include file="include/dbinsttable.jsp" %>
			</div>
			
			<div class="clear"></div>
			
			<button type="submit">Download Selected</button>			
		
		</form>
			
		<form id="form" name="form" method="post" action="/DownloadResults">
			<p class="middle">2) Or, download all results:</p>
			
			<input type="hidden" name="all" value="true" />
			<button type="submit" class="small_margin">Download All</button>			
		</form>
	</div>

<%@ include file="include/footer.jsp" %>
