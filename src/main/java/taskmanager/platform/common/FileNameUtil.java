/*
 * Copyright (c) 2021. Sebastian Hjelm
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * See LICENSE for further details.
 */

package taskmanager.platform.common;

import taskmanager.data.Process;

import java.io.File;

public class FileNameUtil {
	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	public static boolean setProcessPathAndNameFromCommandLine(Process process, String partialName) {
		if (partialName.isEmpty() && process.commandLine.isEmpty()) {
			return false;
		}

		// First, see if the partial name is in the command line, in that case extract it and try to extract the path
		int start = process.commandLine.indexOf(partialName);
		if (start != -1) {
			int startSpace = process.commandLine.lastIndexOf(' ', start);
			int endSpace = process.commandLine.indexOf(' ', start + partialName.length());
			endSpace = (endSpace == -1) ? process.commandLine.length() : endSpace;

			start = process.commandLine.lastIndexOf(partialName, endSpace);
			partialName = process.commandLine.substring(start, endSpace);
			if (partialName.endsWith(":")) {
				partialName = partialName.substring(0, partialName.length() - 1);
			}
			process.fileName = partialName; // This will be wrong if there is a space after the first 15 chars in the name

			String filePath = process.commandLine.substring(startSpace + 1, endSpace);
			File file = new File(filePath);
			if (file.exists()) {
				process.filePath = filePath;
			}
		}

		// Secondly if the partial name isn't in the command line, just take the first binary in the path
		int space = process.commandLine.indexOf(' ');
		if (space == -1) {
			space = process.commandLine.length();
		}
		if (space > 0) {
			// TODO This will compute the wrong result if there is a space in the path, we could improve by greedily
			//  assume that we want the last / and then shrink until the file path exists (and is a file), possibly...
			int separator = process.commandLine.lastIndexOf(File.separator, space);
			process.fileName = process.commandLine.substring(separator + 1, space);
			String filePath = process.commandLine.substring(0, space);
			File file = new File(filePath);
			if (file.exists()) {
				process.filePath = filePath;
			}
		}

		// Lastly, with no command line this is the best we can do (first 15 chars)
		process.fileName = partialName;
		return true;
	}
}
