package taskmanager;

import config.Config;

import java.text.Collator;
import java.util.Comparator;

public class Process {
	public long uniqueId;
	public long id;
	public Measurements<Long> privateWorkingSet;
	public String fileName;
	public String filePath;
	public String commandLine;
	public String description;
	public String userName;
	public Measurements<Long> cpuUsage;
	public Measurements<Long> cpuTime;
	public boolean isDead;
	public long deathTimestamp;

	public boolean hasReadOnce;

	private long lastSysCpu;
	private long lastUserCpu;

	public Process(long uniqueId, long id) {
		this.uniqueId = uniqueId;
		this.id = id;
		privateWorkingSet = new MeasurementContainer<>(0L);
		fileName = "";
		filePath = "";
		commandLine = "";
		description = "";
		userName = "Unknown";
		cpuUsage = new MeasurementContainer<>(0L);
		cpuTime = new MeasurementContainer<>(0L);
	}

	public void copyFrom(Process other, boolean doFullCopy) {
		uniqueId = other.uniqueId;
		id = other.id;
		fileName = other.fileName;
		filePath = other.filePath;
		commandLine = other.commandLine;
		description = other.description;
		userName = other.userName;
		isDead = other.isDead;
		deathTimestamp = other.deathTimestamp;

		if (doFullCopy) {
			privateWorkingSet.copyFrom(other.privateWorkingSet);
			cpuUsage.copyFrom(other.cpuUsage);
			cpuTime.copyFrom(other.cpuTime);
		} else {
			privateWorkingSet.copyDelta(other.privateWorkingSet);
			cpuUsage.copyDelta(other.cpuUsage);
			cpuTime.copyDelta(other.cpuTime);
		}

		hasReadOnce = other.hasReadOnce;

		lastSysCpu = other.lastSysCpu;
		lastUserCpu = other.lastUserCpu;
	}

	public void updateCpu(long sysCpu, long userCpu, long totalCpuDelta, int numCores) {
		if (lastSysCpu != 0 || lastUserCpu != 0) {
			long newCpuTime = sysCpu - lastSysCpu + userCpu - lastUserCpu;
			cpuTime.addValue(newCpuTime);
			cpuUsage.addValue(Math.round(newCpuTime / (double) totalCpuDelta / numCores * Config.DOUBLE_TO_LONG));
		}
		lastSysCpu = sysCpu;
		lastUserCpu = userCpu;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Process) {
			return uniqueId == ((Process) obj).uniqueId;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Long.hashCode(uniqueId);
	}

	public static class IdComparator extends ProcessComparator {
		@Override
		public int doCompare(Process o1, Process o2) {
			return Long.compare(o1.id, o2.id);
		}
	}

	public static class CpuUsageComparator extends ProcessComparator {
		@Override
		public int doCompare(Process o1, Process o2) {
			return Double.compare(o2.cpuUsage.newest(), o1.cpuUsage.newest());
		}
	}

	public static class PrivateWorkingSetComparator extends ProcessComparator {
		@Override
		public int doCompare(Process o1, Process o2) {
			return Long.compare(o2.privateWorkingSet.newest(), o1.privateWorkingSet.newest());
		}
	}

	public static class DeadTimestampsComparator extends ProcessComparator {
		@Override
		public int doCompare(Process o1, Process o2) {
			return -Long.compare(o2.deathTimestamp, o1.deathTimestamp);
		}
	}

	public static class FileNameComparator extends ProcessComparator {
		private Collator collator;

		public FileNameComparator() {
			collator = Collator.getInstance();
			collator.setStrength(Collator.PRIMARY);
		}

		@Override
		public int doCompare(Process o1, Process o2) {
			return collator.compare(o1.fileName, o2.fileName);
		}
	}

	public static class CommandLineComparator extends ProcessComparator {
		private Collator collator;

		public CommandLineComparator() {
			collator = Collator.getInstance();
			collator.setStrength(Collator.PRIMARY);
		}

		@Override
		public int doCompare(Process o1, Process o2) {
			return collator.compare(o1.commandLine, o2.commandLine);
		}
	}

	public static class DescriptionComparator extends ProcessComparator {
		private Collator collator;

		public DescriptionComparator() {
			collator = Collator.getInstance();
			collator.setStrength(Collator.PRIMARY);
		}

		@Override
		public int doCompare(Process o1, Process o2) {
			return collator.compare(o1.description, o2.description);
		}
	}

	public static class UserNameComparator extends ProcessComparator {
		private Collator collator;

		public UserNameComparator() {
			collator = Collator.getInstance();
			collator.setStrength(Collator.PRIMARY);
		}

		@Override
		public int doCompare(Process o1, Process o2) {
			return collator.compare(o1.userName, o2.userName);
		}
	}

	public abstract static class ProcessComparator implements Comparator<Process> {
		private boolean isInverted;

		@Override
		public int compare(Process p1, Process p2) {
			return isInverted ? -doCompare(p1, p2) : doCompare(p1, p2);
		}

		protected abstract int doCompare(Process p1, Process p2);

		public void invert() {
			isInverted = !isInverted;
		}

		public boolean isInverted() {
			return isInverted;
		}
	}
}
