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

package taskmanager.filter.concrete;

import taskmanager.Process;
import taskmanager.filter.NumericalRangeFilter;

public class CpuFilter extends NumericalRangeFilter {
	public CpuFilter(long lowerBound, long upperBound) {
		super(lowerBound, upperBound);
	}

	@Override
	protected long valueToFilter(Process process) {
		return process.cpuUsage.newest();
	}
}