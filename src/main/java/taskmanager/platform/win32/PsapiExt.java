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
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;
import com.sun.jna.platform.win32.BaseTSD.SIZE_T;
import com.sun.jna.platform.win32.Psapi;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.win32.W32APIOptions;

public interface PsapiExt extends Psapi {
	PsapiExt INSTANCE = Native.load("psapi", PsapiExt.class, W32APIOptions.DEFAULT_OPTIONS);

	int K32GetProcessImageFileNameW (WinNT.HANDLE hProcess, char[] lpImageFileName, int nSize);
	boolean GetProcessMemoryInfo(WinNT.HANDLE Process,
								 PROCESS_MEMORY_COUNTERS ppsmemCounters, int cb);

	@FieldOrder({"cb", "pageFaultCount", "peakWorkingSetSize", "workingSetSize", "quotaPeakPagedPoolUsage",
			"quotaPagedPoolUsage", "quotaPeakNonPagedPoolUsage", "quotaNonPagedPoolUsage", "pagefileUsage", "peakPagefileUsage",
			"privateUsage"})
	class PROCESS_MEMORY_COUNTERS extends Structure {
		public DWORD cb;
		public DWORD pageFaultCount;
		public SIZE_T peakWorkingSetSize;
		public SIZE_T workingSetSize;
		public SIZE_T quotaPeakPagedPoolUsage;
		public SIZE_T quotaPagedPoolUsage;
		public SIZE_T quotaPeakNonPagedPoolUsage;
		public SIZE_T quotaNonPagedPoolUsage;
		public SIZE_T pagefileUsage;
		public SIZE_T peakPagefileUsage;
		public SIZE_T privateUsage;
	}
}