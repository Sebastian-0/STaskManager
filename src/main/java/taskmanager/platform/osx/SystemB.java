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

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;

@SuppressWarnings("unused")
public interface SystemB extends oshi.jna.platform.mac.SystemB {
	SystemB INSTANCE = Native.load("System", SystemB.class);

	// Error codes at: https://github.com/st3fan/osx-10.9/blob/master/xnu-2422.1.72/bsd/sys/errno.h
	int KERN_SUCCESS = 0;

	int CTL_KERN = 1;
	int CTL_VM = 2;

	// CTL_KERN options
	int KERN_MAXFILES = 7;
	int KERN_ARGMAX = 8;
	int KERN_PROC = 14;
	int KERN_PROCARGS2 = 49;

	// KERN_PROC options
	int KERN_PROC_PID = 1;

	// CTL_VM options
	int VM_SWAPUSAGE = 5;

	@FieldOrder({"kp_proc", "kp_eproc"})
	class KInfoProc extends Structure {
		// For source, see: https://github.com/st3fan/osx-10.9/blob/master/xnu-2422.1.72/bsd/sys/sysctl.h
		public ExternProc kp_proc;
		public EProc kp_eproc;
	}

	@FieldOrder({"p_starttime", "p_vmspace", "p_sigacts", "p_flag", "p_stat", "p_pid", "p_oppid", "p_dupfd",
			"user_stack", "exit_thread", "p_debugger", "sigwait", "p_estcpu", "p_cpticks", "p_pctcpu", "p_wchan",
			"p_wmesg", "p_swtime", "p_slptime", "p_realtimer", "p_rtime", "p_uticks", "p_sticks", "p_iticks",
			"p_traceflag", "p_tracep", "p_siglist", "p_textvp", "p_holdcnt", "p_sigmask", "p_sigignore", "p_sigcatch",
			"p_priority", "p_usrpri", "p_nice", "p_comm", "p_pgrp", "p_addr", "p_xstat", "p_acflag", "p_ru"})
	class ExternProc extends Structure {
		// For source, see: https://github.com/st3fan/osx-10.9/blob/master/xnu-2422.1.72/bsd/sys/proc.h
		public Timeval p_starttime;
		public Pointer p_vmspace;
		public Pointer p_sigacts;
		public int p_flag;
		public byte p_stat;
		public int p_pid;
		public int p_oppid;
		public int p_dupfd;
		// Mach related
		public Pointer user_stack;
		public Pointer exit_thread;
		public int p_debugger;
		public boolean sigwait; // was byte
		// Scheduling
		public int p_estcpu;	 /* Time averaged value of p_cpticks. */
		public int p_cpticks;	 /* Ticks of cpu time. */
		public int p_pctcpu;	 /* %cpu for this process during p_swtime */
		public Pointer p_wchan;	 /* Sleep address. */
		public String p_wmesg;	 /* Reason for sleep. */
		public int p_swtime;	 /* Time swapped in or out. */
		public int p_slptime;	 /* Time since last blocked. */
		public ITimerVal p_realtimer;	/* Alarm timer. */
		public Timeval p_rtime;			/* Real time. */
		public long p_uticks;		/* Statclock hits in user mode. */
		public long p_sticks;		/* Statclock hits in system mode. */
		public long p_iticks;		/* Statclock hits processing intr. */
		public int p_traceflag;		/* Kernel trace points. */
		public Pointer p_tracep;	/* Trace to vnode. */
		public int p_siglist;		/* Signals arrived but not delivered. */
		public Pointer p_textvp;	/* Vnode of executable. */
		public int p_holdcnt;		/* If non-zero, don't swap. */
		public int p_sigmask;		/* Current signal mask. */
		public int p_sigignore;		/* Signals being ignored. */
		public int p_sigcatch;		/* Signals being caught by user. */
		public byte   p_priority;	/* Process priority. */
		public byte   p_usrpri;		/* User-priority based on p_cpu and p_nice. */
		public byte   p_nice;		/* Process "nice" value. */
		public byte[] p_comm = new byte[16+1]; // MAXCOMLEN = 16
		public Pointer p_pgrp;	/* Pointer to process group. */
		public Pointer p_addr;	/* Kernel virtual addr of u-area (PROC ONLY). */
		public short p_xstat;	/* Exit status for wait; also stop signal. */
		public short p_acflag;	/* Accounting flags. */
		public Pointer p_ru;	/* Exit information. XXX */
	}

