/*
 * Copyright (c) 2020. Sebastian Hjelm
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
