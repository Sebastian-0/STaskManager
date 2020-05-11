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

import taskmanager.data.Process;
import taskmanager.filter.NumericalRangeFilter;

public class DeathTimeFilter extends NumericalRangeFilter {
	public DeathTimeFilter(long lowerBound, long upperBound) {
		super(lowerBound, upperBound);
	}

	@Override
	public boolean apply(Process process) {
		return super.apply(process) || process.deathTimestamp == 0;
	}

	@Override
	protected long valueToFilter(Process process) {
		return System.currentTimeMillis() - process.deathTimestamp;
	}
}