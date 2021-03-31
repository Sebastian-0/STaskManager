/*
 * Copyright (c) 2021. Sebastian Hjelm
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * See LICENSE for further details.
 */

package taskmanager.platform.osx;

import com.sun.jna.platform.mac.SystemB.VMStatistics;
import com.sun.jna.ptr.IntByReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.jna.platform.mac.SystemB;
import taskmanager.data.SystemInformation;
import taskmanager.platform.linux.LinuxExtraInformation;
import taskmanager.platform.linux.LinuxInformationLoader;

public class OsXInformationLoader extends LinuxInformationLoader {
	private static final Logger LOGGER = LoggerFactory.getLogger(OsXInformationLoader.class);

	private static final int KERN_SUCCESS = 0;

	private long lastCpuTime;
	private long currentCpuTime;

	private long nextProcessId;

	@Override
	public void init(SystemInformation systemInformation) {
		super.init(systemInformation);

		systemInformation.extraInformation = new LinuxExtraInformation();
		systemInformation.physicalMemoryTotalInstalled = systemInformation.physicalMemoryTotal;
	}

	@Override
	public void update(SystemInformation systemInformation) {
		super.update(systemInformation);

		updateMemory(systemInformation);
//		updateTotalCpuTime();
//		updateProcesses(systemInformation);
	}

	private void updateMemory(SystemInformation systemInformation) {
		VMStatistics statistics = new VMStatistics();
		if (SystemB.INSTANCE.host_statistics(SystemB.INSTANCE.mach_host_self(), SystemB.HOST_VM_INFO, statistics,
				new IntByReference(statistics.size() / SystemB.INT_SIZE)) != KERN_SUCCESS) {
			LOGGER.warn("Failed to read memory information!");
		} else {
			systemInformation.freeMemory = (statistics.free_count + statistics.inactive_count) * systemInformation.pageSize;
		}

//		Map<String, String> memInfo = FileUtil.getKeyValueMapFromFile(PROC_PATH + "/meminfo", ":");
//		if (memInfo.isEmpty()) {
//			LOGGER.warn("Failed to read /proc/meminfo!");
//		} else {
//			// TODO Memory graph uses memory available while the composition uses memory available as defined by free
//			//  (not the same). Find out how linux defines memory available? Is it even possible?
//			LinuxExtraInformation extraInformation = (LinuxExtraInformation) systemInformation.extraInformation;
//			systemInformation.freeMemory = Long.parseLong(removeUnit(memInfo.get("MemFree"))) * 1024;
//			extraInformation.bufferMemory = Long.parseLong(removeUnit(memInfo.get("Buffers"))) * 1024;
//			extraInformation.cacheMemory = Long.parseLong(removeUnit(memInfo.get("Cached"))) * 1024 + Long.parseLong(removeUnit(memInfo.get("SReclaimable"))) * 1024;
//			extraInformation.sharedMemory = Long.parseLong(removeUnit(memInfo.get("Shmem"))) * 1024;
//
//			extraInformation.swapSize = Long.parseLong(removeUnit(memInfo.get("SwapTotal"))) * 1024;
//			extraInformation.swapUsed = extraInformation.swapSize - Long.parseLong(removeUnit(memInfo.get("SwapFree"))) * 1024;
//		}
	}
}
