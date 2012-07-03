<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<%@ page import="com.tinkerpop.bench.web.Job"%>
<%@ page import="com.tinkerpop.bench.web.JobList"%>
<%@ page import="java.util.TreeMap"%>

<%
	String jsp_title = "Home";
	String jsp_page = "index";
	String jsp_body = "onload=\"body_on_load()\" onunload=\"\"";
	boolean jsp_allow_cache = false;
%>
<%@ include file="header.jsp" %>
	
	<script language="JavaScript">
		<!-- Begin
		
		/*
		 * Toggle/check a checkbox and unselect all others in the group
		 */
		function toggle_one_unselect_others(value)
		{
			field = document.getElementsByName('jobs');

			toggle = true;
			for (i = 0; i < field.length; i++) {
				if (field[i].value != value) {
					if (field[i].checked) {
						toggle = false;
						break;
					}
				}
			}
			
			for (i = 0; i < field.length; i++) {
				p = field[i].checked;
				if (field[i].value != value) {
					field[i].checked = false;
					if (p != false) job_checkbox_on_change(field[i])
				}
				else {
					if (toggle) {
						field[i].checked = !field[i].checked;
						job_checkbox_on_change(field[i])
					}
					else {
						field[i].checked = true;
						if (p != true) job_checkbox_on_change(field[i])
					}
				}
			}
		}
		
		/*
		 * Handler for changing the value of a checkbox
		 */
		function job_checkbox_on_change(checkbox)
		{
			parent = checkbox.parentNode.parentNode;
			if (checkbox.checked) {
				parent.className = parent.className.replace('job_', 'checked_job_');
			}
			else {
				parent.className = parent.className.replace('checked_job_', 'job_');
				parent.className = parent.className.replace('checked_job_', 'job_');
				parent.className = parent.className.replace('checked_job_', 'job_');
			}
		}
		
		/*
		 * Handler for body load
		 */
		function body_on_load()
		{
			field = document.getElementsByName('jobs');
			for (i = 0; i < field.length; i++) {
				if (field[i].checked) {
					job_checkbox_on_change(field[i]);
				}
			}
			
			e = document.getElementById("refreshed");
			if (e.value=="no") {
				e.value="yes";
			}
			else {
				e.value="no";
				location.reload();
			}
		}
		
		//  End -->
	</script>


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
						String c = "job_neutral";
						if (job.isRunning()) {
							c = "job_running";
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
										<input class="checkbox" type="checkbox" name="jobs"
										       value="<%= job.getId() %>"
										       onchange="job_checkbox_on_change(this)"/>
									</div>
									<%
										if (job.isRunning() || job.getExecutionCount() > 0) {
											%>
												<a href="/ShowOutput?job=<%= job.getId() %>&join=t"
												   class="per_row_link">
													<img src="icons/information_32.png"
														 width="20px" height="20px" />
												</a>
											<%
										}
									%>
									<label class="checkbox <%= c %>">
										<p class="checkbox <%= c %>"
										   onclick="toggle_one_unselect_others('<%= job.getId() %>')">
											<%= job.toString() %>
										</p>
									</label>
								</div>
							</div>
						<%
					}
				}
			%>
			</div>
			
			<input type="hidden" id="refreshed" value="no" />
			
			<button type="submit">Run</button>
		</form>
	</div>

<%@ include file="footer.jsp" %>
