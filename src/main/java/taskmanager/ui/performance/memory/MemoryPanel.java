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

package taskmanager.ui.performance.memory;

import net.miginfocom.swing.MigLayout;
import taskmanager.Measurements;
import taskmanager.data.SystemInformation;
import taskmanager.platform.linux.LinuxExtraInformation;
import taskmanager.platform.osx.OsXExtraInformation;
import taskmanager.platform.win32.WindowsExtraInformation;
import taskmanager.ui.TextUtils;
import taskmanager.ui.TextUtils.ValueType;
import taskmanager.ui.performance.GraphPanel;
import taskmanager.ui.performance.GraphPanel.Graph.GraphBuilder;
import taskmanager.ui.performance.GraphType;
import taskmanager.ui.performance.GraphTypeButton;
import taskmanager.ui.performance.RatioItemPanel;
import taskmanager.ui.callbacks.ShowProcessCallback;
import taskmanager.ui.performance.TimelineGraphPanel;
import taskmanager.ui.performance.TimelineGroup;
import taskmanager.ui.performance.common.InformationItemPanel;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Font;

public class MemoryPanel extends JPanel {
	private final Measurements<Long> memoryAvailable;

	private final JLabel labelMaxMemory;

	private final GraphPanel memoryGraph;
	private final TimelineGraphPanel timelineGraph;
	private final MemoryCompositionPanel memoryComposition;

	private final InformationItemPanel inUsePanel;
	private final InformationItemPanel availablePanel;

	// Windows specific
	private final RatioItemPanel committedPanel;
	private final InformationItemPanel cachedPanel;
	private final InformationItemPanel pagedPoolPanel;
	private final InformationItemPanel nonpagedPoolPanel;

	// Linux specific
	private final InformationItemPanel sharedPanel;
	private final RatioItemPanel swapPanel;

	private GraphTypeButton connectedButton;


	public MemoryPanel(TimelineGroup timelineGroup, SystemInformation systemInformation, ShowProcessCallback showProcessCallback) {
		memoryAvailable = systemInformation.memoryUsed;

		JLabel labelHeader = new JLabel("Memory");
		labelHeader.setFont(labelHeader.getFont().deriveFont(24f));

		JLabel labelMemoryUsage = new JLabel("Memory usage");
		JLabel labelZero = new JLabel("0");
		JLabel labelMaxTime = new JLabel("Displaying 60 seconds");
		labelMaxMemory = new JLabel("XX GB");
		JLabel labelComposition = new JLabel("Memory composition");

		memoryGraph = new GraphPanel();
		timelineGraph = new TimelineGraphPanel(labelMaxTime);
		memoryComposition = new MemoryCompositionPanel(systemInformation);

		memoryGraph.addGraph(new GraphBuilder(memoryAvailable, GraphType.Memory).topList(systemInformation.memoryUsedTopList).build());
		timelineGraph.connectGraphPanels(memoryGraph);
		timelineGraph.addGraph(new GraphBuilder(memoryAvailable, GraphType.Memory).build());
		timelineGroup.add(timelineGraph);

		JPanel informationPanel = new JPanel();
		inUsePanel = new InformationItemPanel("In use", ValueType.Bytes);
		availablePanel = new InformationItemPanel("Available", ValueType.Bytes);
		committedPanel = new RatioItemPanel("Committed", ValueType.Bytes);
		cachedPanel = new InformationItemPanel("Cached", ValueType.Bytes);
		pagedPoolPanel = new InformationItemPanel("Paged pool", ValueType.Bytes);
		nonpagedPoolPanel = new InformationItemPanel("Non-paged pool", ValueType.Bytes);
		sharedPanel = new InformationItemPanel("Shared memory", ValueType.Bytes);
		swapPanel = new RatioItemPanel("Swap", ValueType.Bytes);

		Font dataFont = inUsePanel.getFont().deriveFont(Font.BOLD, inUsePanel.getFont().getSize() + 3f);
		inUsePanel.setFont(dataFont);
		availablePanel.setFont(dataFont);
		committedPanel.setFont(dataFont);
		cachedPanel.setFont(dataFont);
		pagedPoolPanel.setFont(dataFont);
		nonpagedPoolPanel.setFont(dataFont);

		informationPanel.setLayout(new MigLayout("wrap 2"));
		informationPanel.add(inUsePanel);
		informationPanel.add(availablePanel);
		if (systemInformation.extraInformation instanceof WindowsExtraInformation) {
			informationPanel.add(committedPanel);
			informationPanel.add(cachedPanel);
			informationPanel.add(pagedPoolPanel);
			informationPanel.add(nonpagedPoolPanel);
		} else if (systemInformation.extraInformation instanceof LinuxExtraInformation) {
			informationPanel.add(sharedPanel);
			informationPanel.add(swapPanel);
		} else if (systemInformation.extraInformation instanceof OsXExtraInformation) {
			informationPanel.add(swapPanel);
		}

		setLayout(new MigLayout());
		add(labelHeader, "wrap");
		add(labelMemoryUsage);
		add(labelMaxMemory, "wrap");
		add(memoryGraph, "span 2, push, grow, wrap");
		add(labelMaxTime);
		add(labelZero, "ax right, wrap");
		add(timelineGraph, "span 2, growx, wrap");
		add(labelComposition, "wrap");
		add(memoryComposition, "span 2, growx, wrap");
		add(informationPanel, "span 2");

		MemoryContextMenu contextMenu = new MemoryContextMenu(systemInformation.memoryUsedTopList, showProcessCallback);
		setComponentPopupMenu(contextMenu);
		memoryGraph.setComponentPopupMenu(contextMenu);
		timelineGraph.setComponentPopupMenu(contextMenu);
	}


