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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SystemInformation {
	/* Time data */
	public long bootTime;
	public long uptime;

	/* Memory data */
	public long pageSize;

	public long physicalMemoryTotalInstalled; // Includes reserved memory
	public long physicalMemoryTotal;
	public Measurements<Long> memoryUsed;
	public Measurements<TopList> memoryUsedTopList;

	// Memory types shown in the memory composition (expanded in extraInformation)
	public long reservedMemory;
	public long freeMemory;

	/* Processor data */
	public int logicalProcessorCount;
	public int physicalProcessorCount;

	public Measurements<Short>[] cpuUsagePerCore;
	public Measurements<Short> cpuUsageTotal;
	public Measurements<TopList> cpuTopList;

	public int totalProcesses;
	public int totalThreads;

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

	public ExtraInformation extraInformation;

	@SuppressWarnings("unchecked")
	public SystemInformation() {
		memoryUsed = new MeasurementContainer<>(0L);
		memoryUsedTopList = new MeasurementContainer<>(TopList.EMPTY);
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
		memoryUsed.copyDelta(other.memoryUsed);
		memoryUsedTopList.copyDelta(other.memoryUsedTopList);

		reservedMemory = other.reservedMemory;
		freeMemory = other.freeMemory;

		pageSize = other.pageSize;

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

		copyProcesses(other);
		copyNetworks(other);
		copyDisks(other);
		copyGpus(other);

		userName = other.userName;

		if (extraInformation == null) {
			if (other.extraInformation != null) {
				extraInformation = other.extraInformation.copy();
			}
		} else {
			extraInformation.copyFrom(other.extraInformation);
		}
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
}