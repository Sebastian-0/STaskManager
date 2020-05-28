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

package taskmanager.platform.win32;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.Advapi32;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Advapi32Util.Account;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Psapi.PERFORMANCE_INFORMATION;
import com.sun.jna.platform.win32.Version;
import com.sun.jna.platform.win32.WinBase.FILETIME;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.ULONGLONGByReference;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.HANDLEByReference;
import com.sun.jna.platform.win32.WinNT.LARGE_INTEGER;
import com.sun.jna.platform.win32.WinNT.LUID;
import com.sun.jna.platform.win32.WinNT.LUID_AND_ATTRIBUTES;
import com.sun.jna.platform.win32.WinNT.TOKEN_PRIVILEGES;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import taskmanager.InformationLoader;
import taskmanager.data.Process;
import taskmanager.data.Status;
import taskmanager.data.SystemInformation;
import taskmanager.platform.win32.NtDllExt.PEB;
import taskmanager.platform.win32.NtDllExt.PROCESS_BASIC_INFORMATION;
import taskmanager.platform.win32.NtDllExt.PROCESS_INFORMATION_CLASS;
import taskmanager.platform.win32.NtDllExt.RTL_USER_PROCESS_PARAMETERS;
import taskmanager.platform.win32.NtDllExt.SYSTEM_INFORMATION_CLASS;
import taskmanager.platform.win32.NtDllExt.SYSTEM_MEMORY_LIST_INFORMATION;
import taskmanager.platform.win32.NtDllExt.SYSTEM_PROCESS_INFORMATION;
import taskmanager.platform.win32.NtDllExt.SYSTEM_THREAD_INFORMATION;
import taskmanager.platform.win32.VersionExt.LANGANDCODEPAGE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static taskmanager.platform.win32.NtDllExt.THREAD_STATE_WAITING;
import static taskmanager.platform.win32.NtDllExt.WAIT_REASON_SUSPENDED;

public class WindowsInformationLoader extends InformationLoader {
	private static final Logger LOGGER = LoggerFactory.getLogger(WindowsInformationLoader.class);

	private long lastCpuTime;
	private long currentCpuTime;

	private long nextProcessId;

	@Override
	public void init(SystemInformation systemInformation) {
		super.init(systemInformation);

		systemInformation.extraInformation = new WindowsExtraInformation();
		systemInformation.physicalMemoryTotalInstalled = readPhysicalMemory();
		systemInformation.reservedMemory = systemInformation.physicalMemoryTotalInstalled - systemInformation.physicalMemoryTotal;

		setUsername(systemInformation);

		enableSeDebugNamePrivilege();
	}

	private long readPhysicalMemory() {
		ULONGLONGByReference totalInstalledMemory = new ULONGLONGByReference();
		Kernel32Ext.INSTANCE.GetPhysicallyInstalledSystemMemory(totalInstalledMemory);
		return totalInstalledMemory.getValue().longValue() * 1024;
	}

	private void setUsername(SystemInformation systemInformation) {
		char[] userName = new char[1024];
		IntByReference size = new IntByReference(userName.length);
		if (Advapi32.INSTANCE.GetUserNameW(userName, size)) {
			systemInformation.userName = new String(Arrays.copyOf(userName, size.getValue() - 1));
		} else {
			LOGGER.warn("Failed to read username, using fallback instead, error code: {}", Integer.toHexString(Native.getLastError()));
		}
	}

	// TODO Verify if getting debug privileges is really necessary or if it can be done another way
	private void enableSeDebugNamePrivilege() {
		HANDLEByReference hToken = new HANDLEByReference();
		if (Advapi32.INSTANCE.OpenProcessToken(Kernel32.INSTANCE.GetCurrentProcess(), WinNT.TOKEN_ADJUST_PRIVILEGES, hToken)) {
			LUID luid = new LUID();
			if (Advapi32.INSTANCE.LookupPrivilegeValue(null, WinNT.SE_DEBUG_NAME, luid)) {
				TOKEN_PRIVILEGES tokenPriv = new TOKEN_PRIVILEGES(1);
				tokenPriv.Privileges[0] = new LUID_AND_ATTRIBUTES(luid, new DWORD(WinNT.SE_PRIVILEGE_ENABLED));
				if (!Advapi32.INSTANCE.AdjustTokenPrivileges(hToken.getValue(), false, tokenPriv, 0, null, null)) {
					LOGGER.error("Failed to enable SE_DEBUG_NAME privilege, error code: {}", Native.getLastError());
				}
			} else {
				LOGGER.error("Failed to fetch LUID for SE_DEBUG_NAME privilege, error code: {}", Native.getLastError());
			}
			Kernel32.INSTANCE.CloseHandle(hToken.getValue());
		} else {
			LOGGER.error("Failed to open task manager process for token privilege adjustment, error code: {}", Native.getLastError());
		}
	}


