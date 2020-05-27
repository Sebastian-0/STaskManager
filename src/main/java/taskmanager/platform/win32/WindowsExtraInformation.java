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

import taskmanager.data.ExtraInformation;

public class WindowsExtraInformation implements ExtraInformation {
	public int handles;

	@Override
	public ExtraInformation copy() {
		WindowsExtraInformation extraInformation = new WindowsExtraInformation();
		extraInformation.copyFrom(this);
		return extraInformation;
	}

	@Override
	public void copyFrom(ExtraInformation info) {
		if (info instanceof WindowsExtraInformation) {
			WindowsExtraInformation other = (WindowsExtraInformation) info;
			handles = other.handles;
		}
	}
}
