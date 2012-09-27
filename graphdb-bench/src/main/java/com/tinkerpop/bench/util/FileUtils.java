package com.tinkerpop.bench.util;

import java.io.File;
import java.io.IOException;


/**
 * Miscellaneous file utils
 * 
 * @author Peter Macko
 */
public class FileUtils {
	
	
	/**
	 * Get sizes of files (in bytes) in the given directory.
	 * 
	 * @param dir the directory
	 * @param fileNames the file names
	 * @return the array of scaled sizes in bytes
	 */
	public static long[] getFileSizesB(File dir, String... fileNames) throws IOException {
		
		if (!dir.exists() || !dir.isDirectory()) {
			throw new IOException("The directory does not exist or is not a directory: " + dir.getAbsolutePath());
		}
		
		
		// Get the files
		
		File[] files = new File[fileNames.length];
		for (int i = 0; i < fileNames.length; i++) {
			File f = new File(dir, fileNames[i]);
			if (!f.exists() || !f.isFile()) {
				throw new IOException("The following file does not exist or is not a regular file: " + f.getName());
			}
			files[i] = f;
		}

		
			
		// Get the size of the files
		
		long[] sizes = new long[files.length];
		for (int i = 0; i < sizes.length; i++) {
			sizes[i] = files[i].length();
		}
		
		return sizes;
	}
	
	
	/**
	 * Get sizes of files (in MB) in the given directory and scale them, so that their sum will
	 * approximately match the specified total size, and each individual size is at least 1.
	 * 
	 * @param dir the directory
	 * @param totalDesiredSize the desired total size in MB
	 * @param fileNames the file names
	 * @return the array of scaled sizes in MB
	 */
	public static long[] getScaledFileSizesMB(File dir, long totalDesiredSize, String... fileNames) throws IOException {
		
		if (!dir.exists() || !dir.isDirectory()) {
			throw new IOException("The directory does not exist or is not a directory: " + dir.getAbsolutePath());
		}
		
		
		// Get the files
		
		File[] files = new File[fileNames.length];
		for (int i = 0; i < fileNames.length; i++) {
			File f = new File(dir, fileNames[i]);
			if (!f.exists() || !f.isFile()) {
				throw new IOException("The following file does not exist or is not a regular file: " + f.getName());
			}
			files[i] = f;
		}

		
			
		// Get the size of the files
		
		long[] sizes = new long[files.length];
		long total = 0;
		for (int i = 0; i < sizes.length; i++) {
			sizes[i] = Math.round(Math.ceil(files[i].length() / 1048576.0));
			total += sizes[i];
		}
		
		
		// Scale the size of the files
		
		double f = totalDesiredSize / (double) total;
		long[] adjusted = new long[sizes.length];
		for (int i = 0; i < sizes.length; i++) {
			adjusted[i] = Math.round(sizes[i] * f);
			if (adjusted[i] < 1) adjusted[i] = 1;
		}
		
		return adjusted;
	}


	/**
	 * Recursively delete the directory if it exists
	 * 
	 * @param dir the directory
	 * @return true if the directory existed, false if not
	 */
	public static boolean deleteDir(File dir) {
		
		// This function was migrated here from LogUtils
	
		if (dir.exists()) {
			for (File file : dir.listFiles()) {
				if (file.isDirectory())
					deleteDir(file.getAbsolutePath());
				else
					file.delete();
			}
			dir.delete();
			
			return true;
		}
		else {
			return false;
		}
	}


	/**
	 * Recursively delete the directory if it exists
	 * 
	 * @param dirStr the directory
	 * @return true if the directory existed, false if not
	 */
	public static boolean deleteDir(String dirStr) {
		return deleteDir(new File(dirStr));
	}
}