	@Override
	public void update(SystemInformation systemInformation) {
		super.update(systemInformation);
		WindowsExtraInformation extraInformation = (WindowsExtraInformation) systemInformation.extraInformation;

		updateTotalCpuTime();
		updateProcesses(systemInformation);

		PERFORMANCE_INFORMATION performanceInfo = fetchPerformanceInformation();
		systemInformation.totalProcesses = performanceInfo.ProcessCount.intValue();
		systemInformation.totalThreads = performanceInfo.ThreadCount.intValue();
		extraInformation.handles = performanceInfo.HandleCount.intValue();

		extraInformation.commitLimit = performanceInfo.CommitLimit.longValue() * systemInformation.pageSize;
		extraInformation.commitUsed = performanceInfo.CommitTotal.longValue() * systemInformation.pageSize;

		extraInformation.kernelPaged = performanceInfo.KernelPaged.longValue() * systemInformation.pageSize;
		extraInformation.kernelNonPaged = performanceInfo.KernelNonpaged.longValue() * systemInformation.pageSize;

		Memory memory = new Memory(new SYSTEM_MEMORY_LIST_INFORMATION().size());
		int status = NtDllExt.INSTANCE.NtQuerySystemInformation(
				SYSTEM_INFORMATION_CLASS.SystemMemoryListInformation.code, memory, (int) memory.size(), null);
		if (status == NtDllExt.STATUS_SUCCESS) {
			SYSTEM_MEMORY_LIST_INFORMATION memoryInfo = Structure.newInstance(NtDllExt.SYSTEM_MEMORY_LIST_INFORMATION.class, memory);
			memoryInfo.read();
			extraInformation.modifiedMemory = memoryInfo.modifiedPageCount.longValue() * systemInformation.pageSize;
			extraInformation.standbyMemory = 0;
			systemInformation.freeMemory = (memoryInfo.freePageCount.longValue() + memoryInfo.zeroPageCount.longValue()) * systemInformation.pageSize;
			for (int i = 0; i < memoryInfo.pageCountByPriority.length; i++) {
				extraInformation.standbyMemory += memoryInfo.pageCountByPriority[i].longValue() * systemInformation.pageSize;
			}
		} else {
			LOGGER.error("Failed to read detailed memory information, error code: {}", Integer.toHexString(status));
		}
	}

	private void updateTotalCpuTime() {
		lastCpuTime = currentCpuTime;
		FILETIME time = new FILETIME();
		Kernel32Ext.INSTANCE.GetSystemTimeAsFileTime(time);
		currentCpuTime = time.toTime();
	}

	private void updateProcesses(SystemInformation systemInformation) {
		List<ProcessInfo> newProcesses = fetchProcessList();
		Set<Long> newProcessIds = new HashSet<>();

		if (newProcesses.isEmpty()) {
			return;
		}

		for (ProcessInfo newProcess : newProcesses) {
			newProcessIds.add(newProcess.process.uniqueProcessId);
			Process process = systemInformation.getProcessById(newProcess.process.uniqueProcessId);
			if (process == null) {
				process = new Process(nextProcessId++, newProcess.process.uniqueProcessId);
				systemInformation.processes.add(process);
			}

			if (!process.hasReadOnce) {
				if (process.id == 0) {
					process.fileName = "System Idle Process";
					process.userName = "SYSTEM";
				} else {
					process.fileName = newProcess.process.imageName.buffer.getWideString(0);
				}

				if (readProcessFileNameCommandLineAndUser(process)) {
					readFileDescription(process);
				}

				if (process.description.isEmpty()) {
					process.description = process.fileName;
				}
			}

			process.status = readProcessStatus(newProcess);

			process.privateWorkingSet.addValue(newProcess.process.workingSetPrivateSize);

			// For some reason we need to extract the value and then put it back inside a new LONG_INTEGER instance before using
			// it otherwise the FILETIME becomes corrupted. Does this have something to do with the memory the NT-call returns?
			process.updateCpu(
					new FILETIME(new LARGE_INTEGER(newProcess.process.kernelTime.getValue())).toTime(),
					new FILETIME(new LARGE_INTEGER(newProcess.process.userTime.getValue())).toTime(),
					(currentCpuTime - lastCpuTime), systemInformation.logicalProcessorCount);

			process.hasReadOnce = true;
		}

		updateDeadProcesses(systemInformation, newProcessIds);
	}

