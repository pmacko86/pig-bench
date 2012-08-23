package com.tinkerpop.bench;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.util.io.graphml.GraphMLWriter;


/**
 * A collection of miscellaneous graph utils
 * 
 * @author Peter Macko
 */
public class GraphUtils {
	
	// Name of a VERTICES index
	public static final String INDEX_VERTICES = "VERTICES";
	
	// Name of a EDGES index
	public static final String INDEX_EDGES = "EDGES";
	
	
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
		
		stat.num_getVertices++;
		for (Vertex w : v.getVertices(Direction.BOTH)) {
			stat.num_getVerticesNext++;
			stat.num_uniqueVertices++;
			neighbors.add(w);
		}
		
		int triangles = 0;
		for (Vertex w : neighbors) {
			stat.num_getVertices++;
			for (Vertex z : w.getVertices(Direction.BOTH)) {
				stat.num_getVerticesNext++;
				if (neighbors.contains(z)) {
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
			out.println("    <edge source=\"" + edge.getVertex(Direction.OUT).getId() + "\" "
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
		
		public int num_getVertices = 0;
		public int num_getVerticesNext = 0;
		public int num_getEdges = 0;
		public int num_getEdgesNext = 0;
		public int num_getAllVertices = 0;
		public int num_getAllVerticesNext = 0;			/* getting the next vertex from the iterator */
		public int num_uniqueVertices = 0;
		
		public boolean has_uniqueVertices = true;	/* set to false if not, never set this back to true */
		
		/**
		 * Convert to string
		 * 
		 * @return the string
		 */
		@Override
		public String toString() {
			return   "getVertices="    + num_getVertices      + ":getVerticesNext="     + num_getVerticesNext    +
					":getEdges="       + num_getEdges         + ":getEdgesNext="        + num_getEdgesNext       +
					":getAllVertices=" + num_getAllVertices   + ":getAllVerticesNext="  + num_getAllVerticesNext +
					 (has_uniqueVertices ? ":uniqueVertices=" + num_uniqueVertices : "");
		}
	}
}
