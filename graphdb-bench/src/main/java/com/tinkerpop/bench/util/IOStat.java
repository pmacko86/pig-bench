package com.tinkerpop.bench.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.commons.lang.StringUtils;


/**
 * IOStat wrapper 
 * 
 * @author Peter Macko
 */
public class IOStat {
	
	private CPUStat cpu;
	private Map<String, DeviceStat> devices;
	
	
	/**
	 * Create an instance of class IOStat
	 * 
	 * @param cpu the avg-cpu stats
	 * @param devices the device stats 
	 */
	private IOStat(CPUStat cpu, Map<String, DeviceStat> devices) {
		this.cpu = cpu;
		this.devices = Collections.unmodifiableMap(devices);
	}
	
	
	/**
	 * Get the avg-cpu stats
	 * 
	 * @return the avg-cpu stats
	 */
	public final CPUStat getCPUStat() {
		return cpu;
	}
	
	
	/**
	 * Get the devices stats
	 * 
	 * @return the map of device names to their stats
	 */
	public final Map<String, DeviceStat> getDeviceStats() {
		return devices;
	}
	
	
	/**
	 * Get the stats for a particular device
	 * 
	 * @param devide the device name
	 * @return the device stats
	 * @throws NoSuchElementException if the device does not exist
	 */
	public final DeviceStat getDeviceStat(String device) {
		if (device.startsWith("/dev/")) {
			device = device.substring(5);
		}
		DeviceStat d = devices.get(device);
		if (d == null) throw new NoSuchElementException();
		return d;
	}
	

