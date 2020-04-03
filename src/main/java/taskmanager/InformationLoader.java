package taskmanager;

import config.Config;
import oshi.SystemInfo;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;
import oshi.hardware.NetworkIF;
import taskmanager.SystemInformation.Disk;
import taskmanager.SystemInformation.Network;
import taskmanager.SystemInformation.TopList;

import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

public abstract class InformationLoader {
	private SystemInfo systemInfoLoader;

	private NetworkIF[] networkInterfaces;
	private HWDiskStore[] disks;

	private long[][] lastCpuLoadTicksPerCore;
	private long[] lastCpuLoadTicks;

	private int numberOfUpdates;

	@SuppressWarnings("unchecked")
	public void init(SystemInformation systemInformation) {
		systemInfoLoader = new SystemInfo();

		systemInformation.logicalProcessorCount = systemInfoLoader.getHardware().getProcessor().getLogicalProcessorCount();
		systemInformation.physicalProcessorCount = systemInfoLoader.getHardware().getProcessor().getPhysicalProcessorCount();
		systemInformation.physicalMemoryTotal = systemInfoLoader.getHardware().getMemory().getTotal();
		systemInformation.pageSize = systemInfoLoader.getHardware().getMemory().getPageSize();
		systemInformation.bootTime = System.currentTimeMillis() / 1000 - systemInfoLoader.getOperatingSystem().getSystemUptime(); // TODO this is incorrect when you take hibernation into account!

		systemInformation.cpuUsagePerCore = new MeasurementContainer[systemInformation.logicalProcessorCount];
		for (int i = 0; i < systemInformation.cpuUsagePerCore.length; i++) {
			systemInformation.cpuUsagePerCore[i] = new MeasurementContainer<>((short) 0);
		}

		networkInterfaces = systemInfoLoader.getHardware().getNetworkIFs();
		systemInformation.networks = new Network[networkInterfaces.length];
		for (int i = 0; i < networkInterfaces.length; i++) {
			systemInformation.networks[i] = new Network();
			systemInformation.networks[i].macAddress = networkInterfaces[i].getMacaddr(); // TODO Update this info periodically with networkInterfaces[i].setNetworkInterface(), should happen seldom though
			systemInformation.networks[i].ipv4Addresses = networkInterfaces[i].getIPv4addr(); // ^
			systemInformation.networks[i].ipv6Addresses = networkInterfaces[i].getIPv6addr(); //-|
			systemInformation.networks[i].name = networkInterfaces[i].getDisplayName();       //-|
			systemInformation.networks[i].compactIpv6();

			// TODO shj: Try using networkInterfaces[i].isConnectorPresent instead?
			try { // TODO Move this to update to continuously add/remove interfaces, how fast is isUp()?
				systemInformation.networks[i].isEnabled = networkInterfaces[i].queryNetworkInterface().isUp();
			} catch (SocketException e) {
				e.printStackTrace();
			}
		}

		disks = systemInfoLoader.getHardware().getDiskStores();
		List<Disk> diskList = new ArrayList<>();
		for (int i = 0; i < disks.length; i++) {
			Disk disk = new Disk();
			disk.index = i;
			disk.size = disks[i].getSize();
			disk.model = disks[i].getModel();
			for (HWPartition partition : disks[i].getPartitions()) { // TODO Use UIID of the first partition to identify disks when new are added/removed
				String name = partition.getMountPoint();
				if (!name.isEmpty()) {
					if (!name.equals("/")) {
						while (name.endsWith("\\") || name.endsWith("/")) {
							name = name.substring(0, name.length() - 1);
						}
					}
					disk.name = name;

					break;
				}
			}
			if (disks[i].getPartitions().length > 0) {
				diskList.add(disk);
			}
		}

		systemInformation.disks = diskList.toArray(new Disk[0]);
	}

