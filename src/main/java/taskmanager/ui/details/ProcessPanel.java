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
import taskmanager.data.SystemInformation;
import taskmanager.ui.SimpleGridBagLayout;
import taskmanager.ui.details.filter.FilterAttributeComboBox;
import taskmanager.ui.details.filter.FilterPanel;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;

public class ProcessPanel extends JPanel {
	private final ProcessTable liveTable;
	private final ProcessTable deadTable;

	private final JPanel container;
	private final JScrollPane liveTableScrollPane;
	private final JSplitPane splitPane;

	public ProcessPanel(ProcessDetailsCallback processCallback, SystemInformation systemInformation) {
		liveTable = new ProcessTable(processCallback, systemInformation, false);
		deadTable = new ProcessTable(processCallback, systemInformation, true);
		ShowAllProcessesCheckbox showAllProcessesCheckbox = new ShowAllProcessesCheckbox(liveTable, deadTable);
		FilterPanel filterPanel = new FilterPanel(liveTable, deadTable);
		JLabel attributeLabel = new JLabel("By:");
		FilterAttributeComboBox attribute = new FilterAttributeComboBox(liveTable.getVisibleColumns(), filterPanel);

		liveTableScrollPane = new JScrollPane(liveTable);
		splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, liveTableScrollPane, new JScrollPane(deadTable));
		splitPane.setResizeWeight(0.9);

		container = new JPanel();
		container.setLayout(new GridLayout(1, 1));

		int insets = 10;
		SimpleGridBagLayout gbl = new SimpleGridBagLayout(this);
		gbl.setInsets(insets, insets, insets / 2, insets);
		gbl.addToGrid(container, 0, 0, 3, 1, GridBagConstraints.BOTH, 1, 1);

		gbl.setInsets(0, insets, insets / 2, insets);
		gbl.addToGrid(showAllProcessesCheckbox, 0, 1, 3, 1, GridBagConstraints.WEST);

		gbl.addToGrid(filterPanel, 0, 2, 1, 1, GridBagConstraints.BOTH, 1, 0);
		gbl.setInsets(0, 0, insets / 2, insets/2);
		gbl.addToGrid(attributeLabel, 1, 2, 1, 1);
		gbl.setInsets(0, 0, insets / 2, insets);
		gbl.addToGrid(attribute, 2, 2, 1, 1);

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
}