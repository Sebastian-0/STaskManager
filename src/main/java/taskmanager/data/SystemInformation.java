/*
 * Copyright (c) 2020. Sebastian Hjelm
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * See LICENSE for further details.
 */

package taskmanager.data;

import taskmanager.MeasurementContainer;
import taskmanager.Measurements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public class SystemInformation {
	/* Time data */
	public long bootTime;
	public long uptime;

	/* Memory data */
	public long physicalMemoryTotalInstalled; // Includes reserved memory
	public long physicalMemoryTotal;
	public Measurements<Long> physicalMemoryUsed;
	public Measurements<TopList> physicalMemoryTopList;
	public long reservedMemory;

	public long pageSize;

	// Windows memory types
	public long standbyMemory;
	public long modifiedMemory;
	public long freeMemory;

	public long commitLimit;
	public long commitUsed;

	public long kernelPaged;
	public long kernelNonPaged;

	/* Processor data */
	public int logicalProcessorCount;
	public int physicalProcessorCount;

	public Measurements<Short>[] cpuUsagePerCore;
	public Measurements<Short> cpuUsageTotal;
	public Measurements<TopList> cpuTopList;

	public int totalProcesses;
	public int totalThreads;
	public int totalHandles;

	public List<Process> processes;
	public List<Process> deadProcesses;

	/* Network data */
	public Network[] networks;

	/* Disk data */
	public Disk[] disks;

	/* GPU data */
	public Gpu[] gpus;

	/* Other system data */
	public String userName;

	@SuppressWarnings("unchecked")
	public SystemInformation() {
		physicalMemoryUsed = new MeasurementContainer<>(0L);
		physicalMemoryTopList = new MeasurementContainer<>(TopList.EMPTY);
		cpuUsagePerCore = new MeasurementContainer[0];
		cpuUsageTotal = new MeasurementContainer<>((short) 0);
		cpuTopList = new MeasurementContainer<>(TopList.EMPTY);
		processes = new ArrayList<>();
		deadProcesses = new ArrayList<>();
		networks = new Network[0];
		disks = new Disk[0];
		gpus = new Gpu[0];
	}

	@SuppressWarnings("unchecked")
	public void copyFrom(SystemInformation other) {
		if (cpuUsagePerCore.length != other.cpuUsagePerCore.length) {
			cpuUsagePerCore = new MeasurementContainer[other.cpuUsagePerCore.length];
		}
		if (networks.length != other.networks.length) {
			networks = new Network[other.networks.length];
		}
		if (disks.length != other.disks.length) {
			disks = new Disk[other.disks.length];
		}
		if (gpus.length != other.gpus.length) {
			gpus = new Gpu[other.gpus.length];
		}

		bootTime = other.bootTime;
		uptime = other.uptime;

		physicalMemoryTotalInstalled = other.physicalMemoryTotalInstalled; // Includes reserved memory
		physicalMemoryTotal = other.physicalMemoryTotal;
		physicalMemoryUsed.copyDelta(other.physicalMemoryUsed);
		physicalMemoryTopList.copyDelta(other.physicalMemoryTopList);
		reservedMemory = other.reservedMemory;

		pageSize = other.pageSize;

		// Windows memory types
		standbyMemory = other.standbyMemory;
		modifiedMemory = other.modifiedMemory;
		freeMemory = other.freeMemory;

		commitLimit = other.commitLimit;
		commitUsed = other.commitUsed;

		kernelPaged = other.kernelPaged;
		kernelNonPaged = other.kernelNonPaged;

		/* Processor data */
		logicalProcessorCount = other.logicalProcessorCount;
		physicalProcessorCount = other.physicalProcessorCount;

		for (int i = 0; i < cpuUsagePerCore.length; i++) {
			if (cpuUsagePerCore[i] == null) {
				cpuUsagePerCore[i] = new MeasurementContainer<>((short) 0);
			}
			cpuUsagePerCore[i].copyDelta(other.cpuUsagePerCore[i]);
		}
		cpuUsageTotal.copyDelta(other.cpuUsageTotal);
		cpuTopList.copyDelta(other.cpuTopList);

		totalProcesses = other.totalProcesses;
		totalThreads = other.totalThreads;
		totalHandles = other.totalHandles;

		copyProcesses(other);
		copyNetworks(other);
		copyDisks(other);
		copyGpus(other);

		userName = other.userName;
	}

	private void copyNetworks(SystemInformation other) {
		for (int i = 0; i < networks.length; i++) {
			boolean isNew = false;
			if (networks[i] == null) {
				networks[i] = new Network();
				isNew = true;
			}
			networks[i].copyFrom(other.networks[i], isNew);
		}
	}

	private void copyDisks(SystemInformation other) {
		for (int i = 0; i < disks.length; i++) {
			boolean isNew = false;
			if (disks[i] == null) {
				disks[i] = new Disk();
				isNew = true;
			}
			disks[i].copyFrom(other.disks[i], isNew);
		}
	}

	private void copyGpus(SystemInformation other) {
		for (int i = 0; i < gpus.length; i++) {
			boolean isNew = false;
			if (gpus[i] == null) {
				gpus[i] = new Gpu();
				isNew = true;
			}
			gpus[i].copyFrom(other.gpus[i], isNew);
		}
	}

	private void copyProcesses(SystemInformation other) {
		copyProcessesFor(other.processes, processes);
		copyProcessesFor(other.deadProcesses, deadProcesses);
	}

	private void copyProcessesFor(List<Process> source, List<Process> target) {
		Set<Long> processIds = new HashSet<>();
		for (Process processNew : source) {
			Process process = null;
			for (Process processOld : target) {
				if (processNew.uniqueId == processOld.uniqueId) {
					process = processOld;
					break;
				}
			}
			boolean isNew = false;
			if (process == null) {
				process = new Process(processNew.uniqueId, processNew.id);
				target.add(process);
				isNew = true;
			}

			processIds.add(process.uniqueId);
			process.copyFrom(processNew, isNew);
		}

		// Remove old processes
		target.removeIf(process -> !processIds.contains(process.uniqueId));
	}

	public Process getProcessById(long pid) {
		for (Process process : processes) {
			if (process.id == pid) {
				return process;
			}
		}
		return null;
	}

	public Process getDeadProcessById(long pid) { // TODO Might be multiple with the same ID
		for (Process process : deadProcesses) {
			if (process.id == pid) {
				return process;
			}
		}
		return null;
	}



	public static class TopList implements Comparable<TopList> { // TODO Support TopLists of Short!
		public static final TopList EMPTY = new TopList(0);

		public final Entry[] entries;

		public TopList(int size) {
			this.entries = new Entry[size];
		}

		public static TopList of(Function<Process, Long> extractor, List<Process> processes, int length) {
			TopList topList = new TopList(Math.min(length, processes.size()));
			for (int i = 0; i < topList.entries.length; i++) {
				topList.entries[i] = new Entry(extractor.apply(processes.get(i)), processes.get(i));
			}
			return topList;
		}

		@Override
		public boolean equals(Object other) {
			if (other instanceof TopList) {
				return Arrays.equals(entries, ((TopList) other).entries);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return Objects.hash((Object[]) entries);
		}

		@Override
		public int compareTo(TopList topList) {
			throw new UnsupportedOperationException("Can't compare two toplists!");
		}


		public static class Entry {
			public final long value;
			public final Process process;

			public Entry(long value, Process process) {
				this.value = value;
				this.process = process;
			}

			@Override
			public boolean equals(Object obj) {
				if (obj instanceof Entry) {
					Entry otherEntry = (Entry) obj;
					return value == otherEntry.value && process.uniqueId == otherEntry.process.uniqueId;
				}
				return false;
			}

			@Override
			public int hashCode() {
				return Objects.hash(value, process.uniqueId);
			}
		}
	}


	public static class Network {
		public Measurements<Long> inRate;
		public Measurements<Long> outRate;

		public String macAddress;
		public String[] ipv4Addresses;
		public String[] ipv6Addresses;

		public String name;

		public boolean isEnabled;

		public Network() {
			inRate = new MeasurementContainer<>(0L);
			outRate = new MeasurementContainer<>(0L);

			ipv4Addresses = new String[0];
			ipv6Addresses = new String[0];
		}

		void copyFrom(Network other, boolean doFullCopy) {
			if (ipv4Addresses.length != other.ipv4Addresses.length) {
				ipv4Addresses = new String[other.ipv4Addresses.length];
			}
			if (ipv6Addresses.length != other.ipv6Addresses.length) {
				ipv6Addresses = new String[other.ipv6Addresses.length];
			}

			if (doFullCopy) {
				inRate.copyFrom(other.inRate);
				outRate.copyFrom(other.outRate);
			} else {
				inRate.copyDelta(other.inRate);
				outRate.copyDelta(other.outRate);
			}

			macAddress = other.macAddress;
			System.arraycopy(other.ipv4Addresses, 0, ipv4Addresses, 0, other.ipv4Addresses.length);
			System.arraycopy(other.ipv6Addresses, 0, ipv6Addresses, 0, other.ipv6Addresses.length);

			name = other.name;
			isEnabled = other.isEnabled;
		}

		public void compactIpv6() {
			for (int i = 0; i < ipv6Addresses.length; i++) {
				ipv6Addresses[i] = ipv6Addresses[i].replaceAll("((?::0\\b){2,}):?(?!\\S*\\b\\1:0\\b)(\\S*)", "::$2");
			}
		}
	}


	public static class Disk {
		public Measurements<Long> writeRate;
		public Measurements<Long> readRate;
		public Measurements<Double> activeFraction; // TODO Replace with long!
		public Measurements<Long> ioQueueLength;

		public int index;
		public String name;
		public String model;
		public long size;

		public Disk() {
			writeRate = new MeasurementContainer<>(0L);
			readRate = new MeasurementContainer<>(0L);
			activeFraction = new MeasurementContainer<>(0d);
			ioQueueLength = new MeasurementContainer<>(0L);
		}

		void copyFrom(Disk other, boolean doFullCopy) {
			if (doFullCopy) {
				writeRate.copyFrom(other.writeRate);
				readRate.copyFrom(other.readRate);
				activeFraction.copyFrom(other.activeFraction);
				ioQueueLength.copyFrom(other.ioQueueLength);
			} else {
				writeRate.copyDelta(other.writeRate);
				readRate.copyDelta(other.readRate);
				activeFraction.copyDelta(other.activeFraction);
				ioQueueLength.copyDelta(other.ioQueueLength);
			}

			index = other.index;
			name = other.name;
			model = other.model;
			size = other.size;
		}
	}


	public static class Gpu {
		public enum Type {
			Nvidia,
			Amd,
			Intel,
			Unknown
		}

		public Measurements<Long> usedMemory;
		public Measurements<Long> utilization;
		public Measurements<Long> temperature;

		public Measurements<Long> encoderUtilization;
		public Measurements<Long> decoderUtilization;

		public int index;
		public Type type;
		public String name;
		public String vendor;
		public int deviceId;
		public String driverVersion;
		public long totalMemory;
		public boolean isSupported;

		public Gpu() {
			usedMemory = new MeasurementContainer<>(0L);
			utilization = new MeasurementContainer<>(0L);
			temperature = new MeasurementContainer<>(0L);
			encoderUtilization = new MeasurementContainer<>(0L);
			decoderUtilization = new MeasurementContainer<>(0L);
		}

		void copyFrom(Gpu other, boolean doFullCopy) {
			if (doFullCopy) {
				usedMemory.copyFrom(other.usedMemory);
				utilization.copyFrom(other.utilization);
				temperature.copyFrom(other.temperature);
				encoderUtilization.copyFrom(other.encoderUtilization);
				decoderUtilization.copyFrom(other.decoderUtilization);
			} else {
				usedMemory.copyDelta(other.usedMemory);
				utilization.copyDelta(other.utilization);
				temperature.copyDelta(other.temperature);
				encoderUtilization.copyDelta(other.encoderUtilization);
				decoderUtilization.copyDelta(other.decoderUtilization);
			}

			index = other.index;
			type = other.type;
			name = other.name;
			vendor = other.vendor;
			driverVersion = other.driverVersion;
			deviceId = other.deviceId;
			totalMemory = other.totalMemory;
			isSupported = other.isSupported;
		}
	}
}