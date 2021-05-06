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

package taskmanager.platform.linux;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.driver.linux.proc.UserGroupInfo;
import oshi.software.os.linux.LinuxOperatingSystem;
import oshi.util.FileUtil;
import taskmanager.InformationLoader;
import taskmanager.data.Process;
import taskmanager.data.Status;
import taskmanager.data.SystemInformation;
import taskmanager.platform.common.FileNameUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class LinuxInformationLoader extends InformationLoader {
	private static final Logger LOGGER = LoggerFactory.getLogger(LinuxInformationLoader.class);

	private static final String PROC_PATH = "/proc";

	private long nextProcessId;

	@Override
	public void init(SystemInformation systemInformation) {
		super.init(systemInformation);

		systemInformation.extraInformation = new LinuxExtraInformation();
		systemInformation.physicalMemoryTotalInstalled = systemInformation.physicalMemoryTotal;
	}

	@Override
	public void update(SystemInformation systemInformation) {
		super.update(systemInformation);

		updateMemory(systemInformation);
		updateProcesses(systemInformation);
	}

	private void updateMemory(SystemInformation systemInformation) {
		Map<String, String> memInfo = FileUtil.getKeyValueMapFromFile(PROC_PATH + "/meminfo", ":");
		if (memInfo.isEmpty()) {
			LOGGER.warn("Failed to read /proc/meminfo!");
		} else {
			// TODO Memory graph uses memory available while the composition uses memory available as defined by free
			//  (not the same). Find out how linux defines memory available? Is it even possible?
			LinuxExtraInformation extraInformation = (LinuxExtraInformation) systemInformation.extraInformation;
			systemInformation.freeMemory = Long.parseLong(removeUnit(memInfo.get("MemFree"))) * 1024;
			extraInformation.bufferMemory = Long.parseLong(removeUnit(memInfo.get("Buffers"))) * 1024;
			extraInformation.cacheMemory = Long.parseLong(removeUnit(memInfo.get("Cached"))) * 1024 + Long.parseLong(removeUnit(memInfo.get("SReclaimable"))) * 1024;
			extraInformation.sharedMemory = Long.parseLong(removeUnit(memInfo.get("Shmem"))) * 1024;

			extraInformation.swapSize = Long.parseLong(removeUnit(memInfo.get("SwapTotal"))) * 1024;
			extraInformation.swapUsed = extraInformation.swapSize - Long.parseLong(removeUnit(memInfo.get("SwapFree"))) * 1024;
		}
	}

	private void updateProcesses(SystemInformation systemInformation) {
		Set<Long> newProcessIds = fetchProcessIds();
		createMissingProcessObjects(systemInformation, newProcessIds);

		int totalThreadCount = 0;
		for (Long pid : newProcessIds) {
			Process process = systemInformation.getProcessById(pid);
			try {
				String processPath = PROC_PATH + "/" + pid;
				Map<String, String> status = FileUtil.getKeyValueMapFromFile(processPath + "/status", ":");
				if (status.isEmpty()) {
					LOGGER.warn("Failed to read /proc/{}/status", process.id);
				}

				String[] stat = FileUtil.getStringFromFile(processPath + "/stat")
						.replaceAll("\\(.*\\)", "(cmd)") // Take care of spaces in the process comm
						.split("\\s+");

				if (!process.hasReadOnce) {
					if (!status.isEmpty()) {
						String userId = status.getOrDefault("Uid", "-1").split("\\s+")[0];
						process.userName = UserGroupInfo.getUser(userId);
						process.commandLine = FileUtil.getStringFromFile(processPath + "/cmdline").replaceAll("" + (char) 0, " ").trim();

						// Read process name and path
						try {
							File target = new File("/proc/" + process.id + "/exe");
							if (target.exists()) {
								Path absolutePath = Files.readSymbolicLink(target.toPath()).toAbsolutePath();
								process.filePath = absolutePath.toString();
								process.fileName = absolutePath.getFileName().toString();
							}
						} catch (IOException e) {
							LOGGER.warn("Failed to read /proc/{}/exe", process.id, e);
						}

						// Fallback for file name/path
						if (process.fileName.isEmpty()) {
							String partialName = FileUtil.getStringFromFile(processPath + "/comm");
							partialName = partialName.isEmpty() ? status.getOrDefault("Name", "") : partialName;
							if (!FileNameUtil.setProcessPathAndNameFromCommandLine(process, partialName)) {
								LOGGER.warn("Process {}: Found no partial name in /proc/{}/[comm, status, cmdline], did the process die too quickly?", process.id, process.id);
							}
						}
						process.hasReadOnce = true;
					}

					if (stat.length > 21) {
						process.startTimestamp = systemInformation.bootTime + Long.parseLong(stat[21]) * 1000 / LinuxOperatingSystem.getHz();
					}

					if (stat.length > 3) {
						long parentId = Long.parseLong(stat[3]);
						Process parent = systemInformation.getProcessById(parentId);
						if (parent != null) {
							process.parentUniqueId = parent.uniqueId;
							process.parentId = parentId;
						} else {
							process.parentUniqueId = -1;
							process.parentId = -1;
						}
					}

//				if (process.description.isEmpty())
//					process.description = process.fileName;
				}

				process.privateWorkingSet.addValue(Long.parseLong(removeUnit(status.getOrDefault("RssAnon", "0 kb"))) * 1024);

				if (stat.length < 20) {
					LOGGER.warn("Failed to read /proc/{}/stat, duplicating previous CPU-values", process.id);
					process.cpuTime.addValue(process.cpuTime.newest());
					process.cpuUsage.addValue(process.cpuUsage.newest());
				} else {
					long utime = Long.parseLong(stat[13]) * 1000 / LinuxOperatingSystem.getHz();
					long stime = Long.parseLong(stat[14]) * 1000 / LinuxOperatingSystem.getHz();
					process.updateCpu(stime, utime, systemInformation.logicalProcessorCount);

					process.status = parseStatus(stat[2]);

					totalThreadCount += Integer.parseInt(stat[19]);
				}
			} catch (Throwable e) {
				LOGGER.error("Exception when updating process '{}' ({})!", process.fileName, pid, e);
				process.hasReadOnce = false; // Force full write at next update so that no data is missing
			}
		}

		// Remove old processes
		updateDeadProcesses(systemInformation, newProcessIds);

		systemInformation.totalProcesses = newProcessIds.size();
		systemInformation.totalThreads = totalThreadCount;

		String fileNr = FileUtil.getStringFromFile("/proc/sys/fs/file-nr");
		if (fileNr.isEmpty()) {
			LOGGER.warn("Failed to read /proc/sys/fs/file-nr!");
		} else {
			LinuxExtraInformation extraInformation = (LinuxExtraInformation) systemInformation.extraInformation;
			extraInformation.openFileDescriptors = Long.parseLong(fileNr.split("\\s+")[0]);
			extraInformation.openFileDescriptorsLimit = Long.parseLong(fileNr.split("\\s+")[2]);
		}
	}

	private void createMissingProcessObjects(SystemInformation systemInformation, Set<Long> newProcessIds) {
		for (Long pid : newProcessIds) {
			Process process = systemInformation.getProcessById(pid);
			if (process == null) {
				systemInformation.processes.add(new Process(nextProcessId++, pid));
			}
		}
	}

	private Set<Long> fetchProcessIds() {
		Set<Long> processIds = new LinkedHashSet<>();
		File processDir = new File(PROC_PATH);
		File[] files = processDir.listFiles(f -> f.isDirectory() && f.getName().matches("[0-9]+"));
		if (files != null) {
			for (File file : files) {
				processIds.add(Long.parseLong(file.getName()));
			}
		}
		return processIds;
	}

	private String removeUnit(String value) {
		if (!value.toLowerCase().endsWith("kb")) {
			throw new IllegalArgumentException("Currently only supports the kb unit, error parsing: " + value);
		}
		return value.substring(0, value.length() - 3);
	}

	private Status parseStatus(String token) {
		switch (token.toUpperCase()) {
			case "D":
				return Status.Waiting;
			case "Z":
				return Status.Zombie;
			case "T":
				return Status.Suspended;
			case "X":
				return Status.Dead;
			default:
				return Status.Running;
		}
	}
}