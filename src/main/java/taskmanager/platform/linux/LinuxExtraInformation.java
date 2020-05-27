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

import taskmanager.data.ExtraInformation;

public class LinuxExtraInformation implements ExtraInformation {
	public int openFileDescriptors;
	public int openFileDescriptorsLimit;

	@Override
	public ExtraInformation copy() {
		LinuxExtraInformation extraInformation = new LinuxExtraInformation();
		extraInformation.copyFrom(this);
		return extraInformation;
	}

	@Override
	public void copyFrom(ExtraInformation info) {
		if (info instanceof LinuxExtraInformation) {
			LinuxExtraInformation other = (LinuxExtraInformation) info;
			openFileDescriptors = other.openFileDescriptors;
			openFileDescriptorsLimit = other.openFileDescriptorsLimit;
		}
	}
}
