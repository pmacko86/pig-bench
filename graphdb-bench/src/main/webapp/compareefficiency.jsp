<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<%@ page import="au.com.bytecode.opencsv.CSVReader"%>
<%@ page import="com.tinkerpop.bench.Bench"%>
<%@ page import="com.tinkerpop.bench.DatabaseEngine"%>
<%@ page import="com.tinkerpop.bench.Workload"%>
<%@ page import="com.tinkerpop.bench.analysis.*"%>
<%@ page import="com.tinkerpop.bench.benchmark.BenchmarkMicro"%>
<%@ page import="com.tinkerpop.bench.log.SummaryLogEntry"%>
<%@ page import="com.tinkerpop.bench.log.SummaryLogReader"%>
<%@ page import="com.tinkerpop.bench.util.*"%>
<%@ page import="com.tinkerpop.bench.web.*"%>
<%@ page import="com.tinkerpop.bench.web.ChartProperties.LinearFunction"%>
<%@ page import="java.awt.Color"%>
<%@ page import="java.io.*"%>
<%@ page import="java.text.SimpleDateFormat"%>
<%@ page import="java.util.regex.Pattern"%>
<%@ page import="java.util.*"%>
<%@ page import="org.apache.commons.lang.StringEscapeUtils"%>
<%@ page import="org.apache.commons.lang.StringUtils"%>

<%
	String jsp_title = "Compare Efficiency";
	String jsp_page = "compareefficiency";
	String jsp_body = "onload=\"body_on_load()\" onunload=\"\"";
	boolean jsp_allow_cache = true;
