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
import net.miginfocom.swing.MigLayout;
import taskmanager.Measurements;
import taskmanager.data.SystemInformation;
import taskmanager.platform.linux.LinuxExtraInformation;
import taskmanager.platform.win32.WindowsExtraInformation;
import taskmanager.ui.TextUtils.ValueType;
import taskmanager.ui.performance.GraphPanel;
import taskmanager.ui.performance.GraphPanel.Graph.GraphBuilder;
import taskmanager.ui.performance.GraphPanel.ShortToLong;
import taskmanager.ui.performance.GraphType;
import taskmanager.ui.performance.GraphTypeButton;
import taskmanager.ui.performance.RatioItemPanel;
import taskmanager.ui.performance.ShowProcessCallback;
import taskmanager.ui.performance.TimelineGraphPanel;
import taskmanager.ui.performance.TimelineGroup;
import taskmanager.ui.performance.common.InformationItemPanel;

import javax.swing.JLabel;
import javax.swing.JPanel;
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


	public CpuPanel(TimelineGroup timelineGroup, SystemInformation systemInformation, ShowProcessCallback showProcessCallback) {
		cpuUsage = new ShortToLong(systemInformation.cpuUsageTotal);

		JLabel labelHeader = new JLabel("CPU");
		labelHeader.setFont(labelHeader.getFont().deriveFont(24f));

		JLabel labelCpuUtilization = new JLabel("% utilization");
		JLabel labelZero = new JLabel("0");
		JLabel labelMaxTime = new JLabel("Displaying 60 seconds");
		JLabel labelMaxCpu = new JLabel("100%");

		singleCpuPanel = new GraphPanel();
		timelineGraph = new TimelineGraphPanel(labelMaxTime);
		multiCpuPanel = new MultiCpuPanel(timelineGraph, systemInformation);

		singleCpuPanel.addGraph(new GraphBuilder(cpuUsage, GraphType.Cpu).topList(systemInformation.cpuTopList).build());
		timelineGraph.connectGraphPanels(singleCpuPanel);
		timelineGraph.addGraph(new GraphBuilder(cpuUsage, GraphType.Cpu).build());
		timelineGroup.add(timelineGraph);

		containerPanel = new JPanel();
		containerPanel.setLayout(new GridLayout(1, 1));
		containerPanel.add(singleCpuPanel);

		JPanel informationPanel = new JPanel();
		utilizationLabel = new InformationItemPanel("Utilization", ValueType.Percentage);
		processesLabel = new InformationItemPanel("Processes", ValueType.Raw);
		threadsLabel = new InformationItemPanel("Threads", ValueType.Raw);
		uptimeLabel = new InformationItemPanel("Uptime", ValueType.TimeFull);
		handlesLabel = new InformationItemPanel("Handles", ValueType.Raw);
		fileDescriptorsLabel = new RatioItemPanel("Open file descriptors", ValueType.Raw);

		informationPanel.setLayout(new MigLayout("wrap 2"));
		informationPanel.add(utilizationLabel);
		informationPanel.add(processesLabel);
		informationPanel.add(threadsLabel);
		if (systemInformation.extraInformation instanceof WindowsExtraInformation) {
			informationPanel.add(handlesLabel);
		} else if (systemInformation.extraInformation instanceof LinuxExtraInformation) {
			informationPanel.add(fileDescriptorsLabel);
		}
		informationPanel.add(uptimeLabel);

		setLayout(new MigLayout());
		add(labelHeader, "wrap");
		add(labelCpuUtilization);
		add(labelMaxCpu, "wrap");
		add(containerPanel, "span 2, push, grow, wrap");
		add(labelMaxTime);
		add(labelZero, "ax right, wrap");
		add(timelineGraph, "span 2, growx, wrap");
		add(informationPanel, "span 2");

		CpuContextMenu contextMenu = new CpuContextMenu(this, systemInformation.cpuTopList, showProcessCallback);
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
		connectedButton = new GraphTypeButton("CPU");
		connectedButton.setIsLogarithmic(singleCpuPanel.isLogarithmic());
		connectedButton.addGraph(new GraphBuilder(cpuUsage, GraphType.Cpu).build());
		return connectedButton;
	}
}