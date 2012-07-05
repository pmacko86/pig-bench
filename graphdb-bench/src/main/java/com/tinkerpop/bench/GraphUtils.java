package com.tinkerpop.bench;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;

import com.tinkerpop.blueprints.pgm.Edge;
import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.Vertex;
import com.tinkerpop.blueprints.pgm.util.graphml.GraphMLWriter;

public class GraphUtils {
	
	
	/**
	 * Calculate local clustering coefficient.
	 * 
	 *     C_i = |{e_jk}| / |N_i|(|N_i| - 1)
	 * 
	 * Reference: http://sonivis.org/wiki/index.php/Global_Clustering_Coefficient
	 *
	 * @param v the vector
	 * @param stat the statistics object
	 * @return the local clustering coefficient
	 */
	public static double localClusteringCoefficient(Vertex v, OpStat stat) {
		
		HashSet<Vertex> neighbors = new HashSet<Vertex>();
		
		stat.num_getOutEdges++;
		for (Edge e : v.getOutEdges()) {
			stat.num_getInVertex++;
			stat.num_uniqueVertices++;
			Vertex w = e.getInVertex();
			neighbors.add(w);
		}
		
		int triangles = 0;
		for (Vertex w : neighbors) {
			stat.num_getOutEdges++;
			for (Edge e : w.getOutEdges()) {
				stat.num_getInVertex++;
				if (neighbors.contains(e.getInVertex())) {
					triangles++;
				}
				else {
					stat.num_uniqueVertices++;
				}
			}
		}
		
		int K = neighbors.size();
		return K <= 1 ? 0 : (triangles / (double) (K * (K-1)));
	}
	
	
	/**
	 * Dump the sorted lists of vertex and edge IDs to the given output
	 * 
	 * @param out the print stream
	 * @param graph the graph
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void printSortedIdLists(PrintStream out, Graph graph) {
		
		ArrayList<Comparable> v = new ArrayList<Comparable>();
		ArrayList<Comparable> e = new ArrayList<Comparable>();
		for (Vertex vertex : graph.getVertices()) v.add((Comparable) vertex.getId());
		for (Edge edge : graph.getEdges()) e.add((Comparable) edge.getId());
		java.util.Collections.sort(v);
		java.util.Collections.sort(e);
		
		out.print("\n===== Vertices =====\n");
		int i = 1;
		for (Comparable c : v) {
			out.print("" + c + "\t");
			if (i++ % 20 == 0) out.print("\n");
		}
		if ((i-1) % 20 != 0) out.print("\n");
		
		out.print("\n===== Edges =====\n");
		i = 1;
		for (Comparable c : e) {
			System.err.print("" + c + "\t");
			if (i++ % 20 == 0) out.print("\n");
		}
		if ((i-1) % 20 != 0) out.print("\n");
	}
	
	
	/**
	 * Export the graph in the GraphML format
	 * 
	 * @param out the print stream
	 * @param graph the graph
	 * @param sorted whether the node/edge lists should be sorted
	 * @throws IOException on error
	 */
	public static void printGraphML(PrintStream out, Graph graph, boolean sorted) throws IOException {
		
		GraphMLWriter writer = new GraphMLWriter(graph);
		writer.setNormalize(sorted);
		writer.outputGraph(out);
				
		/*out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		out.println("<graphml xmlns=\"http://graphml.graphdrawing.org/xmlns\"");
		out.println("         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
		out.println("         xsi:schemaLocation=\"http://graphml.graphdrawing.org/xmlns");
		out.println("         http://graphml.graphdrawing.org/xmlns/1.0/graphml.xsd\">");
		out.println("<!-- Exported by graphdb-bench -->");
		
		HashSet<String> nodeKeys = new HashSet<String>();
		for (Vertex vertex : graph.getVertices()) {
			nodeKeys.addAll(vertex.getPropertyKeys());
		}
		for (String key : nodeKeys) {
			out.println("  <key id=\"" + key + "\" for=\"node\" attr.name=\"" + key + "\" attr.type=\"string\"/>");
			if (key.contains("\"")) throw new RuntimeException("The following node key contains \'\"\': " + key);
		}
		
		HashSet<String> edgeKeys = new HashSet<String>();
		for (Edge edge : graph.getEdges()) {
			edgeKeys.addAll(edge.getPropertyKeys());
		}
		for (String key : edgeKeys) {
			if (key.contains("\"")) throw new RuntimeException("The following node key contains \'\"\': " + key);
			out.println("  <key id=\"" + key + "\" for=\"edge\" attr.name=\"" + key + "\" attr.type=\"string\"/>");
		}
		
		out.println("  <graph id=\"G\" edgedefault=\"directed\">");
		
		// TODO: Sort
		
		for (Vertex vertex : graph.getVertices()) {
			out.println("    <node id=\"" + vertex.getId() + "\">");
			for (String key : vertex.getPropertyKeys()) {
				if (key.contains("\"")) throw new RuntimeException("The following node key contains \'\"\': " + key);
				String value = OutputUtils.encodeXMLString("" + vertex.getProperty(key));
				out.println("      <data key=\"" + key + "\">" + value + "</data>");
			}
			out.println("    </node>");
		}
		
		for (Edge edge : graph.getEdges()) {
			out.println("    <edge source=\"" + edge.getOutVertex().getId() + "\" "
					+ "target=\"" + edge.getInVertex ().getId() + "\" "
					+ "label=\"" + edge.getLabel() + "\">");
			for (String key : edge.getPropertyKeys()) {
				if (key.contains("\"")) throw new RuntimeException("The following node key contains \'\"\': " + key);
				String value = OutputUtils.encodeXMLString("" + edge.getProperty(key));
				out.println("      <data key=\"" + key + "\">" + value + "</data>");
			}
			out.println("    </edge>");
		}
		
		out.println("  </graph>");
		out.println("</graphml>");*/
	}
	
	
	/**
	 * A detailed operation breakdown in terms of the number of operations
	 */
	public static class OpStat {
		
		public int num_getInVertex = 0;
		public int num_getOutVertex = 0;
		public int num_getInEdges = 0;
		public int num_getOutEdges = 0;
		public int num_getVertices = 0;
		public int num_getVerticesNext = 0;			/* getting the next vertex from the iterator */
		public int num_uniqueVertices = 0;
		
		public boolean has_uniqueVertices = true;	/* set to false if not, never set this back to true */
		
		/**
		 * Convert to string
		 * 
		 * @return the string
		 */
		@Override
		public String toString() {
			return   "getInVertex=" + num_getInVertex + ":getOutVertex="     + num_getOutVertex +
					":getInEdges="  + num_getInEdges  + ":getOutEdges="      + num_getOutEdges  +
					":getVertices=" + num_getVertices + ":getVerticesNext="  + num_getVerticesNext +
					 (has_uniqueVertices ? ":uniqueVertices=" + num_uniqueVertices : "");
		}
	}
}
