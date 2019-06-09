package taskmanager.linux;

import oshi.software.os.linux.LinuxUserGroupInfo;
import oshi.util.FileUtil;
import taskmanager.InformationLoader;
import taskmanager.Process;
import taskmanager.SystemInformation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.*;

public class LinuxInformationLoader extends InformationLoader {
	private static final String PROC_PATH = "/proc";

	private LinuxUserGroupInfo userGroupInfo;

	private long lastCpuTime;
	private long currentCpuTime;

	private long nextProcessId;

	public LinuxInformationLoader() {
		userGroupInfo = new LinuxUserGroupInfo();
	}

	@Override
	public void init(SystemInformation systemInformation) {
		super.init(systemInformation);

		systemInformation.physicalMemoryTotalInstalled = systemInformation.physicalMemoryTotal;
//    systemInformation.reservedMemory = systemInformation.physicalMemoryTotalInstalled - systemInformation.physicalMemoryTotal;

		systemInformation.processes.add(new Process(nextProcessId++, 0));
	}


	@Override
	public void update(SystemInformation systemInformation) {
		super.update(systemInformation);

		updateTotalCpuTime();
		updateProcesses(systemInformation);
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

		for (Long pid : newProcessIds) {
			Process process = findProcess(systemInformation.processes, pid);
			if (process == null) {
				process = new Process(nextProcessId++, pid);
				systemInformation.processes.add(process);
			}

			String processPath = PROC_PATH + "/" + pid;

			if (!process.hasReadOnce) {
				Map<String, String> status = FileUtil.getKeyValueMapFromFile(processPath + "/status", ":");
				if (!status.isEmpty()) {
					String userId = status.getOrDefault("Uid", "-1").split("\\s+")[0];
					process.userName = userGroupInfo.getGroupName(userId);

					process.commandLine = readFileAsString(processPath + "/cmdline").replaceAll("" + (char) 0, " ").trim();
					String partialName = readFileAsString(processPath + "/comm");
					if (partialName.isEmpty()) {
						System.out.println("Found no name: " + status.getOrDefault("Name", "Not found!"));
					} else {
						if (process.commandLine.isEmpty()) {
							process.fileName = partialName;
						} else {
							int start = process.commandLine.indexOf(partialName);
							if (start == -1) {
								System.out.println("Error? " + partialName + " vs. " + process.commandLine);
							} else {
								int end = process.commandLine.indexOf(' ', start + partialName.length());
								end = (end == -1) ? process.commandLine.length() : end;
								process.fileName = process.commandLine.substring(start, end);
								if (process.fileName.endsWith(":")) {
									process.fileName = process.fileName.substring(0, process.fileName.length() - 1);
								}
							}
						}
					}

					try {
						File target = new File("/proc/" + process.id + "/exe");
						if (target.exists()) {
							Path absolutePath = Files.readSymbolicLink(target.toPath()).toAbsolutePath();
							process.filePath = absolutePath.toString();
							process.fileName = absolutePath.getFileName().toString();
						}
					} catch (NoSuchFileException e) {
						System.out.println("The exe file for " + process.id + " is invalid!");
						System.err.println("The exe file for " + process.id + " is invalid!");
					} catch (IOException e) {
						e.printStackTrace();
					}
					process.hasReadOnce = true;
				} else {
					System.out.println("Failed to read status for: " + process.fileName);
				}
//				if (process.description.isEmpty())
//					process.description = process.fileName;
			}

			Map<String, String> status = FileUtil.getKeyValueMapFromFile(processPath + "/status", ":");
			process.privateWorkingSet.addValue(Long.parseLong(removeUnit(status.getOrDefault("RssAnon", "0 kb"))) * 1024);

			int attempts = 0;
			String stat = "";
			while (stat.isEmpty() && attempts < 100) {
				stat = FileUtil.getStringFromFile(processPath + "/stat");
				attempts += 1;
			}
			if (attempts > 1) {
				System.out.println("Did several attempts to open file: " + attempts);
			}
			if (stat.isEmpty()) {
				System.out.println("File was empty, did more than 100 attempts!");
				process.cpuTime.addValue(process.cpuTime.newest());
				process.cpuUsage.addValue(process.cpuUsage.newest());
			} else {
				String[] tokens = stat.split("\\s+");
				long utime = Long.parseLong(tokens[13]);
				long stime = Long.parseLong(tokens[14]);
				process.updateCpu(stime, utime, (currentCpuTime - lastCpuTime), 1); // Set cores to 1 since the total time is already divided by cores
			}
		}

		// Remove old processes
		ListIterator<Process> itr = systemInformation.processes.listIterator();
		while (itr.hasNext()) {
			Process process = itr.next();
			if (!newProcessIds.contains(process.id)) {
				process.isDead = true;
				process.deathTimestamp = System.currentTimeMillis();
				itr.remove();
				systemInformation.deadProcesses.add(process);
			}
		}
	}

	private String removeUnit(String vmRSS) {
		return vmRSS.substring(0, vmRSS.length() - 3);
	}


	private String readFileAsString(String path) {
		try {
			List<String> lines = Files.readAllLines(new File(path).toPath());
			if (lines.isEmpty())
				return "";
			return lines.get(0);
		} catch (IOException e) {
			System.out.println("readFileAsString(): Failed to read file: " + path);
		}
		return "";
	}

	private Set<Long> fetchProcessIds() {
		Set<Long> processIds = new HashSet<>();
		File processDir = new File(PROC_PATH);
		for (File file : processDir.listFiles()) {
			String fileName = file.getName();
			if (file.isDirectory() && fileName.matches("[0-9]+")) {
				Long pid = Long.parseLong(fileName);
				processIds.add(pid);
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
}
