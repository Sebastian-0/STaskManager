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

package taskmanager;

import config.Config;
import oshi.SystemInfo;
import oshi.hardware.GraphicsCard;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;
import oshi.hardware.NetworkIF;
import taskmanager.data.Process;
import taskmanager.data.Status;
import taskmanager.data.SystemInformation;
import taskmanager.data.Disk;
import taskmanager.data.Gpu;
import taskmanager.data.Gpu.Type;
import taskmanager.data.Network;
import taskmanager.data.TopList;
import taskmanager.platform.common.NvidiaGpuLoader;

import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

public abstract class InformationLoader {
	private SystemInfo systemInfoLoader;
	private NvidiaGpuLoader nvidiaGpuLoader;

	private NetworkIF[] networkInterfaces;
	private HWDiskStore[] disks;
	private GraphicsCard[] gpus;

	private long[][] lastCpuLoadTicksPerCore;
	private long[] lastCpuLoadTicks;

	private int numberOfUpdates;

	@SuppressWarnings("unchecked")
	public void init(SystemInformation systemInformation) {
		systemInfoLoader = new SystemInfo();
		nvidiaGpuLoader = new NvidiaGpuLoader();

		systemInformation.logicalProcessorCount = systemInfoLoader.getHardware().getProcessor().getLogicalProcessorCount();
		systemInformation.physicalProcessorCount = systemInfoLoader.getHardware().getProcessor().getPhysicalProcessorCount();
		systemInformation.physicalMemoryTotal = systemInfoLoader.getHardware().getMemory().getTotal();
		systemInformation.pageSize = systemInfoLoader.getHardware().getMemory().getPageSize();
		systemInformation.bootTime = System.currentTimeMillis() / 1000 - systemInfoLoader.getOperatingSystem().getSystemUptime(); // TODO this is incorrect when you take hibernation into account!

		systemInformation.cpuUsagePerCore = new MeasurementContainer[systemInformation.logicalProcessorCount];
		for (int i = 0; i < systemInformation.cpuUsagePerCore.length; i++) {
			systemInformation.cpuUsagePerCore[i] = new MeasurementContainer<>((short) 0);
		}

		initNetworkInterfaces(systemInformation);
		initDisks(systemInformation);
		initGpus(systemInformation);

		systemInformation.userName = System.getProperty("user.name", "");
	}

	private void initNetworkInterfaces(SystemInformation systemInformation) {
		networkInterfaces = systemInfoLoader.getHardware().getNetworkIFs();
		systemInformation.networks = new Network[networkInterfaces.length];
		for (int i = 0; i < networkInterfaces.length; i++) {
			systemInformation.networks[i] = new Network();
			systemInformation.networks[i].macAddress = networkInterfaces[i].getMacaddr(); // TODO Update this info periodically with networkInterfaces[i].setNetworkInterface(), should happen seldom though
			systemInformation.networks[i].ipv4Addresses = networkInterfaces[i].getIPv4addr(); // ^
			systemInformation.networks[i].ipv6Addresses = networkInterfaces[i].getIPv6addr(); //-|
			systemInformation.networks[i].name = networkInterfaces[i].getDisplayName();       //-|
			systemInformation.networks[i].compactIpv6();

			// TODO shj: Try using networkInterfaces[i].isConnectorPresent instead?
			try { // TODO Move this to update to continuously add/remove interfaces, how fast is isUp()?
				systemInformation.networks[i].isEnabled = networkInterfaces[i].queryNetworkInterface().isUp();
			} catch (SocketException e) {
				e.printStackTrace();
			}
		}
	}

	private void initDisks(SystemInformation systemInformation) {
		disks = systemInfoLoader.getHardware().getDiskStores();
		List<Disk> diskList = new ArrayList<>();
		int idx = 0;
		for (int i = 0; i < disks.length; i++) {
			Disk disk = new Disk();
			disk.index = idx;
			disk.size = disks[i].getSize();
			disk.model = disks[i].getModel();
			for (HWPartition partition : disks[i].getPartitions()) { // TODO Use UIID of the first partition to identify disks when new are added/removed
				String name = partition.getMountPoint();
				if (!name.isEmpty()) {
					if (!name.equals("/")) {
						while (name.endsWith("\\") || name.endsWith("/")) {
							name = name.substring(0, name.length() - 1);
						}
					}
					disk.name = name;

					break;
				}
			}
			if (disks[i].getPartitions().length > 0) {
				diskList.add(disk);
				idx += 1;
			}
		}

		systemInformation.disks = diskList.toArray(new Disk[0]);
	}

	private void initGpus(SystemInformation systemInformation) {
		gpus = systemInfoLoader.getHardware().getGraphicsCards();
		systemInformation.gpus = new Gpu[gpus.length];
		for (int i = 0; i < gpus.length; i++) {
			Gpu gpu = new Gpu();
			gpu.index = i;
			gpu.name = gpus[i].getName();
			gpu.deviceId = Integer.decode(gpus[i].getDeviceId());
			gpu.vendor = gpus[i].getVendor();
			gpu.totalMemory = gpus[i].getVRam();

			String vendor = gpu.vendor.toLowerCase();
			if (vendor.contains("nvidia")) { // TODO Does this work in windows?
				gpu.type = Type.Nvidia;
			} else if (vendor.contains("amd")) { // TODO Verify that AMD/Intel works?
				gpu.type = Type.Amd;
			} else if (vendor.contains("intel")) {
				gpu.type = Type.Intel;
			} else {
				gpu.type = Type.Unknown;
			}

			systemInformation.gpus[i] = gpu;
		}
	}

