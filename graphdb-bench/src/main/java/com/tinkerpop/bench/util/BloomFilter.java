package com.tinkerpop.bench.util;

import java.util.BitSet;
import java.util.Random;


/**
 * A Bloom filter optimized for avoiding garbage collection. Please note that
 * this implementation is not thread-safe.
 * 
 * @author Peter Macko
 */
public class BloomFilter {
	
	// Inspired by:
	//   https://github.com/MagnusS/Java-BloomFilter/blob/master/src/com/skjegstad/utils/BloomFilter.java
	//   https://github.com/Greplin/greplin-bloom-filter/blob/master/src/main/java/com/greplin/bloomfilter/RepeatedMurmurHash.java

	private BitSet bitset;
	private double bitsPerElement;
	private int capacity;
	private int bitSetSize;
	private int numHashes;
	private int size;
	
	private int[] hashResults;
	private byte[] temp;
	

	/**
	 * Construct an empty Bloom filter.
	 *
	 * @param bitsPerElement is the number of bits used per element
	 * @param capacity is the expected number of elements the filter will contain
	 * @param k is the number of hash functions used
	 */
	public BloomFilter(double bitsPerElement, int capacity, int numHashes) {
		
		if (numHashes < 2) {
			throw new IllegalArgumentException("numHashes must be at least 2");
		}
		
		this.capacity = capacity;
		this.numHashes = numHashes;
		this.bitsPerElement = bitsPerElement;
		this.bitSetSize = (int) Math.ceil(bitsPerElement * capacity);
		
		this.bitset = new BitSet(bitSetSize);
		this.size = 0;
		
		this.hashResults = new int[numHashes];
		this.temp = new byte[16];
	}
	
	
	/**
	 * Constructs an empty Bloom filter with the given false positive probability
	 *
	 * @param capacity is the expected number of elements in the Bloom filter
	 * @param falsePositiveProbability is the desired false positive probability
	 */
	public BloomFilter(int capacity, double falsePositiveProbability) {
		
		// k = ceil(-log_2(false prob.))
		// c = k / ln(2)
		
		this (Math.ceil(-MathUtils.log(falsePositiveProbability, 2)) / Math.log(2),
			  capacity,
			  (int) Math.ceil(-MathUtils.log(falsePositiveProbability, 2)));
	}
	
	
	/**
	 * Get the capacity
	 * 
	 * @return the capacity
	 */
	public int getCapacity() {
		return capacity;
	}
	
	
	/**
	 * Get the number of bits per element
	 * 
	 * @return the number of allocated bits per element
	 */
	public double getBitsPerElement() {
		return bitsPerElement;
	}
	
	
	/**
	 * Get the approximate number of elements added to the filter
	 * 
	 * @return the number of elements
	 */
	public int size() {
		return size;
	}
	
	
	/**
	 * Clear
	 */
	public void clear() {
		
		if (size != 0) {
			bitset.clear();
			size = 0;
		}
	}

	
	/**
	 * Compute a hash value using Murmur hashing
	 * 
	 * @param data the data
	 * @param len the length of the data
	 * @param seed the initial seed
	 * @return the hash value
	 */
	private static int hashMurmur(byte[] data, int len, int seed) {
		
		// https://github.com/Greplin/greplin-bloom-filter/blob/master/src/main/java/com/greplin/bloomfilter/RepeatedMurmurHash.java
		
		int m = 0x5bd1e995;
		int r = 24;

		int h = seed ^ len;
		int chunkLen = len >> 2;

		for (int i = 0; i < chunkLen; i++) {
			int iChunk = i << 2;
			int k = data[iChunk + 3];
			k = k << 8;
			k = k | (data[iChunk + 2] & 0xff);
			k = k << 8;
			k = k | (data[iChunk + 1] & 0xff);
			k = k << 8;
			k = k | (data[iChunk + 0] & 0xff);
			k *= m;
			k ^= k >>> r;
			k *= m;
			h *= m;
			h ^= k;
		}

		int lenMod = chunkLen << 2;
		int left = len - lenMod;

		if (left != 0) {
			if (left >= 3) {
				h ^= (int) data[len - 3] << 16;
			}
			if (left >= 2) {
				h ^= (int) data[len - 2] << 8;
			}
			if (left >= 1) {
				h ^= (int) data[len - 1];
			}

			h *= m;
		}

		h ^= h >>> 13;
		h *= m;
		h ^= h >>> 15;

		return h;
	}


