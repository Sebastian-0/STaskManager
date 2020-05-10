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
import taskmanager.InformationLoader;
import taskmanager.Process;
import taskmanager.SystemInformation;
import taskmanager.platform.win32.NtDllExt.PEB;
import taskmanager.platform.win32.NtDllExt.PROCESS_BASIC_INFORMATION;
import taskmanager.platform.win32.NtDllExt.RTL_USER_PROCESS_PARAMETERS;
import taskmanager.platform.win32.NtDllExt.SYSTEM_INFORMATION_CLASS;
import taskmanager.platform.win32.NtDllExt.SYSTEM_MEMORY_LIST_INFORMATION;
import taskmanager.platform.win32.NtDllExt.SYSTEM_PROCESS_INFORMATION;
import taskmanager.platform.win32.VersionExt.LANGANDCODEPAGE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

public class WindowsInformationLoader extends InformationLoader {
	private long lastCpuTime;
	private long currentCpuTime;

	private long nextProcessId;

	@Override
	public void init(SystemInformation systemInformation) {
		super.init(systemInformation);

		ULONGLONGByReference totalInstalledMemory = new ULONGLONGByReference();
		Kernel32Ext.INSTANCE.GetPhysicallyInstalledSystemMemory(totalInstalledMemory);
		systemInformation.physicalMemoryTotalInstalled = totalInstalledMemory.getValue().longValue() * 1024;

		systemInformation.reservedMemory = systemInformation.physicalMemoryTotalInstalled - systemInformation.physicalMemoryTotal;

		readUsername(systemInformation);

		enableSeDebugNamePrivilege();
	}

	private void readUsername(SystemInformation systemInformation) {
		char[] userName = new char[1024];
		IntByReference size = new IntByReference(userName.length);
		if (Advapi32.INSTANCE.GetUserNameW(userName, size)) {
			systemInformation.userName = new String(Arrays.copyOf(userName, size.getValue() - 1));
		} else {
			System.out.println("Failed to read username, using fallback instead! Error: " + Integer.toHexString(Native.getLastError()));
		}
	}

	private void enableSeDebugNamePrivilege() {
		HANDLEByReference hToken = new HANDLEByReference();
		TOKEN_PRIVILEGES tokenPriv = new TOKEN_PRIVILEGES(1);
		LUID luidDebug = new LUID();

		if (Advapi32.INSTANCE.OpenProcessToken(Kernel32.INSTANCE.GetCurrentProcess(), WinNT.TOKEN_ADJUST_PRIVILEGES, hToken)) {
			if (Advapi32.INSTANCE.LookupPrivilegeValue(null, WinNT.SE_DEBUG_NAME, luidDebug)) {
				tokenPriv.Privileges[0] = new LUID_AND_ATTRIBUTES(luidDebug, new DWORD(WinNT.SE_PRIVILEGE_ENABLED));
				if (Advapi32.INSTANCE.AdjustTokenPrivileges(hToken.getValue(), false, tokenPriv, 0, null, null)) {
					// Always successful, even in the cases which lead to OpenProcess failure
					System.out.println("SUCCESSFULLY CHANGED TOKEN PRIVILEGES");
				} else {
					System.out.println("FAILED TO CHANGE TOKEN PRIVILEGES, CODE: " + Native.getLastError());
				}
			}
		}

		Kernel32.INSTANCE.CloseHandle(hToken.getValue());
	}


	@Override
	public void update(SystemInformation systemInformation) {
		super.update(systemInformation);

		updateTotalCpuTime();
		updateProcesses(systemInformation);

		PERFORMANCE_INFORMATION performanceInfo = fetchPerformanceInformation();
		systemInformation.totalProcesses = performanceInfo.ProcessCount.intValue();
		systemInformation.totalThreads = performanceInfo.ThreadCount.intValue();
		systemInformation.totalHandles = performanceInfo.HandleCount.intValue();

		systemInformation.commitLimit = performanceInfo.CommitLimit.longValue() * systemInformation.pageSize;
		systemInformation.commitUsed = performanceInfo.CommitTotal.longValue() * systemInformation.pageSize;

		systemInformation.kernelPaged = performanceInfo.KernelPaged.longValue() * systemInformation.pageSize;
		systemInformation.kernelNonPaged = performanceInfo.KernelNonpaged.longValue() * systemInformation.pageSize;

		Memory memory = new Memory(new SYSTEM_MEMORY_LIST_INFORMATION().size());
		int status = NtDllExt.INSTANCE.NtQuerySystemInformation(
				SYSTEM_INFORMATION_CLASS.SystemMemoryListInformation.code, memory, (int) memory.size(), null);
		if (status == 0) {
			SYSTEM_MEMORY_LIST_INFORMATION memoryInfo = Structure.newInstance(NtDllExt.SYSTEM_MEMORY_LIST_INFORMATION.class, memory);
			memoryInfo.read();
			systemInformation.modifiedMemory = memoryInfo.ModifiedPageCount.longValue() * systemInformation.pageSize;
			systemInformation.standbyMemory = 0;
			systemInformation.freeMemory = (memoryInfo.FreePageCount.longValue() + memoryInfo.ZeroPageCount.longValue()) * systemInformation.pageSize;
			for (int i = 0; i < memoryInfo.PageCountByPriority.length; i++) {
				systemInformation.standbyMemory += memoryInfo.PageCountByPriority[i].longValue() * systemInformation.pageSize;
			}
		} else {
			System.out.println("update(): Failed to read SYSTEM_MEMORY_LIST_INFORMATION: " + Integer.toHexString(status));
		}
	}

