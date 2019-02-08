package taskmanager;

import config.Config;
import oshi.SystemInfo;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HWPartition;
import oshi.hardware.NetworkIF;
import taskmanager.SystemInformation.Disk;
import taskmanager.SystemInformation.Network;

import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

public abstract class InformationLoader {
	private SystemInfo systemInfoLoader;

	private NetworkIF[] networkInterfaces;
	private HWDiskStore[] disks;

	@SuppressWarnings("unchecked")
	public void init(SystemInformation systemInformation) {
		systemInfoLoader = new SystemInfo();

		systemInformation.logicalProcessorCount = systemInfoLoader.getHardware().getProcessor().getLogicalProcessorCount();
		systemInformation.physicalProcessorCount = systemInfoLoader.getHardware().getProcessor().getPhysicalProcessorCount();
		systemInformation.physicalMemoryTotal = systemInfoLoader.getHardware().getMemory().getTotal();
		systemInformation.pageSize = systemInfoLoader.getHardware().getMemory().getPageSize();
		systemInformation.bootTime = System.currentTimeMillis() / 1000 - systemInfoLoader.getHardware().getProcessor().getSystemUptime(); // TODO this is incorrect when you take hibernation into account!

		systemInformation.cpuUsagePerCore = new MeasurementContainer[systemInformation.logicalProcessorCount];
		for (int i = 0; i < systemInformation.cpuUsagePerCore.length; i++) {
			systemInformation.cpuUsagePerCore[i] = new MeasurementContainer<Double>(0d);
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

			try { // TODO Move this to update to continuously add/remove interfaces, how fast is isUp()?
				systemInformation.networks[i].isEnabled = networkInterfaces[i].getNetworkInterface().isUp();
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
		double[] loadPerCore = systemInfoLoader.getHardware().getProcessor().getProcessorCpuLoadBetweenTicks();
		for (int i = 0; i < loadPerCore.length; i++) {
			systemInformation.cpuUsagePerCore[i].addValue(loadPerCore[i]);
		}
		systemInformation.cpuUsageTotal.addValue(systemInfoLoader.getHardware().getProcessor().getSystemCpuLoad());

		final int deadKeepTime = Config.getInt(Config.KEY_DEAD_PROCESS_KEEP_TIME) * 1000;
		systemInformation.deadProcesses.removeIf(process -> System.currentTimeMillis() - process.deathTimestamp > deadKeepTime);

		for (int i = 0; i < networkInterfaces.length; i++) {
			long received = networkInterfaces[i].getBytesRecv();
			long sent = networkInterfaces[i].getBytesSent();
			networkInterfaces[i].updateNetworkStats();
			systemInformation.networks[i].inRate.addValue(networkInterfaces[i].getBytesRecv() - received);
			systemInformation.networks[i].outRate.addValue(networkInterfaces[i].getBytesSent() - sent);
		}

		int i = 0;
		for (HWDiskStore disk : disks) {
			if (disk.getPartitions().length > 0) {
				long w1 = disk.getWriteBytes();
				long r1 = disk.getReadBytes();
				long t1 = disk.getTimeStamp();
				long a1 = disk.getTransferTime();

				boolean diskExists = disk.updateDiskStats();

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
}
