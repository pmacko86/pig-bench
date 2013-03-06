<%@ page import="com.tinkerpop.bench.analysis.*"%>
<%@ page import="com.tinkerpop.bench.util.*"%>
<%@ page import="com.tinkerpop.bench.web.*"%>
<%@ page import="java.io.*"%>
<%@ page import="java.util.*"%>

<%

/* 
 * VARIABLE EXPORT LIST
 */

// A map of selected [[database engine, database instance name], operation name] pairs to a sorted set of selected jobs
TreeMap<Pair<DatabaseEngineAndInstance, String>, SortedSet<Job>>
	selectedDatabaseInstanceAndOperationToSelectedJobsMap
	= new TreeMap<Pair<DatabaseEngineAndInstance, String>, SortedSet<Job>>();


/*
 * Main body
 */

if (true) {
	
	SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("[yyyy/MM/dd HH:mm:ss]");
	if (!selectedOperations.isEmpty()) {
		%>
	
			<script language="JavaScript">
				<!-- Begin
				
				/*
				 * Toggle the visibility of the select job(s) div
				 */
				function selectoperations_toggle_visibility(element)
				{
					div_element = document.getElementById('select_job');
					link_element = document.getElementById('select_job_toggle');

					if (div_element.style.display == "none") {
						link_element.innerHTML = "Hide";
						div_element.style.display = "block";
					}
					else {
						link_element.innerHTML = "Show";
						div_element.style.display = "none";
					}
				}
				
				//  End -->
			</script>
			
			<div>
				<a id="select_job_toggle" class="like_button" style="margin-top:10px; margin-bottom:10px;"
				   href="javascript:selectoperations_toggle_visibility(this)">
					Show
				</a>
			</div>
		
			<div id="select_job" style="display:none;">
			
			<%
				for (String operationName : selectedOperations) {
					String niceOperationName = operationName;
					if (niceOperationName.startsWith("Operation")) {
						niceOperationName = niceOperationName.substring(9);
					}

					
					%>
						<p class="middle_inner"><%= niceOperationName %></p>
					<%
					
					for (DatabaseEngineAndInstance p : selectedDatabaseInstances) {
						AnalysisContext ac = AnalysisContext.getInstance(p);
						
						String untransformedOperationName = operationName;
						Map<DatabaseEngineAndInstance, String> tm = operationNameTransforms.get(operationName);
						if (tm != null && tm.containsKey(p)) untransformedOperationName = tm.get(p);
						
						SortedSet<Job> jobs = ac.getJobsWithTag(untransformedOperationName);
						if (jobs == null || jobs.isEmpty()) continue;
						
						String inputName = p.getEngine().getShortName()
								+ "-" + p.getInstanceSafe("")
								+ "-" + operationName;
						
						String[] params = WebUtils.getStringParameterValues(request, inputName);
						TreeSet<Job> selectedJobsForSelectedDatabaseInstanceAndOperation = new TreeSet<Job>();
						if (params != null) {
							for (String pm : params) {
								long jobId = Long.parseLong(pm);
								Job job = JobList.getInstance().getFinishedJobByPersistentId(jobId);
								if (job == null) continue;
								if (!jobs.contains(job)) continue;
								selectedJobsForSelectedDatabaseInstanceAndOperation.add(job);
							}
						}
						selectedDatabaseInstanceAndOperationToSelectedJobsMap.put(
								new Pair<DatabaseEngineAndInstance, String>(p, operationName),
								selectedJobsForSelectedDatabaseInstanceAndOperation);
						
						if (selectedJobsForSelectedDatabaseInstanceAndOperation.isEmpty()) {
							Job lastGoodJob = null;
							for (Job job : jobs) {
								if (job.getArguments().contains("--use-stored-procedures")) continue;
								lastGoodJob = job;
							}
							if (lastGoodJob == null) {
								lastGoodJob = jobs.last();
							}
							if (lastGoodJob != null) {
								selectedJobsForSelectedDatabaseInstanceAndOperation.add(lastGoodJob);
							}
						}

						%>
							<label class="lesser">
								<%= p.getEngine().getLongName() %>
								<span class="small">
								<%= p.getInstanceSafe("&lt;default&gt;") %>
								</span>
							</label>
							<select name="<%= inputName %>" id="<%= inputName %>"
									onchange="form_submit();">
						<%
						
						int index = 0;
						for (Job job : jobs) {
							index++;
							
							String extraTags = "";
							if (selectedJobsForSelectedDatabaseInstanceAndOperation.contains(job)) {
								extraTags += " selected=\"selected\"";
							}
							
							String prefix = dateTimeFormatter.format(job.getExecutionTime()) + " ";
							
							%>
								<option value="<%= job.getPersistentId() %>"<%= extraTags %>><%= prefix + job.toString() %></option>
							<%
						}
						%>
							</select>
						<%
					}
				}
			%>
			<div class="clear"></div>
		</div>
		<%
	}
}
%>
