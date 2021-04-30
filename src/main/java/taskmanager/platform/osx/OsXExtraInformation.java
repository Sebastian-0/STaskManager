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

package taskmanager.platform.osx;

import taskmanager.data.ExtraInformation;

public class OsXExtraInformation implements ExtraInformation<OsXExtraInformation> {
	public long openFileDescriptors;
	public long openFileDescriptorsLimit;

	public long bufferMemory; // TODO replace these fields with proper memory fields or remove them?
	public long cacheMemory;
	public long sharedMemory;

	public long swapSize;
	public long swapUsed;


	@Override
	public OsXExtraInformation copy() {
		OsXExtraInformation extraInformation = new OsXExtraInformation();
		extraInformation.copyFrom(this);
		return extraInformation;
	}

	@Override
	public void copyFrom(OsXExtraInformation other) {
		openFileDescriptors = other.openFileDescriptors;
		openFileDescriptorsLimit = other.openFileDescriptorsLimit;

		bufferMemory = other.bufferMemory;
		cacheMemory = other.cacheMemory;
		sharedMemory = other.sharedMemory;

		swapSize = other.swapSize;
		swapUsed = other.swapUsed;
	}
}