	private Status readProcessStatus(ProcessInfo process) {
		if (isSuspended(process)) {
			return Status.Suspended;
		}
		return Status.Running;
	}

	private boolean isSuspended(ProcessInfo process) {
		for (SYSTEM_THREAD_INFORMATION thread : process.threads) {
			if (thread.threadState != THREAD_STATE_WAITING || thread.waitReason != WAIT_REASON_SUSPENDED) {
				return false;
			}
		}
		return process.threads.length != 0;
	}

	private boolean readProcessFileNameCommandLineAndUser(Process process) {
		WinNT.HANDLE handle = Kernel32.INSTANCE.OpenProcess( // TODO Try again with only PROCESS_QUERY_LIMITED_INFORMATION, might give you the user name at least
				WinNT.PROCESS_QUERY_INFORMATION | WinNT.PROCESS_VM_READ,
				false,
				(int) process.id);
		if (handle == null) {
			LOGGER.warn("Failed to open process {} ({}), error code: {}", process.fileName, process.id, Integer.toHexString(Native.getLastError()));
			return false;
		}

		try {
			Memory mem = new Memory(new PROCESS_BASIC_INFORMATION().size());
			int status = NtDllExt.INSTANCE.NtQueryInformationProcess(handle, PROCESS_INFORMATION_CLASS.ProcessBasicInformation.code, mem, (int) mem.size(), null);
			if (status != 0) {
				LOGGER.warn("Failed to read basic process information for {} ({}), error code: {}", process.fileName, process.id, Integer.toHexString(status));
				return false;
			}

			PROCESS_BASIC_INFORMATION processInfo = Structure.newInstance(NtDllExt.PROCESS_BASIC_INFORMATION.class, mem);
			processInfo.read();

			mem = new Memory(new PEB().size());
			if (!readProcessMemory(handle, mem, processInfo.pebBaseAddress, process, PEB.class.getSimpleName())) {
				return false;
			}
			PEB peb = Structure.newInstance(NtDllExt.PEB.class, mem);
			peb.read();

			mem = new Memory(new RTL_USER_PROCESS_PARAMETERS().size());
			if (!readProcessMemory(handle, mem, peb.processParameters, process, RTL_USER_PROCESS_PARAMETERS.class.getSimpleName())) {
				return false;
			}
			RTL_USER_PROCESS_PARAMETERS parameters = Structure.newInstance(NtDllExt.RTL_USER_PROCESS_PARAMETERS.class, mem);
			parameters.read();

			mem = new Memory(parameters.imagePathName.length + 2);
			if (!readProcessMemory(handle, mem, parameters.imagePathName.buffer, process, "image path")) {
				return false;
			}
			process.filePath = mem.getWideString(0);

			mem = new Memory(parameters.commandLine.length + 2);
			if (!readProcessMemory(handle, mem, parameters.commandLine.buffer, process, "command line")) {
				return false;
			}
			process.commandLine = mem.getWideString(0);

			HANDLEByReference tokenRef = new HANDLEByReference();
			if (Advapi32.INSTANCE.OpenProcessToken(handle, WinNT.TOKEN_QUERY, tokenRef)) {
				Account account = Advapi32Util.getTokenAccount(tokenRef.getValue());
				process.userName = account.name;
			} else {
				LOGGER.warn("Failed to get process user for {} ({}), error code: {}", process.fileName, process.id, Integer.toHexString(Native.getLastError()));
				return false;
			}
		} finally {
			Kernel32.INSTANCE.CloseHandle(handle);
		}

		return true;
	}

	private boolean readProcessMemory(WinNT.HANDLE handle, Memory mem, Pointer address, Process process, String targetStruct) {
		boolean success = Kernel32.INSTANCE.ReadProcessMemory(handle, address, mem, (int) mem.size(), null);
		if (!success) {
			LOGGER.warn("Failed to read {} information for {} ({}), error code: {}", targetStruct, process.fileName, process.id, Integer.toHexString(Native.getLastError()));
		}
		return success;
	}

