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
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.HANDLE;

public class WindowsProcess {
	public static boolean kill(long pid) {
		HANDLE handle = Kernel32.INSTANCE.OpenProcess(WinNT.PROCESS_TERMINATE, false, (int) pid);
		boolean success = Kernel32.INSTANCE.TerminateProcess(handle, 1);
		Kernel32.INSTANCE.CloseHandle(handle);

		if (!success) {
			System.out.println("WindowsProcess: kill(): Failed to kill process (" + pid + "): " + Integer.toHexString(Native.getLastError()));
			return false;
		}

		return true;
	}
}