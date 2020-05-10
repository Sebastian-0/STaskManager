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

package taskmanager.filter;

import taskmanager.Process;

public abstract class NumericalRangeFilter implements Filter {
	private final long lowerBound;
	private final long upperBound;

	public NumericalRangeFilter(long lowerBound, long upperBound) {
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
	}

	@Override
	public boolean apply(Process process) {
		long value = valueToFilter(process);
		return value >= lowerBound && value <= upperBound;
	}

	protected abstract long valueToFilter(Process process);
}