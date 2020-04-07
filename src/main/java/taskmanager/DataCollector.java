
package taskmanager;

import com.sun.jna.Platform;
import config.Config;
import taskmanager.linux.LinuxInformationLoader;
import taskmanager.win32.WindowsInformationLoader;

import javax.swing.SwingUtilities;

public class DataCollector extends Thread {
	private boolean isTransferLocked;

	private InformationUpdateCallback ui;

	private InformationLoader loader;

	private SystemInformation systemInformationPrivate;
	private SystemInformation systemInformationShared;

	public DataCollector(InformationUpdateCallback ui) {
		this.ui = ui;
		systemInformationPrivate = new SystemInformation();
		systemInformationShared = new SystemInformation();
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
		if (Platform.isWindows()) {
			loader = new WindowsInformationLoader();
		} else if (Platform.isLinux()) {
			loader = new LinuxInformationLoader();
		} else {
			throw new UnsupportedOperationException("You are running an unsupported operating system!");
		}

		loader.init(systemInformationPrivate);

		updateInformation(true); // TODO This adds an extra measurement which causes the first two to have a time difference of 0 sec. Is this bad? Could be a good thing since the first measurements are 0 or incorrect?
	}


	int count = 0;
	long totalTime = 0;

	@Override
	public void run() {
		do {
			// Fetch all process info
			long startTime = System.currentTimeMillis();
			updateInformation(false);
			long delta = System.currentTimeMillis() - startTime;
			totalTime += delta;
			if (++count % 1000 == 0) {
				System.out.println(String.format("Data collection time: %dms (avg: %.1fms, runs: %d)", delta, totalTime / (float) count, count));
			}

			try {
				Thread.sleep(Math.max(0, (long) (1000 / Config.getFloat(Config.KEY_UPDATE_RATE) - delta)));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} while (!ui.hasTerminated());
	}


	private void updateInformation(boolean isInit) {
		loader.update(systemInformationPrivate);
		updateUi(isInit);
	}

	private void updateUi(boolean isInit) {
		lockTransfer();
		systemInformationShared.copyFrom(systemInformationPrivate);
		unlockTransfer();

		if (isInit) {
			ui.init(systemInformationShared);
		} else if (systemInformationShared.processes.size() > 0) {
			SwingUtilities.invokeLater(() -> ui.update(systemInformationShared)); // TODO Move the SwingUtilities-call to the UI!
		}
	}
}