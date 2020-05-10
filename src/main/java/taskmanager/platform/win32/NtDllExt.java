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

	@FieldOrder({"Length", "MaximumLength", "Buffer"})
	class UNICODE_STRING extends Structure {
		public short Length;
		public short MaximumLength;
		public Pointer Buffer;
	}

	@FieldOrder({"UniqueProcess", "UniqueThread"})
	class CLIENT_ID extends Structure {
		public long UniqueProcess; // HANDLE
		public long UniqueThread;  // HANDLE
	}

	@FieldOrder({"KernelTime", "UserTime", "CreateTime", "WaitTime", "StartAddress", "ClientId", "Priority",
			"BasePriority", "ContextSwitches", "ThreadState", "WaitReason"})
	class SYSTEM_THREAD_INFORMATION extends Structure {
		public long      KernelTime;
		public long      UserTime;
		public long      CreateTime;
		public int       WaitTime;
		public PVOID     StartAddress;
		public CLIENT_ID ClientId;
		public int       Priority;
		public int       BasePriority;
		public int       ContextSwitches;
		public int       ThreadState;
		public int       WaitReason; // Should be an enum
	}

	@FieldOrder({"NextEntryOffset", "NumberOfThreads", "WorkingSetPrivateSize", "HardFaultCount",
			"NumberOfThreadsHighWatermark", "CycleTime", "CreateTime", "UserTime", "KernelTime", "ImageName", "BasePriority",
			"UniqueProcessId", "InheritedFromUniqueProcessId", "HandleCount", "SessionId", "UniqueProcessKey",
			"PeakVirtualSize", "VirtualSize", "PageFaultCount", "PeakWorkingSetSize", "WorkingSetSize",
			"QuotaPeakPagedPoolUsage", "QuotaPagedPoolUsage", "QuotaPeakNonPagedPoolUsage", "QuotaNonPagedPoolUsage",
			"PagefileUsage", "PeakPagefileUsage", "PrivatePageCount", "ReadOperationCount", "WriteOperationCount",
			"OtherOperationCount", "ReadTransferCount", "WriteTransferCount", "OtherTransferCount"})
    class SYSTEM_PROCESS_INFORMATION extends Structure { // TODO Try to speed up by removing replacing Types with primitives?
		public int            NextEntryOffset;
		public int            NumberOfThreads;
		public long           WorkingSetPrivateSize;
		public int            HardFaultCount;
		public int            NumberOfThreadsHighWatermark;
		public long           CycleTime;
		public LARGE_INTEGER  CreateTime;
		public LARGE_INTEGER  UserTime;
		public LARGE_INTEGER  KernelTime;
		public UNICODE_STRING ImageName;
		public int            BasePriority;
		public long           UniqueProcessId;              // HANDLE
		public long           InheritedFromUniqueProcessId; // HANDLE
		public int            HandleCount;
		public int            SessionId;
		public Pointer        UniqueProcessKey;
		public SIZE_T         PeakVirtualSize;
		public SIZE_T         VirtualSize;
		public int            PageFaultCount;
		public SIZE_T         PeakWorkingSetSize;
		public SIZE_T         WorkingSetSize;
		public SIZE_T         QuotaPeakPagedPoolUsage;
		public SIZE_T         QuotaPagedPoolUsage;
		public SIZE_T         QuotaPeakNonPagedPoolUsage;
		public SIZE_T         QuotaNonPagedPoolUsage;
		public SIZE_T         PagefileUsage;
		public SIZE_T         PeakPagefileUsage;
		public SIZE_T         PrivatePageCount;
		public long           ReadOperationCount;
		public long           WriteOperationCount;
		public long           OtherOperationCount;
		public long           ReadTransferCount;
		public long           WriteTransferCount;
		public long           OtherTransferCount;
	}

	@FieldOrder({"ExitStatus", "PebBaseAddress", "AffinityMask", "BasePriority", "UniqueProcessId",
			"InheritedFromUniqueProcessId"})
	class PROCESS_BASIC_INFORMATION extends Structure
	{
		public int          ExitStatus;
		public Pointer      PebBaseAddress; // PPEB
		public ULONG_PTR    AffinityMask;
		public int          BasePriority;
		public WinNT.HANDLE UniqueProcessId;
		public WinNT.HANDLE InheritedFromUniqueProcessId;
	}

	@FieldOrder({"Reserved", "TimerResolution", "PageSize", "NumberOfPhysicalPages", "LowestPhysicalPageNumber",
	"HighestPhysicalPageNumber", "AllocationGranularity", "MinimumUserModeAddress", "MaximumUserModeAddress",
	"ActiveProcessorsAffinityMask", "NumberOfProcessors"})
	class SYSTEM_BASIC_INFORMATION extends Structure {
		public ULONG Reserved;
		public ULONG TimerResolution;
		public ULONG PageSize;
		public ULONG NumberOfPhysicalPages;
		public ULONG LowestPhysicalPageNumber;
		public ULONG HighestPhysicalPageNumber;
		public ULONG AllocationGranularity;
		public ULONG_PTR MinimumUserModeAddress;
		public ULONG_PTR MaximumUserModeAddress;
		public ULONG_PTR ActiveProcessorsAffinityMask;
		public CHAR NumberOfProcessors;
	}

	@FieldOrder({"ZeroPageCount", "FreePageCount", "ModifiedPageCount", "ModifiedNoWritePageCount", "BadPageCount",
			"PageCountByPriority", "RepurposedPagesByPriority", "ModifiedPageCountPageFile"})
	class SYSTEM_MEMORY_LIST_INFORMATION extends Structure {
		public ULONG_PTR ZeroPageCount;
		public ULONG_PTR FreePageCount;
		public ULONG_PTR ModifiedPageCount;
		public ULONG_PTR ModifiedNoWritePageCount;
		public ULONG_PTR BadPageCount;
		public ULONG_PTR[] PageCountByPriority = new ULONG_PTR[8];
		public ULONG_PTR[] RepurposedPagesByPriority = new ULONG_PTR[8];
		public ULONG_PTR ModifiedPageCountPageFile;
	}

	@FieldOrder({"Reserved1", "Reserved1_2", "BeingDebugged", "Reserved2", "Reserved3", "Reserved3_2", "Ldr",
			"ProcessParameters", "Reserved4", "Reserved4_2", "Reserved4_3", "AtlThunkSListPtr", "Reserved5", "Reserved6",
			"Reserved7", "Reserved8", "AtlThunkSListPtr32", "Reserved9", "Reserved10", "PostProcessInitRoutine", "Reserved11",
			"Reserved12", "SessionId"})
	class PEB extends Structure	{
		public BYTE    Reserved1;
		public BYTE    Reserved1_2;
		public BYTE    BeingDebugged;
		public BYTE    Reserved2;
		public PVOID   Reserved3;
		public PVOID   Reserved3_2;
		public Pointer Ldr; // PPEB_LDR_DATA
		public Pointer ProcessParameters; // PRTL_USER_PROCESS_PARAMETERS
		public PVOID   Reserved4;
		public PVOID   Reserved4_2;
		public PVOID   Reserved4_3;
		public PVOID   AtlThunkSListPtr;
		public PVOID   Reserved5;
		public ULONG   Reserved6;
		public PVOID   Reserved7;
		public ULONG   Reserved8;
		public ULONG   AtlThunkSListPtr32;
		public PVOID[] Reserved9 = new PVOID[45];
		public BYTE[]  Reserved10 = new BYTE[96];
		public Pointer PostProcessInitRoutine; // PPS_POST_PROCESS_INIT_ROUTINE
		public BYTE[]  Reserved11 = new BYTE[128];
		public PVOID   Reserved12;
		public ULONG   SessionId;
	}

	@FieldOrder({"Reserved1", "Reserved2", "ImagePathName", "CommandLine"})
	class RTL_USER_PROCESS_PARAMETERS extends Structure	{
		public BYTE[]         Reserved1 = new BYTE[16];
		public PVOID[]        Reserved2 = new PVOID[10];
		public UNICODE_STRING ImagePathName;
		public UNICODE_STRING CommandLine;
	}
}