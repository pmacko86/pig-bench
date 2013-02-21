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
	String jsp_title = "Add Predefined Jobs";
	String jsp_page = "addpredefinedloadjobs";
	String jsp_body = "onload=\"set_div_visibility()\" onunload=\"\"";
	boolean jsp_allow_cache = true;
%>
<%@ include file="include/header.jsp" %>
	
	<div class="stylized_form">
		<form id="form" name="form" method="post" action="/AddPredefinedJobs">
			<h1>Add Predefined Load Jobs</h1>
			<p class="header">Please identify which sets of predefined database load jobs to add</p>
			
			<%
				boolean dbinst_simple = false;
				boolean dbinst_choose_many = true;
				boolean dbinst_choose_nonexistent = true;
				boolean dbinst_choose_based_on_available_datasets = true;
				String dbinst_onchange = null;
			%>	
			<div class="db_table_div">
				<%@ include file="include/dbinsttable.jsp" %>
			</div>
			
			<input id="type" name="type" type="hidden" value="load" />

			<div class="clear"></div>
			<button type="submit">Add Load Jobs</button>
			<div class="spacer"></div>
		</form>
	</div>

<%@ include file="include/footer.jsp" %>
