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

import com.sun.jna.Memory;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import taskmanager.SystemInformation;
import taskmanager.SystemInformation.Gpu;
import taskmanager.SystemInformation.Gpu.Type;
import taskmanager.platform.common.Nvml.nvmlMemory_t;
import taskmanager.platform.common.Nvml.nvmlPciInfo_t;
import taskmanager.platform.common.Nvml.nvmlUtilization_t;
import taskmanager.ui.TaskManager;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class NvidiaGpuLoader {
	private static final Logger LOGGER = LoggerFactory.getLogger(NvidiaGpuLoader.class);

	public void update(SystemInformation systemInformation) {
		try {
			doUpdate(systemInformation);
		} catch (IllegalArgumentException e) {
			LOGGER.error("Failed to load NVIDIA GPU information", e);
		}
	}

	private void doUpdate(SystemInformation systemInformation) {
		if (Nvml.INSTANCE != null) {
			check(Nvml.INSTANCE.nvmlInit());
			try {
				byte[] version = new byte[80];
				check(Nvml.INSTANCE.nvmlSystemGetDriverVersion(version, version.length));
				int idx = 0;
				for (int i = 0; i < version.length; i++) {
					if (version[i] == 0) {
						idx = i-1;
						break;
					}
				}
				String driverVersion = new String(version, 0, idx, StandardCharsets.US_ASCII);

				IntByReference deviceCount = new IntByReference();
				check(Nvml.INSTANCE.nvmlDeviceGetCount(deviceCount));

				for (int i = 0; i < deviceCount.getValue(); i++) {
					LongByReference gpuHandle = new LongByReference();
					check(Nvml.INSTANCE.nvmlDeviceGetHandleByIndex(i, gpuHandle));

					Memory mem = new Memory(new nvmlPciInfo_t().size());
					check(Nvml.INSTANCE.nvmlDeviceGetPciInfo(gpuHandle.getValue(), mem));
					nvmlPciInfo_t pci = Structure.newInstance(nvmlPciInfo_t.class, mem);
					pci.read();

					int deviceId = pci.pciDeviceId >> 16;
					Gpu gpu = Arrays.stream(systemInformation.gpus)
									.filter(g -> g.deviceId == deviceId)
									.findAny()
									.orElse(null);

					if (gpu == null) {
						LOGGER.warn("Failed to find matching GPU for pci device: {}", Integer.toHexString(pci.pciDeviceId));
						continue;
					}

					gpu.type = Type.Nvidia;
					gpu.isSupported = true;
					gpu.driverVersion = driverVersion;

					// Read memory usage
					mem = new Memory(new nvmlMemory_t().size());
					check(Nvml.INSTANCE.nvmlDeviceGetMemoryInfo(gpuHandle.getValue(), mem));
					nvmlMemory_t memory = Structure.newInstance(nvmlMemory_t.class, mem);
					memory.read();

					gpu.totalMemory = memory.total;
					gpu.usedMemory.addValue(memory.used);

					// Read utilization
					mem = new Memory(new nvmlUtilization_t().size());
					check(Nvml.INSTANCE.nvmlDeviceGetUtilizationRates(gpuHandle.getValue(), mem));
					nvmlUtilization_t utilization = Structure.newInstance(nvmlUtilization_t.class, mem);
					utilization.read();

					gpu.utilization.addValue((long) (utilization.gpu * Config.DOUBLE_TO_LONG / 100));

					// Read temperature
					IntByReference temperature = new IntByReference();
					check(Nvml.INSTANCE.nvmlDeviceGetTemperature(gpuHandle.getValue(), Nvml.NVML_TEMPERATURE_GPU, temperature));

					gpu.temperature.addValue((long) temperature.getValue());

					// Read encoder utilization
					IntByReference encoderUtilization = new IntByReference();
					check(Nvml.INSTANCE.nvmlDeviceGetEncoderUtilization(gpuHandle.getValue(), encoderUtilization, new IntByReference()));
					gpu.encoderUtilization.addValue((long) (encoderUtilization.getValue() * Config.DOUBLE_TO_LONG / 100));

					// Read decoder utilization
					IntByReference decoderUtilization = new IntByReference();
					check(Nvml.INSTANCE.nvmlDeviceGetDecoderUtilization(gpuHandle.getValue(), decoderUtilization, new IntByReference()));
					gpu.decoderUtilization.addValue((long) (decoderUtilization.getValue() * Config.DOUBLE_TO_LONG / 100));
				}
			} finally {
				check(Nvml.INSTANCE.nvmlShutdown());
			}
		}
	}

	private void check(int code) {
		if (code != Nvml.NVML_SUCCESS) {
			String error = Nvml.INSTANCE.nvmlErrorString(code);
			throw new IllegalStateException("NVML operation failed: " + error + " (code " + code + ")");
		}
	}
}