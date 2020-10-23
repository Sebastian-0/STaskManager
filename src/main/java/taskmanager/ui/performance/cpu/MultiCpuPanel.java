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

package taskmanager.ui.performance.cpu;

import config.Config;
import taskmanager.Measurements;
import taskmanager.data.SystemInformation;
import taskmanager.ui.TextUtils.ValueType;
import taskmanager.ui.performance.GraphPanel;
import taskmanager.ui.performance.GraphPanel.Graph.GraphBuilder;
import taskmanager.ui.performance.GraphPanel.ShortToLong;
import taskmanager.ui.performance.GraphType;
import taskmanager.ui.performance.TimelineGraphPanel;

import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import java.awt.GridLayout;

public class MultiCpuPanel extends JPanel {
	private final GraphPanel[] graphs;

	@SuppressWarnings("unchecked")
	public MultiCpuPanel(TimelineGraphPanel timeline, SystemInformation systemInformation) {
		int numCores = systemInformation.logicalProcessorCount;
		Measurements<Long>[] measurements = new Measurements[numCores];
		graphs = new GraphPanel[numCores];

		for (int i = 0; i < numCores; i++) {
			measurements[i] = new ShortToLong(systemInformation.cpuUsagePerCore[i]);
			graphs[i] = new GraphPanel();
			graphs[i].addGraph(new GraphBuilder(measurements[i], GraphType.Cpu).build());
		}

		int height = (int) (Math.log(numCores - 1) / Math.log(2)) + 1;
		int width = numCores / height + (numCores % height == 0 ? 0 : 1);

		setLayout(new GridLayout(height, width, 5, 5));

		for (int i = 0; i < numCores; i++) {
			add(graphs[i]);
		}

		timeline.connectGraphPanels(graphs);
	}

	@Override
	public void setComponentPopupMenu(JPopupMenu popup) {
		super.setComponentPopupMenu(popup);
		for (GraphPanel graph : graphs) {
			graph.setComponentPopupMenu(popup);
		}
	}

	public void update() {
		final int total = Config.DOUBLE_TO_LONG;
		for (GraphPanel graph : graphs) {
			graph.setMaxDatapointValue(total);
			graph.newDatapoint();
		}
	}
}