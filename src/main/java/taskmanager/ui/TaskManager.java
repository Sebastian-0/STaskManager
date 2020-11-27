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

package taskmanager.ui;

import config.Config;
import config.TextureStorage;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import taskmanager.DataCollector;
import taskmanager.InformationUpdateCallback;
import taskmanager.data.Process;
import taskmanager.data.Status;
import taskmanager.data.SystemInformation;
import taskmanager.ui.details.ProcessDetailsCallback;
import taskmanager.ui.details.ProcessPanel;
import taskmanager.ui.menubar.MenuBar;
import taskmanager.ui.performance.PerformancePanel;
import taskmanager.ui.performance.ShowProcessCallback;
import taskmanager.ui.processdialog.ProcessDialog;
import taskmanager.ui.tray.Tray;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.SystemTray;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class TaskManager extends JFrame implements InformationUpdateCallback, ProcessDetailsCallback, ApplicationCallback, ShowProcessCallback {
	private static final Logger LOGGER = LoggerFactory.getLogger(TaskManager.class);

	private static final String ICON_LARGE_NAME = "icon_large";
	private static final String ICON_SMALL_NAME = "icon_small";

	private DataCollector dataCollector;
	private SystemInformation systemInformation;

	private Comparator<Process> comparator;
	private Comparator<Process> deadComparator;

	private JTabbedPane tabbedPane;
	private ProcessPanel processPanel;
	private PerformancePanel performancePanel;

	private Map<Long, ProcessDialog> processDialogs;
	private Map<Long, ProcessDialog> deadProcessDialogs;

	private Tray trayIcon;
	private boolean hasTerminated;

	public TaskManager() {
		setTitle("Task manager");
		loadProgramIcon();

		dataCollector = new DataCollector(this);
		systemInformation = new SystemInformation();
		comparator = new Process.IdComparator();
		deadComparator = new Process.DeadTimestampsComparator();

		processDialogs = new HashMap<>();
		deadProcessDialogs = new HashMap<>();

		addWindowListener(windowListener);
		addWindowStateListener(windowListener);
		addComponentListener(componentListener);

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setAlwaysOnTop(Config.getBoolean(Config.KEY_ALWAYS_ON_TOP));
	}

	private void loadProgramIcon() {
		Image image = TextureStorage.instance().getTexture(ICON_LARGE_NAME);
		Image image16 = TextureStorage.instance().getTexture(ICON_SMALL_NAME);

		List<Image> images = new ArrayList<Image>();
		images.add(image);
		images.add(image16);
		setIconImages(images);
	}

	public void init() {
		SystemInformation systemInformationNew = dataCollector.init();

		copyData(systemInformationNew);

		initSystemTray();

		setJMenuBar(new MenuBar(this));

		processPanel = new ProcessPanel(this, this.systemInformation);
		performancePanel = new PerformancePanel(this.systemInformation, this);

		tabbedPane = new JTabbedPane();
		tabbedPane.addTab("Processes", processPanel);
		tabbedPane.addTab("Performance", performancePanel);

		tabbedPane.setSelectedIndex(Config.getInt(Config.KEY_LAST_TAB, 0));
		tabbedPane.addChangeListener(e -> Config.put(Config.KEY_LAST_TAB, String.valueOf(tabbedPane.getSelectedIndex())));

		setLayout(new MigLayout("fill, ins 0", "fill", "fill")); // TODO maybe overkill
		getContentPane().add(tabbedPane);

		Dimension previousSize = getPreviousSize();
		if (previousSize.width > 0) {
			setPreferredSize(previousSize);
		}
		setExtendedState(getPreviousExtendedState());
		pack();

		setLocationRelativeTo(null);

		if (!shouldMinimizeToTray(getExtendedState())) {
			setVisible(true);
		}

		dataCollector.start();
	}

	private void copyData(SystemInformation other) {
		systemInformation.copyFrom(other);
		systemInformation.processes.sort(comparator);
		systemInformation.deadProcesses.sort(deadComparator);
	}

	private void initSystemTray() {
		if (!SystemTray.isSupported()) {
			// TODO When we have settings in the program we should disable the tray minimization setting if this happens
			LOGGER.warn("No system tray support! Disabling minimize to tray...");
			Config.put(Config.KEY_MINIMIZE_TO_TRAY, "false"); // Force no tray minimization when we have no tray
		} else {
			try {
				trayIcon = new Tray(this, TextureStorage.instance().getTexture(ICON_SMALL_NAME));
				SystemTray.getSystemTray().add(trayIcon);
			} catch (AWTException e) {
				LOGGER.error("Failed to create tray icon", e);
				Config.put(Config.KEY_MINIMIZE_TO_TRAY, "false"); // Force no tray minimization when we have no tray
			}
		}
	}

	private Dimension getPreviousSize() {
		return new Dimension(
				Config.getInt(Config.KEY_LAST_WINDOW_WIDTH, 890),
				Config.getInt(Config.KEY_LAST_WINDOW_HEIGHT, 740));
	}

	private int getPreviousExtendedState() {
		return Config.getInt(Config.KEY_LAST_EXTENDED_STATE, NORMAL);
	}

	private boolean shouldMinimizeToTray(int state) {
		return (state & ICONIFIED) != 0 &&
				Config.getBoolean(Config.KEY_MINIMIZE_TO_TRAY) &&
				trayIcon != null;
	}

	@Override
	public void update(SystemInformation systemInformationNew) {
		SwingUtilities.invokeLater(() -> {
			dataCollector.lockTransfer();
			copyData(systemInformationNew);
			dataCollector.unlockTransfer();

			processPanel.update();
			performancePanel.update(systemInformation);

			Set<Long> openProcessIds = new HashSet<>();
			for (Process process : systemInformation.processes) {
				if (processDialogs.containsKey(process.uniqueId)) {
					openProcessIds.add(process.uniqueId);
				}
			}

			Iterator<Entry<Long, ProcessDialog>> itr = processDialogs.entrySet().iterator();
			while (itr.hasNext()) {
				Entry<Long, ProcessDialog> entry = itr.next();
				ProcessDialog dialog = entry.getValue();
				if (!openProcessIds.contains(entry.getKey())) {
					itr.remove();
					dialog.processEnded();
					deadProcessDialogs.put(entry.getKey(), dialog);
				}
			}

			processDialogs.forEach((id, d) -> d.update());
			processDialogs.entrySet().removeIf(e -> !e.getValue().isVisible());
			deadProcessDialogs.entrySet().removeIf(e -> !e.getValue().isVisible());

			if (trayIcon != null) {
				trayIcon.update(systemInformation);
			}
		});
	}

	@Override
	public void dataCollectorFailed() {
		JOptionPane.showMessageDialog(
				this,
				"<html>The taskmanager data collection encountered an internal error, the program will terminate! <br/>" +
						"See the log file for details.</html>",
				"Unexpected error",
				JOptionPane.ERROR_MESSAGE);
		exit();
	}

	@Override
	public void focus() {
		if ((getExtendedState() & JFrame.ICONIFIED) != 0) {
			setVisible(true);
			setExtendedState(getExtendedState() & ~JFrame.ICONIFIED);
			Config.put(Config.KEY_LAST_EXTENDED_STATE, "" + getExtendedState());
		}
		toFront();
	}

	@Override
	public void exit() {
		synchronized (this) {
			hasTerminated = true;
		}
		dispose();
		if (trayIcon != null) {
			SystemTray.getSystemTray().remove(trayIcon);
		}
	}

	@Override
	public void configChanged() {
		setAlwaysOnTop(Config.getBoolean(Config.KEY_ALWAYS_ON_TOP));
		processPanel.updateShouldShowDeadProcesses();
		performancePanel.updateAreTimelinesLinked();
	}

	@Override
	public JFrame frame() {
		return this;
	}

	@Override
	public synchronized boolean hasTerminated() {
		return hasTerminated;
	}

	@Override
	public void openDialog(Process process) {
		ProcessDialog dialog = processDialogs.get(process.uniqueId);
		if (process.status == Status.Dead) {
			dialog = deadProcessDialogs.get(process.uniqueId);
		}
		if (dialog == null || !dialog.isVisible()) {
			dialog = new ProcessDialog(this, process);
			dialog.setVisible(true);
			if (process.status == Status.Dead) {
				deadProcessDialogs.put(process.uniqueId, dialog);
			} else {
				processDialogs.put(process.uniqueId, dialog);
			}
		} else {
			dialog.toFront();
		}
	}

	@Override
	public void setComparator(Comparator<Process> comparator, boolean isDeadList) {
		if (isDeadList) {
			this.deadComparator = comparator;
			systemInformation.deadProcesses.sort(comparator);
		} else {
			this.comparator = comparator;
			systemInformation.processes.sort(comparator);
		}
		if (processPanel != null) {
			processPanel.update();
		}
	}

	@Override
	public void showProcess(long uniqueId) {
		tabbedPane.setSelectedComponent(processPanel);
		processPanel.showProcess(uniqueId);
	}

	private final WindowAdapter windowListener = new WindowAdapter() {
		@Override
		public void windowClosing(WindowEvent e) {
			synchronized (TaskManager.this) {
				exit();
			}
		}

		@Override
		public void windowStateChanged(WindowEvent e) {
			Config.put(Config.KEY_LAST_EXTENDED_STATE, "" + e.getNewState());
			
			if (shouldMinimizeToTray(e.getNewState())) {
				setVisible(false);
			} else if (!isVisible()) {
				setVisible(true);
			}
		}
	};

	private final ComponentAdapter componentListener = new ComponentAdapter() {
		@Override
		public void componentResized(ComponentEvent e) {
			if ((getExtendedState() & MAXIMIZED_HORIZ) == 0) {
				Config.put(Config.KEY_LAST_WINDOW_WIDTH, "" + getWidth());
			}
			if ((getExtendedState() & MAXIMIZED_VERT) == 0) {
				Config.put(Config.KEY_LAST_WINDOW_HEIGHT, "" + getHeight());
			}
		}
	};


	public static void main(String[] args) throws InvocationTargetException, InterruptedException {
//		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//		UIManager.put("Panel.background", Color.white);

		LOGGER.info("Running with PID: {}", ProcessHandle.current().pid());

		System.setProperty("awt.useSystemAAFontSettings","on");
		System.setProperty("swing.aatext", "true");

		ToolTipManager.sharedInstance().setDismissDelay(8000);

		SwingUtilities.invokeAndWait(() -> Thread.currentThread().setUncaughtExceptionHandler(new LoggedUncaughtExceptionHandler()));

		TaskManager taskManager = new TaskManager();
		try {
			taskManager.init();
		} catch (Throwable e) {
			LOGGER.error("Failed to start!", e);
			JOptionPane.showMessageDialog(
					taskManager,
					"<html>The taskmanager failed to start! <br/>" +
							"See the log file for details.</html>",
					"Unexpected error",
					JOptionPane.ERROR_MESSAGE);
			taskManager.exit();
		}
	}
}