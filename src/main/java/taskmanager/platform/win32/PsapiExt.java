/*
 * Copyright (c) 2020. Sebastian Hjelm
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

	@FieldOrder({"cb", "PageFaultCount", "PeakWorkingSetSize", "WorkingSetSize", "QuotaPeakPagedPoolUsage",
			"QuotaPagedPoolUsage", "QuotaPeakNonPagedPoolUsage", "QuotaNonPagedPoolUsage", "PagefileUsage", "PeakPagefileUsage",
			"PrivateUsage"})
	class PROCESS_MEMORY_COUNTERS extends Structure {
		public DWORD cb;
		public DWORD PageFaultCount;
		public SIZE_T PeakWorkingSetSize;
		public SIZE_T WorkingSetSize;
		public SIZE_T QuotaPeakPagedPoolUsage;
		public SIZE_T QuotaPagedPoolUsage;
		public SIZE_T QuotaPeakNonPagedPoolUsage;
		public SIZE_T QuotaNonPagedPoolUsage;
		public SIZE_T PagefileUsage;
		public SIZE_T PeakPagefileUsage;
		public SIZE_T PrivateUsage;
	}
}