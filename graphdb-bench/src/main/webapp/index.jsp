<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<%@ page import="com.tinkerpop.bench.web.*"%>
<%@ page import="java.util.TreeMap"%>

<%
	String jsp_title = "Home";
	String jsp_page = "index";
	String jsp_body = "onload=\"body_on_load()\" onunload=\"\"";
	boolean jsp_allow_cache = false;
%>
<%@ include file="include/header.jsp" %>

	<script src="/include/scroll-sneak.js"></script>
	
	<script language="JavaScript">
		<!-- Begin
		
		var scroll_sneak;
		
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
		<form id="form" name="form" method="post" action="/JobListControl">
			<h1>List of Jobs</h1>
			<p class="header">Completed, Running, and Scheduled Jobs</p>
			
			<div class="job_list_div">
				<div class="header">
					<div class="header_inner">
						Jobs
					</div>
				</div>
			<%
				boolean running = false;
				int numJobs = 0;
				int numRunnableJobs = 0;
				if (JobList.getInstance().getJobs().isEmpty()) {
					%>
						<p class="job_none">There are no completed or scheduled jobs.</p>
					<%
				}
				else {
					for (Job job : JobList.getInstance().getJobs()) {
						String c = "job_neutral";
						numJobs++;
						if (job.isRunning()) {
							c = "job_running";
							running = true;
						}
						else if (job.getExecutionCount() > 0) {
							if (job.getLastStatus() == 0) {
								c = "job_done";
							}
							else {
								c = "job_failed";
							}
						}
						else {
							numRunnableJobs++;
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
												<a href="/ShowOutput?job=<%= job.getId() %>&join=f"
												   class="per_row_link">
													<img src="icons/information_32.png"
														 width="20px" height="20px" />
												</a>
											<%
										}
									%>
									<label class="<%= c %>">
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
				<!-- <div class="footer">
					<div class="footer_inner">
						<div class="clear"></div>
					</div>
				</div> -->
			</div>
			
			<input type="hidden" name="refreshed" id="refreshed" value="no" />
			
			<div class="footer_detached">
				<div class="footer_inner_detached">
					<button type="submit" class="footer" name="action" value="remove">Remove Selected</button>		
					<button type="submit" class="footer" name="action" value="remove_all">Remove All</button>
					<button type="submit" class="footer" name="action" value="remove_finished">Remove Completed</button>
					<button type="submit" class="footer" name="action" value="move_to_bottom">Move to the Bottom</button>
					<button type="submit" class="footer" name="action" value="duplicate">Duplicate</button>
					<div class="clear"></div>
				</div>
			</div> 
		</form>
		
		
		<div id="job_control">
			<p class="middle_header">Job Execution Control</p>
			
			<%
				boolean jobControlEnabled = WebUtils.isJobControlEnabled(null);
			%>
			
			<form id="form_start" ethod="post" action="/JobExecutionControl">
				<input type="hidden" name="action" value="start" />
				<button type="submit"<%= !jobControlEnabled || running || numRunnableJobs == 0
					? " disabled=\"disabled\"" : "" %>>Start</button>
			</form>
			
			<form id="form_pause" method="post" action="/JobExecutionControl">
				<input type="hidden" name="action" value="pause" />
				<button type="submit"<%= !jobControlEnabled || !running || JobList.getInstance().isPaused()
					? " disabled=\"disabled\"" : "" %>>Pause</button>
			</form>
			
			<%
				if (!jobControlEnabled) {
			%>
					<p class="status">The server administrator has disabled starting new jobs.</p>
			<%
				}
				else if (!running) {
			%>
					<p class="status">There are no scheduled jobs.</p>
			<%
				}
				else if (!running) {
			%>
					<p class="status">The job execution is currently paused.</p>
			<%
				}
				else if (JobList.getInstance().isPaused()) {
			%>
					<p class="status status_running">Waiting for the current job to finish...</p>
			<%
				}
				else {
			%>
					<p class="status status_running">Running...</p>
			<%
				}
			%>
			
			<div class="clear"></div>
		</div>
	</div>
	
	<script language="JavaScript">
		<!-- Begin
		
		scroll_sneak = new ScrollSneak(location.href);
		document.getElementById('form').onsubmit = scroll_sneak.sneak;
		document.getElementById('form_start').onsubmit = scroll_sneak.sneak;
		document.getElementById('form_pause').onsubmit = scroll_sneak.sneak;
		
		//  End -->
	</script>

<%@ include file="include/footer.jsp" %>
