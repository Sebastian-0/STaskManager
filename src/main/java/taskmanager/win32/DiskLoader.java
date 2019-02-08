package taskmanager.win32;

import com.sun.jna.platform.win32.PdhUtil;
import com.sun.jna.platform.win32.PdhUtil.PdhEnumObjectItems;
import com.sun.jna.platform.win32.PdhUtil.PdhException;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiQuery;
import com.sun.jna.platform.win32.COM.WbemcliUtil.WmiResult;

import oshi.util.platform.windows.PerfDataUtil;
import oshi.util.platform.windows.PerfDataUtil.PerfCounter;
import oshi.util.platform.windows.WmiQueryHandler;
import oshi.util.platform.windows.WmiUtil;
import taskmanager.SystemInformation;
import taskmanager.SystemInformation.Disk;

public class DiskLoader {
	private static final String PHYSICAL_DISK = "PhysicalDisk";
	private static final String TOTAL = "_Total";

	public enum PhysicalDiskProperties {
		NAME, CAPTION, DISKREADBYTESPERSEC, DISKWRITEBYTESPERSEC, PERCENTDISKTIME, PERCENTIDLETIME;
	}

	private PerfCounter[] diskReadBytesCounter;
	private PerfCounter[] diskWriteBytesCounter;
	private PerfCounter[] diskIdleCounter;
	private PerfCounter[] diskActiveCounter;
	private WmiQuery<PhysicalDiskProperties> physicalDiskQuery;

	private long[] previousReadBytes;
	private long[] previousWriteBytes;
	private long[] previousIdleTime;
	private long[] previousActiveTime;

	public void init(SystemInformation info) {
		boolean performanceCountersWorked = true;
		try {
			PdhEnumObjectItems objectItems = PdhUtil.PdhEnumObjectItems(null, null, PHYSICAL_DISK, 100);
			if (!objectItems.getInstances().isEmpty()) {
				int size = objectItems.getInstances().size() - 1;
				diskReadBytesCounter = new PerfCounter[size];
				diskWriteBytesCounter = new PerfCounter[size];
				diskIdleCounter = new PerfCounter[size];
				diskActiveCounter = new PerfCounter[size];
				info.disks = new Disk[size];
				previousReadBytes = new long[size];
				previousWriteBytes = new long[size];
				previousIdleTime = new long[size];
				previousActiveTime = new long[size];

				int idx = 0;
				for (String instance : objectItems.getInstances()) {
					if (!instance.equals(TOTAL)) {
						info.disks[idx] = new Disk();
						info.disks[idx].index = Integer.parseInt(instance.split("\\s+")[0]);

						diskReadBytesCounter[idx] = PerfDataUtil.createCounter(PHYSICAL_DISK, instance, "Disk Read Bytes/sec");
						if (!PerfDataUtil.addCounterToQuery(diskReadBytesCounter[idx])) {
							throw new PdhException(0);
						}

						diskWriteBytesCounter[idx] = PerfDataUtil.createCounter(PHYSICAL_DISK, instance, "Disk Write Bytes/sec");
						if (!PerfDataUtil.addCounterToQuery(diskWriteBytesCounter[idx])) {
							throw new PdhException(0);
						}

						diskIdleCounter[idx] = PerfDataUtil.createCounter(PHYSICAL_DISK, instance, "% Idle Time");
						if (!PerfDataUtil.addCounterToQuery(diskIdleCounter[idx])) {
							throw new PdhException(0);
						}

						diskActiveCounter[idx] = PerfDataUtil.createCounter(PHYSICAL_DISK, instance, "% Disk Time");
						if (!PerfDataUtil.addCounterToQuery(diskActiveCounter[idx])) {
							throw new PdhException(0);
						}

						idx++;
					}
				}
			}
		} catch (PdhException e) {
			System.out.println("init(): Failed to create performance counters for " + PHYSICAL_DISK);
			performanceCountersWorked = false;
		}

		if (!performanceCountersWorked) {
			PerfDataUtil.removeAllCounters(PHYSICAL_DISK);
			this.diskReadBytesCounter = null;
			this.diskWriteBytesCounter = null;
			this.diskIdleCounter = null;
			this.diskActiveCounter = null;
			physicalDiskQuery = new WmiQuery<>("Win32_PerfRawData_PerfDisk_PhysicalDisk", PhysicalDiskProperties.class);

			WmiResult<PhysicalDiskProperties> drives = WmiQueryHandler.getInstance().queryWMI(this.physicalDiskQuery);
			info.disks = new Disk[drives.getResultCount() - 1];
			previousReadBytes = new long[drives.getResultCount() - 1];
			previousWriteBytes = new long[drives.getResultCount() - 1];
			previousIdleTime = new long[drives.getResultCount() - 1];
			previousActiveTime = new long[drives.getResultCount() - 1];

			int idx = 0;
			for (int i = 0; i < drives.getResultCount(); i++) {
				String name = WmiUtil.getString(drives, PhysicalDiskProperties.NAME, i);
				if (!name.equals(TOTAL)) {
					info.disks[idx] = new Disk();
					info.disks[idx].index = Integer.parseInt(name.split("\\s+")[0]);
					idx++;
				}
			}
		}
	}

