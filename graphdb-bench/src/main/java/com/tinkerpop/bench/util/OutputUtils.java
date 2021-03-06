package com.tinkerpop.bench.util;

import java.text.DecimalFormat;

import com.tinkerpop.bench.Bench;
import com.tinkerpop.blueprints.Direction;


/**
 * A collection of miscellaneous utilities for file and console output
 * 
 * @author Peter Macko
 */
public class OutputUtils {
	
	public static final DecimalFormat DOUBLE_FORMAT_3 = new DecimalFormat("0.000");
	
	
	/**
	 * Returns the string where all non-ascii and <, &, > are encoded as numeric entities. I.e. "&lt;A &amp; B &gt;"
	 * .... (insert result here). The result is safe to include anywhere in a text field in an XML-string. If there was
	 * no characters to protect, the original string is returned.
	 * 
	 * @param originalUnprotectedString
	 *            original string which may contain characters either reserved in XML or with different representation
	 *            in different encodings (like 8859-1 and UFT-8)
	 * @return the new string
	 */
	public static String encodeXMLString(String originalUnprotectedString) {
		
		// Original author: http://stackoverflow.com/users/53897/thorbjorn-ravn-andersen
		// From: http://stackoverflow.com/questions/439298/best-way-to-encode-text-data-for-xml-in-java
		
	    if (originalUnprotectedString == null) {
	        return null;
	    }
	    boolean anyCharactersProtected = false;

	    StringBuffer stringBuffer = new StringBuffer();
	    for (int i = 0; i < originalUnprotectedString.length(); i++) {
	        char ch = originalUnprotectedString.charAt(i);

	        boolean controlCharacter = ch < 32;
	        boolean unicodeButNotAscii = ch > 126;
	        boolean characterWithSpecialMeaningInXML = ch == '<' || ch == '&' || ch == '>';

	        if (characterWithSpecialMeaningInXML || unicodeButNotAscii || controlCharacter) {
	            stringBuffer.append("&#" + (int) ch + ";");
	            anyCharactersProtected = true;
	        } else {
	            stringBuffer.append(ch);
	        }
	    }
	    if (anyCharactersProtected == false) {
	        return originalUnprotectedString;
	    }

	    return stringBuffer.toString();
	}
	
	
	/**
	 * Format time in ms
	 * 
	 * @param t the time in milliseconds
	 * @return a formatted string
	 */
	public static String formatTimeMS(long t) {
		double s = t / 1000.0;
		int m = (int) (s / 60); s -= m * 60;
		return String.format("%d minute%s %.3f seconds", m, m == 1 ? "" : "s", s);
	}
	
	
	/**
	 * Format and simplify a file name, such as by removing the current directory prefix
	 * 
	 * @param name the file or directory name
	 * @return a simplified file name
	 */
	public static String simplifyFileName(String name) {
		
		String s = name;
		
		
		// Remove the GraphDB directory prefix
		
		String p = Bench.graphdbBenchDir;
		if (!"".equals(p) && !p.endsWith("/")) p += "/";
		if (s.startsWith(p)) s = s.substring(p.length());
		
		
		return s;
	}
	
	
	/**
	 * Convert a Blueprints Direction to a tag string
	 * 
	 * @param d the direction
	 * @return the tag string
	 */
	public static String toTag(Direction d) {
		
		switch (d) {
		case IN  : return "in";
		case OUT : return "out";
		case BOTH: return "both";
		default: throw new IllegalArgumentException("Invalid direction");
		}
	}
	
	
	/**
	 * Return the given array as a linear combination string
	 * 
	 * @param values the values
	 * @param variable the variable name
	 * @return null if the values are null
	 */
	public static String formatLinearCombination(Double[] values, String variable) {
		
		if (values == null) return null;
		if (MathUtils.ifNeitherIsNull(values) == null) return null;
		if (values.length == 0) return null;
		
		StringBuilder b = new StringBuilder();
		b.append(DOUBLE_FORMAT_3.format(values[0]));
		
		for (int i = 1; i < values.length; i++) {
			b.append(" + ");
			b.append(DOUBLE_FORMAT_3.format(values[i]));
			b.append("*");
			b.append(variable);
			if (i > 1) {
				b.append("^");
				b.append(i);
			}
		}
		
		return b.toString();
	}
	
	
	/**
	 * Format a Double
	 * 
	 * @param value the value
	 * @return the formatted value, or null
	 */
	public static String format(Double value) {
		return format(value, DOUBLE_FORMAT_3);
	}
	
	
	/**
	 * Format a Double
	 * 
	 * @param value the value
	 * @param format the format
	 * @return the formatted value, or null
	 */
	public static String format(Double value, DecimalFormat format) {
		if (value == null) return null;
		return format.format(value.doubleValue());
	}
}
