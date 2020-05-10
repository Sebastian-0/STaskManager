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

import java.io.IOException;

public class LinuxProcess {
	public static boolean kill(long pid) {
		try {
			Runtime.getRuntime().exec("kill -9 " + pid);
		} catch (IOException e) {
			return false;
		}

		return true;
	}
}