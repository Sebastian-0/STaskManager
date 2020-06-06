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

package taskmanager.platform.common;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface Nvml extends Library {
	int NVML_SUCCESS = 0;
	int NVML_ERROR_UNINITIALIZED = 1;
	int NVML_ERROR_INVALID_ARGUMENT = 2;
	int NVML_ERROR_NOT_SUPPORTED = 3;
	int NVML_ERROR_NO_PERMISSION = 4;
	int NVML_ERROR_DRIVER_NOT_LOADED = 9;
	int NVML_ERROR_UNKNOWN = 999;

	int NVML_DEVICE_PCI_BUS_ID_BUFFER_SIZE = 32;
	int NVML_DEVICE_PCI_BUS_ID_BUFFER_V2_SIZE = 16;

	int NVML_TEMPERATURE_GPU = 0;

	Nvml INSTANCE = load();

	static Nvml load() {
		Logger logger = LoggerFactory.getLogger(Nvml.class);
		try {
			if (Platform.isLinux()) {
				return Native.load("nvidia-ml", Nvml.class);
			}
			if (Platform.isWindows()) {
				return Native.load("nvml", Nvml.class);
			}
			logger.warn("Unsupported platform for Nvidia ML library");
		} catch (UnsatisfiedLinkError e) {
			logger.error("Nvidia driver not present, perhaps no card is installed?", e);
		}
		return null;
	}

	String nvmlErrorString (int errorCode);

	int nvmlInit();
	int nvmlShutdown();

	int nvmlSystemGetDriverVersion(byte[] version, int length);

	int nvmlDeviceGetCount(IntByReference unitCount);
	int nvmlDeviceGetHandleByIndex(int index, LongByReference deviceHandle);

	int nvmlDeviceGetMemoryInfo(long deviceHandle, Pointer memory);
	int nvmlDeviceGetPciInfo(long deviceHandle, Pointer pci);
	int nvmlDeviceGetUtilizationRates(long deviceHandle, Pointer utilization);
	int nvmlDeviceGetTemperature(long deviceHandle, int sensorType, IntByReference temp);
	int nvmlDeviceGetDecoderUtilization(long deviceHandle, IntByReference utilization, IntByReference samplingPeriodUs);
	int nvmlDeviceGetEncoderUtilization(long deviceHandle, IntByReference utilization, IntByReference samplingPeriodUs);

	@FieldOrder({"total", "free", "used"})
	class nvmlMemory_t extends Structure {
		public long total;
		public long free;
		public long used;
	}

	@FieldOrder({"busIdLegacy", "domain", "bus", "device", "pciDeviceId", "pciSubSystemId", "busId"})
	class nvmlPciInfo_t extends Structure {
		public byte[] busIdLegacy = new byte[NVML_DEVICE_PCI_BUS_ID_BUFFER_V2_SIZE];
		public int domain;
		public int bus;
		public int device;
		public int pciDeviceId;
		public int pciSubSystemId;
		public byte[] busId = new byte[NVML_DEVICE_PCI_BUS_ID_BUFFER_SIZE];
	}

	@FieldOrder({"gpu", "memory"})
	class nvmlUtilization_t extends Structure {
		public int gpu;
		public int memory;
	}
}