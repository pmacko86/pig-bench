package com.tinkerpop.bench.util;

import java.util.Comparator;


/**
 * Natural string comparator
 * 
 * @author Peter Macko (pmacko@eecs.harvard.edu)
 */
public class NaturalStringComparator implements Comparator<String> {
	
	protected boolean useMultipliers = true;

	
	/**
	 * Compare two strings
	 * 
	 * @param a the first string
	 * @param b the second string
	 * @return the result of comparison
	 */
	@Override
	public int compare(String a, String b) {
		
		
		// Deal with nulls
		
		if (a == null && b == null) return 0;
		if (a == null) return -1;
		if (b == null) return  1;
		
	
		// Compare by chunks
		
		int ai = 0;
		int bi = 0;
		
		int al = a.length();
		int bl = b.length();
		
		while (true) {
			
			
			// Check if we finished
			
			if (ai >= al && bi >= bl) return 0;
			if (ai >= al) return -1;
			if (bi >= bl) return  1;
				
			
			// Get the next chunk
			
			boolean ad = Character.isDigit(a.charAt(ai));
			boolean bd = Character.isDigit(b.charAt(bi));
			
			if (ad && bd) {
				
				
				// Get the numbers
				
				String asn = "";
				while (ai < al && Character.isDigit(a.charAt(ai))) {
					asn += a.charAt(ai++);
				}
				
				String bsn = "";
				while (bi < bl && Character.isDigit(b.charAt(bi))) {
					bsn += b.charAt(bi++);
				}
				
				long an = Long.parseLong(asn);
				long bn = Long.parseLong(bsn);
				
				
				// Get the multipliers
				
				if (useMultipliers) {
					
					if (ai < al) {
						
						char c = Character.toUpperCase(a.charAt(ai));
						boolean isM = true;
						boolean isB = ai + 1 < al && Character.toUpperCase(a.charAt(ai + 1)) == 'B';
						
						switch (c) {
						case 'B': isB = false; break;
						case 'K': an *= isB ? 1024l : 1000l; break;
						case 'M': an *= isB ? 1024l * 1024l : 1000l * 1000l; break;
						case 'G': an *= isB ? 1024l * 1024l * 1024l : 1000l * 1000l * 1000l; break;
						case 'T': an *= isB ? 1024l * 1024l * 1024l * 1024l : 1000l * 1000l * 1000l * 1000l; break;
						default : isM = false;
						}
						
						if (isM) {
							ai++;
							if (isB) ai++;
						}
					}
					
					if (bi < bl) {
						
						char c = Character.toUpperCase(b.charAt(bi));
						boolean isM = true;
						boolean isB = bi + 1 < bl && Character.toUpperCase(b.charAt(bi + 1)) == 'B';
						
						switch (c) {
						case 'B': isB = false; break;
						case 'K': bn *= isB ? 1024l : 1000l; break;
						case 'M': bn *= isB ? 1024l * 1024l : 1000l * 1000l; break;
						case 'G': bn *= isB ? 1024l * 1024l * 1024l : 1000l * 1000l * 1000l; break;
						case 'T': bn *= isB ? 1024l * 1024l * 1024l * 1024l : 1000l * 1000l * 1000l * 1000l; break;
						default : isM = false;
						}
						
						if (isM) {
							bi++;
							if (isB) bi++;
						}
					}
				}
				
				
				// Compare
				
				if (an < bn) return -1;
				if (an > bn) return  1;
				
				if (!asn.equals(bsn)) {
					if (asn.length() > bsn.length()) return -1;
					if (asn.length() < bsn.length()) return  1;
					if (asn.compareTo(bsn) < 0) return -1;
					if (asn.compareTo(bsn) > 0) return  1;
				}
			}
			else {
			
				if (a.charAt(ai) < b.charAt(bi)) return -1;
				if (a.charAt(ai) > b.charAt(bi)) return  1;
				
				ai++;
				bi++;
			}
		}
	}
}