	/**
	 * Determine if iostat is installed
	 *
	 * @return true if it is installed
	 */
	public static boolean isInstalled() {
		int r = -1;
		try {
			String[] a = {"/bin/sh", "-c", "which iostat"};
			Process p = Runtime.getRuntime().exec(a);
			r = p.waitFor();
		}
		catch (Exception e) {}
		return r == 0;
	}
	
	
	/**
	 * Run iostat once and get the results
	 * 
	 * @return the results
	 * @throws IOException on error
	 */
	public static IOStat run() throws IOException {
		
		
		// Run the process
		
		Process p = Runtime.getRuntime().exec(new String[] { "iostat", "-p", "-k" });

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
				throw new IOException("iostat failed with return code " + result);
			}
		}
		catch (InterruptedException e) {
			throw new IOException("iostat was interrupted");
		}
		
		if (error) {
			throw new IOException("iostat failed");
		}
		
		
		// Parse
		
		/*
		 * Sample output:
		 * 
		 * Linux 3.2.0-38-generic (auburn)         03/20/2013      _x86_64_        (2 CPUStat)
		 * 
		 * avg-cpu:  %user   %nice %system %iowait  %steal   %idle
		 *            0.35    0.05    0.41    0.60    0.00   98.58
		 * 
		 * DeviceStat:            tps    kB_read/s    kB_wrtn/s    kB_read    kB_wrtn
		 * sda               1.86        23.32        28.96    4130096    5128264
		 * sdb               0.57        20.76        11.93    3675572    2113197
 		 */
		
		CPUStat cpu = null;
		HashMap<String, DeviceStat> devices = new HashMap<String, DeviceStat>();
		
		boolean in_cpu = false;
		boolean in_dev = false;
		
		int lineCount = 0;
		
		for (String outputLine : output) {
			lineCount++;
			
			String l = outputLine.trim();
			String[] tokens = StringUtils.split(l, " \t");
			
			if (l.equals("")) {
				in_cpu = false;
				in_dev = false;
				continue;
			}
			
			if (tokens[0].equals("avg-cpu:")) {
				in_dev = false;
				in_cpu = true;
				
				boolean ok = true;
				if (tokens.length == 7) {
					if (!tokens[1].equals("%user")) ok = false;
					if (!tokens[2].equals("%nice")) ok = false;
					if (!tokens[3].equals("%system")) ok = false;
					if (!tokens[4].equals("%iowait")) ok = false;
					if (!tokens[5].equals("%steal")) ok = false;
					if (!tokens[6].equals("%idle")) ok = false;
				}
				else {
					ok = false;
				}
				
				if (!ok) {
					throw new IOException("Unsupported iostat avg-cpu headers");
				}
				continue;
			}
			
			if (in_cpu) {
				if (cpu != null) {
					throw new IOException("More than one avg-cpu line");
				}
				
				if (tokens.length != 6) {
					throw new IOException("Unsupported format of the avg-cpu line");
				}
				
				cpu = new CPUStat();
				cpu.user = Double.parseDouble(tokens[0]);
				cpu.nice = Double.parseDouble(tokens[1]);
				cpu.system = Double.parseDouble(tokens[2]);
				cpu.iowait = Double.parseDouble(tokens[3]);
				cpu.steal = Double.parseDouble(tokens[4]);
				cpu.idle = Double.parseDouble(tokens[5]);
				continue;
			}
			
			if (tokens[0].equals("Device:")) {
				in_cpu = false;
				in_dev = true;
				
				boolean ok = true;
				if (tokens.length == 6) {
					if (!tokens[1].equals("tps")) ok = false;
					if (!tokens[2].equals("kB_read/s")) ok = false;
					if (!tokens[3].equals("kB_wrtn/s")) ok = false;
					if (!tokens[4].equals("kB_read")) ok = false;
					if (!tokens[5].equals("kB_wrtn")) ok = false;
				}
				else {
					ok = false;
				}
				
				if (!ok) {
					throw new IOException("Unsupported iostat device headers");
				}
				continue;
			}
			
			if (in_dev) {
				if (tokens.length != 6) {
					throw new IOException("Unsupported format of a device line");
				}
				
				DeviceStat d = new DeviceStat();
				d.name = tokens[0];
				d.tps = Double.parseDouble(tokens[1]);
				d.kB_read_ps = Double.parseDouble(tokens[2]);
				d.kB_wrtn_ps = Double.parseDouble(tokens[3]);
				d.kB_read = Long.parseLong(tokens[4]);
				d.kB_wrtn = Long.parseLong(tokens[5]);
				devices.put(d.name, d);
				continue;
			}
			
			if (lineCount > 1) {
				throw new IOException("Unexpected line in the iostat output: " + l);
			}
		}
		
		
		if (cpu == null) {
			throw new IOException("No avg-cpu line in the iostat output");
		}
		
		if (devices.isEmpty()) {
			throw new IOException("No device stats found in the iostat output");
		}
		
		return new IOStat(cpu, devices);
	}
	
	
	/**
	 * The CPUStat stats
	 * 
	 * @author Peter Macko
	 */
	public static class CPUStat {
		
		double user, nice, system, iowait, steal, idle;

		
		/**
		 * The percentage of the user time
		 * 
		 * @return the user
		 */
		public final double getUser() {
			return user;
		}

		
		/**
		 * The percentage og the nice time
		 * 
		 * @return the nice
		 */
		public final double getNice() {
			return nice;
		}

		
		/**
		 * The percentage of the system time
		 * 
		 * @return the system
		 */
		public final double getSystem() {
			return system;
		}

		
		/**
		 * The percentage of the IO time
		 * 
		 * @return the iowait
		 */
		public final double getIowait() {
			return iowait;
		}

		
		/**
		 * The percentage of the steal time
		 * 
		 * @return the steal
		 */
		public final double getSteal() {
			return steal;
		}

		
		/**
		 * The percentage of the idle time
		 * 
		 * @return the idle
		 */
		public final double getIdle() {
			return idle;
		}
	}
	
	
	/**
	 * The device stats
	 * 
	 * @author Peter Macko
	 */
	public static class DeviceStat {
		
		String name;
		
		double tps, kB_read_ps, kB_wrtn_ps;
		long kB_read, kB_wrtn;
		
		
		/**
		 * Get the device name
		 * 
		 * @return the name
		 */
		public final String getName() {
			return name;
		}
		
		
		/**
		 * Get the number of transactions per second
		 * 
		 * @return the tps
		 */
		public final double getTps() {
			return tps;
		}
		
		
		/**
		 * Get the number of kB read per second
		 * 
		 * @return the kB_read/s
		 */
		public final double getkB_read_ps() {
			return kB_read_ps;
		}
		
		
		/**
		 * Get the number of kB written per second
		 * 
		 * @return the kB_wrtn_ps
		 */
		public final double getkB_wrtn_ps() {
			return kB_wrtn_ps;
		}
		
		
		/**
		 * Get the total number of kB read
		 * 
		 * @return the kB_read
		 */
		public final long getkB_read() {
			return kB_read;
		}
		
		
		/**
		 * Get the total number of kB written
		 * 
		 * @return the kB_wrtn
		 */
		public final long getkB_wrtn() {
			return kB_wrtn;
		}
	}
}
