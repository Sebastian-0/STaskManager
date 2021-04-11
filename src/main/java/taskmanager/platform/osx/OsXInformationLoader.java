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

import com.sun.jna.Memory;
import com.sun.jna.platform.mac.SystemB.Passwd;
import com.sun.jna.platform.mac.SystemB.ProcTaskAllInfo;
import com.sun.jna.platform.mac.SystemB.VMStatistics;
import com.sun.jna.ptr.IntByReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.jna.platform.mac.SystemB;
import oshi.util.Constants;
import taskmanager.InformationLoader;
import taskmanager.data.Process;
import taskmanager.data.Status;
import taskmanager.data.SystemInformation;
import taskmanager.platform.linux.LinuxExtraInformation;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

public class OsXInformationLoader extends InformationLoader {
	private static final Logger LOGGER = LoggerFactory.getLogger(OsXInformationLoader.class);

	// TODO: Replace max size with double fetch instead? First get amount of pids and then get the list
	private static final int MAXIMUM_NUMBER_OF_PROCESSES = 10_000;

	private static final int KERN_SUCCESS = 0;

	private final int[] pidFetchArray = new int[MAXIMUM_NUMBER_OF_PROCESSES];

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
			long pid = pidFetchArray[i];
			if (pid == 0) {
				continue;
			}

			newProcessIds.add(pid);
			Process process = systemInformation.getProcessById(pid);
			if (process == null) {
				process = new Process(nextProcessId++, pid);
				systemInformation.processes.add(process);
			}

			ProcTaskAllInfo allInfo = new ProcTaskAllInfo();
			int status = SystemB.INSTANCE.proc_pidinfo((int) pid, SystemB.PROC_PIDTASKALLINFO, 0, allInfo, allInfo.size());
			if (status != 0) {
				LOGGER.warn("Failed to read process information for {}: {}", pid, status);
				continue;
			}

			if (!process.hasReadOnce) {
				Memory pathBuffer = new Memory(SystemB.PROC_PIDPATHINFO_MAXSIZE);
				status = SystemB.INSTANCE.proc_pidpath((int) pid, pathBuffer, (int) pathBuffer.size());
				if (status == 0) {
					process.filePath = pathBuffer.getString(0).trim();

					String[] toks = process.filePath.split(File.pathSeparator);
					process.fileName = toks[toks.length - 1];

//					String partialName = Native.toString(allInfo.pbsd.pbi_comm, StandardCharsets.UTF_8);
				} else {
					LOGGER.warn("Failed to read process path for {}: {}", pid, status);
				}

//					process.commandLine = FileUtil.getStringFromFile(processPath + "/cmdline").replaceAll("" + (char) 0, " ").trim();

				Passwd passwd = SystemB.INSTANCE.getpwuid(allInfo.pbsd.pbi_uid);
				if (passwd != null) {
					process.userName = passwd.pw_name;
				} else {
					process.userName = Constants.UNKNOWN;
				}

				process.startTimestamp = allInfo.pbsd.pbi_start_tvsec * 1000L + allInfo.pbsd.pbi_start_tvusec / 1000L;

				long parentId = allInfo.pbsd.pbi_ppid;
				Process parent = systemInformation.getProcessById(parentId);
				if (parent != null) {
					process.parentUniqueId = parent.uniqueId;
					process.parentId = parentId;
				} else {
					process.parentUniqueId = -1;
					process.parentId = -1;
				}
				process.hasReadOnce = true;
			}

			switch (allInfo.pbsd.pbi_status) {
				case 1: // High prio sleep
					process.status = Status.Sleeping;
					break;
				case 2: // Low prio sleep
					process.status = Status.Waiting;
					break;
				case 3: // Running
					process.status = Status.Running;
					break;
				case 4: // Idle
					process.status = Status.Sleeping;
					break;
				case 5: // Zombie
					process.status = Status.Zombie;
					break;
				case 6: // Stopped? Is that suspended?
					process.status = Status.Suspended;
					break;
				default:
					LOGGER.info("Unknown status {} for process {}", allInfo.pbsd.pbi_status, pid);
			}

			process.privateWorkingSet.addValue(allInfo.ptinfo.pti_resident_size);

			long stime = allInfo.ptinfo.pti_total_system;
			long utime = allInfo.ptinfo.pti_total_user;

//			process.updateCpu(stime, utime, , 1);
		}

		updateDeadProcesses(systemInformation, newProcessIds);

		systemInformation.totalProcesses = newProcessIds.size();

		// TODO: See in MacOperatingSystem and MacOSProcess
	}
}