	public void update(SystemInformation systemInformation) {
		systemInformation.uptime = System.currentTimeMillis() / 1000 - systemInformation.bootTime;
		systemInformation.physicalMemoryUsed.addValue(systemInformation.physicalMemoryTotal - systemInfoLoader.getHardware().getMemory().getAvailable());

		// Update the CPU usage
		double[] loadPerCore;
		if (lastCpuLoadTicksPerCore == null) {
			loadPerCore = new double[systemInformation.cpuUsagePerCore.length];
		} else {
			loadPerCore = systemInfoLoader.getHardware().getProcessor().getProcessorCpuLoadBetweenTicks(lastCpuLoadTicksPerCore);
		}
		for (int i = 0; i < loadPerCore.length; i++) {
			systemInformation.cpuUsagePerCore[i].addValue((short) Math.round(loadPerCore[i] * Config.DOUBLE_TO_LONG));
		}

		if (lastCpuLoadTicks == null) {
			systemInformation.cpuUsageTotal.addValue((short) 0);
		} else {
			systemInformation.cpuUsageTotal.addValue(
					(short) Math.round(systemInfoLoader.getHardware().getProcessor().getSystemCpuLoadBetweenTicks(lastCpuLoadTicks) * Config.DOUBLE_TO_LONG));
		}

		lastCpuLoadTicksPerCore = systemInfoLoader.getHardware().getProcessor().getProcessorCpuLoadTicks();
		lastCpuLoadTicks = systemInfoLoader.getHardware().getProcessor().getSystemCpuLoadTicks();

		final int deadKeepTime = Config.getInt(Config.KEY_DEAD_PROCESS_KEEP_TIME) * 1000;
		systemInformation.deadProcesses.removeIf(process -> System.currentTimeMillis() - process.deathTimestamp > deadKeepTime);

		updateNetworkInterfaces(systemInformation);
		updateDisks(systemInformation);

		if (numberOfUpdates > 0) {
			updateTopLists(systemInformation);
		}

		numberOfUpdates++;
	}

	private void updateNetworkInterfaces(SystemInformation systemInformation) {
		for (int i = 0; i < networkInterfaces.length; i++) {
			long received = networkInterfaces[i].getBytesRecv();
			long sent = networkInterfaces[i].getBytesSent();
			networkInterfaces[i].updateAttributes();
			systemInformation.networks[i].inRate.addValue(networkInterfaces[i].getBytesRecv() - received);
			systemInformation.networks[i].outRate.addValue(networkInterfaces[i].getBytesSent() - sent);
		}
	}

	private void updateDisks(SystemInformation systemInformation) {
		int i = 0;
		for (HWDiskStore disk : disks) {
			if (disk.getPartitions().length > 0) {
				long w1 = disk.getWriteBytes();
				long r1 = disk.getReadBytes();
				long t1 = disk.getTimeStamp();
				long a1 = disk.getTransferTime();

				boolean diskExists = disk.updateAtrributes();

				long w2 = disk.getWriteBytes();
				long r2 = disk.getReadBytes();
				long t2 = disk.getTimeStamp();
				long a2 = disk.getTransferTime();

				if (diskExists) {
					systemInformation.disks[i].activeFraction.addValue(Math.max(0, (a2 - a1) / (double) (t2 - t1)));
					systemInformation.disks[i].writeRate.addValue(w2 - w1);
					systemInformation.disks[i].readRate.addValue(r2 - r1);
					systemInformation.disks[i].ioQueueLength.addValue(disk.getCurrentQueueLength());
				}

				i++;
			}
		}
	}

	private void updateTopLists(SystemInformation systemInformation) {
		final int topListSize = Config.getInt(Config.KEY_METRIC_TOP_LIST_SIZE);

		// Cpu
		systemInformation.processes.sort((p1, p2) -> (int) (p2.cpuUsage.newest() - p1.cpuUsage.newest()));
		TopList cpuTopList = TopList.of(p -> p.cpuUsage.newest(), systemInformation.processes, topListSize);
		systemInformation.cpuTopList.addValue(cpuTopList);

		// Memory
		systemInformation.processes.sort((p1, p2) -> signum(p2.privateWorkingSet.newest() - p1.privateWorkingSet.newest()));
		TopList memoryTopList = TopList.of(p -> p.privateWorkingSet.newest(), systemInformation.processes, topListSize);
		systemInformation.physicalMemoryTopList.addValue(memoryTopList);
	}

	private static int signum(long value) {
		if (value > 0) {
			return 1;
		} else if (value < 0) {
			return -1;
		}
		return 0;
	}
}
