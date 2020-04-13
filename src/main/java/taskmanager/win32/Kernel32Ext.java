/*
 * Copyright (c) 2020. Sebastian Hjelm
 */

package taskmanager.win32;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.win32.W32APIOptions;

public interface Kernel32Ext extends Kernel32 {
	Kernel32Ext INSTANCE = Native.load("kernel32", Kernel32Ext.class, W32APIOptions.DEFAULT_OPTIONS);

	void GetSystemTimeAsFileTime(FILETIME time);

	boolean OpenProcessToken(WinNT.HANDLE ProcessHandle, DWORD DesiredAccess, WinNT.HANDLEByReference TokenHandle);

	boolean GetPhysicallyInstalledSystemMemory(ULONGLONGByReference TotalMemoryInKilobytes);
}