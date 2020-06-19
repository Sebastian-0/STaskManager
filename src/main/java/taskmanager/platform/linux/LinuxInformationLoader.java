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
import oshi.util.FileUtil;
import taskmanager.InformationLoader;
import taskmanager.data.Process;
import taskmanager.data.Status;
import taskmanager.data.SystemInformation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LinuxInformationLoader extends InformationLoader {
	private static final Logger LOGGER = LoggerFactory.getLogger(LinuxInformationLoader.class);

	private static final String PROC_PATH = "/proc";

	private long lastCpuTime;
	private long currentCpuTime;

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
		updateTotalCpuTime();
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

	private void updateTotalCpuTime() {
		lastCpuTime = currentCpuTime;

		List<String> lines = FileUtil.readFile(PROC_PATH + "/stat");
		String[] tokens = lines.get(0).split("\\s+");
		long time = 0;
		for (int i = 1; i < tokens.length; i++) {
			time += Long.parseLong(tokens[i]);
		}

		currentCpuTime = time;
	}

	private void updateProcesses(SystemInformation systemInformation) {
		Set<Long> newProcessIds = fetchProcessIds();

		int totalThreadCount = 0;
		for (Long pid : newProcessIds) {
			Process process = findProcess(systemInformation.processes, pid);
			if (process == null) {
				process = new Process(nextProcessId++, pid);
				systemInformation.processes.add(process);
			}

			String processPath = PROC_PATH + "/" + pid;
			Map<String, String> status = FileUtil.getKeyValueMapFromFile(processPath + "/status", ":");

			if (!process.hasReadOnce) {
				if (!status.isEmpty()) {
					String userId = status.getOrDefault("Uid", "-1").split("\\s+")[0];
					process.userName = UserGroupInfo.getGroupName(userId);
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
						processFileNameAndPathFallback(process, processPath, status);
					}
					process.hasReadOnce = true;
				} else {
					LOGGER.warn("Failed to read /proc/{}/status", process.id);
				}
//				if (process.description.isEmpty())
//					process.description = process.fileName;
			}

			process.privateWorkingSet.addValue(Long.parseLong(removeUnit(status.getOrDefault("RssAnon", "0 kb"))) * 1024);

			String stat = FileUtil.getStringFromFile(processPath + "/stat");
			if (stat.isEmpty()) {
				LOGGER.warn("Failed to read /proc/{}/stat, duplicating previous CPU-values", process.id);
				process.cpuTime.addValue(process.cpuTime.newest());
				process.cpuUsage.addValue(process.cpuUsage.newest());
			} else {
				String[] tokens = stat.split("\\s+");
				long utime = Long.parseLong(tokens[13]);
				long stime = Long.parseLong(tokens[14]);
				// TODO Maybe use a delta of the process uptime (like LinuxOperatingSystem#getProcess():286)?
				process.updateCpu(stime, utime, (currentCpuTime - lastCpuTime), 1); // Set cores to 1 since the total time is already divided by cores

				process.status = parseStatus(tokens[2]);

				totalThreadCount += Integer.parseInt(tokens[19]);
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

	private Set<Long> fetchProcessIds() {
		Set<Long> processIds = new HashSet<>();
		File processDir = new File(PROC_PATH);
		File[] files = processDir.listFiles(f -> f.isDirectory() && f.getName().matches("[0-9]+"));
		if (files != null) {
			for (File file : files) {
				processIds.add(Long.parseLong(file.getName()));
			}
		}
		return processIds;
	}

	private Process findProcess(List<Process> processes, long processId) {
		for (Process process : processes) {
			if (process.id == processId) {
				return process;
			}
		}
		return null;
	}

	private void processFileNameAndPathFallback(Process process, String processPath, Map<String, String> status) {
		String partialName = FileUtil.getStringFromFile(processPath + "/comm");
		partialName = partialName.isEmpty() ? status.getOrDefault("Name", "") : partialName;
		if (partialName.isEmpty() && process.commandLine.isEmpty()) {
			LOGGER.warn("Process {}: Found no partial name in /proc/{}/[comm, status, cmdline], did the process die too quickly?", process.id, process.id);
			return;
		}

		// First, see if the partial name is in the command line, in that case extract it and try to extract the path
		int start = process.commandLine.indexOf(partialName);
		if (start != -1) {
			int startSpace = process.commandLine.lastIndexOf(' ', start);
			int endSpace = process.commandLine.indexOf(' ', start + partialName.length());
			endSpace = (endSpace == -1) ? process.commandLine.length() : endSpace;

			start = process.commandLine.lastIndexOf(partialName, endSpace);
			partialName = process.commandLine.substring(start, endSpace);
			if (partialName.endsWith(":")) {
				partialName = partialName.substring(0, partialName.length() - 1);
			}
			process.fileName = partialName;

			String filePath = process.commandLine.substring(startSpace + 1, endSpace);
			File file = new File(filePath);
			if (file.exists()) {
				process.filePath = filePath;
			}
			return;
		}

		// Secondly if the partial name isn't in the command line, just take the first binary in the path
		int space = process.commandLine.indexOf(' ');
		if (space == -1) {
			space = process.commandLine.length();
		}
		if (space > 0) {
			int separator = process.commandLine.lastIndexOf(File.separator, space);
			process.fileName = process.commandLine.substring(separator + 1, space);
			String filePath = process.commandLine.substring(0, space);
			File file = new File(filePath);
			if (file.exists()) {
				process.filePath = filePath;
			}
			return;
		}

		// Lastly, with no command line this is the best we can do (first 15 chars)
		process.fileName = partialName;
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