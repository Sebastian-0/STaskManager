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

package taskmanager.platform.linux;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class LinuxProcess {
	private static final Logger LOGGER = LoggerFactory.getLogger(LinuxProcess.class);

	public static boolean kill(long pid) {
		try {
			Runtime.getRuntime().exec("kill -9 " + pid);
		} catch (IOException e) {
			LOGGER.error("Failed to kill process {}", pid, e);
			return false;
		}
		return true;
	}

	public static boolean suspend(long pid) {
		try {
			Runtime.getRuntime().exec("kill -STOP " + pid);
		} catch (IOException e) {
			LOGGER.error("Failed to suspend process {}", pid, e);
			return false;
		}
		return true;
	}

	public static boolean resume(long pid) {
		try {
			Runtime.getRuntime().exec("kill -CONT " + pid);
		} catch (IOException e) {
			LOGGER.error("Failed to resume process {}", pid, e);
			return false;
		}
		return true;
	}
}