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

package taskmanager.ui.performance;

import config.Config;
import taskmanager.data.SystemInformation;
import taskmanager.ui.performance.cpu.CpuPanel;
import taskmanager.ui.performance.disks.DiskPanel;
import taskmanager.ui.performance.gpus.GpuPanel;
import taskmanager.ui.performance.memory.MemoryPanel;
import taskmanager.ui.performance.network.NetworkPanel;

import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import java.util.ArrayList;
import java.util.List;

public class PerformancePanel extends JSplitPane implements PerformanceButtonListener {
	private final GraphSelectionPanel graphSelectionPanel;

	private final JScrollPane selectedPanelContainer;

	private final MemoryPanel memoryPanel;
	private final CpuPanel cpuPanel;
	private final DiskPanel[] diskPanels;
	private final NetworkPanel[] networkPanels;
	private final GpuPanel[] gpuPanels;

	public PerformancePanel(SystemInformation systemInformation) {
		super(JSplitPane.HORIZONTAL_SPLIT);

		selectedPanelContainer = new JScrollPane();
		selectedPanelContainer.setBorder(null);

		TimelineGroup timelineGroup = new TimelineGroup();
		timelineGroup.setLinked(Config.getBoolean(Config.KEY_LINK_TIMELINES));
		memoryPanel = new MemoryPanel(timelineGroup, systemInformation);
		cpuPanel = new CpuPanel(timelineGroup, systemInformation);
		diskPanels = new DiskPanel[systemInformation.disks.length];
		networkPanels = new NetworkPanel[systemInformation.networks.length];
		gpuPanels = new GpuPanel[systemInformation.gpus.length];

		for (int i = 0; i < diskPanels.length; i++) {
			diskPanels[i] = new DiskPanel(timelineGroup, systemInformation.disks[i]);
		}
		for (int i = 0; i < networkPanels.length; i++) {
			networkPanels[i] = new NetworkPanel(timelineGroup, systemInformation.networks[i]);
		}
		for (int i = 0; i < gpuPanels.length; i++) {
			gpuPanels[i] = new GpuPanel(timelineGroup, systemInformation.gpus[i]);
		}

		List<GraphTypeButton> buttons = new ArrayList<>();
		buttons.add(cpuPanel.createGraphButton());
		buttons.add(memoryPanel.createMemoryButton());
		for (int i = 0; i < diskPanels.length; i++) {
			GraphTypeButton button = diskPanels[i].createGraphButton(i);
			buttons.add(button); // TODO make this more versatile so that you can continuously add/remove disks
		}
		for (int i = 0; i < networkPanels.length; i++) {
			GraphTypeButton button = networkPanels[i].createGraphButton(i);
			if (systemInformation.networks[i].isEnabled) { // TODO make this more versatile so that you can continuously add/remove interfaces
				buttons.add(button);
			}
		}
		for (int i = 0; i < gpuPanels.length; i++) {
			GraphTypeButton button = gpuPanels[i].createGraphButton(i);
			buttons.add(button);
		}

		graphSelectionPanel = new GraphSelectionPanel(this, buttons.toArray(new GraphTypeButton[0]));
		JScrollPane selectionPanelContainer = new JScrollPane(graphSelectionPanel);

		add(selectionPanelContainer);
		add(selectedPanelContainer);

		selectedPanelContainer.setViewportView(cpuPanel);
	}

	public void update(SystemInformation systemInformation) {
		memoryPanel.update(systemInformation);
		cpuPanel.update(systemInformation);

		for (DiskPanel diskPanel : diskPanels) {
			diskPanel.update();
		}
		for (NetworkPanel networkPanel : networkPanels) {
			networkPanel.update();
		}
		for (GpuPanel gpuPanel : gpuPanels) {
			gpuPanel.update();
		}
	}

	@Override
	public void swapTo(GraphType type, int index) {
		graphSelectionPanel.deselectAll();
		if (type == GraphType.Cpu) {
			selectedPanelContainer.setViewportView(cpuPanel);
		} else if (type == GraphType.Memory) {
			selectedPanelContainer.setViewportView(memoryPanel);
		} else if (type == GraphType.Disk) {
			selectedPanelContainer.setViewportView(diskPanels[index]);
		} else if (type == GraphType.Network) {
			selectedPanelContainer.setViewportView(networkPanels[index]);
		} else if (type == GraphType.Gpu) {
			selectedPanelContainer.setViewportView(gpuPanels[index]);
		}

		revalidate();
		repaint();
	}
}