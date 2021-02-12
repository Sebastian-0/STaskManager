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

package taskmanager.ui.details;

import config.Config;
import net.miginfocom.swing.MigLayout;
import taskmanager.data.Process;
import taskmanager.data.SystemInformation;
import taskmanager.ui.callbacks.ProcessDetailsCallback;
import taskmanager.ui.details.filter.FilterAttributeComboBox;
import taskmanager.ui.details.filter.FilterPanel;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import java.awt.GridLayout;
import java.util.List;

public class ProcessPanel extends JPanel {
	private final SystemInformation systemInformation;

	private final ProcessTable liveTable;
	private final ProcessTable deadTable;

	private final FilterPanel filterPanel;

	private final JPanel container;
	private final JScrollPane liveTableScrollPane;
	private final JSplitPane splitPane;

	public ProcessPanel(ProcessDetailsCallback processCallback, SystemInformation systemInformation) {
		this.systemInformation = systemInformation;
		liveTable = new ProcessTable(processCallback, systemInformation, false);
		deadTable = new ProcessTable(processCallback, systemInformation, true);
		ShowAllProcessesCheckbox showAllProcessesCheckbox = new ShowAllProcessesCheckbox(liveTable, deadTable);
		filterPanel = new FilterPanel(liveTable, deadTable);
		JLabel attributeLabel = new JLabel("By:");
		FilterAttributeComboBox attribute = new FilterAttributeComboBox(liveTable.getVisibleColumns(), filterPanel);

		liveTableScrollPane = new JScrollPane(liveTable);
		splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, liveTableScrollPane, new JScrollPane(deadTable));
		splitPane.setResizeWeight(Config.getFloat(Config.KEY_LAST_PROCESS_LIST_SPLIT_RATIO, 0.8f));
		splitPane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, e -> {
			double ratio = splitPane.getDividerLocation() / (double) splitPane.getHeight();
			Config.put(Config.KEY_LAST_PROCESS_LIST_SPLIT_RATIO, String.valueOf(ratio));
		});

		container = new JPanel();
		container.setLayout(new GridLayout(1, 1));

		setLayout(new MigLayout());
		add(container, "push, grow, span 3, wrap");

		add(showAllProcessesCheckbox, "span 3, wrap");

		add(filterPanel, "growx");
		add(attributeLabel);
		add(attribute);

		updateShouldShowDeadProcesses();
	}

	public void updateShouldShowDeadProcesses() {
		container.remove(splitPane);
		container.remove(liveTableScrollPane);
		if (Config.getBoolean(Config.KEY_SHOW_DEAD_PROCESSES)) {
			container.add(splitPane);
			splitPane.setLeftComponent(liveTableScrollPane); // Reset the table to avoid it getting disabled
		} else {
			container.add(liveTableScrollPane);
		}
		revalidate();
		repaint();
	}

	public void update() {
		liveTable.update();
		deadTable.update();
	}

	public void showProcess(long uniqueId) {
		if (isProcessInList(systemInformation.processes, uniqueId)) {
			showProcess(liveTable, uniqueId);
		} else if (isProcessInList(systemInformation.deadProcesses, uniqueId) &&
				Config.getBoolean(Config.KEY_SHOW_DEAD_PROCESSES)) {
			showProcess(deadTable, uniqueId);
		}
	}

	private boolean isProcessInList(List<Process> processes, long uniqueId) {
		return processes.stream().anyMatch(p -> p.uniqueId == uniqueId);
	}

	private void showProcess(ProcessTable targetTable, long uniqueId) {
		if (!targetTable.showProcess(uniqueId)) {
			filterPanel.clearFilter();
			targetTable.showProcess(uniqueId);
		}
	}
}