	@FieldOrder({"it_interval", "it_value"})
	class ITimerVal extends Structure {
		// For source, see: https://github.com/st3fan/osx-10.9/blob/master/xnu-2422.1.72/bsd/sys/time.h
		public Timeval it_interval;
		public Timeval it_value;
	}

	@FieldOrder({"e_paddr", "e_sess", "e_pcred", "e_ucred", "e_vm", "e_ppid", "e_pgid", "e_jobc", "e_tdev", "e_tpgid",
			"e_tsess", "e_wmesg", "e_xsize", "e_xrssize", "e_xccount", "e_xswrss", "e_flag", "e_login", "e_spare"})
	class EProc extends Structure {
		// For source, see: https://github.com/st3fan/osx-10.9/blob/master/xnu-2422.1.72/bsd/sys/sysctl.h
		public Pointer e_paddr;		/* address of proc */
		public Pointer e_sess;		/* session pointer */
		public PCred e_pcred;		/* process credentials */
		public UCred e_ucred;		/* current credentials */
		public VMSpace e_vm;		/* address space */
		public int e_ppid;			/* parent process id */
		public int e_pgid;			/* process group id */
		public short   e_jobc;		/* job control counter */
		public int     e_tdev;		/* controlling tty dev */
		public int     e_tpgid;		/* tty process group id */
		public Pointer e_tsess;		/* tty session pointer */
		public byte[]  e_wmesg = new byte[7+1];	/* wchan message */ // 7 = WMESGLEN
		public int    e_xsize;		/* text size */
		public short  e_xrssize;	/* text rss */
		public short  e_xccount;	/* text references */
		public short  e_xswrss;
		public int    e_flag;
		public byte[] e_login = new byte[12];	/* short setlogin() name */ // 12 = COMAPT_MAXLOGNAME
		public int[] e_spare = new int[4];
	}

	@FieldOrder({"pc_lock", "pc_ucred", "p_ruid", "p_svuid", "p_rgid", "p_svgid", "p_refcnt"})
	class PCred extends Structure {
		// For source, see: https://github.com/st3fan/osx-10.9/blob/master/xnu-2422.1.72/bsd/sys/sysctl.h
		public byte[] pc_lock = new byte[72];
		public Pointer pc_ucred;	/* Current credentials. */
		public int p_ruid;			/* Real user id. */
		public int p_svuid;			/* Saved effective user id. */
		public int p_rgid;			/* Real group id. */
		public int p_svgid;			/* Saved effective group id. */
		public int p_refcnt;		/* Number of references. */
	}

	@FieldOrder({"cr_ref", "cr_uid", "cr_ngroups", "cr_groups"})
	class UCred extends Structure {
		// For source, see: https://github.com/st3fan/osx-10.9/blob/master/xnu-2422.1.72/bsd/sys/sysctl.h
		public int cr_ref;			/* reference count */
		public int cr_uid;			/* effective user id */
		public short cr_ngroups;	/* number of groups */
		public int[] cr_groups = new int[16];	/* groups */ // NGROUPS = NGROUPS_MAX = 16
	}

	@FieldOrder({"vm_refcnt", "vm_shm", "vm_rssize", "vm_swrss", "vm_tsize", "vm_dsize", "vm_ssize", "vm_taddr",
			"vm_daddr", "vm_maxsaddr"})
	class VMSpace extends Structure {
		// For source, see: https://github.com/st3fan/osx-10.9/blob/master/xnu-2422.1.72/bsd/sys/vm.h
		public int vm_refcnt;	/* number of references */
		public Pointer vm_shm;	/* SYS5 shared memory private data XXX */
		public int vm_rssize; 	/* current resident set size in pages */
		public int vm_swrss;	/* resident set size before last swap */
		public int vm_tsize;	/* text size (pages) XXX */
		public int vm_dsize;	/* data size (pages) XXX */
		public int vm_ssize;	/* stack size (pages) */
		public Pointer vm_taddr;	/* user virtual address of text XXX */
		public Pointer vm_daddr;	/* user virtual address of data XXX */
		public Pointer vm_maxsaddr;	/* user VA at max stack growth */
	}

	@FieldOrder({"info"})
	class ProcFDInfoList extends Structure {
		public ProcFdInfo[] info;

		public ProcFDInfoList(int size) {
			info = new ProcFdInfo[size];
		}
	}
}