	private void updateTotalCpuTime() {
		lastCpuTime = currentCpuTime;
		FILETIME time = new FILETIME();
		Kernel32Ext.INSTANCE.GetSystemTimeAsFileTime(time);
		currentCpuTime = time.toTime();
	}

	private void updateProcesses(SystemInformation systemInformation) {
		List<SYSTEM_PROCESS_INFORMATION> newProcesses = fetchProcessList();
		Set<Long> processIds = new HashSet<>();

		if (newProcesses.isEmpty()) {
			return;
		}

		for (SYSTEM_PROCESS_INFORMATION newProcess : newProcesses) {
			processIds.add(newProcess.UniqueProcessId);
			Process process = systemInformation.getProcessById(newProcess.UniqueProcessId);
			if (process == null) {
				process = new Process(nextProcessId++, newProcess.UniqueProcessId);
				systemInformation.processes.add(process);
			}

			if (!process.hasReadOnce) {
				if (process.id == 0) {
					process.fileName = "System Idle Process";
					process.userName = "SYSTEM";
				} else {
					process.fileName = newProcess.ImageName.Buffer.getWideString(0);
				}

				if (readProcessFileNameCommandLineAndUser(process)) {
					readFileDescription(process);
				}

				if (process.description.isEmpty())
					process.description = process.fileName;
			}

			process.privateWorkingSet.addValue(newProcess.WorkingSetPrivateSize);

			// For some reason we need to extract the value and then put it back inside a new LONG_INTEGER instance before using
			// it otherwise the FILETIME becomes corrupted. Does this have something to do with the memory the NT-call returns?
			process.updateCpu(
					new FILETIME(new LARGE_INTEGER(newProcess.KernelTime.getValue())).toTime(),
					new FILETIME(new LARGE_INTEGER(newProcess.UserTime.getValue())).toTime(),
					(currentCpuTime - lastCpuTime), systemInformation.logicalProcessorCount);

			process.hasReadOnce = true;
		}

		// Remove old processes
		ListIterator<Process> itr = systemInformation.processes.listIterator();
		while (itr.hasNext()) {
			Process process = itr.next();
			if (!processIds.contains(process.id)) {
				process.isDead = true;
				process.deathTimestamp = System.currentTimeMillis();
				itr.remove();
				systemInformation.deadProcesses.add(process);
			}
		}
	}