	public void update(SystemInformation systemInformation) {
		if (physicalDiskQuery == null) {
			for (int i = 0; i < systemInformation.disks.length; i++) {
				PerfDataUtil.updateQuery(PHYSICAL_DISK);
				long newReads = PerfDataUtil.queryCounter(diskReadBytesCounter[i]);
				long newWrites = PerfDataUtil.queryCounter(diskWriteBytesCounter[i]);
				long newPercentIdle = PerfDataUtil.queryCounter(diskIdleCounter[i]);
				long newPercentActive = PerfDataUtil.queryCounter(diskActiveCounter[i]);

				updateDisk(systemInformation.disks, i, newReads, newWrites, newPercentIdle, newPercentActive);
			}
		} else {
			WmiResult<PhysicalDiskProperties> drives = WmiQueryHandler.getInstance().queryWMI(this.physicalDiskQuery);
			for (int i = 0; i < drives.getResultCount(); i++) {
				String name = WmiUtil.getString(drives, PhysicalDiskProperties.NAME, i);
				if (!name.equals(TOTAL)) {
					int index = Integer.parseInt(name.split("\\s+")[0]);
					for (int j = 0; j < systemInformation.disks.length; j++) {
						if (systemInformation.disks[j].index == index) {
							long newReads = WmiUtil.getUint64(drives, PhysicalDiskProperties.DISKREADBYTESPERSEC, i);
							long newWrites = WmiUtil.getUint64(drives, PhysicalDiskProperties.DISKWRITEBYTESPERSEC, i);
							long newPercentIdle = WmiUtil.getUint64(drives, PhysicalDiskProperties.PERCENTIDLETIME, i);
							long newPercentActive = WmiUtil.getUint64(drives, PhysicalDiskProperties.PERCENTDISKTIME, i);

							updateDisk(systemInformation.disks, j, newReads, newWrites, newPercentIdle, newPercentActive);
						}
					}
				}
			}
		}
	}

	private void updateDisk(Disk[] disks, int index, long newReads, long newWrites, long newPercentIdle, long newPercentActive) {
		long deltaReads = newReads - previousReadBytes[index];
		long deltaWrites = newWrites - previousWriteBytes[index];
		long deltaIdleTime = newPercentIdle - previousIdleTime[index];
		long deltaActiveTime = newPercentActive - previousActiveTime[index];

		if (previousActiveTime[index] != 0 || previousIdleTime[index] != 0) {
			disks[index].readRate.addValue(deltaReads);
			disks[index].writeRate.addValue(deltaWrites);
			disks[index].activeFraction.addValue(deltaActiveTime / (double) (deltaActiveTime + deltaIdleTime));
		}

		previousReadBytes[index] = newReads;
		previousWriteBytes[index] = newWrites;
		previousIdleTime[index] = newPercentIdle;
		previousActiveTime[index] = newPercentActive;
	}
}
