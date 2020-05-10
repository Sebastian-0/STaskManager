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

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;
import com.sun.jna.platform.win32.BaseTSD.SIZE_T;
import com.sun.jna.platform.win32.BaseTSD.ULONG_PTR;
import com.sun.jna.platform.win32.NtDll;
import com.sun.jna.platform.win32.WinDef.BYTE;
import com.sun.jna.platform.win32.WinDef.CHAR;
import com.sun.jna.platform.win32.WinDef.PVOID;
import com.sun.jna.platform.win32.WinDef.ULONG;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.LARGE_INTEGER;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.W32APIOptions;

public interface NtDllExt extends NtDll {
	int STATUS_BUFFER_OVERFLOW = 0x80000005;
	int STATUS_BUFFER_TOO_SMALL = 0xC0000023;
	int STATUS_INFO_LENGTH_MISMATCH = 0xC0000004;

	NtDllExt INSTANCE = Native.load("ntdll", NtDllExt.class, W32APIOptions.DEFAULT_OPTIONS);

	int NtQueryInformationProcess(WinNT.HANDLE hProcess, int informationClass, Pointer informationOut, int informationLength, IntByReference returnLength);
	int NtQuerySystemInformation(int informationClass, Pointer informationOut, int informationLength, IntByReference returnLength);

	int NtSuspendProcess(WinNT.HANDLE hProcess); // TODO Does pausing work? (see ntpsapi.h in processhacker for other functions)
	int NtResumeProcess(WinNT.HANDLE hProcess);

	enum SYSTEM_INFORMATION_CLASS {
		SystemProcessInformation(5),
		SystemMemoryListInformation(80);

		final int code;

		SYSTEM_INFORMATION_CLASS(int code) {
			this.code = code;
		}
	}

	@FieldOrder({"length", "maximumLength", "buffer"})
	class UNICODE_STRING extends Structure {
		public short length;
		public short maximumLength;
		public Pointer buffer;
	}

	@FieldOrder({"uniqueProcess", "uniqueThread"})
	class CLIENT_ID extends Structure {
		public long uniqueProcess; // HANDLE
		public long uniqueThread;  // HANDLE
	}

	@FieldOrder({"kernelTime", "userTime", "createTime", "waitTime", "startAddress", "clientId", "priority",
			"basePriority", "contextSwitches", "threadState", "waitReason"})
	class SYSTEM_THREAD_INFORMATION extends Structure {
		public long kernelTime;
		public long userTime;
		public long createTime;
		public int waitTime;
		public PVOID startAddress;
		public CLIENT_ID clientId;
		public int priority;
		public int basePriority;
		public int contextSwitches;
		public int threadState;
		public int waitReason; // Should be an enum
	}

	@FieldOrder({"nextEntryOffset", "numberOfThreads", "workingSetPrivateSize", "hardFaultCount",
			"numberOfThreadsHighWatermark", "cycleTime", "createTime", "userTime", "kernelTime", "imageName", "basePriority",
			"uniqueProcessId", "inheritedFromUniqueProcessId", "handleCount", "sessionId", "uniqueProcessKey",
			"peakVirtualSize", "virtualSize", "pageFaultCount", "peakWorkingSetSize", "workingSetSize",
			"quotaPeakPagedPoolUsage", "quotaPagedPoolUsage", "quotaPeakNonPagedPoolUsage", "quotaNonPagedPoolUsage",
			"pagefileUsage", "peakPagefileUsage", "privatePageCount", "readOperationCount", "writeOperationCount",
			"otherOperationCount", "readTransferCount", "writeTransferCount", "otherTransferCount"})
    class SYSTEM_PROCESS_INFORMATION extends Structure { // TODO Try to speed up by removing replacing Types with primitives?
		public int nextEntryOffset;
		public int numberOfThreads;
		public long workingSetPrivateSize;
		public int hardFaultCount;
		public int numberOfThreadsHighWatermark;
		public long cycleTime;
		public LARGE_INTEGER createTime;
		public LARGE_INTEGER userTime;
		public LARGE_INTEGER kernelTime;
		public UNICODE_STRING imageName;
		public int basePriority;
		public long uniqueProcessId;              // HANDLE
		public long inheritedFromUniqueProcessId; // HANDLE
		public int handleCount;
		public int sessionId;
		public Pointer uniqueProcessKey;
		public SIZE_T peakVirtualSize;
		public SIZE_T virtualSize;
		public int pageFaultCount;
		public SIZE_T peakWorkingSetSize;
		public SIZE_T workingSetSize;
		public SIZE_T quotaPeakPagedPoolUsage;
		public SIZE_T quotaPagedPoolUsage;
		public SIZE_T quotaPeakNonPagedPoolUsage;
		public SIZE_T quotaNonPagedPoolUsage;
		public SIZE_T pagefileUsage;
		public SIZE_T peakPagefileUsage;
		public SIZE_T privatePageCount;
		public long readOperationCount;
		public long writeOperationCount;
		public long otherOperationCount;
		public long readTransferCount;
		public long writeTransferCount;
		public long otherTransferCount;
	}

