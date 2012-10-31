package com.tinkerpop.bench.util;

/**
 * Utilities for text output to the console
 * 
 * @author Peter Macko (pmacko@eecs.harvard.edu)
 */
public class ConsoleUtils {
	
	/**
	 * Whether to use color output to the console
	 */
	public static boolean useColor = true;
	
	/**
	 * Whether to use any escape sequences (including \r, \b)
	 */
	public static boolean useEscapeSequences = true;
	
	private static final String SPACES = "                                        ";
	private static final String BACKSPACES = "\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b";

	private static final int COLOR_WARN = 1;
	private static final int COLOR_ERROR = 8 + 1;
	private static final int COLOR_HEADER = 8 + 7;
	private static final int COLOR_SECTION_HEADER = 8 + 4;
	
	private static boolean useEscapeCharacter = false;
	
	private static long lastProgressDraw = 0;
	private static long lastProgressValue = -1;
	private static long lastProgressMax = -1;
	private static String lastProgressExtra = "";


	/**
	 * Determine whether the escape characters can be used in the standard output
	 */
	static {
		String osName = System.getProperty("os.name");
		useEscapeCharacter = (osName.startsWith("Linux") || osName.startsWith("Mac"));
	}
	
	
	/**
	 * Returns a stdout escape sequence for foreground color, or an empty string if the escape character
	 * should not be used
	 */
	private static String escapeColor(int color) {
		if (!useEscapeCharacter || !useColor || !useEscapeSequences) return "";
		if ((color < 0) || (color >= 16)) return "";
		
		return "" + (char)27 + ((color < 8) ? ("[0;3" + color) : ("[1;3" + (color - 8))) + "m";
	}
	
	
	/**
	 * Returns a stdout escape sequence for normal color output
	 */
	private static String escapeNormal() {
		if (!useEscapeCharacter || !useColor || !useEscapeSequences) return "";
		return "" + (char)27 + "[0;0m";
	}
	
	
	/**
	 * Print a warning to the screen
	 */
	public static void warn(String message) {
		System.out.println(escapeColor(COLOR_WARN) + "Warning:" + escapeNormal() + " " + message);
	}	
	
	
	/**
	 * Print an error to the screen
	 */
	public static void error(String message) {
		System.out.println(escapeColor(COLOR_ERROR) + "Error:" + escapeNormal() + " " + message);
	}	
	

	/**
	 * Print a header to the screen
	 */
	public static void header(String name) {
		System.out.println(escapeColor(COLOR_HEADER) + name + escapeNormal());
	}	

	
	/**
	 * Print a section header to the screen
	 */
	public static void sectionHeader(String name) {
		System.out.println("\n\n" + escapeColor(COLOR_SECTION_HEADER) + name + escapeNormal() + "\n");
	}
	
	
	/**
	 * Get a string of backspaces of the given length
	 * 
	 * @param length the number of backspaces
	 * @return the string of backspaces
	 */
	public static String getBackspaces(int length) {
		int l = BACKSPACES.length();
		int n = length;
		String s = "";
		while (n >= l) {
			s += BACKSPACES;
			n -= l;
		}
		s += BACKSPACES.substring(l - n);
		return s;
	}
	
	
	/**
	 * Get a string of spaces of the given length
	 * 
	 * @param length the number of spaces
	 * @return the string of spaces
	 */
	public static String getSpaces(int length) {
		int l = SPACES.length();
		int n = length;
		String s = "";
		while (n >= l) {
			s += SPACES;
			n -= l;
		}
		s += SPACES.substring(l - n);
		return s;
	}
	
	
	/**
	 * Print a progress indicator
	 * 
	 * @param value the position
	 * @param max the maximum value
	 * @param extra extra text to display before the percentages
	 */
	public static void printProgressIndicator(long value, long max, String extra) {
		if (value > max) value = max;
		long t = System.currentTimeMillis();
		if (t < lastProgressDraw + (useEscapeSequences ? 100 : 1000) && value != max) return;
		lastProgressDraw = t;
		
		if (useEscapeSequences) {
			
			String before = extra == null ? "" : "[" + extra + "] ";
			String before_b = "";
			if (extra != null) {
				before_b += getBackspaces(before.length());
			}
				
			String clear = "";
			if (lastProgressExtra.length() > 0) {
				clear += getSpaces(lastProgressExtra.length());
				clear += getBackspaces(lastProgressExtra.length());
			}
			lastProgressExtra = before;
			
			System.out.printf(": %s%6.2f%%%s%s\b\b\b\b\b\b\b\b\b", before, 100 * value / (float) max, clear, before_b);
		}
		else {
			
			if (extra != null) {
				if (!extra.equals(lastProgressExtra) || lastProgressMax != max || lastProgressValue >= value) {
					System.out.print(" [" + extra + "]");
				}
				lastProgressExtra = extra;
			}

			System.out.printf("...%.2f%%", 100 * value / (float) max);
		}
		
		lastProgressValue = value;
		lastProgressMax = max;
	}
	
	
	/**
	 * Print a progress indicator
	 * 
	 * @param value the position
	 * @param max the maximum value
	 */
	public static void printProgressIndicator(long value, long max) {
		printProgressIndicator(value, max, null);
	}
	
	
	/**
	 * Print values in multiple columns
	 * 
	 * @param width the column width
	 * @param values the values
	 */
	public static void printColumns(int width, String... values) {
		
		for (int i = 0; i < values.length; i++) {
			String v = values[i];
			
			System.out.print(v);
			
			if (i + 1 < values.length) {
				if (v.length() >= width) {
					System.out.print(" ");
				}
				else {
					System.out.print(getSpaces(width - v.length()));
				}
			}
		}
		
		System.out.println();
	}
}
