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

public class Gpu {
	public enum Type {
		Nvidia,
		Amd,
		Intel,
		Unknown
	}

	public Measurements<Long> usedMemory;
	public Measurements<Long> utilization;
	public Measurements<Long> temperature;

	public Measurements<Long> encoderUtilization;
	public Measurements<Long> decoderUtilization;

	public int index;
	public Type type;
	public String name;
	public String vendor;
	public int deviceId;
	public String driverVersion;
	public long totalMemory;

	public boolean memorySupported;
	public boolean utilizationSupported;
	public boolean temperatureSupported;
	public boolean encoderSupported;
	public boolean decoderSupported;

	public Gpu() {
		usedMemory = new MeasurementContainer<>(0L);
		utilization = new MeasurementContainer<>(0L);
		temperature = new MeasurementContainer<>(0L);
		encoderUtilization = new MeasurementContainer<>(0L);
		decoderUtilization = new MeasurementContainer<>(0L);
	}

	void copyFrom(Gpu other, boolean doFullCopy) {
		if (doFullCopy) {
			usedMemory.copyFrom(other.usedMemory);
			utilization.copyFrom(other.utilization);
			temperature.copyFrom(other.temperature);
			encoderUtilization.copyFrom(other.encoderUtilization);
			decoderUtilization.copyFrom(other.decoderUtilization);
		} else {
			usedMemory.copyDelta(other.usedMemory);
			utilization.copyDelta(other.utilization);
			temperature.copyDelta(other.temperature);
			encoderUtilization.copyDelta(other.encoderUtilization);
			decoderUtilization.copyDelta(other.decoderUtilization);
		}

		index = other.index;
		type = other.type;
		name = other.name;
		vendor = other.vendor;
		driverVersion = other.driverVersion;
		deviceId = other.deviceId;
		totalMemory = other.totalMemory;
		memorySupported = other.memorySupported;
		utilizationSupported = other.utilizationSupported;
		temperatureSupported = other.temperatureSupported;
		encoderSupported = other.encoderSupported;
		decoderSupported = other.decoderSupported;
	}
}
