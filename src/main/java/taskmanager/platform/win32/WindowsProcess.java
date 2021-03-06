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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import taskmanager.platform.win32.dll.NtDllExt;

public class WindowsProcess {
	private static final Logger LOGGER = LoggerFactory.getLogger(WindowsProcess.class);

	public static boolean kill(long pid) {
		HANDLE handle = Kernel32.INSTANCE.OpenProcess(WinNT.PROCESS_TERMINATE, false, (int) pid);
		try {
			if (!Kernel32.INSTANCE.TerminateProcess(handle, 1)) {
				LOGGER.error("Failed to kill process {}, error code: {}", pid, Integer.toHexString(Native.getLastError()));
				return false;
			}
		} finally {
			Kernel32.INSTANCE.CloseHandle(handle);
		}
		return true;
	}

	public static boolean suspend(long pid) {
		HANDLE handle = Kernel32.INSTANCE.OpenProcess(WinNT.PROCESS_SUSPEND_RESUME, false, (int) pid);
		try {
			int status = NtDllExt.INSTANCE.NtSuspendProcess(handle);
			if (status != NtDllExt.STATUS_SUCCESS) {
				LOGGER.error("Failed to suspend process {}, error code: {}", pid, Integer.toHexString(status));
				return false;
			}
		} finally {
			Kernel32.INSTANCE.CloseHandle(handle);
		}
		return true;
	}

	public static boolean resume(long pid) {
		HANDLE handle = Kernel32.INSTANCE.OpenProcess(WinNT.PROCESS_SUSPEND_RESUME, false, (int) pid);
		try {
			int status = NtDllExt.INSTANCE.NtResumeProcess(handle);
			if (status != NtDllExt.STATUS_SUCCESS) {
				LOGGER.error("Failed to resume process {}, error code: {}", pid, Integer.toHexString(status));
				return false;
			}
		} finally {
			Kernel32.INSTANCE.CloseHandle(handle);
		}
		return true;
	}
}