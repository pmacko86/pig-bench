<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<%@ page import="com.tinkerpop.bench.web.Job"%>
<%@ page import="com.tinkerpop.bench.web.JobList"%>
<%@ page import="java.util.TreeMap"%>

<%
	String jsp_title = "Home";
	String jsp_page = "index";
%>
<%@ include file="header.jsp" %>
	
	<div class="job_list_form">
		<form id="form" name="form" method="post" action="/RunBenchmark">
			<h1>List of Jobs</h1>
			
			<div class="job_list_div">
			<%
				if (JobList.getInstance().getJobs().isEmpty()) {
					%>
						<p class="job_none">There are no completed or scheduled jobs.</p>
					<%
				}
				else {
					for (Job job : JobList.getInstance().getJobs()) {
						if (job.getExecutionCount() == 0) {
							%>
								<label class="checkbox job_neutral">
							<%
						}
						else if (job.getLastStatus() == 0) {
							%>
								<label class="checkbox job_done">
							<%
						}
						else {
							%>
								<label class="checkbox job_failed">
							<%
						}
						%>
								<input class="checkbox" type="checkbox" name="jobs" value="<%= job.getId() %>"/>
								<p class="checkbox"><%= job.toString() %></p>
							</label>
						<%
					}
				}
			%>
			</div>
			
			<button type="submit">Run</button>
		</form>
	</div>

<%@ include file="footer.jsp" %>
