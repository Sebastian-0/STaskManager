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

package taskmanager;

import com.sun.jna.Platform;
import config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import taskmanager.data.SystemInformation;
import taskmanager.platform.linux.LinuxInformationLoader;
import taskmanager.platform.osx.OsXInformationLoader;
import taskmanager.platform.win32.WindowsInformationLoader;

public class DataCollector extends Thread {
	private static final Logger LOGGER = LoggerFactory.getLogger(DataCollector.class);

	private final InformationUpdateCallback uiCallback;

	private final SystemInformation systemInformationPrivate;
	private final SystemInformation systemInformationShared;
	private boolean isTransferLocked;

	private final InformationLoader loader;

	private int numDataFetches = 0;
	private long totalDataFetchTime = 0;

	public DataCollector(InformationUpdateCallback uiCallback) {
		this.uiCallback = uiCallback;
		this.systemInformationPrivate = new SystemInformation();
		this.systemInformationShared = new SystemInformation();
		this.loader = createInformationLoader();
	}

	private InformationLoader createInformationLoader() {
		if (Platform.isWindows()) {
			return new WindowsInformationLoader();
		} else if (Platform.isLinux()) {
			return new LinuxInformationLoader();
		} else if (Platform.isMac()) {
			return new OsXInformationLoader();
		} else {
			throw new UnsupportedOperationException("You are running an unsupported operating system!");
		}
	}

	public synchronized void lockTransfer() {
		while (isTransferLocked) {
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		isTransferLocked = true;
	}

	public synchronized void unlockTransfer() {
		isTransferLocked = false;
		notifyAll();
	}

	public SystemInformation init() {
		loader.init(systemInformationPrivate);
		// TODO This adds an extra measurement which causes the first two to have a time difference of 0 sec.
		//  Is this bad? Could be a good thing since the first measurements are 0 or incorrect?
		updateSystemInformation();
		return systemInformationShared;
	}

	@Override
	public void run() {
		try {
			do {
				long startTime = System.currentTimeMillis();
				updateSystemInformation();
				updateUi();
				long delta = System.currentTimeMillis() - startTime;
				totalDataFetchTime += delta;
				if (numDataFetches++ % 1000 == 0) {
					LOGGER.info("Data collection duration: {}ms (avg: {}ms, runs: {})",
							delta, String.format("%.1f", totalDataFetchTime / (float) numDataFetches), numDataFetches);
				}

				try {
					Thread.sleep(Math.max(0, (long) (1000 / Config.getFloat(Config.KEY_UPDATE_RATE) - delta)));
				} catch (InterruptedException ignored) {
				}
			} while (!uiCallback.hasTerminated());
		} catch (Throwable e) {
			LOGGER.error("Unexpected error during data collection", e);
			uiCallback.dataCollectorFailed();
		}
	}

	private void updateSystemInformation() {
		loader.update(systemInformationPrivate);
		lockTransfer();
		systemInformationShared.copyFrom(systemInformationPrivate);
		unlockTransfer();
	}

	private void updateUi() {
		if (systemInformationShared.processes.size() > 0) {
			uiCallback.update(systemInformationShared);
		}
	}
}