%>
<%@ include file="/include/header.jsp" %>
	
	<script src="/include/d3.v2.js"></script>
	<script src="/include/d3bp.js"></script>
	<script src="/include/scroll-sneak.js"></script>
	<script src="/include/BlobBuilder.min.js"></script>
	<script src="/include/FileSaver.min.js"></script>
	
	<script language="JavaScript">
		<!-- Begin
		
		var scroll_sneak;
				
		/*
		 * Handler for body load
		 */
		function body_on_load()
		{
			e = document.getElementById("refreshed");
			if (e.value=="no") {
				e.value="yes";
			}
			else {
				e.value="no";
				//location.reload();
			}
		}
		
		/*
		 * Submit the form
		 */
		function form_submit() {
			scroll_sneak.sneak();
			document.getElementById('form').submit();
			return true;
		}
				
		//  End -->
	</script>
	
	<div class="stylized_form">
		<form id="form" name="form" method="post" action="/compareefficiency.jsp">
			<h1>Compare Efficiency</h1>
			<p class="header">Compare the implementation efficiency of selected operations across multiple databases</p>
			
			
			<!-- Database Engine / Instance Names -->
			
			<p class="header2">1) Select two or more database engine names / instance names:</p>
			
			<%
				boolean dbinst_simple = false;
				boolean dbinst_choose_many = true;
				boolean dbinst_choose_nonexistent = false;
				boolean dbinst_choose_based_on_available_datasets = false;
				String dbinst_onchange = "form_submit();";
			%>
			<div class="db_table_div">
				<%@ include file="include/dbinsttable.jsp" %>
			</div>
			
			
			
			<!-- Operations -->
			
			<%
				// Operations
				
				if (!selectedDatabaseInstances.isEmpty()) {
					%>
						<p class="middle">2) Select operations to compare:</p>
					<%
				}
			
				boolean selectoperations_selectMultiple = true;
			%>
			<%@ include file="include/selectoperations.jsp" %>

			
			
			<!-- Jobs -->
		
			<%								
				if (!selectedOperations.isEmpty()) {
					%>	
						<p class="middle">3) Select relevant jobs for each operation and each database engine/instance pair:</p>
					<%
				}
			
				boolean selectjobs_selectMultiple = false;
			%>
			<%@ include file="include/selectjobs.jsp" %>

			
			
			<!-- Models -->
			<%								
				TreeSet<DatabaseEngineAndInstance> selectedDatabaseInstances_sortedByInstance
					= new TreeSet<DatabaseEngineAndInstance>(new DatabaseEngineAndInstance.ByInstance());
				selectedDatabaseInstances_sortedByInstance.addAll(selectedDatabaseInstances);
				
				boolean sameDatabaseEngine = true;
				DatabaseEngine lastEngine = null;
				
				for (DatabaseEngineAndInstance p : selectedDatabaseInstances) {
					if (lastEngine == null) {
						lastEngine = p.getEngine();
					}
					else {
						if (lastEngine != p.getEngine()) {
							sameDatabaseEngine = false;
						}
					}
				}
				
				
				/*
				 * Create a map: nice operation name --> DBEI --> [runtime, predictions]
				 */
						
				TreeMap<String, TreeMap<DatabaseEngineAndInstance, Pair<Double, List<Prediction>>>>
					niceOpName_DBEI_RuntimeAndPrediction
					= new TreeMap<String, TreeMap<DatabaseEngineAndInstance, Pair<Double, List<Prediction>>>>();
				
				
				// Additional map: [nice operation name, DBEI] --> exception
				
				HashMap<Pair<String, DatabaseEngineAndInstance>, Exception>
					niceOpNameAndDBEI_Exception = new HashMap<Pair<String, DatabaseEngineAndInstance>, Exception>();
				
				
				// Additional map: nice operation type name --> { prediction names / descriptions }
				
				TreeMap<String, TreeSet<String>> niceOpTypeName_PredictionDescriptions
					= new TreeMap<String, TreeSet<String>>();
				
				
				// Additional map: nice operation type name --> selected prediction name / description
				
				HashMap<String, String> niceOpTypeName_SelectedPredictionDescription
					= new HashMap<String, String>();
				
		
				if (!selectedOperations.isEmpty()) {
					%>	
						<p class="middle">4) Select the appropriate models:</p>
					<%
					
					
					// Create a map: operation name --> DBEI --> job
					
					TreeMap<String, TreeMap<DatabaseEngineAndInstance, Job>> operationsToJobMaps
						= new TreeMap<String, TreeMap<DatabaseEngineAndInstance, Job>>();
					
					for (String operationName : selectedOperations) {
						TreeMap<DatabaseEngineAndInstance, Job> currentJobs = new TreeMap<DatabaseEngineAndInstance, Job>();
						for (DatabaseEngineAndInstance p : selectedDatabaseInstances_sortedByInstance) {
							SortedSet<Job> jobs = selectedDatabaseInstanceAndOperationToSelectedJobsMap.get(
									new Pair<DatabaseEngineAndInstance, String>(p, operationName));
							currentJobs.put(p, jobs.first());
						}
						operationsToJobMaps.put(operationName, currentJobs);
					}
					
					
					// Create a map: nice operation name --> DBEI --> [runtime, predictions]
					
					for (Map.Entry<String, TreeMap<DatabaseEngineAndInstance, Job>> map : operationsToJobMaps.entrySet()) {
					
						String operationName = map.getKey();
						boolean many = AnalysisUtils.isManyOperation(operationName);
						
						String newOperationName = AnalysisUtils.convertNameOfManyOperation(operationName);
						
						String niceOperationName = newOperationName;
						if (niceOperationName.startsWith("Operation")) {
							niceOperationName = niceOperationName.substring(9);
						}
						
						String niceOperationTypeName = niceOperationName;
						if (niceOperationTypeName.contains("-")) {
							niceOperationTypeName = niceOperationTypeName.substring(0, niceOperationTypeName.indexOf('-'));
						}
						
						TreeMap<DatabaseEngineAndInstance, Pair<Double, List<Prediction>>>
							DBEI_RuntimeAndPrediction
							= new TreeMap<DatabaseEngineAndInstance, Pair<Double, List<Prediction>>>();
						
						niceOpName_DBEI_RuntimeAndPrediction.put(niceOperationName, DBEI_RuntimeAndPrediction);
						
						for (Map.Entry<DatabaseEngineAndInstance, Job> e : map.getValue().entrySet()) {
							
							Pair<Double, List<Prediction>> p = null;
							
							try {
								AnalysisContext context = AnalysisContext.getInstance(e.getKey());
								Job job = e.getValue();
								
								SummaryLogEntry entry = SummaryLogReader.getEntryForOperation(job.getSummaryFile(), operationName);
								if (entry == null) {
									DBEI_RuntimeAndPrediction.put(e.getKey(), new Pair<Double, List<Prediction>>(null, null));
									continue;
								}
								if (many) entry = AnalysisUtils.convertLogEntryForManyOperation(entry, job);
								
								double runtime = entry.getDefaultRunTimes().getMean() / 1000000.0;
								if (runtime <= 0) {
									DBEI_RuntimeAndPrediction.put(e.getKey(), new Pair<Double, List<Prediction>>(null, null));
									continue;
								}
								
								OperationModel model = context.getModelFor(operationName);
								List<Prediction> l = model == null ? null : model.predictFromName(operationName);
								if (l != null && l.isEmpty()) l = null;
								
								p = new Pair<Double, List<Prediction>>(runtime, l);
								DBEI_RuntimeAndPrediction.put(e.getKey(), p);
								
								if (l != null) {
									TreeSet<String> pd = niceOpTypeName_PredictionDescriptions.get(niceOperationTypeName);
									if (pd == null) {
										pd = new TreeSet<String>();
										niceOpTypeName_PredictionDescriptions.put(niceOperationTypeName, pd);
									}
									for (Prediction x : l) pd.add(x.getDescription());
									
									String selected = niceOpTypeName_SelectedPredictionDescription.get(niceOperationTypeName);
									if (selected == null) {
										selected = l.get(l.size() - 1).getDescription();
										niceOpTypeName_SelectedPredictionDescription.put(niceOperationTypeName, selected);
									}
								}
							}
							catch (Exception exception) {
								DBEI_RuntimeAndPrediction.put(e.getKey(), null);
								niceOpNameAndDBEI_Exception.put(
										new Pair<String, DatabaseEngineAndInstance>(niceOperationName, e.getKey()),
										exception);
							}
						}
					}
					
					
					// Create a map: nice operation type name --> selected prediction name / description
					
					for (String niceOperationTypeName : niceOpTypeName_PredictionDescriptions.keySet()) {
						String[] params = WebUtils.getStringParameterValues(request, niceOperationTypeName);
						if (params != null) {
							if (niceOpTypeName_PredictionDescriptions.get(niceOperationTypeName).contains(params[0])) {
								niceOpTypeName_SelectedPredictionDescription.put(niceOperationTypeName, params[0]);
							}
						}
					}
					
					
					// Show the selection UI
					
					for (String niceOperationTypeName : niceOpTypeName_PredictionDescriptions.keySet()) {
						%>
							<label class="lesser_no_sub">
								<%= niceOperationTypeName %>
							</label>
							<select name="<%= niceOperationTypeName %>" id="<%= niceOperationTypeName %>"
									onchange="form_submit();" class="tight">
						<%
						
						int index = 0;
						for (String prediction : niceOpTypeName_PredictionDescriptions.get(niceOperationTypeName)) {
							index++;
							
							String extraTags = "";
							if (niceOpTypeName_SelectedPredictionDescription.get(niceOperationTypeName).equals(prediction)) {
								extraTags += " selected=\"selected\"";
							}
							
							%>
								<option value="<%= StringEscapeUtils.escapeHtml(prediction) %>"<%= extraTags %>><%= prediction %></option>
							<%
						}
						%>
							</select>
						<%
					}
				}
			%>

			
			<input type="hidden" name="refreshed" id="refreshed" value="no" />

			<div class="spacer"></div>
		</form>
	</div>
		
	<div class="basic_form">
		<%		
		
			if (selectedOperations.size() > 0) {
				
				
				// Create a map: operations
				
				%>
					<h2>Summary</h2>
					
					<table class="basic_table">
						<tr>
							<th>Operation Name</th>
							<%
							
							for (DatabaseEngineAndInstance dbei : selectedDatabaseInstances_sortedByInstance) {
								%>
									<th><%= sameDatabaseEngine ? dbei.getInstanceSafe("&lt;defaukt&gt;") : dbei.toString() %></th>
								<%
							}
							
							%>
						</tr>
						<%
						
						for (Map.Entry<String, TreeMap<DatabaseEngineAndInstance, Pair<Double, List<Prediction>>>> map
								: niceOpName_DBEI_RuntimeAndPrediction.entrySet()) {
						
							String niceOperationName = map.getKey();
							
							String niceOperationTypeName = niceOperationName;
							if (niceOperationTypeName.contains("-")) {
								niceOperationTypeName = niceOperationTypeName.substring(0, niceOperationTypeName.indexOf('-'));
							}
							
							%>
								<tr>
									<td><%= niceOperationName %></td>
							<%
							
							for (Map.Entry<DatabaseEngineAndInstance, Pair<Double, List<Prediction>>> e : map.getValue().entrySet()) {
								
								Pair<Double, List<Prediction>> p = e.getValue();
								
								if (p == null) {
									Exception exception = niceOpNameAndDBEI_Exception.get(
											new Pair<String, DatabaseEngineAndInstance>(niceOperationName, e.getKey()));
									if (exception == null) {
										%>
											<td class="na_right">ERROR</td>
										<%
									}
									else {
										%>
											<td class="na_right"><%= exception.getClass().getSimpleName() %></td>
										<%
										//exception.printStackTrace();
									}
									continue;
								}
								
								if (p.getFirst() == null) {
									%>
										<td class="na_right">&mdash;</td>
									<%
									continue;
								}
								
								double runtime = p.getFirst().doubleValue();
								List<Prediction> l = p.getSecond();
								if (l == null || l.isEmpty()) {
									%>
										<td class="numeric"><%= String.format("%.3f", runtime) %> ms</td>
									<%
									continue;
								}
								
								
								// Get selected last prediction
								
								String selectedPrediction = niceOpTypeName_SelectedPredictionDescription.get(niceOperationTypeName);
								Prediction prediction = null;
								
								for (Prediction pr : l) {
									if (pr.getDescription().equals(selectedPrediction)) {
										prediction = pr;
										break;
									}
								}
								
								if (prediction == null) {
									%>
										<td class="na_right">N/A</td>
									<%
									continue;
								}
								
								
								// Get the predicted value and the efficiency score
								
								double predictedRuntime = prediction.getPredictedAverageRuntime();
								if (predictedRuntime <= 0) {
									%>
										<td class="na_right">INV_PREDICTION</td>
									<%
									continue;
								}
								
								double score = predictedRuntime / runtime;
								
								double x;
								double tooBigCutoff = 5;
								if (score <= 1) {
									x = score * score * score;
								}
								else if (score <= tooBigCutoff) {
									x = 1 + (score - 1) * (score - 1) * (score - 1);
								}
								else {
									x = (0.65 / 0.35) + (score - tooBigCutoff);
								}
								
								double H = Math.min(x * 0.35, score <= tooBigCutoff ? 0.65 : 0.8);
								double S = 1.0;
								double B = 0.85;
								
								Color color = Color.getHSBColor((float)H, (float)S, (float)B);
								String strColor = "rgb(" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + ")";
								String tooltip = "Runtime: " + String.format("%.3f", runtime) + " ms\n";
								tooltip += "Predicted: " + String.format("%.3f", predictedRuntime) + " ms\n";
								tooltip += "\nModel: " + prediction.getDescription() + "\n";
								
								%>
									<td class="numeric" style="background: <%= strColor %>; color: white"
										title="<%= tooltip %>">
										<%= Math.round(score * 100) %>%
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
	</div>
	
	<script language="JavaScript">
		<!-- Begin
		
		scroll_sneak = new ScrollSneak(location.href);
		
		//  End -->
	</script>

<%@ include file="include/footer.jsp" %>