	@FieldOrder({"exitStatus", "pebBaseAddress", "affinityMask", "basePriority", "uniqueProcessId",
			"inheritedFromUniqueProcessId"})
	class PROCESS_BASIC_INFORMATION extends Structure
	{
		public int exitStatus;
		public Pointer pebBaseAddress; // PPEB
		public ULONG_PTR affinityMask;
		public int basePriority;
		public WinNT.HANDLE uniqueProcessId;
		public WinNT.HANDLE inheritedFromUniqueProcessId;
	}

	@FieldOrder({"reserved", "timerResolution", "pageSize", "numberOfPhysicalPages", "lowestPhysicalPageNumber",
	"highestPhysicalPageNumber", "allocationGranularity", "minimumUserModeAddress", "maximumUserModeAddress",
	"activeProcessorsAffinityMask", "numberOfProcessors"})
	class SYSTEM_BASIC_INFORMATION extends Structure {
		public ULONG reserved;
		public ULONG timerResolution;
		public ULONG pageSize;
		public ULONG numberOfPhysicalPages;
		public ULONG lowestPhysicalPageNumber;
		public ULONG highestPhysicalPageNumber;
		public ULONG allocationGranularity;
		public ULONG_PTR minimumUserModeAddress;
		public ULONG_PTR maximumUserModeAddress;
		public ULONG_PTR activeProcessorsAffinityMask;
		public CHAR numberOfProcessors;
	}

	@FieldOrder({"zeroPageCount", "freePageCount", "modifiedPageCount", "modifiedNoWritePageCount", "badPageCount",
			"pageCountByPriority", "repurposedPagesByPriority", "modifiedPageCountPageFile"})
	class SYSTEM_MEMORY_LIST_INFORMATION extends Structure {
		public ULONG_PTR zeroPageCount;
		public ULONG_PTR freePageCount;
		public ULONG_PTR modifiedPageCount;
		public ULONG_PTR modifiedNoWritePageCount;
		public ULONG_PTR badPageCount;
		public ULONG_PTR[] pageCountByPriority = new ULONG_PTR[8];
		public ULONG_PTR[] repurposedPagesByPriority = new ULONG_PTR[8];
		public ULONG_PTR modifiedPageCountPageFile;
	}

	@FieldOrder({"reserved1", "reserved1_2", "beingDebugged", "reserved2", "reserved3", "reserved3_2", "ldr",
			"processParameters", "reserved4", "reserved4_2", "reserved4_3", "atlThunkSListPtr", "reserved5", "reserved6",
			"reserved7", "reserved8", "atlThunkSListPtr32", "reserved9", "reserved10", "postProcessInitRoutine", "reserved11",
			"reserved12", "sessionId"})
	class PEB extends Structure	{
		public BYTE reserved1;
		public BYTE reserved1_2;
		public BYTE beingDebugged;
		public BYTE reserved2;
		public PVOID reserved3;
		public PVOID reserved3_2;
		public Pointer Ldr; // PPEB_LDR_DATA
		public Pointer processParameters; // PRTL_USER_PROCESS_PARAMETERS
		public PVOID reserved4;
		public PVOID reserved4_2;
		public PVOID reserved4_3;
		public PVOID atlThunkSListPtr;
		public PVOID reserved5;
		public ULONG reserved6;
		public PVOID reserved7;
		public ULONG reserved8;
		public ULONG atlThunkSListPtr32;
		public PVOID[] reserved9 = new PVOID[45];
		public BYTE[] reserved10 = new BYTE[96];
		public Pointer postProcessInitRoutine; // PPS_POST_PROCESS_INIT_ROUTINE
		public BYTE[]reserved11 = new BYTE[128];
		public PVOID reserved12;
		public ULONG sessionId;
	}

	@FieldOrder({"reserved1", "reserved2", "imagePathName", "commandLine"})
	class RTL_USER_PROCESS_PARAMETERS extends Structure	{
		public BYTE[] reserved1 = new BYTE[16];
		public PVOID[] reserved2 = new PVOID[10];
		public UNICODE_STRING imagePathName;
		public UNICODE_STRING commandLine;
	}
}