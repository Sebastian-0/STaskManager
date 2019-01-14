package taskmanager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class SystemInformation {
	/* Time data */
	public long bootTime;
	public long uptime;

	/* Memory data */
	public long physicalMemoryTotalInstalled; // Includes reserved memory
	public long physicalMemoryTotal;
	public Measurements<Long> physicalMemoryUsed;
	public long reservedMemory;

	public long pageSize;

	// Windows memory types
	public long standbyMemory;
	public long modifiedMemory;
	public long freeMemory;

	public long commitLimit;
	public long commitUsed;

	public long kernelPaged;
	public long kernelNonpaged;

	/* Processor data */
	public int logicalProcessorCount;
	public int physicalProcessorCount;

	public Measurements<Double>[] cpuUsagePerCore;
	public Measurements<Double> cpuUsageTotal;

	public int totalProcesses;
	public int totalThreads;
	public int totalHandles;

	public List<Process> processes;
	public List<Process> deadProcesses;

	/* Network data */
	public Network[] networks;

	/* Disk data */
	public Disk[] disks;

	@SuppressWarnings("unchecked")
	public SystemInformation() {
		physicalMemoryUsed = new MeasurementContainer<Long>(0L);
		cpuUsagePerCore = new MeasurementContainer[0];
		cpuUsageTotal = new MeasurementContainer<Double>(0d);
		processes = new ArrayList<>();
		deadProcesses = new ArrayList<>();
		networks = new Network[0];
		disks = new Disk[0];
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

		bootTime = other.bootTime;
		uptime = other.uptime;

		physicalMemoryTotalInstalled = other.physicalMemoryTotalInstalled; // Includes reserved memory
		physicalMemoryTotal = other.physicalMemoryTotal;
		physicalMemoryUsed.copyFrom(other.physicalMemoryUsed);
		reservedMemory = other.reservedMemory;

		pageSize = other.pageSize;

		// Windows memory types
		standbyMemory = other.standbyMemory;
		modifiedMemory = other.modifiedMemory;
		freeMemory = other.freeMemory;

		commitLimit = other.commitLimit;
		commitUsed = other.commitUsed;

		kernelPaged = other.kernelPaged;
		kernelNonpaged = other.kernelNonpaged;

		/* Processor data */
		logicalProcessorCount = other.logicalProcessorCount;
		physicalProcessorCount = other.physicalProcessorCount;

		for (int i = 0; i < cpuUsagePerCore.length; i++) {
			if (cpuUsagePerCore[i] == null) {
				cpuUsagePerCore[i] = new MeasurementContainer<Double>(0d);
			}
			cpuUsagePerCore[i].copyFrom(other.cpuUsagePerCore[i]);
		}
		cpuUsageTotal.copyFrom(other.cpuUsageTotal);

		totalProcesses = other.totalProcesses;
		totalThreads = other.totalThreads;
		totalHandles = other.totalHandles;

		copyProcesses(other);
		copyNetworks(other);
		copyDisks(other);
	}

	private void copyDisks(SystemInformation other) {
		for (int i = 0; i < disks.length; i++) {
			if (disks[i] == null) {
				disks[i] = new Disk();
			}
			disks[i].copyFrom(other.disks[i]);
		}
	}

	private void copyNetworks(SystemInformation other) {
		for (int i = 0; i < networks.length; i++) {
			if (networks[i] == null) {
				networks[i] = new Network();
			}
			networks[i].copyFrom(other.networks[i]);
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
			if (process == null) {
				process = new Process(processNew.uniqueId, processNew.id);
				target.add(process);
			}

			processIds.add(process.uniqueId);
			process.copyFrom(processNew);
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


	public static class Network {
		public Measurements<Long> inRate;
		public Measurements<Long> outRate;

		public String macAddress;
		public String[] ipv4Addresses;
		public String[] ipv6Addresses;

		public String name;

		public boolean isEnabled;

		public Network() {
			inRate = new MeasurementContainer<Long>(0L);
			outRate = new MeasurementContainer<Long>(0L);

			ipv4Addresses = new String[0];
			ipv6Addresses = new String[0];
		}

		void copyFrom(Network other) {
			if (ipv4Addresses.length != other.ipv4Addresses.length) {
				ipv4Addresses = new String[other.ipv4Addresses.length];
			}
			if (ipv6Addresses.length != other.ipv6Addresses.length) {
				ipv6Addresses = new String[other.ipv6Addresses.length];
			}

			inRate.copyFrom(other.inRate);
			outRate.copyFrom(other.outRate);

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
		public Measurements<Double> activeFraction;
		public Measurements<Long> ioQueueLength;

		public int index;
		public String name;
		public String model;
		public long size;

		public Disk() {
			writeRate = new MeasurementContainer<Long>(0L);
			readRate = new MeasurementContainer<Long>(0L);
			activeFraction = new MeasurementContainer<Double>(0d);
			ioQueueLength = new MeasurementContainer<Long>(0L);
		}

		void copyFrom(Disk other) {
			writeRate.copyFrom(other.writeRate);
			readRate.copyFrom(other.readRate);
			activeFraction.copyFrom(other.activeFraction);

			index = other.index;
			name = other.name;
			model = other.model;
			size = other.size;
		}
	}
}
