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
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.mac.SystemB.Passwd;
import com.sun.jna.platform.mac.SystemB.ProcTaskAllInfo;
import com.sun.jna.platform.mac.SystemB.VMStatistics64;
import com.sun.jna.platform.mac.SystemB.XswUsage;
import com.sun.jna.ptr.IntByReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.jna.platform.mac.SystemB.ProcFdInfo;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import taskmanager.InformationLoader;
import taskmanager.data.Process;
import taskmanager.data.Status;
import taskmanager.data.SystemInformation;
import taskmanager.platform.osx.SystemB.KInfoProc;
import taskmanager.platform.osx.SystemB.ProcFDInfoList;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OsXInformationLoader extends InformationLoader {
	private static final Logger LOGGER = LoggerFactory.getLogger(OsXInformationLoader.class);

	private final Cache cache = new Cache();

	private final Set<Long> processesWithFailedKInfoProc = new LinkedHashSet<>();

	private int maximumProgramArgumentsSize;

	private long nextProcessId;

	@Override
	public void init(SystemInformation systemInformation) {
		super.init(systemInformation);

		systemInformation.extraInformation = new OsXExtraInformation();
		systemInformation.physicalMemoryTotalInstalled = systemInformation.physicalMemoryTotal;

		readMaximumProgramArguments();
	}

	private void readMaximumProgramArguments() {
		int[] mib = { SystemB.CTL_KERN, SystemB.KERN_ARGMAX };
		IntByReference argmax = cache.intByReference(0);
		int status = SystemB.INSTANCE.sysctl(mib, mib.length, argmax.getPointer(), cache.intByReference(SystemB.INT_SIZE), null, 0);
		if (status != 0) {
			LOGGER.error("Failed to fetch maximum size of program argument list, error: {}", Native.getLastError());
		} else {
			maximumProgramArgumentsSize = argmax.getValue();
		}
	}

	@Override
	public void update(SystemInformation systemInformation) {
		super.update(systemInformation);

		updateMemory(systemInformation);
		updateProcesses(systemInformation);

		// TODO: Read max file descriptors using sysctl and KERN_MAXFILES
		OsXExtraInformation extraInformation = (OsXExtraInformation) systemInformation.extraInformation;

		int[] mib = { SystemB.CTL_KERN, SystemB.KERN_MAXFILES };

		IntByReference maxFds = cache.intByReference(0);

		IntByReference ref = cache.intByReference(SystemB.INT_SIZE);
		int st = SystemB.INSTANCE.sysctl(mib, mib.length, maxFds.getPointer(), ref, null, 0);
		if (st != 0) {
			LOGGER.error("Failed to read KERN_MAXFILES: {}", Native.getLastError());
		} else {
			extraInformation.openFileDescriptorsLimit = maxFds.getValue();
		}
	}

	private void updateMemory(SystemInformation systemInformation) {
		OsXExtraInformation extraInformation = (OsXExtraInformation) systemInformation.extraInformation;

		VMStatistics64 statistics = cache.vmStatistics64;
		if (SystemB.INSTANCE.host_statistics64(SystemB.INSTANCE.mach_host_self(), SystemB.HOST_VM_INFO64, statistics,
				cache.intByReference(statistics.size() / SystemB.INT_SIZE)) != SystemB.KERN_SUCCESS) {
			LOGGER.warn("Failed to read VMStatistics!");
		} else {
			// Note:
			// - wired + active + inactive + free does not sum up to total memory, not completely sure why but
			//     it seems that "stolen pages" might have something to do with it: https://stackoverflow.com/a/43300124
			// - Compressed memory is a part of inactive
			// - File cache is not the same as the swap file, it's other file bound caches of memory
			extraInformation.wiredMemory = statistics.wire_count * systemInformation.pageSize;
			extraInformation.activeMemory = statistics.active_count * systemInformation.pageSize;
			extraInformation.inactiveMemory = statistics.inactive_count * systemInformation.pageSize;
			systemInformation.freeMemory = statistics.free_count * systemInformation.pageSize;
			extraInformation.compressedMemory = statistics.compressor_page_count * systemInformation.pageSize;
			extraInformation.fileCache = statistics.external_page_count * systemInformation.pageSize;
		}
		
		updateSwap(extraInformation);
	}

	private void updateSwap(OsXExtraInformation extraInformation) {
		int[] mib = { SystemB.CTL_VM, SystemB.VM_SWAPUSAGE };

		XswUsage xswUsage = cache.xswUsage;

		IntByReference ref = cache.intByReference(xswUsage.size());
		int st = SystemB.INSTANCE.sysctl(mib, mib.length, xswUsage.getPointer(), ref, null, 0);
		if (st != 0 || ref.getValue() != xswUsage.size()) {
			LOGGER.error("Failed to read XswUsage: {}", Native.getLastError());
		} else {
			xswUsage.read();

			extraInformation.swapSize = xswUsage.xsu_total;
			extraInformation.swapUsed = xswUsage.xsu_used;
		}
	}

	private void updateProcesses(SystemInformation systemInformation) {
		int totalProcessesCount = SystemB.INSTANCE.proc_listpids(SystemB.PROC_ALL_PIDS, 0, cache.pidFetchArray,
				cache.pidFetchArray.length * SystemB.INT_SIZE) / SystemB.INT_SIZE;

		createMissingProcessObjects(systemInformation, totalProcessesCount);

		int totalThreadCount = 0;
		int totalFileDescriptorsCount = 0;
		Set<Long> newProcessIds = new LinkedHashSet<>();
		for (int i = 0; i < totalProcessesCount; i++) {
			long pid = cache.pidFetchArray[i];
			newProcessIds.add(pid);
			Process process = systemInformation.getProcessById(pid);

			ProcTaskAllInfo allInfo = cache.procTaskAllInfo;
			int status = SystemB.INSTANCE.proc_pidinfo((int) pid, SystemB.PROC_PIDTASKALLINFO, 0, allInfo, allInfo.size());
			if (status < 0) {
				LOGGER.warn("Failed to read process information for {}: {}", pid, Native.getLastError());
				allInfo = null;
			} else if (status != allInfo.size()) {
				// Failed to read, possibly because we don't have access
				allInfo = null;
			}

			ProcFdInfo fdInfo = cache.procFdInfo;
			status = SystemB.INSTANCE.proc_pidinfo((int) pid, SystemB.PROC_PIDLISTFDS, 0, null, 0);
			if (status < 0 ) {
				LOGGER.warn("Failed to read process fd list for {}: {}", pid, Native.getLastError());
			} else if (status != 0) {
				int fds = status / fdInfo.size();

				ProcFDInfoList fdList = cache.procFDInfoList(fds);
				status = SystemB.INSTANCE.proc_pidinfo((int) pid, SystemB.PROC_PIDLISTFDS, 0, fdList, fdList.size());
				if (status < 0 ) {
					LOGGER.warn("Failed to read process fd list (2) for {}: {}", pid, Native.getLastError());
				} else if (status != 0) {
					fds = status / fdInfo.size();
					totalFileDescriptorsCount += fds;
				}
			}

			if (!process.hasReadOnce) {
				initialProcessSetup(systemInformation, process, allInfo);
				process.hasReadOnce = true;
			}

			int processStatus = -1;
			if (allInfo != null) {
				totalThreadCount += allInfo.ptinfo.pti_threadnum;
				processStatus = allInfo.pbsd.pbi_status;

				process.privateWorkingSet.addValue(allInfo.ptinfo.pti_resident_size);

				long stime = allInfo.ptinfo.pti_total_system / 1_000_000;
				long utime = allInfo.ptinfo.pti_total_user / 1_000_000;
				process.updateCpu(stime, utime, systemInformation.logicalProcessorCount);
			} else {
				KInfoProc kInfoProc = readKInfoProc(process);
				if (kInfoProc != null) {
					processStatus = kInfoProc.kp_proc.p_stat;
				}
			}

			switch (processStatus) {
				case 1: // Idle == newly created
					process.status = Status.Running;
					break;
				case 2: // Running
					process.status = Status.Running;
					break;
				case 3: // Sleeping on an address
					process.status = Status.Sleeping;
					break;
				case 4: // Stopped/Suspended?
					process.status = Status.Suspended;
					break;
				case 5: // Zombie == Waiting for collection by parent
					process.status = Status.Zombie;
					break;
				case -1: // Failed to read status
					process.status = Status.Unknown;
					break;
				default:
					LOGGER.info("Unknown status {} for process {}", processStatus, pid);
			}
		}

		updateDeadProcesses(systemInformation, newProcessIds);

		systemInformation.totalProcesses = newProcessIds.size();
		systemInformation.totalThreads = totalThreadCount; // TODO Currently excludes the threads of all root processes...

		OsXExtraInformation extraInformation = (OsXExtraInformation) systemInformation.extraInformation;
		extraInformation.openFileDescriptors = totalFileDescriptorsCount; // TODO Currently excludes the fds of all root processes...
	}

	private void createMissingProcessObjects(SystemInformation systemInformation, int count) {
		for (int i = 0; i < count; i++) {
			long pid = cache.pidFetchArray[i];
			Process process = systemInformation.getProcessById(pid);
			if (process == null) {
				systemInformation.processes.add(new Process(nextProcessId++, pid));
			}
		}
	}

	private void initialProcessSetup(SystemInformation systemInformation, Process process, ProcTaskAllInfo allInfo) {
		if (process.id == 0) {
			process.fileName = "kernel_task";
			process.commandLine = "";
		} else {
			process.commandLine = getCommandLine(process.id);

			Memory pathBuffer = cache.memory(SystemB.PROC_PIDPATHINFO_MAXSIZE);
			int status = SystemB.INSTANCE.proc_pidpath((int) process.id, pathBuffer, (int) pathBuffer.size());
			if (status > 0) {
				process.filePath = pathBuffer.getString(0).trim();

				String[] toks = process.filePath.split(File.separator);
				process.fileName = toks[toks.length - 1];

//					String partialName = Native.toString(allInfo.pbsd.pbi_comm, StandardCharsets.UTF_8);
				// TODO Better path fetching?
			} else {
				LOGGER.warn("Failed to read process path for {}: {}", process.id, Native.getLastError());
			}
		}

		long parentId = -1;
		int userId = -1;
		if (allInfo != null) {
			parentId = allInfo.pbsd.pbi_ppid;
			userId = allInfo.pbsd.pbi_uid;
			process.startTimestamp = allInfo.pbsd.pbi_start_tvsec * 1000L + allInfo.pbsd.pbi_start_tvusec / 1000L;
		} else {
			// Try reading KInfoProc as a fallback
			KInfoProc kInfoProc = readKInfoProc(process);
			if (kInfoProc != null) {
				parentId = kInfoProc.kp_eproc.e_ppid;
				userId = kInfoProc.kp_eproc.e_pcred.p_ruid;
				process.startTimestamp = kInfoProc.kp_proc.p_starttime.tv_sec.longValue() * 1000L + kInfoProc.kp_proc.p_starttime.tv_usec / 1000L;
			}
			// TODO Further improve by using libtop source? https://opensource.apple.com/source/top/top-73/libtop.c
			//  Otherwise specify somehow that the process wont get memory/cpu set (replace with --- in the UI?)
		}

		if (userId != -1) {
			Passwd passwd = SystemB.INSTANCE.getpwuid(userId);
			if (passwd != null) {
				process.userName = passwd.pw_name;
			} else {
				process.userName = Constants.UNKNOWN;
			}
		}

		Process parent = systemInformation.getProcessById(parentId);
		if (parent != null) {
			process.parentUniqueId = parent.uniqueId;
			process.parentId = parentId;
		} else {
			process.parentUniqueId = -1;
			process.parentId = -1;
		}
	}

	private KInfoProc readKInfoProc(Process process) {
		if (processesWithFailedKInfoProc.contains(process.uniqueId)) {
			return null;
		}

		int[] mib = { SystemB.CTL_KERN, SystemB.KERN_PROC, SystemB.KERN_PROC_PID, (int) process.id };

		KInfoProc kInfoProc = cache.kInfoProc;

		IntByReference size = cache.intByReference(kInfoProc.size());
		int status = SystemB.INSTANCE.sysctl(mib, mib.length, kInfoProc.getPointer(), size, null, 0);
		if (status != 0 || size.getValue() != kInfoProc.size()) {
			LOGGER.error("Failed to read KInfoProc: {}", Native.getLastError());
			processesWithFailedKInfoProc.add(process.uniqueId);
			return null;
		}
		kInfoProc.read();
		return kInfoProc;
	}

	private String getCommandLine(long pid) {
		if (pid == 0 || maximumProgramArgumentsSize == 0) {
			return "";
		}

		Pointer procargs = cache.memory(maximumProgramArgumentsSize);

		int[] mib = { SystemB.CTL_KERN, SystemB.KERN_PROCARGS2, (int) pid };
		IntByReference argmax = cache.intByReference(maximumProgramArgumentsSize);
		int status = SystemB.INSTANCE.sysctl(mib, mib.length, procargs, argmax, null, 0);
		if (status != 0) {
			// Fallback due to random failures for the previous system call, probably an OSX bug?
			String cmdLine = ExecutingCommand.getFirstAnswer("ps -o command= -p " + pid);
			if (!cmdLine.isEmpty()) {
				return cmdLine;
			}

			LOGGER.warn("Failed to read command line for {}, error code: {}", pid, Native.getLastError());
			return "";
		}

		List<String> result = new ArrayList<>();

		int nargs = procargs.getInt(0);
		int offset = SystemB.INT_SIZE;
		while (nargs-- > 0 && offset < argmax.getValue()) {
			String arg = procargs.getString(offset);
			result.add(arg);
			offset += arg.length() + 1;
		}

		return String.join(" ", result);
	}

	private static class Cache {
		// TODO: Replace max size with double fetch instead? First get amount of pids and then get the list
		//  Can also get maximum amount from sysctl with KERN_MAXPROC
		private static final int MAXIMUM_NUMBER_OF_PROCESSES = 10_000;

		// Processes
		int[] pidFetchArray = new int[MAXIMUM_NUMBER_OF_PROCESSES];
		ProcTaskAllInfo procTaskAllInfo = new ProcTaskAllInfo();
		ProcFdInfo procFdInfo = new ProcFdInfo();
		Map<Integer, ProcFDInfoList> procFDInfos = new HashMap<>();
		KInfoProc kInfoProc = new KInfoProc();

		// Memory
		VMStatistics64 vmStatistics64 = new VMStatistics64();
		XswUsage xswUsage = new XswUsage();

		// General stuff
		IntByReference[] intByReference = new IntByReference[10];
		int intByReferenceIdx;

		Map<Integer, Memory> memories = new HashMap<>();

		public Cache() {
			for (int i = 0; i < intByReference.length; i++) {
				intByReference[i] = new IntByReference();
			}
		}

		public IntByReference intByReference(int value) {
			IntByReference result = intByReference[intByReferenceIdx];
			result.setValue(value);
			intByReferenceIdx = (intByReferenceIdx + 1) % intByReference.length;
			return result;
		}

		public Memory memory(int size) {
			return memories.computeIfAbsent(size, Memory::new);
		}

		public ProcFDInfoList procFDInfoList(int size) {
			return procFDInfos.computeIfAbsent(size, ProcFDInfoList::new);
		}
	}
}