	/**
	 * Compute hashes and store then in an array
	 * 
	 * @param data the data
	 * @param len the length of the data	
	 * @param out the array reference for output
	 */
	private void hash(byte[] data, int len, int[] out) {

		int hashA = hashMurmur(data, len, 0);
		int hashB = hashMurmur(data, len, hashA);

		for (int i = 0; i < out.length; i++) {
			out[i] = hashA + i * hashB;
		}
	}
	
	
	/**
	 * Determine if the element is in the set
	 * 
	 * @param data the data
	 * @param len the length of the data
	 * @return true if it is in the set
	 */
	private final boolean contains(byte[] data, int len) {
		
		hash(data, len, hashResults);
		for (int i = 0; i < numHashes; i++) {
			if (!bitset.get(Math.abs(hashResults[i] % bitSetSize))) return false;
		}
		
		return true;
	}
	
	
	/**
	 * Add an the element to the set
	 * 
	 * @param data the data
	 * @param len the length of the data
	 * @return true if the element was added; false if it is already there
	 */
	private final boolean add(byte[] data, int len) {
		
		hash(data, len, hashResults);
		for (int i = 0; i < numHashes; i++) {
			if (!bitset.get(Math.abs(hashResults[i] % bitSetSize))) {
				for (int j = 0; j < numHashes; j++) {
					bitset.set(Math.abs(hashResults[j] % bitSetSize));
				}
				size++;
				return true;
			}
		}
		
		return false;
	}
	
	
	/**
	 * Determine if the element is in the set
	 * 
	 * @param data the data
	 * @return true if it is in the set
	 */
	public boolean contains(byte[] data) {
		return contains(data, data.length);
	}
	
	
	/**
	 * Add an the element to the set
	 * 
	 * @param data the data
	 * @return true if the element was added; false if it is already there
	 */
	public boolean add(byte[] data) {
		return add(data, data.length);
	}
	
	
	/**
	 * Determine if the element is in the set
	 * 
	 * @param n the element
	 * @return true if it is in the set
	 */
	public boolean contains(int n) {
		
		for (int i = 0; i < 4; i++) {
			temp[i] = (byte) (n & 0xff);
			n >>>= 8;
		}
		
		return contains(temp, 4);
	}
	
	
	/**
	 * Add an the element to the set
	 * 
	 * @param n the element
	 * @return true if the element was added; false if it is already there
	 */
	public boolean add(int n) {
		
		for (int i = 0; i < 4; i++) {
			temp[i] = (byte) (n & 0xff);
			n >>>= 8;
		}
		
		return add(temp, 4);
	}
	
	
	/**
	 * Determine if the element is in the set
	 * 
	 * @param n the element
	 * @return true if it is in the set
	 */
	public boolean contains(long n) {
		
		for (int i = 0; i < 8; i++) {
			temp[i] = (byte) (n & 0xff);
			n >>>= 8;
		}
		
		return contains(temp, 8);
	}
	
	
	/**
	 * Add an the element to the set
	 * 
	 * @param n the element
	 * @return true if the element was added; false if it is already there
	 */
	public boolean add(long n) {
		
		for (int i = 0; i < 8; i++) {
			temp[i] = (byte) (n & 0xff);
			n >>>= 8;
		}
		
		return add(temp, 8);
	}
	
	
	/**
	 * Determine if the element is in the set
	 * 
	 * @param e the element
	 * @return true if it is in the set
	 */
	public boolean contains(Integer e) {
		return contains(e.intValue());
	}
	
	
	/**
	 * Add an the element to the set
	 * 
	 * @param e the element
	 * @return true if the element was added; false if it is already there
	 */
	public boolean add(Integer e) {
		return add(e.intValue());
	}
	
	
	/**
	 * Determine if the element is in the set
	 * 
	 * @param e the element
	 * @return true if it is in the set
	 */
	public boolean contains(Long e) {
		return contains(e.longValue());
	}
	
	
	/**
	 * Add an the element to the set
	 * 
	 * @param e the element
	 * @return true if the element was added; false if it is already there
	 */
	public boolean add(Long e) {
		return add(e.longValue());
	}
	
	
	/**
	 * Determine if the element is in the set
	 * 
	 * @param e the element
	 * @return true if it is in the set
	 */
	public boolean contains(String e) {
		return contains(e.getBytes());
	}
	
	
	/**
	 * Add an the element to the set
	 * 
	 * @param e the element
	 * @return true if the element was added; false if it is already there
	 */
	public boolean add(String e) {
		return add(e.getBytes());
	}
	
	
	/**
	 * Determine if the element is in the set
	 * 
	 * @param e the element
	 * @return true if it is in the set
	 */
	public boolean contains(Object e) {
		
		Class<?> c = e.getClass();
		if (c == Integer.class) return contains(((Integer) e).intValue());
		if (c == Long.class   ) return contains(((Long   ) e).longValue());
		if (c == String.class ) return contains(((String ) e).getBytes());
		
		return contains(e.hashCode());
	}
	
	
	/**
	 * Add an the element to the set
	 * 
	 * @param e the element
	 * @return true if the element was added; false if it is already there
	 */
	public boolean add(Object e) {
		
		Class<?> c = e.getClass();
		if (c == Integer.class) return add(((Integer) e).intValue());
		if (c == Long.class   ) return add(((Long   ) e).longValue());
		if (c == String.class ) return add(((String ) e).getBytes());
		
		return add(e.hashCode());
	}
	
	
	/**
	 * A test function
	 */
	public static void main(String[] args) {
		
		if (true) {
			ConsoleUtils.header("Checking correctness");
			
			BloomFilter b = new BloomFilter(10 * 1000, 0.001);
			Random random = new Random();
			
			long[] r = new long[10 * 1000];
			for (int i = 0; i < r.length; i++) {
				r[i] = random.nextLong();
			}
			
			for (int i = 0; i < r.length; i++) {
				b.add(r[i]);
			}
			
			int has = 0;
			for (int i = 0; i < r.length; i++) {
				if (b.contains(r[i])) has++;
			}
			
			if (has != r.length) {
				System.out.println("Failed - inserted " + r.length + " elements, but only " + has + " are there");
				System.exit(1);
			}
			
			has = 0;
			for (int i = 0; i < r.length; i++) {
				if (b.contains(random.nextLong())) has++;
			}
			
			System.out.println("False positives: " + has + " out of " + r.length);
			
			if (has > 2 * r.length * 0.001) {
				System.out.println("That's too many.");
				System.exit(1);
			}
			
			System.out.println("Succeeded.");
			System.out.println();
		}
		
		System.gc();
		
		if (true) {
			
			ConsoleUtils.header("Testing performance");
			
			BloomFilter b = new BloomFilter(10 * 1000 * 1000, 0.001);
			Random random = new Random();
			
			long[] r = new long[10 * 1000 * 1000];
			System.out.println("Creating " + r.length + " random elements of type \"long\"...");
			for (int i = 0; i < r.length; i++) {
				r[i] = random.nextLong();
			}
			
			System.out.println("Inserting " + r.length + " elements...");
			long start = System.currentTimeMillis();
			for (int i = 0; i < r.length; i++) {
				b.add(r[i]);
			}
			long end = System.currentTimeMillis();
			
			System.out.println("Inserted " + r.length
					+ " elements in [" + OutputUtils.formatTimeMS(end - start) + "]"
					+ " at rate " + String.format("%.3f", 0.001 * (r.length / ((double) (end - start)))) + " Mop/s.");
		}
	}
}
