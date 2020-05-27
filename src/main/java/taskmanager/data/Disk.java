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

package taskmanager.data;

import taskmanager.MeasurementContainer;
import taskmanager.Measurements;

public class Disk {
	public Measurements<Long> writeRate;
	public Measurements<Long> readRate;
	public Measurements<Double> activeFraction; // TODO Replace with long!
	public Measurements<Long> ioQueueLength;

	public int index;
	public String name;
	public String model;
	public long size;

	public Disk() {
		writeRate = new MeasurementContainer<>(0L);
		readRate = new MeasurementContainer<>(0L);
		activeFraction = new MeasurementContainer<>(0d);
		ioQueueLength = new MeasurementContainer<>(0L);
	}

	void copyFrom(Disk other, boolean doFullCopy) {
		if (doFullCopy) {
			writeRate.copyFrom(other.writeRate);
			readRate.copyFrom(other.readRate);
			activeFraction.copyFrom(other.activeFraction);
			ioQueueLength.copyFrom(other.ioQueueLength);
		} else {
			writeRate.copyDelta(other.writeRate);
			readRate.copyDelta(other.readRate);
			activeFraction.copyDelta(other.activeFraction);
			ioQueueLength.copyDelta(other.ioQueueLength);
		}

		index = other.index;
		name = other.name;
		model = other.model;
		size = other.size;
	}
}
