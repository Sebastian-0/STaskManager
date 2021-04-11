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
import taskmanager.InformationLoader;
import taskmanager.data.SystemInformation;
import taskmanager.platform.linux.LinuxExtraInformation;

import java.util.LinkedHashSet;
import java.util.Set;

public class OsXInformationLoader extends InformationLoader {
	private static final Logger LOGGER = LoggerFactory.getLogger(OsXInformationLoader.class);

	// TODO: Replace max size with double fetch instead? First get amount of pids and then get the list
	private static final int MAXIMUM_NUMBER_OF_PROCESSES = 10_000;

	private static final int KERN_SUCCESS = 0;

	private int[] pidFetchArray = new int[MAXIMUM_NUMBER_OF_PROCESSES];

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
		updateProcesses(systemInformation);
	}

	private void updateMemory(SystemInformation systemInformation) {
		VMStatistics statistics = new VMStatistics();
		if (SystemB.INSTANCE.host_statistics(SystemB.INSTANCE.mach_host_self(), SystemB.HOST_VM_INFO, statistics,
				new IntByReference(statistics.size() / SystemB.INT_SIZE)) != KERN_SUCCESS) {
			LOGGER.warn("Failed to read memory information!");
		} else {
			systemInformation.freeMemory = (statistics.free_count + statistics.inactive_count) * systemInformation.pageSize;
		}

		// For more OSX memory info, see: http://web.mit.edu/darwin/src/modules/xnu/osfmk/man/vm_statistics.html
//			LinuxExtraInformation extraInformation = (LinuxExtraInformation) systemInformation.extraInformation;
//			extraInformation.bufferMemory = Long.parseLong(removeUnit(memInfo.get("Buffers"))) * 1024;
//			extraInformation.cacheMemory = Long.parseLong(removeUnit(memInfo.get("Cached"))) * 1024 + Long.parseLong(removeUnit(memInfo.get("SReclaimable"))) * 1024;
//			extraInformation.sharedMemory = Long.parseLong(removeUnit(memInfo.get("Shmem"))) * 1024;
//
//			extraInformation.swapSize = Long.parseLong(removeUnit(memInfo.get("SwapTotal"))) * 1024;
//			extraInformation.swapUsed = extraInformation.swapSize - Long.parseLong(removeUnit(memInfo.get("SwapFree"))) * 1024;
	}

	private void updateProcesses(SystemInformation systemInformation) {
		int count = SystemB.INSTANCE.proc_listpids(SystemB.PROC_ALL_PIDS, 0, pidFetchArray,
				pidFetchArray.length * SystemB.INT_SIZE) / SystemB.INT_SIZE;

		Set<Long> newProcessIds = new LinkedHashSet<>();
		for (int i = 0; i < count; i++) {
			if (pidFetchArray[i] == 0) {
				continue;
			}
			newProcessIds.add((long) pidFetchArray[i]);
		}

		updateDeadProcesses(systemInformation, newProcessIds);

		systemInformation.totalProcesses = newProcessIds.size();

		// TODO: See in MacOperatingSystem and MacOSProcess
	}
}
