<%@ page import="com.tinkerpop.bench.analysis.*"%>
<%@ page import="com.tinkerpop.bench.util.*"%>
<%@ page import="com.tinkerpop.bench.web.*"%>
<%@ page import="java.io.*"%>
<%@ page import="java.util.*"%>

<%

/* 
 * VARIABLE EXPORT LIST
 */

// A set of selected operations 
TreeSet<String> selectedOperations = new TreeSet<String>();

// A set of operation name transformations
HashMap<String, Map<DatabaseEngineAndInstance, String>> operationNameTransforms
	= new HashMap<String, Map<DatabaseEngineAndInstance, String>>();


/*
 * Main body
 */

if (true) {
	
	//boolean selectoperations_selectMultiple = WebUtils.getBooleanParameter(request, "select_multiple", false);
	
	
	// Get the intersection of all available operations
	
	TreeSet<String> availableOperations = new TreeSet<String>();
	HashSet<String> temp = new HashSet<String>();
	
	boolean first = true;
	for (DatabaseEngineAndInstance p : selectedDatabaseInstances) {
		Map<String, SortedSet<Job>> m = AnalysisContext.getInstance(p).getJobsForAllOperationsWithTags();
		
		temp.clear();
		if (m != null) {
			for (String a : m.keySet()) {
				String org = a;

				if (a.startsWith("OperationLoadFGF-bulkload-")) {
					a = "OperationLoadFGF-incremental-*";
				}
				if (a.startsWith("OperationLoadFGF-incremental-")) {
					a = "OperationLoadFGF-incremental-*";
				}
				if (a.startsWith("OperationLoadGraphML-")) {
					a = "OperationLoadGraphML-*";
				}
				
				(first ? availableOperations : temp).add(a);
				
				if (a != org) {
					Map<DatabaseEngineAndInstance, String> t = operationNameTransforms.get(a);
					if (t == null) operationNameTransforms.put(a, t = new HashMap<DatabaseEngineAndInstance, String>());
					t.put(p, org);
				}
			}
		}
		
		if (first) {
			first = false;
		}
		else {
			availableOperations.retainAll(temp);
		}
	}
		
	
	// Get the set of selected operations intersected with the set of all available operations
	
	String[] a_selectedOperations = WebUtils.getStringParameterValues(request, "operations");
	if (a_selectedOperations != null) {
		for (String a : a_selectedOperations) {
			
			if (a.startsWith("OperationLoadFGF-bulkload-")) {
				a = "OperationLoadFGF-incremental-*";
			}
			if (a.startsWith("OperationLoadFGF-incremental-")) {
				a = "OperationLoadFGF-incremental-*";
			}
			if (a.startsWith("OperationLoadGraphML-")) {
				a = "OperationLoadGraphML-*";
			}
			
			if (availableOperations.contains(a)) {
				selectedOperations.add(a);
			}
		}
	}

	
	// Operations
	
	if (!availableOperations.isEmpty()) {
		String lastOperationNameWithoutTag = "";
		String lastTag = "";
		boolean divOpen = false;
		
		for (String n : availableOperations) {
			
			String extraTags = "";
			if (selectedOperations.contains(n)) {
				extraTags += " checked=\"checked\"";
			}
			
			String niceName = n;
			if (niceName.startsWith("Operation")) niceName = niceName.substring(9);
			
			int tagStart = niceName.indexOf('-');
			String operationNameWithoutTag = tagStart > 0 ? niceName.substring(0, tagStart) : niceName;
			boolean sameOperationNameWithoutTag = lastOperationNameWithoutTag.equals(operationNameWithoutTag);
			lastOperationNameWithoutTag = operationNameWithoutTag;
			
			String tag = tagStart > 0 ? niceName.substring(tagStart+1) : "";
			String[] t = tag.split("-");
			String[] l = lastTag.split("-");
			boolean sameTagPrefix = true;
			if (t.length == l.length) {
				for (int i = 0; i < t.length-1; i++) {
					if (!t[i].equals(l[i])) sameTagPrefix = false;
				}
			}
			if (t.length < l.length) {
				for (int i = 0; i < t.length; i++) {
					if (!t[i].equals(l[i])) sameTagPrefix = false;
				}
			}
			if (t.length > l.length) {
				for (int i = 0; i < l.length; i++) {
					if (!t[i].equals(l[i])) sameTagPrefix = false;
				}
			}			
			lastTag = tag;
			
			if (tagStart > 0) {
				if (!sameOperationNameWithoutTag) {
					if (divOpen) {
						%>
							</div>
						<%
						divOpen = false;
					}
					divOpen = true;
					%>
						<label class="checkbox">
							<%= operationNameWithoutTag %>
						</label>
						<div class="checkbox_lesser">
					<%
				}
				else if (!sameTagPrefix) {
					if (divOpen) {
						%>
							</div>
						<%
						divOpen = false;
					}
					divOpen = true;
					%>
						<div class="checkbox_lesser">
					<%
				}
				%>
				<label class="checkbox_lesser">
					<input class="checkbox_lesser" type="<%= selectoperations_selectMultiple ? "checkbox" : "radio" %>" name="operations"
							onchange="form_submit();" <%= extraTags %>
							value="<%= n %>"/>
					<%= niceName.substring(tagStart + 1) %>
				</label>
			<%
			}
			else {
				if (divOpen) {
					%>
						</div>
					<%
					divOpen = false;
				}
				%>
					<label class="checkbox">
						<input class="checkbox" type="<%= selectoperations_selectMultiple ? "checkbox" : "radio" %>" name="operations"
								onchange="form_submit();" <%= extraTags %>
								value="<%= n %>"/>
						<%= niceName %>
					</label>
				<%
			}
		}
		if (divOpen) {
			%>
				</div>
			<%
			divOpen = false;
		}
	}
}
%>