	public void update(SystemInformation systemInformation) {
		labelMaxMemory.setText(TextUtils.valueToString(systemInformation.physicalMemoryTotal, ValueType.Bytes));

		long memoryUsed = systemInformation.memoryUsed.newest();

		memoryGraph.setMaxDatapointValue(systemInformation.physicalMemoryTotal);
		timelineGraph.setMaxDatapointValue(systemInformation.physicalMemoryTotal);
		connectedButton.setMaxDatapointValue(systemInformation.physicalMemoryTotal);
		memoryGraph.newDatapoint();
		timelineGraph.newDatapoint();
		connectedButton.newDatapoint(memoryUsed);

		memoryComposition.update(systemInformation);

		// Labels
		inUsePanel.updateValue(memoryUsed);
		availablePanel.updateValue(systemInformation.physicalMemoryTotal - memoryUsed);

		if (systemInformation.extraInformation instanceof WindowsExtraInformation) {
			WindowsExtraInformation extraInformation = (WindowsExtraInformation) systemInformation.extraInformation;
			committedPanel.setMaximum(extraInformation.commitLimit);
			committedPanel.updateValue(extraInformation.commitUsed);
			cachedPanel.updateValue(extraInformation.standbyMemory + extraInformation.modifiedMemory);
			pagedPoolPanel.updateValue(extraInformation.kernelPaged);
			nonpagedPoolPanel.updateValue(extraInformation.kernelNonPaged);
		} else if (systemInformation.extraInformation instanceof LinuxExtraInformation) {
			LinuxExtraInformation extraInformation = (LinuxExtraInformation) systemInformation.extraInformation;
			sharedPanel.updateValue(extraInformation.sharedMemory);
			swapPanel.setMaximum(extraInformation.swapSize);
			swapPanel.updateValue(extraInformation.swapUsed);
		} else if (systemInformation.extraInformation instanceof OsXExtraInformation) {
			OsXExtraInformation extraInformation = (OsXExtraInformation) systemInformation.extraInformation;
			swapPanel.setMaximum(extraInformation.swapSize);
			swapPanel.updateValue(extraInformation.swapUsed);
		}
	}


	public GraphTypeButton createMemoryButton() {
		connectedButton = new GraphTypeButton("Memory");
		connectedButton.setIsLogarithmic(memoryGraph.isLogarithmic());
		connectedButton.addGraph(new GraphBuilder(memoryAvailable, GraphType.Memory).build());
		return connectedButton;
	}
}