	public void update(SystemInformation systemInformation) {
		systemInformation.uptime = System.currentTimeMillis() / 1000 - systemInformation.bootTime;
		systemInformation.memoryUsed.addValue(systemInformation.physicalMemoryTotal - systemInfoLoader.getHardware().getMemory().getAvailable());

		// Update the CPU usage
		double[] loadPerCore;
		if (lastCpuLoadTicksPerCore == null) {
			loadPerCore = new double[systemInformation.cpuUsagePerCore.length];
		} else {
			loadPerCore = systemInfoLoader.getHardware().getProcessor().getProcessorCpuLoadBetweenTicks(lastCpuLoadTicksPerCore);
		}
		for (int i = 0; i < loadPerCore.length; i++) {
			systemInformation.cpuUsagePerCore[i].addValue((short) Math.round(loadPerCore[i] * Config.DOUBLE_TO_LONG));
		}

		if (lastCpuLoadTicks == null) {
			systemInformation.cpuUsageTotal.addValue((short) 0);
		} else {
			systemInformation.cpuUsageTotal.addValue(
					(short) Math.round(systemInfoLoader.getHardware().getProcessor().getSystemCpuLoadBetweenTicks(lastCpuLoadTicks) * Config.DOUBLE_TO_LONG));
		}

		lastCpuLoadTicksPerCore = systemInfoLoader.getHardware().getProcessor().getProcessorCpuLoadTicks();
		lastCpuLoadTicks = systemInfoLoader.getHardware().getProcessor().getSystemCpuLoadTicks();

		final int deadKeepTime = Config.getInt(Config.KEY_DEAD_PROCESS_KEEP_TIME) * 1000;
		systemInformation.deadProcesses.removeIf(process -> System.currentTimeMillis() - process.deathTimestamp > deadKeepTime);

		updateNetworkInterfaces(systemInformation);
		updateDisks(systemInformation);

		nvidiaGpuLoader.update(systemInformation);

		if (numberOfUpdates > 0) {
			updateTopLists(systemInformation);
		}

		numberOfUpdates++;
	}

	private void updateNetworkInterfaces(SystemInformation systemInformation) {
		for (int i = 0; i < networkInterfaces.length; i++) {
			long received = networkInterfaces[i].getBytesRecv();
			long sent = networkInterfaces[i].getBytesSent();
			networkInterfaces[i].updateAttributes();
			systemInformation.networks[i].inRate.addValue(networkInterfaces[i].getBytesRecv() - received);
			systemInformation.networks[i].outRate.addValue(networkInterfaces[i].getBytesSent() - sent);
		}
	}

	private void updateDisks(SystemInformation systemInformation) {
		int i = 0;
		for (HWDiskStore disk : disks) {
			if (disk.getPartitions().length > 0) {
				long w1 = disk.getWriteBytes();
				long r1 = disk.getReadBytes();
				long t1 = disk.getTimeStamp();
				long a1 = disk.getTransferTime();

				boolean diskExists = disk.updateAtrributes();

				long w2 = disk.getWriteBytes();
				long r2 = disk.getReadBytes();
				long t2 = disk.getTimeStamp();
				long a2 = disk.getTransferTime();

				if (diskExists) {
					systemInformation.disks[i].activeFraction.addValue(Math.max(0, (a2 - a1) / (double) (t2 - t1)));
					systemInformation.disks[i].writeRate.addValue(w2 - w1);
					systemInformation.disks[i].readRate.addValue(r2 - r1);
					systemInformation.disks[i].ioQueueLength.addValue(disk.getCurrentQueueLength());
				}

				i++;
			}
		}
	}

	private void updateTopLists(SystemInformation systemInformation) {
		final int topListSize = Config.getInt(Config.KEY_METRIC_TOP_LIST_SIZE);

		// Cpu
		systemInformation.processes.sort((p1, p2) -> (int) (p2.cpuUsage.newest() - p1.cpuUsage.newest()));
		TopList cpuTopList = TopList.of(p -> p.cpuUsage.newest(), systemInformation.processes, topListSize);
		systemInformation.cpuTopList.addValue(cpuTopList);

		// Memory
		systemInformation.processes.sort((p1, p2) -> signum(p2.privateWorkingSet.newest() - p1.privateWorkingSet.newest()));
		TopList memoryTopList = TopList.of(p -> p.privateWorkingSet.newest(), systemInformation.processes, topListSize);
		systemInformation.memoryUsedTopList.addValue(memoryTopList);
	}

	private static int signum(long value) {
		if (value > 0) {
			return 1;
		} else if (value < 0) {
			return -1;
		}
		return 0;
	}

	protected void updateDeadProcesses(SystemInformation systemInformation, Set<Long> processIds) {
		ListIterator<Process> itr = systemInformation.processes.listIterator();
		while (itr.hasNext()) {
			Process process = itr.next();
			if (!processIds.contains(process.id)) {
				if (!systemInformation.deadProcesses.contains(process)) {
					process.status = Status.Dead;
					process.deathTimestamp = System.currentTimeMillis();
					systemInformation.deadProcesses.add(process);
				} else {
					itr.remove();
				}
			}
		}
	}
}