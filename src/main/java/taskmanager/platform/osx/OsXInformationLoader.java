/*
 * Copyright (c) 2021. Sebastian Hjelm
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * See LICENSE for further details.
 */

package taskmanager.platform.osx;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;
import com.sun.jna.platform.mac.SystemB.Passwd;
import com.sun.jna.platform.mac.SystemB.ProcTaskAllInfo;
import com.sun.jna.platform.mac.SystemB.Timeval;
import com.sun.jna.platform.mac.SystemB.VMStatistics;
import com.sun.jna.ptr.IntByReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oshi.jna.platform.mac.SystemB;
import oshi.util.Constants;
import oshi.util.ExecutingCommand;
import taskmanager.InformationLoader;
import taskmanager.data.Process;
import taskmanager.data.Status;
import taskmanager.data.SystemInformation;
import taskmanager.platform.linux.LinuxExtraInformation;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class OsXInformationLoader extends InformationLoader {
	private static final Logger LOGGER = LoggerFactory.getLogger(OsXInformationLoader.class);

	// TODO: Replace max size with double fetch instead? First get amount of pids and then get the list
	//  Can also get maximum amount from sysctl with KERN_MAXPROC
	private static final int MAXIMUM_NUMBER_OF_PROCESSES = 10_000;

	// Error codes at: https://opensource.apple.com/source/xnu/xnu-201/bsd/sys/errno.h.auto.html
	private static final int KERN_SUCCESS = 0;

	private static final int CTL_KERN = 1;

	private static final int KERN_ARGMAX = 8;
	private static final int KERN_PROCARGS2 = 49;

	private int maximumProgramArguments;
	private final int[] pidFetchArray = new int[MAXIMUM_NUMBER_OF_PROCESSES];

	private long nextProcessId;

	@Override
	public void init(SystemInformation systemInformation) {
		super.init(systemInformation);

		systemInformation.extraInformation = new LinuxExtraInformation();
		systemInformation.physicalMemoryTotalInstalled = systemInformation.physicalMemoryTotal;

		readMaximumProgramArguments();
	}

	private void readMaximumProgramArguments() {
		int[] mib = { CTL_KERN, KERN_ARGMAX };
		IntByReference argmax = new IntByReference();
		int status = SystemB.INSTANCE.sysctl(mib, mib.length, argmax.getPointer(), new IntByReference(SystemB.INT_SIZE), null, 0);
		if (status != 0) {
			LOGGER.error("Failed to fetch maximum size of program argument list, error: {}", Native.getLastError());
		} else {
			maximumProgramArguments = argmax.getValue();
		}
	}

	@Override
	public void update(SystemInformation systemInformation) {
		super.update(systemInformation);

		updateMemory(systemInformation);
		updateProcesses(systemInformation);
	}

	private void updateMemory(SystemInformation systemInformation) {
		VMStatistics statistics = new VMStatistics();
		if (SystemB.INSTANCE.host_statistics(SystemB.INSTANCE.mach_host_self(), SystemB.HOST_VM_INFO, statistics,
				new IntByReference(statistics.size() / SystemB.INT_SIZE)) != KERN_SUCCESS) {
			LOGGER.warn("Failed to read memory information!");
		} else {
			systemInformation.freeMemory = (statistics.free_count + statistics.inactive_count) * systemInformation.pageSize;
		}

		// For more OSX memory info, see: http://web.mit.edu/darwin/src/modules/xnu/osfmk/man/vm_statistics.html
//			LinuxExtraInformation extraInformation = (LinuxExtraInformation) systemInformation.extraInformation;
//			extraInformation.bufferMemory = Long.parseLong(removeUnit(memInfo.get("Buffers"))) * 1024;
//			extraInformation.cacheMemory = Long.parseLong(removeUnit(memInfo.get("Cached"))) * 1024 + Long.parseLong(removeUnit(memInfo.get("SReclaimable"))) * 1024;
//			extraInformation.sharedMemory = Long.parseLong(removeUnit(memInfo.get("Shmem"))) * 1024;
//
//			extraInformation.swapSize = Long.parseLong(removeUnit(memInfo.get("SwapTotal"))) * 1024;
//			extraInformation.swapUsed = extraInformation.swapSize - Long.parseLong(removeUnit(memInfo.get("SwapFree"))) * 1024;
	}

	private void updateProcesses(SystemInformation systemInformation) {
		int count = SystemB.INSTANCE.proc_listpids(SystemB.PROC_ALL_PIDS, 0, pidFetchArray,
				pidFetchArray.length * SystemB.INT_SIZE) / SystemB.INT_SIZE;

		Set<Long> newProcessIds = new LinkedHashSet<>();
		for (int i = 0; i < count; i++) {
			long pid = pidFetchArray[i];

			newProcessIds.add(pid);
			Process process = systemInformation.getProcessById(pid);
			if (process == null) {
				process = new Process(nextProcessId++, pid);
				systemInformation.processes.add(process);
			}

			// TODO: Try getting process info with sysctl and KERN_PROC + pid (alt. + KERN_PROC_ALL)
			//  See docs for returned struct kinfo_proc: https://opensource.apple.com/source/xnu/xnu-344/bsd/sys/sysctl.h
			//  and extern_proc: https://opensource.apple.com/source/xnu/xnu-201/bsd/sys/proc.h

			int[] mib = { CTL_KERN, 14, 1, (int) pid}; // 14 = KERN_PROC, 1 = KERN_PROC_PID

			KInfoProc infoProc = new KInfoProc();
			Pointer mem = new Memory(infoProc.size());
			int st = SystemB.INSTANCE.sysctl(mib, mib.length, mem, new IntByReference(infoProc.size()), null, 0);
			if (st != 0) {
				System.out.println("ERROR: " + Native.getLastError());
			}


			boolean allInfoFailed = false;
			ProcTaskAllInfo allInfo = new ProcTaskAllInfo();
			int status = SystemB.INSTANCE.proc_pidinfo((int) pid, SystemB.PROC_PIDTASKALLINFO, 0, allInfo, allInfo.size());
			if (status < 0) {
				LOGGER.warn("Failed to read process information for {}: {}", pid, Native.getLastError());
				continue;
			} else if (status != allInfo.size()) {
				// Failed to read, possibly because we don't have access
				allInfoFailed = true;
			}

			if (!process.hasReadOnce) {
				process.commandLine = getCommandLine(pid);

				Memory pathBuffer = new Memory(SystemB.PROC_PIDPATHINFO_MAXSIZE);
				status = SystemB.INSTANCE.proc_pidpath((int) pid, pathBuffer, (int) pathBuffer.size());
				if (status > 0) {
					process.filePath = pathBuffer.getString(0).trim();

					String[] toks = process.filePath.split(File.separator);
					process.fileName = toks[toks.length - 1];

//					String partialName = Native.toString(allInfo.pbsd.pbi_comm, StandardCharsets.UTF_8);
				} else {
					LOGGER.warn("Failed to read process path for {}: {}", pid, Native.getLastError());
				}

				if (!allInfoFailed) {
					Passwd passwd = SystemB.INSTANCE.getpwuid(allInfo.pbsd.pbi_uid);
					if (passwd != null) {
						process.userName = passwd.pw_name;
					} else {
						process.userName = Constants.UNKNOWN;
					}

					process.startTimestamp = allInfo.pbsd.pbi_start_tvsec * 1000L + allInfo.pbsd.pbi_start_tvusec / 1000L;

					long parentId = allInfo.pbsd.pbi_ppid;
					Process parent = systemInformation.getProcessById(parentId);
					if (parent != null) {
						process.parentUniqueId = parent.uniqueId;
						process.parentId = parentId;
					} else {
						process.parentUniqueId = -1;
						process.parentId = -1;
					}
				}
				process.hasReadOnce = true;
			}

			if (allInfoFailed) {
				continue;
			}

			switch (allInfo.pbsd.pbi_status) {
				case 1: // Idle == newly created
					process.status = Status.Running;
					break;
				case 2: // Running
					process.status = Status.Running;
					break;
				case 3: // Sleeping on an address
					process.status = Status.Sleeping;
					break;
				case 4: // Stopped/Suspended?
					process.status = Status.Suspended;
					break;
				case 5: // Zombie == Waiting for collection by parent
					process.status = Status.Zombie;
					break;
				default:
					LOGGER.info("Unknown status {} for process {}", allInfo.pbsd.pbi_status, pid);
			}

			process.privateWorkingSet.addValue(allInfo.ptinfo.pti_resident_size);

			long stime = allInfo.ptinfo.pti_total_system / 1_000_000;
			long utime = allInfo.ptinfo.pti_total_user / 1_000_000;
			process.updateCpu(stime, utime, systemInformation.logicalProcessorCount);
		}

		updateDeadProcesses(systemInformation, newProcessIds);

		systemInformation.totalProcesses = newProcessIds.size();

		// TODO: Read max file descriptors using sysctl and KERN_MAXFILES
	}

	private String getCommandLine(long pid) {
		if (pid == 0 || maximumProgramArguments == 0) {
			return "";
		}

		Pointer procargs = new Memory(maximumProgramArguments);

		int[] mib = { CTL_KERN, KERN_PROCARGS2, (int) pid };
		IntByReference argmax = new IntByReference(maximumProgramArguments);
		int status = SystemB.INSTANCE.sysctl(mib, mib.length, procargs, argmax, null, 0);
		if (status != 0) {
			// Fallback due to random failures for the previous system call, probably an OSX bug?
			String cmdLine = ExecutingCommand.getFirstAnswer("ps -o command= -p " + pid);
			if (!cmdLine.isEmpty()) {
				return cmdLine;
			}

			LOGGER.warn("Failed to read command line for {}, error code: {}", pid, Native.getLastError());
			return "";
		}

		List<String> result = new ArrayList<>();

		int nargs = procargs.getInt(0);
		int offset = SystemB.INT_SIZE;
		while (nargs-- > 0 && offset < argmax.getValue()) {
			String arg = procargs.getString(offset);
			result.add(arg);
			offset += arg.length() + 1;
		}

		return String.join(" ", result);
	}

	@FieldOrder({"kp_proc", "kp_eproc"})
	public static class KInfoProc extends Structure {
		public ExternProc kp_proc;
		public EProc kp_eproc;
	}

	@FieldOrder({"e_paddr", "e_sess", "e_pcred", "e_ucred", "e_vm", "e_ppid", "e_pgid", "e_jobc", "e_tdev", "e_tpgid",
			"e_tsess", "e_wmesg", "e_xsize", "e_xrssize", "e_xccount", "e_xswrss", "e_flag", "e_login", "e_spare"})
	public static class EProc extends Structure {
		public Pointer e_paddr;		/* address of proc */
		public Pointer e_sess;	/* session pointer */
		public PCred e_pcred;		/* process credentials */
		public UCred e_ucred;		/* current credentials */
		public VMSpace e_vm;		/* address space */
		public int e_ppid;			/* parent process id */
		public int e_pgid;			/* process group id */
		public short   e_jobc;			/* job control counter */
		public int     e_tdev;			/* controlling tty dev */
		public int     e_tpgid;		/* tty process group id */
		public Pointer e_tsess;	/* tty session pointer */
		public char[]  e_wmesg = new char[7+1];	/* wchan message */ // 7 = WMESGLEN
		public int    e_xsize;		/* text size */
		public short  e_xrssize;		/* text rss */
		public short  e_xccount;		/* text references */
		public short  e_xswrss;
		public long   e_flag;
		public char[] e_login = new char[12];	/* short setlogin() name */ // 12 = COMAPT_MAXLOGNAME
		public long[] e_spare = new long[4];
	}

	@FieldOrder({"p_forw", "p_back", "p_vmspace", "p_sigacts", "p_flag", "p_stat", "p_pid", "p_oppid", "p_dupfd",
			"user_stack", "exit_thread", "p_debugger", "sigwait", "p_estcpu", "p_cpticks", "p_pctcpu", "p_wchan",
			"p_wmesg", "p_swtime", "p_slptime", "p_realtimer", "p_rtime", "p_uticks", "p_sticks", "p_iticks",
			"p_traceflag", "p_tracep", "p_siglist", "p_textvp", "p_holdcnt", "p_sigmask", "p_sigignore", "p_sigcatch",
			"p_priority", "p_usrpri", "p_nice", "p_comm", "p_pgrp", "p_addr", "p_xstat", "p_acflag", "p_ru"})
	public static class ExternProc extends Structure {
		public Pointer p_forw;
		public Pointer p_back;
		public Pointer p_vmspace;
		public Pointer p_sigacts;
		public int p_flag;
		public byte p_stat;
		public int p_pid;
		public int p_oppid;
		public int p_dupfd;
		// Mach related
		public String user_stack; // Or pointer
		public Pointer exit_thread; // Or pointer
		public int p_debugger;
		public byte sigwait;
		// Scheduling
		public int p_estcpu;	 /* Time averaged value of p_cpticks. */
		public int p_cpticks;	 /* Ticks of cpu time. */
		public int p_pctcpu;	 /* %cpu for this process during p_swtime */
		public Pointer p_wchan;	 /* Sleep address. */
		public String p_wmesg;	 /* Reason for sleep. */
		public int p_swtime;	 /* Time swapped in or out. */
		public int p_slptime;	 /* Time since last blocked. */
		public ITimerVal p_realtimer;	/* Alarm timer. */
		public Timeval p_rtime;	/* Real time. */
		public long p_uticks;		/* Statclock hits in user mode. */
		public long p_sticks;		/* Statclock hits in system mode. */
		public long p_iticks;		/* Statclock hits processing intr. */
		public int p_traceflag;		/* Kernel trace points. */
		public Pointer p_tracep;	/* Trace to vnode. */
		public int p_siglist;		/* Signals arrived but not delivered. */
		public Pointer p_textvp;	/* Vnode of executable. */
		public int p_holdcnt;		/* If non-zero, don't swap. */
		public int p_sigmask;	/* Current signal mask. */
		public int p_sigignore;	/* Signals being ignored. */
		public int p_sigcatch;	/* Signals being caught by user. */
		public byte   p_priority;	/* Process priority. */
		public byte   p_usrpri;	/* User-priority based on p_cpu and p_nice. */
		public byte   p_nice;		/* Process "nice" value. */
		public byte[] p_comm = new byte[16+1]; // MAXCOMLEN = 16
		public Pointer p_pgrp;	/* Pointer to process group. */
		public Pointer p_addr;	/* Kernel virtual addr of u-area (PROC ONLY). */
		public short p_xstat;	/* Exit status for wait; also stop signal. */
		public short p_acflag;	/* Accounting flags. */
		public Pointer p_ru;	/* Exit information. XXX */
	}

	@FieldOrder({"it_interval", "it_value"})
	public static class ITimerVal extends Structure {
		public Timeval it_interval;
		public Timeval it_value;
	}

	@FieldOrder({"vm_refcnt", "vm_shm", "vm_rssize", "vm_swrss", "vm_tsize", "vm_dsize", "vm_ssize", "vm_taddr",
			"vm_daddr", "vm_maxsaddr"})
	public static class VMSpace extends Structure { // Strings -> Pointer?
		public int vm_refcnt;	/* number of references */
		public String vm_shm;		/* SYS5 shared memory private data XXX */
		public int vm_rssize; 	/* current resident set size in pages */
		public int vm_swrss;	/* resident set size before last swap */
		public int vm_tsize;	/* text size (pages) XXX */
		public int vm_dsize;	/* data size (pages) XXX */
		public int vm_ssize;	/* stack size (pages) */
		public String vm_taddr;	/* user virtual address of text XXX */
		public String vm_daddr;	/* user virtual address of data XXX */
		public String vm_maxsaddr;	/* user VA at max stack growth */
	}

	@FieldOrder({"cr_ref", "cr_uid", "cr_ngroups", "cr_groups"})
	public static class UCred extends Structure {
		public long cr_ref;			/* reference count */
		public int  cr_uid;			/* effective user id */
		public short cr_ngroups;		/* number of groups */
		public int[] cr_groups = new int[16];	/* groups */ // NGROUPS = NGROUPS_MAX = 16
	}

	@FieldOrder({"pc_lock", "pc_ucred", "p_ruid", "p_svuid", "p_rgid", "p_svgid", "p_refcnt"})
	public static class PCred extends Structure {
		public LockBsd pc_lock;
		public Pointer pc_ucred;	/* Current credentials. */
		public int p_ruid;			/* Real user id. */
		public int p_svuid;		/* Saved effective user id. */
		public int p_rgid;			/* Real group id. */
		public int p_svgid;		/* Saved effective group id. */
		public int p_refcnt;		/* Number of references. */
	}

	@FieldOrder({"lk_interlock", "lk_flags", "lk_sharecount", "lk_waitcount", "lk_exclusivecount", "lk_prio",
			"lk_wmesg", "lk_timo", "lk_lockholder", "lk_lockthread"})
	public static class LockBsd extends Structure {
		public int[] lk_interlock = new int[9];		/* lock on remaining fields */ // 10 for PPC else 9
		public int lk_flags;		/* see below */
		public int lk_sharecount;		/* # of accepted shared locks */
		public int lk_waitcount;		/* # of processes sleeping for lock */
		public short lk_exclusivecount;	/* # of recursive exclusive locks */
		public short lk_prio;		/* priority at which to sleep */
		public String lk_wmesg;		/* resource sleeping (for tsleep) */
		public int lk_timo;		/* maximum sleep time (for tsleep) */
		public int lk_lockholder;		/* pid of exclusive lock holder */
		public Pointer lk_lockthread;		/* thread which acquired excl lock */
	}
}