	private boolean readFileDescription(Process process) {
		IntByReference size = new IntByReference();
		int versionInfoSize = Version.INSTANCE.GetFileVersionInfoSize(process.filePath, size);
		if (versionInfoSize == 0) {
			LOGGER.warn("Failed to read file version info size for {} ({}), error code: {}", process.fileName, process.id, Integer.toHexString(Native.getLastError()));
			return false;
		}

		Memory mem = new Memory(versionInfoSize);
		if (!Version.INSTANCE.GetFileVersionInfo(process.filePath, 0, (int) mem.size(), mem)) {
			LOGGER.warn("Failed to read file version info for {} ({}), error code: {}", process.fileName, process.id, Integer.toHexString(Native.getLastError()));
			return false;
		}

		PointerByReference pointerRef = new PointerByReference();
		if (!Version.INSTANCE.VerQueryValue(mem, "\\VarFileInfo\\Translation", pointerRef, size)) {
			LOGGER.warn("Failed to find Translations in file version info for {} ({}), error code: {}", process.fileName, process.id, Integer.toHexString(Native.getLastError()));
			return false;
		}

		// TODO Read the proper language in the future (nLangs = size.getValue() / new LANGANDCODEPAGE().size())
		LANGANDCODEPAGE language = Structure.newInstance(VersionExt.LANGANDCODEPAGE.class, pointerRef.getValue());
		language.read();
		String query = "\\StringFileInfo\\" + String.format("%04x%04x", language.wLanguage.intValue(), language.wCodePage.intValue()).toUpperCase() + "\\FileDescription";

		if (!Version.INSTANCE.VerQueryValue(mem, query, pointerRef, size)) {
			LOGGER.warn("Failed to find FileDescription in file version info for {} ({}), error code: {}", process.fileName, process.id, Integer.toHexString(Native.getLastError()));
			return false;
		}

		process.description = pointerRef.getValue().getWideString(0).trim();

		return true;
	}

	private List<ProcessInfo> fetchProcessList() {
		Memory memory = new Memory(1);
		IntByReference size = new IntByReference();
		int status = NtDllExt.INSTANCE.NtQuerySystemInformation(SYSTEM_INFORMATION_CLASS.SystemProcessInformation.code, memory, (int) memory.size(), size);
		if (status == NtDllExt.STATUS_BUFFER_OVERFLOW ||
				status == NtDllExt.STATUS_BUFFER_TOO_SMALL ||
				status == NtDllExt.STATUS_INFO_LENGTH_MISMATCH) {
			memory = new Memory(size.getValue());
			status = NtDllExt.INSTANCE.NtQuerySystemInformation(SYSTEM_INFORMATION_CLASS.SystemProcessInformation.code, memory, (int) memory.size(), size);
			if (status != 0) {
				LOGGER.error("Failed to read process list, NtQuerySystemInformation failed with error code: {}", Integer.toHexString(status));
				return new ArrayList<>();
			}
		} else {
			LOGGER.error("Failed to read process list size, NtQuerySystemInformation failed with error code: {}", Integer.toHexString(status));
			return new ArrayList<>();
		}

		List<ProcessInfo> processes = new ArrayList<>();
		int offset = 0;
		do {
			SYSTEM_PROCESS_INFORMATION processInformation = Structure.newInstance(NtDllExt.SYSTEM_PROCESS_INFORMATION.class, memory.share(offset));
			processInformation.read();

			// Fetch thread information
			int threadSize = new SYSTEM_THREAD_INFORMATION().size();
			SYSTEM_THREAD_INFORMATION[] threads = new SYSTEM_THREAD_INFORMATION[processInformation.numberOfThreads];
			for (int i = 0; i < processInformation.numberOfThreads; i++) {
				SYSTEM_THREAD_INFORMATION thread = Structure.newInstance(SYSTEM_THREAD_INFORMATION.class,
						memory.share(offset + processInformation.size() + i * threadSize));
				thread.read();
				threads[i] = thread;
			}

			processes.add(new ProcessInfo(processInformation, threads));

			if (processInformation.nextEntryOffset == 0) {
				offset = 0;
			} else {
				offset += processInformation.nextEntryOffset;
			}
		} while (offset > 0);

		return processes;
	}

	private PERFORMANCE_INFORMATION fetchPerformanceInformation() {
		PERFORMANCE_INFORMATION performanceInformation = new PERFORMANCE_INFORMATION();
		PsapiExt.INSTANCE.GetPerformanceInfo(performanceInformation, performanceInformation.size());
		return performanceInformation;
	}

	private static class ProcessInfo {
		SYSTEM_PROCESS_INFORMATION process;
		SYSTEM_THREAD_INFORMATION[] threads;

		public ProcessInfo(SYSTEM_PROCESS_INFORMATION process, SYSTEM_THREAD_INFORMATION[] threads) {
			this.process = process;
			this.threads = threads;
		}
	}
}