	private boolean readProcessFileNameCommandLineAndUser(Process process) {
		WinNT.HANDLE handle = Kernel32.INSTANCE.OpenProcess( // TODO Try again with only PROCESS_QUERY_LIMITED_INFORMATION, might give you the user name at least
				WinNT.PROCESS_QUERY_INFORMATION | WinNT.PROCESS_VM_READ,
				false,
				(int) process.id);
		if (handle == null) {
			System.out.println("readProcessCommandLine(): Failed to open " + process.fileName + ": " + Integer.toHexString(Native.getLastError()));
			return false;
		}

		try {
			Memory mem = new Memory(new PROCESS_BASIC_INFORMATION().size());
			int status = NtDllExt.INSTANCE.NtQueryInformationProcess(handle, 0, mem, (int) mem.size(), null);
			if (status != 0) {
				System.out.println("readProcessCommandLine(): Failed to read process information for " + process.fileName + ": " + Integer.toHexString(status));
				return false;
			}

			PROCESS_BASIC_INFORMATION processInfo = Structure.newInstance(NtDllExt.PROCESS_BASIC_INFORMATION.class, mem);
			processInfo.read();

			mem = new Memory(new PEB().size());
			if (!readProcessMemory(handle, mem, processInfo.PebBaseAddress, process, "PEB"))
				return false;

			PEB peb = Structure.newInstance(NtDllExt.PEB.class, mem);
			peb.read();

			mem = new Memory(new RTL_USER_PROCESS_PARAMETERS().size());
			if (!readProcessMemory(handle, mem, peb.ProcessParameters, process, "RTL_USER_PROCESS_PARAMETERS"))
				return false;

			RTL_USER_PROCESS_PARAMETERS parameters = Structure.newInstance(NtDllExt.RTL_USER_PROCESS_PARAMETERS.class, mem);
			parameters.read();

			mem = new Memory(parameters.ImagePathName.Length + 2);
			if (!readProcessMemory(handle, mem, parameters.ImagePathName.Buffer, process, "image path"))
				return false;

			process.filePath = mem.getWideString(0);

			mem = new Memory(parameters.CommandLine.Length + 2);
			if (!readProcessMemory(handle, mem, parameters.CommandLine.Buffer, process, "command line"))
				return false;

			process.commandLine = mem.getWideString(0);

			HANDLEByReference tokenRef = new HANDLEByReference();
			if (Advapi32.INSTANCE.OpenProcessToken(handle, WinNT.TOKEN_QUERY, tokenRef)) {
				Account account = Advapi32Util.getTokenAccount(tokenRef.getValue());
				process.userName = account.name;
			} else {
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
			System.out.println("readProcessCommandLine(): Failed to read " + targetStruct + " information for " + process.fileName + ": " + Integer.toHexString(Native.getLastError()));
			Kernel32.INSTANCE.CloseHandle(handle);
		}
		return success;
	}

	private boolean readFileDescription(Process process) {
		IntByReference size = new IntByReference();
		int versionInfoSize = Version.INSTANCE.GetFileVersionInfoSize(process.filePath, size);
		if (versionInfoSize == 0) {
			System.out.println("readFileDescription(): Failed to read FileVersionSize for " + process.filePath + ": " + Integer.toHexString(Native.getLastError()));
			return false;
		}

		Memory mem = new Memory(versionInfoSize);
		if (!Version.INSTANCE.GetFileVersionInfo(process.filePath, 0, (int) mem.size(), mem)) {
			System.out.println("readFileDescription(): Failed to read FileVersionInfo for " + process.filePath + ": " + Integer.toHexString(Native.getLastError()));
			return false;
		}

		PointerByReference pointerRef = new PointerByReference();
		if (!Version.INSTANCE.VerQueryValue(mem, "\\VarFileInfo\\Translation", pointerRef, size)) {
			System.out.println("readFileDescription(): Failed to read Translations for " + process.filePath + ": " + Integer.toHexString(Native.getLastError()));
			return false;
		}

		int nLangs = size.getValue() / new LANGANDCODEPAGE().size();
		LANGANDCODEPAGE language = Structure.newInstance(VersionExt.LANGANDCODEPAGE.class, pointerRef.getValue());
		language.read();
		String query = "\\StringFileInfo\\" + String.format("%04x%04x", language.wLanguage.intValue(), language.wCodePage.intValue()).toUpperCase() + "\\FileDescription";

		if (!Version.INSTANCE.VerQueryValue(mem, query, pointerRef, size)) {
			System.out.println("readFileDescription(): Failed to read FileDescription for " + process.filePath + ": " + Integer.toHexString(Native.getLastError()));
			return false;
		}

		process.description = pointerRef.getValue().getWideString(0).trim();

		return true;
	}

	private List<SYSTEM_PROCESS_INFORMATION> fetchProcessList() {
		List<SYSTEM_PROCESS_INFORMATION> processes = new ArrayList<>();

		Memory memory = new Memory(1);
		IntByReference size = new IntByReference();
		int status = NtDllExt.INSTANCE.NtQuerySystemInformation(SYSTEM_INFORMATION_CLASS.SystemProcessInformation.code, memory, (int) memory.size(), size);
		if (status == NtDllExt.STATUS_BUFFER_OVERFLOW ||
				status == NtDllExt.STATUS_BUFFER_TOO_SMALL ||
				status == NtDllExt.STATUS_INFO_LENGTH_MISMATCH) {
			memory = new Memory(size.getValue());
			status = NtDllExt.INSTANCE.NtQuerySystemInformation(5, memory, (int) memory.size(), size);
			if (status != 0) { // TODO; Possibly add loop to account for processes disappearing
				System.out.println("fetchProcessList(): NtQuerySystemInformation failed with: " + Integer.toHexString(status));
			}
		} else {
			System.out.println("fetchProcessList(): NtQuerySystemInformation failed with: " + Integer.toHexString(status));
		}

		if (status == 0) {
			int offset = 0;
			do {
				SYSTEM_PROCESS_INFORMATION proccessInformation = Structure.newInstance(NtDllExt.SYSTEM_PROCESS_INFORMATION.class, memory.share(offset));
				proccessInformation.read();
				processes.add(proccessInformation);

				// Fetch thread information
//				if (procInfo.NumberOfThreads > 0) {
//					SYSTEM_THREAD_INFORMATION thread = (SYSTEM_THREAD_INFORMATION) Structure.newInstance(NtDllExt.SYSTEM_THREAD_INFORMATION.class, memory.share(offset + procInfo.size()));
//					System.out.println(thread.CreateTime);
//				}

				if (proccessInformation.NextEntryOffset == 0)
					offset = 0;
				else
					offset += proccessInformation.NextEntryOffset;
			} while (offset > 0);
		}

		return processes;
	}

	private PERFORMANCE_INFORMATION fetchPerformanceInformation() {
		PERFORMANCE_INFORMATION performanceInformation = new PERFORMANCE_INFORMATION();
		PsapiExt.INSTANCE.GetPerformanceInfo(performanceInformation, performanceInformation.size());
		return performanceInformation;
	}
}