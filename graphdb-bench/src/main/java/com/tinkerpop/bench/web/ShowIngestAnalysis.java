package com.tinkerpop.bench.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import au.com.bytecode.opencsv.CSVWriter;

import com.tinkerpop.bench.DatabaseEngine;
import com.tinkerpop.bench.analysis.IngestAnalysis;
import com.tinkerpop.bench.analysis.IngestAnalysis.DatasetDescription;
import com.tinkerpop.bench.analysis.ModelAnalysis;
import com.tinkerpop.bench.util.MathUtils;
import com.tinkerpop.bench.util.OutputUtils;


/**
 * A servlet for showing the results of ingest analysis
 * 
 * @author Peter Macko (pmacko@eecs.harvard.edu)
 */
@SuppressWarnings("serial")
public class ShowIngestAnalysis extends HttpServlet {

	/**
	 * Create an instance of class ShowIngestAnalysis
	 */
	public ShowIngestAnalysis() {
	}


	/**
	 * Respond to a POST request
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		doGet(request, response);
	}


	/**
	 * Respond to a GET request
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{

		// Get the database engine name / instance name pairs

		String[] pairs = WebUtils.getStringParameterValues(request, "database_engine_instance");
		List<DatabaseEngineAndInstance> dbeis = new ArrayList<DatabaseEngineAndInstance>();

		if (pairs != null) {
			for (String p : pairs) {
				int d = p.indexOf('|');
				dbeis.add(new DatabaseEngineAndInstance(DatabaseEngine.ENGINES.get(p.substring(0, d)), p.substring(d+1)));
			}
		}


		// Other parameters

		String show = WebUtils.getStringParameter(request, "show");
		if (show == null) show = "data";

		String format = WebUtils.getStringParameter(request, "format");
		if (format == null) format = "plain";

		boolean predictions = WebUtils.getBooleanParameter(request, "predictions", false);
		boolean hideIndexCreation = WebUtils.getBooleanParameter(request, "hide_index_creation", false);
		boolean mergeShutdown = WebUtils.getBooleanParameter(request, "merge_shutdown", false);


		// Get the writer and write out the information

		PrintWriter writer = response.getWriter();

		if (show.equals("data")) printIngestAnalysis(writer, dbeis, format, predictions, !hideIndexCreation, mergeShutdown, response);
		else if (show.equals("prediction_details")) printIngestPredictionDetails(writer, dbeis, format, response);
		else {
			response.setContentType("text/plain");
			response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
			writer.println("Error: Invalid value of the \"show\" argument: " + show);
		}
	}


	/**
	 * Write out the ingest analysis to the given writer
	 * 
	 * @param writer the writer
	 * @param dbeis  the database engine name / instance name pairs
	 * @param format the format
	 * @param predictions whether to show the model predictions
	 * @param showIndexCreation whether to show the index creation
	 * @param mergeShutdown true to merge the shutdown times into the main times
	 * @param response the response, or null if none
	 */
	public static void printIngestAnalysis(PrintWriter writer, Collection<DatabaseEngineAndInstance> dbeis,
			String format, boolean predictions, boolean showIndexCreation, boolean mergeShutdown,
			HttpServletResponse response) {

		// Depending on the format type...

		if ("html".equals(format)) {
			if (response != null) {
				response.setContentType("text/html");
				response.setStatus(HttpServletResponse.SC_OK);
			}

			writer.println("<table class=\"basic_table\">");
			writer.println("<tr>");
			writer.println("\t<th>Database Engine</th>");
			writer.println("\t<th>Database Instance</th>");
			writer.println("\t<th>Bulk Load (s)</th>");
			if (!mergeShutdown) writer.println("\t<th>Shutdown after Bulk Load (ms)</th>");
			writer.println("\t<th>Incremental Load (s)</th>");
			if (!mergeShutdown) writer.println("\t<th>Shutdown after Incremental Load (s)</th>");
			if (showIndexCreation) {
				writer.println("\t<th>Index Creation (s)</th>");
				if (!mergeShutdown) writer.println("\t<th>Shutdown after Index Creation (s)</th>");
			}
			writer.println("\t<th>Total (s)</th>");
			writer.println("</tr>");

			double[] data = new double[(6 - (showIndexCreation ? 0 : 2)) / (!mergeShutdown ? 1 : 2)];

			for (DatabaseEngineAndInstance dbei : dbeis) {
				IngestAnalysis ia = IngestAnalysis.getInstance(dbei);


				// Data

				int index = 0;
				if (!mergeShutdown) {
					data[index++] = ia.getBulkLoadTime();
					data[index++] = ia.getBulkLoadShutdownTime();
					data[index++] = ia.getIncrementalLoadTime();
					data[index++] = ia.getIncrementalLoadShutdownTime();
					if (showIndexCreation) {
						data[index++] = ia.getCreateIndexTime();
						data[index++] = ia.getCreateIndexShutdownTime();
					}
				}
				else {
					data[index++] = ia.getBulkLoadTime() + ia.getBulkLoadShutdownTime();
					data[index++] = ia.getIncrementalLoadTime() + ia.getIncrementalLoadShutdownTime();
					if (showIndexCreation) {
						data[index++] = ia.getCreateIndexTime() + ia.getCreateIndexShutdownTime();
					}
				}

				writer.println("<tr>");
				writer.println("\t<td>" + dbei.getEngine().getLongName() + "</td>");
				writer.println("\t<td>" + dbei.getInstanceSafe("&lt;default&gt;") + "</td>");

				for (int i = 0; i < data.length; i++) {
					writer.println("\t<td class=\"numeric\">" + String.format("%.3f", data[i]/1000.0) + "</td>");
				}
				writer.println("\t<td class=\"numeric\">" + String.format("%.3f", MathUtils.sum(data)/1000.0) + "</td>");
				writer.println("</tr>");


				// Prediction

				if (!predictions || ia.getIncrementalLoadTimePrediction() == 0) continue;

				writer.println("<tr>");
				writer.println("\t<td class=\"na_right\" colspan=\"2\">(prediction)</td>");
				writer.println("\t<td class=\"na_right\">&mdash;</td>");
				if (!mergeShutdown) writer.println("\t<td class=\"na_right\">&mdash;</td>");
				writer.println("\t<td class=\"numeric\">" + String.format("%.3f", ia.getIncrementalLoadTimePrediction()/1000.0) + "</td>");
				if (!mergeShutdown) writer.println("\t<td class=\"na_right\">&mdash;</td>");
				if (showIndexCreation) {
					writer.println("\t<td class=\"na_right\">&mdash;</td>");
					if (!mergeShutdown) writer.println("\t<td class=\"na_right\">&mdash;</td>");
				}
				writer.println("\t<td class=\"na_right\">&mdash;</td>");
				writer.println("</tr>");
			}

			writer.println("</table>");
		}

		else if ("csv".equals(format)) {
			if (response != null) {
				response.setContentType("text/plain");
				response.setStatus(HttpServletResponse.SC_OK);
			}

			CSVWriter w = new CSVWriter(writer);
			String[] buffer = new String[9];

			int index = 0;
			buffer[index++] = "dbengine";
			buffer[index++] = "dbinstance";
			buffer[index++] = "type";
			buffer[index++] = "field";
			buffer[index++] = "label";
			buffer[index++] = "mean";
			buffer[index++] = "stdev";
			buffer[index++] = "min";
			buffer[index++] = "max";
			assert index == buffer.length;

			w.writeNext(buffer);

			String lastInstance = null;

			for (DatabaseEngineAndInstance dbei : dbeis) {
				IngestAnalysis ia = IngestAnalysis.getInstance(dbei);
				String s;
				int i;


				// Group by placeholders

				//if ("operation".equals(groupBy)) {
				// XXX Hack, beware!
				if (lastInstance != null && !dbei.getInstanceSafe().equals(lastInstance)) {
					for (int j = 0; j < buffer.length; j++) buffer[j] = "";
					buffer[4] = "----" + lastInstance;
					w.writeNext(buffer);
					lastInstance = dbei.getInstanceSafe();
				}
				if (lastInstance == null) lastInstance = dbei.getInstanceSafe();
				//}


				// Data

				index = 0;
				buffer[index++] = dbei.getEngine().getLongName();
				buffer[index++] = dbei.getInstanceSafe("<default>");
				buffer[index++] = "observed";

				if (!mergeShutdown) {
					i = index; s = Double.toString(1000000.0 * ia.getBulkLoadTime() / 1000.0 /* HACK, beware! */ );
					buffer[i++] = "Bulk Load";
					buffer[i++] = buffer[0] + " : " + buffer[1] + " : " + buffer[3] + " : " + buffer[2];
					buffer[i++] = s; buffer[i++] = "0"; buffer[i++] = s; buffer[i++] = s;
					w.writeNext(buffer);
	
					i = index; s = Double.toString(1000000.0 * ia.getBulkLoadShutdownTime() / 1000.0 /* HACK, beware! */ );
					buffer[i++] = "Shutdown after Bulk Load";
					buffer[i++] = buffer[0] + " : " + buffer[1] + " : " + buffer[3] + " : " + buffer[2];
					buffer[i++] = s; buffer[i++] = "0"; buffer[i++] = s; buffer[i++] = s;
					w.writeNext(buffer);
	
					i = index; s = Double.toString(1000000.0 * ia.getIncrementalLoadTime() / 1000.0 /* HACK, beware! */ );
					buffer[i++] = "Incremental Load";
					buffer[i++] = buffer[0] + " : " + buffer[1] + " : " + buffer[3] + " : " + buffer[2];
					buffer[i++] = s; buffer[i++] = "0"; buffer[i++] = s; buffer[i++] = s;
					w.writeNext(buffer);
	
					i = index; s = Double.toString(1000000.0 * ia.getIncrementalLoadShutdownTime() / 1000.0 /* HACK, beware! */ );
					buffer[i++] = "Shutdown after Incremental Load";
					buffer[i++] = buffer[0] + " : " + buffer[1] + " : " + buffer[3] + " : " + buffer[2];
					buffer[i++] = s; buffer[i++] = "0"; buffer[i++] = s; buffer[i++] = s;
					w.writeNext(buffer);
	
					if (showIndexCreation) {
						i = index; s = Double.toString(1000000.0 * ia.getCreateIndexTime() / 1000.0 /* HACK, beware! */ );
						buffer[i++] = "Index Creation";
						buffer[i++] = buffer[0] + " : " + buffer[1] + " : " + buffer[3] + " : " + buffer[2];
						buffer[i++] = s; buffer[i++] = "0"; buffer[i++] = s; buffer[i++] = s;
						w.writeNext(buffer);
	
						i = index; s = Double.toString(1000000.0 * ia.getCreateIndexShutdownTime() / 1000.0 /* HACK, beware! */ );
						buffer[i++] = "Shutdown after Index Creation";
						buffer[i++] = buffer[0] + " : " + buffer[1] + " : " + buffer[3] + " : " + buffer[2];
						buffer[i++] = s; buffer[i++] = "0"; buffer[i++] = s; buffer[i++] = s;
						w.writeNext(buffer);
					}
	
	
					// Prediction
	
					if (!predictions || ia.getIncrementalLoadTimePrediction() == 0) continue;
	
					index = 0;
					buffer[index++] = dbei.getEngine().getLongName() + " (pred.)";
					buffer[index++] = dbei.getInstanceSafe("<default>");
					buffer[index++] = "predicted";
	
					i = index; s = Double.toString(1000000.0 * ia.getBulkLoadTime() / 1000.0 /* HACK, beware! */ );
					buffer[i++] = "Bulk Load";
					buffer[i++] = buffer[0] + " : " + buffer[1] + " : " + buffer[3] + " : " + buffer[2];
					buffer[i++] = s; buffer[i++] = "0"; buffer[i++] = s; buffer[i++] = s;
					w.writeNext(buffer);
	
					i = index; s = Double.toString(1000000.0 * ia.getBulkLoadShutdownTime() / 1000.0 /* HACK, beware! */ );
					buffer[i++] = "Shutdown after Bulk Load";
					buffer[i++] = buffer[0] + " : " + buffer[1] + " : " + buffer[3] + " : " + buffer[2];
					buffer[i++] = s; buffer[i++] = "0"; buffer[i++] = s; buffer[i++] = s;
					w.writeNext(buffer);
	
					i = index; s = Double.toString(1000000.0 * ia.getIncrementalLoadTimePrediction() / 1000.0 /* HACK, beware! */ );
					buffer[i++] = "Incremental Load (Prediction)";
					buffer[i++] = buffer[0] + " : " + buffer[1] + " : " + buffer[3] + " : " + buffer[2];
					buffer[i++] = s; buffer[i++] = "0"; buffer[i++] = s; buffer[i++] = s;
					w.writeNext(buffer);
	
					i = index; s = Double.toString(1000000.0 * 0 / 1000.0 /* HACK, beware! */ );
					buffer[i++] = "Shutdown after Incremental Load";
					buffer[i++] = buffer[0] + " : " + buffer[1] + " : " + buffer[3] + " : " + buffer[2];
					buffer[i++] = s; buffer[i++] = "0"; buffer[i++] = s; buffer[i++] = s;
					w.writeNext(buffer);
	
					if (showIndexCreation) {
						i = index; s = Double.toString(1000000.0 * ia.getCreateIndexTime() / 1000.0 /* HACK, beware! */ );
						buffer[i++] = "Index Creation";
						buffer[i++] = buffer[0] + " : " + buffer[1] + " : " + buffer[3] + " : " + buffer[2];
						buffer[i++] = s; buffer[i++] = "0"; buffer[i++] = s; buffer[i++] = s;
						w.writeNext(buffer);
	
						i = index; s = Double.toString(1000000.0 * ia.getCreateIndexShutdownTime() / 1000.0 /* HACK, beware! */ );
						buffer[i++] = "Shutdown after Index Creation";
						buffer[i++] = buffer[0] + " : " + buffer[1] + " : " + buffer[3] + " : " + buffer[2];
						buffer[i++] = s; buffer[i++] = "0"; buffer[i++] = s; buffer[i++] = s;
						w.writeNext(buffer);
					}
				}
				else {
					i = index; s = Double.toString(1000000.0 * (ia.getBulkLoadShutdownTime() + ia.getBulkLoadTime()) / 1000.0 /* HACK, beware! */ );
					buffer[i++] = "Bulk";
					buffer[i++] = buffer[0] + " : " + buffer[1] + " : " + buffer[3] + " : " + buffer[2];
					buffer[i++] = s; buffer[i++] = "0"; buffer[i++] = s; buffer[i++] = s;
					w.writeNext(buffer);
	
					i = index; s = Double.toString(1000000.0 * (ia.getIncrementalLoadShutdownTime() + ia.getIncrementalLoadTime()) / 1000.0 /* HACK, beware! */ );
					buffer[i++] = "Incremental";
					buffer[i++] = buffer[0] + " : " + buffer[1] + " : " + buffer[3] + " : " + buffer[2];
					buffer[i++] = s; buffer[i++] = "0"; buffer[i++] = s; buffer[i++] = s;
					w.writeNext(buffer);
	
					if (showIndexCreation) {
						i = index; s = Double.toString(1000000.0 * (ia.getCreateIndexShutdownTime() + ia.getCreateIndexTime()) / 1000.0 /* HACK, beware! */ );
						buffer[i++] = "Index";
						buffer[i++] = buffer[0] + " : " + buffer[1] + " : " + buffer[3] + " : " + buffer[2];
						buffer[i++] = s; buffer[i++] = "0"; buffer[i++] = s; buffer[i++] = s;
						w.writeNext(buffer);
					}
	
	
					// Prediction
	
					if (!predictions || ia.getIncrementalLoadTimePrediction() == 0) continue;
	
					index = 0;
					buffer[index++] = dbei.getEngine().getLongName() + " (pred.)";
					buffer[index++] = dbei.getInstanceSafe("<default>");
					buffer[index++] = "predicted";
	
					i = index; s = Double.toString(1000000.0 * (ia.getBulkLoadShutdownTime() + ia.getBulkLoadTime()) / 1000.0 /* HACK, beware! */ );
					buffer[i++] = "Bulk";
					buffer[i++] = buffer[0] + " : " + buffer[1] + " : " + buffer[3] + " : " + buffer[2];
					buffer[i++] = s; buffer[i++] = "0"; buffer[i++] = s; buffer[i++] = s;
					w.writeNext(buffer);
	
					i = index; s = Double.toString(1000000.0 * ia.getIncrementalLoadTimePrediction() / 1000.0 /* HACK, beware! */ );
					buffer[i++] = "Incremental (pred.)";
					buffer[i++] = buffer[0] + " : " + buffer[1] + " : " + buffer[3] + " : " + buffer[2];
					buffer[i++] = s; buffer[i++] = "0"; buffer[i++] = s; buffer[i++] = s;
					w.writeNext(buffer);
	
					if (showIndexCreation) {
						i = index; s = Double.toString(1000000.0 * (ia.getCreateIndexShutdownTime() + ia.getCreateIndexTime()) / 1000.0 /* HACK, beware! */ );
						buffer[i++] = "Index";
						buffer[i++] = buffer[0] + " : " + buffer[1] + " : " + buffer[3] + " : " + buffer[2];
						buffer[i++] = s; buffer[i++] = "0"; buffer[i++] = s; buffer[i++] = s;
						w.writeNext(buffer);
					}
				}
			}
			
			try {
				w.close();
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		else {
			if (response != null) {
				response.setContentType("text/plain");
				response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
			}

			writer.println("Invalid format.");
		}		
	}


	/**
	 * Write out the ingest analysis to the given writer
	 * 
	 * @param writer the writer
	 * @param dbeis  the database engine name / instance name pairs
	 * @param format the format
	 * @param predictions whether to show the model predictions
	 * @param showIndexCreation whether to show the index creation
	 * @param response the response, or null if none
	 */
	public static void printIngestPredictionDetails(PrintWriter writer, Collection<DatabaseEngineAndInstance> dbeis,
			String format, HttpServletResponse response) {

		if (!"html".equals(format) && !"plain".equals(format)) {
			if (response != null) {
				response.setContentType("text/plain");
				response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
				writer.println("Invalid format: " + format);
				return;
			}
			else throw new IllegalArgumentException("Invalid format: " + format);
		}
		boolean html = "html".equals(format);

		for (DatabaseEngineAndInstance dbei : dbeis) {
			IngestAnalysis ia = IngestAnalysis.getInstance(dbei);
			ModelAnalysis m = ModelAnalysis.getInstance(dbei);
			DatasetDescription d = ia.getIncrementalLoadDataset();

			if (html) writer.print("<h2>");
			writer.print(dbei.toString());
			if (html) writer.println("</h2>"); else writer.print("\n\n");
			
			if (html) writer.print("<pre>");
			writer.println("Wv   = " + OutputUtils.format(m.Wv));
			writer.println("We   = " + OutputUtils.format(m.We));
			writer.println("Wp   = " + OutputUtils.format(m.Wp));
			writer.println("Rvup = " + OutputUtils.format(m.Rvup));
			writer.println();
			// XXX Hard-coded -- bad!!!
			writer.println("Prediction = " + d.getNumVertices() + " Wv + "
					   + d.getNumEdges() + " We + "
					   + (d.getNumEdges() + d.getNumVertices()*2) + " Wp + "
					   + d.getNumExternalVertices() + " Rvup");
			writer.println("           = " + d.getNumVertices() + " (" + OutputUtils.format(m.Wv) + ") + "
					   + d.getNumEdges() + " (" + OutputUtils.format(m.We) + ") + "
					   + (d.getNumEdges() + d.getNumVertices()*2) + " (" + OutputUtils.format(m.Wp) + ") + "
					   + d.getNumExternalVertices() + " (" + OutputUtils.format(m.Rvup) + ")");
			writer.println("           = " + OutputUtils.format(d.getNumVertices() * m.Wv) + " + "
					   + OutputUtils.format(d.getNumEdges() * m.We) + " + "
					   + OutputUtils.format((d.getNumEdges() + d.getNumVertices()*2) * m.Wp) + " + "
					   + OutputUtils.format(d.getNumExternalVertices() * m.Rvup) + "");
			writer.println("           = " + OutputUtils.format(ia.getIncrementalLoadTimePrediction()) + " ms");
			writer.println("Observed   = " + OutputUtils.format(ia.getIncrementalLoadTime()) + " + "
					   + OutputUtils.format(ia.getIncrementalLoadShutdownTime()) + "");
			writer.println("           = " + OutputUtils.format(ia.getIncrementalLoadTime()
					+ ia.getIncrementalLoadShutdownTime()) + " ms");
			if (html) writer.println("</pre>");
		}
	}
}
