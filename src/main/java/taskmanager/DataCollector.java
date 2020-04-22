/*
 * Copyright (c) 2020. Sebastian Hjelm
 */

package taskmanager;

import com.sun.jna.Platform;
import config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import taskmanager.platform.linux.LinuxInformationLoader;
import taskmanager.platform.win32.WindowsInformationLoader;

import javax.swing.SwingUtilities;

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

	public void init() {
		loader.init(systemInformationPrivate);
		collectSystemInformation(true); // TODO This adds an extra measurement which causes the first two to have a time difference of 0 sec. Is this bad? Could be a good thing since the first measurements are 0 or incorrect?
	}

	@Override
	public void run() {
		do {
			long startTime = System.currentTimeMillis();
			collectSystemInformation(false);
			long delta = System.currentTimeMillis() - startTime;
			totalDataFetchTime += delta;
			if (numDataFetches++ % 1000 == 0) {
				LOGGER.info("Data collection duration: {}ms (avg: {}ms, runs: {})",
						delta, String.format("%.1f", totalDataFetchTime / (float) numDataFetches), numDataFetches);
			}

			try {
				Thread.sleep(Math.max(0, (long) (1000 / Config.getFloat(Config.KEY_UPDATE_RATE) - delta)));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} while (!uiCallback.hasTerminated());
	}

	private void collectSystemInformation(boolean isInit) {
		loader.update(systemInformationPrivate);
		updateUi(isInit);
	}

	private void updateUi(boolean isInit) {
		lockTransfer();
		systemInformationShared.copyFrom(systemInformationPrivate);
		unlockTransfer();

		if (isInit) {
			uiCallback.init(systemInformationShared);
		} else if (systemInformationShared.processes.size() > 0) {
			SwingUtilities.invokeLater(() -> uiCallback.update(systemInformationShared)); // TODO Move the SwingUtilities-call to the UI!
		}
	}
}