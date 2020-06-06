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
import taskmanager.platform.linux.LinuxExtraInformation;
import taskmanager.platform.win32.WindowsExtraInformation;
import taskmanager.ui.SimpleGridBagLayout;
import taskmanager.ui.TextUtils.ValueType;
import taskmanager.ui.performance.GraphPanel;
import taskmanager.ui.performance.GraphPanel.ShortToLong;
import taskmanager.ui.performance.GraphType;
import taskmanager.ui.performance.GraphTypeButton;
import taskmanager.ui.performance.InformationItemPanel;
import taskmanager.ui.performance.RatioItemPanel;
import taskmanager.ui.performance.TimelineGraphPanel;
import taskmanager.ui.performance.TimelineGroup;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;

public class CpuPanel extends JPanel {
	private final Measurements<Long> cpuUsage;

	private final JPanel containerPanel;

	private final GraphPanel singleCpuPanel;
	private final MultiCpuPanel multiCpuPanel;
	private final TimelineGraphPanel timelineGraph;

	private final InformationItemPanel utilizationLabel;
	private final InformationItemPanel processesLabel;
	private final InformationItemPanel threadsLabel;
	private final InformationItemPanel uptimeLabel;

	// Windows specific
	private final InformationItemPanel handlesLabel;

	// Linux specific
	private final RatioItemPanel fileDescriptorsLabel;

	private GraphTypeButton connectedButton;


	public CpuPanel(TimelineGroup timelineGroup, SystemInformation systemInformation) {
		cpuUsage = new ShortToLong(systemInformation.cpuUsageTotal);

		JLabel labelHeader = new JLabel("CPU");
		labelHeader.setFont(labelHeader.getFont().deriveFont(24f));

		JLabel labelCpuUtilization = new JLabel("% utilization");
		JLabel labelZero = new JLabel("0");
		JLabel labelMaxTime = new JLabel("Displaying 60 seconds");
		JLabel labelMaxCpu = new JLabel("100%");

		singleCpuPanel = new GraphPanel(GraphType.Cpu, ValueType.Percentage);
		timelineGraph = new TimelineGraphPanel(singleCpuPanel, labelMaxTime);
		multiCpuPanel = new MultiCpuPanel(timelineGraph, systemInformation);

		singleCpuPanel.addGraph(cpuUsage, systemInformation.cpuTopList);
		timelineGraph.addGraph(cpuUsage);
		timelineGroup.add(timelineGraph);

		containerPanel = new JPanel();
		containerPanel.setLayout(new GridLayout(1, 1));
		containerPanel.add(singleCpuPanel);

		JPanel realTimePanel = new JPanel();
		utilizationLabel = new InformationItemPanel("Utilization", ValueType.Percentage);
		processesLabel = new InformationItemPanel("Processes", ValueType.Raw);
		threadsLabel = new InformationItemPanel("Threads", ValueType.Raw);
		uptimeLabel = new InformationItemPanel("Uptime", ValueType.TimeFull);
		handlesLabel = new InformationItemPanel("Handles", ValueType.Raw);
		fileDescriptorsLabel = new RatioItemPanel("Open file descriptors", ValueType.Raw);

		SimpleGridBagLayout realTimeLayout = new SimpleGridBagLayout(realTimePanel);
		realTimeLayout.addToGrid(utilizationLabel, 0, 0, 1, 1, GridBagConstraints.HORIZONTAL, 1, 0);
		realTimeLayout.addToGrid(processesLabel, 1, 0, 1, 1, GridBagConstraints.HORIZONTAL, 1, 0);
		realTimeLayout.addToGrid(threadsLabel, 0, 1, 1, 1, GridBagConstraints.HORIZONTAL, 1, 0);
		realTimeLayout.addToGrid(uptimeLabel, 0, 2, 1, 1, GridBagConstraints.HORIZONTAL, 1, 0);
		if (systemInformation.extraInformation instanceof WindowsExtraInformation) {
			realTimeLayout.addToGrid(handlesLabel, 1, 1, 1, 1, GridBagConstraints.HORIZONTAL, 1, 0);
		} else if (systemInformation.extraInformation instanceof LinuxExtraInformation) {
			realTimeLayout.addToGrid(fileDescriptorsLabel, 1, 1, 1, 1, GridBagConstraints.HORIZONTAL, 1, 0);
		}

		SimpleGridBagLayout layout = new SimpleGridBagLayout(this);
		layout.addToGrid(labelHeader, 0, 0, 1, 1, GridBagConstraints.WEST);
		layout.setInsets(0, 5, 0, 5);
		layout.addToGrid(labelCpuUtilization, 0, 1, 1, 1, GridBagConstraints.WEST);
		layout.addToGrid(labelMaxCpu, 1, 1, 1, 1, GridBagConstraints.EAST);
		layout.setInsets(2, 5, 2, 5);
		layout.addToGrid(containerPanel, 0, 2, 2, 1, GridBagConstraints.BOTH, 1, 1);
		layout.setInsets(0, 5, 0, 5);
		layout.addToGrid(labelMaxTime, 0, 3, 1, 1, GridBagConstraints.WEST);
		layout.addToGrid(labelZero, 1, 3, 1, 1, GridBagConstraints.EAST);
		layout.setInsets(5, 5, 5, 5);
		layout.addToGrid(timelineGraph, 0, 4, 2, 1, GridBagConstraints.HORIZONTAL, 1, 0);
		layout.addToGrid(realTimePanel, 0, 5, 2, 1, GridBagConstraints.WEST);

		CpuContextMenu contextMenu = new CpuContextMenu(this);
		setComponentPopupMenu(contextMenu);
		singleCpuPanel.setComponentPopupMenu(contextMenu);
		multiCpuPanel.setComponentPopupMenu(contextMenu);
		timelineGraph.setComponentPopupMenu(contextMenu);
	}


