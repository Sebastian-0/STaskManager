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
import com.sun.jna.Structure;
import com.sun.jna.platform.mac.SystemB.Passwd;
import com.sun.jna.platform.mac.SystemB.ProcTaskAllInfo;
import com.sun.jna.platform.mac.SystemB.VMStatistics;
import com.sun.jna.ptr.IntByReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import taskmanager.InformationLoader;
import taskmanager.data.Process;
import taskmanager.data.Status;
import taskmanager.data.SystemInformation;
import taskmanager.platform.linux.LinuxExtraInformation;
import taskmanager.platform.osx.SystemB.KInfoProc;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class OsXInformationLoader extends InformationLoader {
	private static final Logger LOGGER = LoggerFactory.getLogger(OsXInformationLoader.class);

	// TODO: Replace max size with double fetch instead? First get amount of pids and then get the list
	//  Can also get maximum amount from sysctl with KERN_MAXPROC
	private static final int MAXIMUM_NUMBER_OF_PROCESSES = 10_000;

	private int maximumProgramArguments;
	private final int[] pidFetchArray = new int[MAXIMUM_NUMBER_OF_PROCESSES];

	private long nextProcessId;

	@Override
	public void init(SystemInformation systemInformation) {
		super.init(systemInformation);

		systemInformation.extraInformation = new LinuxExtraInformation();
		systemInformation.physicalMemoryTotalInstalled = systemInformation.physicalMemoryTotal;

		readMaximumProgramArguments();
	}

	private void readMaximumProgramArguments() {
		int[] mib = { SystemB.CTL_KERN, SystemB.KERN_ARGMAX };
		IntByReference argmax = new IntByReference();
		int status = SystemB.INSTANCE.sysctl(mib, mib.length, argmax.getPointer(), new IntByReference(SystemB.INT_SIZE), null, 0);
		if (status != 0) {
			LOGGER.error("Failed to fetch maximum size of program argument list, error: {}", Native.getLastError());
		} else {
			maximumProgramArguments = argmax.getValue();
		}
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
				new IntByReference(statistics.size() / SystemB.INT_SIZE)) != SystemB.KERN_SUCCESS) {
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

			newProcessIds.add(pid);
			Process process = systemInformation.getProcessById(pid);
			if (process == null) {
				process = new Process(nextProcessId++, pid);
				systemInformation.processes.add(process);
			}

			boolean allInfoFailed = false;
			ProcTaskAllInfo allInfo = new ProcTaskAllInfo();
			int status = SystemB.INSTANCE.proc_pidinfo((int) pid, SystemB.PROC_PIDTASKALLINFO, 0, allInfo, allInfo.size());
			if (status < 0) {
				LOGGER.warn("Failed to read process information for {}: {}", pid, Native.getLastError());
				allInfoFailed = true;
			} else if (status != allInfo.size()) {
				// Failed to read, possibly because we don't have access
				allInfoFailed = true;
			}

			if (!process.hasReadOnce) {
				initialProcessSetup(systemInformation, process, allInfoFailed ? null : allInfo);
				process.hasReadOnce = true;
			}

			if (!allInfoFailed) {
				switch (allInfo.pbsd.pbi_status) {
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
					default:
						LOGGER.info("Unknown status {} for process {}", allInfo.pbsd.pbi_status, pid);
				}

				process.privateWorkingSet.addValue(allInfo.ptinfo.pti_resident_size);

				long stime = allInfo.ptinfo.pti_total_system / 1_000_000;
				long utime = allInfo.ptinfo.pti_total_user / 1_000_000;
				process.updateCpu(stime, utime, systemInformation.logicalProcessorCount);
			}
		}

		updateDeadProcesses(systemInformation, newProcessIds);

		systemInformation.totalProcesses = newProcessIds.size();

		// TODO: Read max file descriptors using sysctl and KERN_MAXFILES
	}

	private void initialProcessSetup(SystemInformation systemInformation, Process process, ProcTaskAllInfo allInfo) {
		process.commandLine = getCommandLine(process.id);

		Memory pathBuffer = new Memory(SystemB.PROC_PIDPATHINFO_MAXSIZE);
		int status = SystemB.INSTANCE.proc_pidpath((int) process.id, pathBuffer, (int) pathBuffer.size());
		if (status > 0) {
			process.filePath = pathBuffer.getString(0).trim();

			String[] toks = process.filePath.split(File.separator);
			process.fileName = toks[toks.length - 1];

//					String partialName = Native.toString(allInfo.pbsd.pbi_comm, StandardCharsets.UTF_8);
		} else {
			LOGGER.warn("Failed to read process path for {}: {}", process.id, Native.getLastError());
		}

		long parentId = -1;
		int userId = -1;
		if (allInfo != null) {
			userId = allInfo.pbsd.pbi_uid;

			process.startTimestamp = allInfo.pbsd.pbi_start_tvsec * 1000L + allInfo.pbsd.pbi_start_tvusec / 1000L;

			parentId = allInfo.pbsd.pbi_ppid;
		} else {
			// Try reading KInfoProc as a fallback, see struct docs:
			// - kinfo_proc: https://opensource.apple.com/source/xnu/xnu-344/bsd/sys/sysctl.h
			// - extern_proc: https://opensource.apple.com/source/xnu/xnu-201/bsd/sys/proc.h
			int[] mib = { SystemB.CTL_KERN, SystemB.KERN_PROC, SystemB.KERN_PROC_PID, (int) process.id };

			KInfoProc infoProc = new KInfoProc();
			Pointer mem = new Memory(infoProc.size());

			IntByReference ref = new IntByReference(infoProc.size());
			int st = SystemB.INSTANCE.sysctl(mib, mib.length, mem, ref, null, 0);
			if (st != 0 || ref.getValue() != infoProc.size()) {
				LOGGER.error("Failed to read KERN_PROC_PID: {}", Native.getLastError());
			} else {
				KInfoProc proc = Structure.newInstance(KInfoProc.class, mem);
				proc.read();
				parentId = proc.kp_eproc.e_ppid;
				userId = proc.kp_eproc.e_pcred.p_ruid;

				System.out.println(proc.kp_proc.p_rtime.tv_usec);
			}
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

	private String getCommandLine(long pid) {
		if (pid == 0 || maximumProgramArguments == 0) {
			return "";
		}

		Pointer procargs = new Memory(maximumProgramArguments);

		int[] mib = { SystemB.CTL_KERN, SystemB.KERN_PROCARGS2, (int) pid };
		IntByReference argmax = new IntByReference(maximumProgramArguments);
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
}
