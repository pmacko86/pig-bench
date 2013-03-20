package com.tinkerpop.bench.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Mount info wrapper 
 * 
 * @author Peter Macko
 */
public class MountInfo {
	
	private static final Pattern pattern = Pattern.compile("(.*) on (/.*) type (.*) \\((.*)\\)");
	
	private List<MountInfoRecord> records;
	private Map<String, MountInfoRecord> map;
	
	
	/**
	 * Create an instance of class IOStat
	 * 
	 * @param records the records
	 */
	private MountInfo(ArrayList<MountInfoRecord> records) {
		this.records = Collections.unmodifiableList(records);
	
		HashMap<String, MountInfoRecord> m = new HashMap<String, MountInfo.MountInfoRecord>();
		for (MountInfoRecord r : records) {
			if (!r.device.equals("none")) {
				m.put(r.device, r);
			}
		}
		
		this.map = Collections.unmodifiableMap(m);
	}
	
	
	/**
	 * Get the records
	 * 
	 * @return the records
	 */
	public final List<MountInfoRecord> getRecords() {
		return records;
	}
	
	
	/**
	 * Get the map of all device mount records, excluding the "none" devices
	 * 
	 * @return the map of device names to their mount records
	 */
	public final Map<String, MountInfoRecord> getMountMap() {
		return map;
	}
	
	
	/**
	 * Get the record for a particular device
	 * 
	 * @param devide the device name other than "none"
	 * @return the device stats
	 * @throws IllegalArgumentException if the device name is null or "none"
	 * @throws NoSuchElementException if the device does not exist
	 */
	public MountInfoRecord getRecordForDevice(String device) {
		
		if (device == null || device.equals("none")) {
			throw new IllegalArgumentException();
		}
		
		MountInfoRecord r = map.get(device);
		if (r == null) {
			r = map.get("/dev/" + device);
			if (r == null) {
				throw new NoSuchElementException();
			}
		}
		
		return r;
	}
	
	
	/**
	 * Get the device mount info record for the given file
	 * 
	 * @param file the file
	 * @return the mount info record
	 * @throws NoSuchElementException if no such record could be found
	 */
	public MountInfoRecord getRecordForFile(File file) {
		String abs = file.getAbsolutePath();
		
		MountInfoRecord best = null;
		int maxLen = -1;
		
		for (MountInfoRecord r : records) {
			if (abs.equals(r.directory)) return r;
			if (abs.startsWith(r.directory.endsWith("/") ? r.directory : (r.directory + "/"))) {
				if (maxLen < r.directory.length()) {
					maxLen = r.directory.length();
					best = r;
				}
			}
		}
		
		if (best == null) {
			throw new NoSuchElementException("Could not determine the mountpoint for " + abs);
		}
		
		return best;
	}
	
	
	/**
	 * Run mount and get the results
	 * 
	 * @return the results
	 * @throws IOException on error
	 */
	public static MountInfo run() throws IOException {
		
		
		// Run the process
		
		Process p = Runtime.getRuntime().exec(new String[] { "mount" });

		BufferedReader es = new BufferedReader(new InputStreamReader(p.getErrorStream()));
		BufferedReader os = new BufferedReader(new InputStreamReader(p.getInputStream()));

		String line;
		boolean error = false;
		ArrayList<String> output = new ArrayList<String>();
		
		while ((line = os.readLine()) != null) output.add(line);
		while ((line = es.readLine()) != null) error = true;

		es.close();
		os.close();

		try {
			int result = p.waitFor();
			if (result != 0) {
				throw new IOException("mount failed with return code " + result);
			}
		}
		catch (InterruptedException e) {
			throw new IOException("mount was interrupted");
		}
		
		if (error) {
			throw new IOException("mount failed");
		}
		
		
		// Parse
		
		/*
		 * Sample output:
		 * 
		 * /dev/sda3 on / type ext4 (rw,errors=remount-ro)
		 * proc on /proc type proc (rw,noexec,nosuid,nodev)
		 * sysfs on /sys type sysfs (rw,noexec,nosuid,nodev)
		 * none on /sys/fs/fuse/connections type fusectl (rw)
 		 */
		
		ArrayList<MountInfoRecord> records = new ArrayList<MountInfo.MountInfoRecord>();
		
		for (String outputLine : output) {			
			Matcher m = pattern.matcher(outputLine);
			
			if (m.find()) {
				MountInfoRecord r = new MountInfoRecord();
				r.device = m.group(1);
				r.directory = m.group(2);
				r.type = m.group(3);
				r.options = m.group(4).split(",");
				records.add(r);
			}
			else {
				throw new IOException("Could not parse line: " + outputLine);
			}
		}
		
		return new MountInfo(records);
	}
	
	
	/**
	 * A mount info record
	 */
	public static class MountInfoRecord {
		
		String device, directory, type;
		String[] options;

		
		/**
		 * Get the device name
		 * 
		 * @return the device name
		 */
		public final String getDevice() {
			return device;
		}

		
		/**
		 * Get the directory
		 * 
		 * @return the directory
		 */
		public final String getDirectory() {
			return directory;
		}

		
		/**
		 * Get the file system type
		 * 
		 * @return the type
		 */
		public final String getType() {
			return type;
		}

		
		/**
		 * Get the mount options
		 * 
		 * @return the options
		 */
		public final String[] getOptions() {
			return options;
		}
		
		
		/**
		 * Create and return a human-readable String version of the record
		 * 
		 * @return the string version of the record
		 */
		@Override
		public String toString() {
			String s = device + " on " + directory + " type " + type;
			if (options.length > 0) {
				s += " (" + options[0];
				for (int i = 1; i < options.length; i++) {
					s += "," + options[i];
				}
				s += ")";
			}
			return s;
		}
	}
}
