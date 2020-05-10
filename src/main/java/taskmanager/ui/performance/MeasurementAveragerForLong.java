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

package taskmanager.ui.performance;

import taskmanager.Measurements;

import java.util.Iterator;

public class MeasurementAveragerForLong extends MeasurementAverager<Long> {
	public MeasurementAveragerForLong(Measurements<Long> measurements) {
		super(measurements);
	}

	@Override
	protected Long computeAverage(Iterator<Long> iterator, int stepSize) {
		if (iterator != null && iterator.hasNext()) {
			long total = 0;
			for (int i = 0; i < stepSize; i++) {
				total += iterator.next();
			}
			return total / stepSize;
		}
		return 0L;
	}
}