	public void update(SystemInformation systemInformation) {
		final int total = Config.DOUBLE_TO_LONG;

		singleCpuPanel.setMaxDatapointValue(total);
		singleCpuPanel.newDatapoint();

		multiCpuPanel.update();

		timelineGraph.setMaxDatapointValue(total);
		connectedButton.setMaxDatapointValue(total);
		timelineGraph.newDatapoint();
		connectedButton.newDatapoint(cpuUsage.newest());

		utilizationLabel.updateValue(cpuUsage.newest());
		processesLabel.updateValue(systemInformation.totalProcesses);
		threadsLabel.updateValue(systemInformation.totalThreads);
		uptimeLabel.updateValue(systemInformation.uptime);

		if (systemInformation.extraInformation instanceof WindowsExtraInformation) {
			WindowsExtraInformation extraInformation = (WindowsExtraInformation) systemInformation.extraInformation;
			handlesLabel.updateValue(extraInformation.handles);
		} else if (systemInformation.extraInformation instanceof LinuxExtraInformation) {
			LinuxExtraInformation extraInformation = (LinuxExtraInformation) systemInformation.extraInformation;
			fileDescriptorsLabel.setMaximum(extraInformation.openFileDescriptorsLimit);
			fileDescriptorsLabel.updateValue(extraInformation.openFileDescriptors);
		}
	}


	public void showTotalCpu() {
		containerPanel.removeAll();
		containerPanel.add(singleCpuPanel);
		containerPanel.revalidate();
		containerPanel.repaint();
	}

	public void showCoresCpu() {
		containerPanel.removeAll();
		containerPanel.add(multiCpuPanel);
		containerPanel.revalidate();
		containerPanel.repaint();
	}


	public GraphTypeButton createGraphButton() {
		connectedButton = new GraphTypeButton(GraphType.Cpu, ValueType.Percentage, "CPU");
		connectedButton.setIsLogarithmic(singleCpuPanel.isLogarithmic());
		connectedButton.addGraph(cpuUsage);
		return connectedButton;
	}
}