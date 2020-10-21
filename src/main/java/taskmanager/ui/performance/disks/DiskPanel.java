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

package taskmanager.ui.performance.disks;

import config.Config;
import taskmanager.data.Disk;
import taskmanager.ui.SimpleGridBagLayout;
import taskmanager.ui.TextUtils;
import taskmanager.ui.TextUtils.ValueType;
import taskmanager.ui.performance.GraphPanel;
import taskmanager.ui.performance.GraphPanel.DoubleToLong;
import taskmanager.ui.performance.GraphType;
import taskmanager.ui.performance.GraphTypeButton;
import taskmanager.ui.performance.common.InformationItemPanel;
import taskmanager.ui.performance.TimelineGraphPanel;
import taskmanager.ui.performance.TimelineGroup;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.GridBagConstraints;

public class DiskPanel extends JPanel {
	private final Disk disk;

	private final JLabel labelTransferMax;
	
	private final GraphPanel activeTimeGraph;
	private final GraphPanel transferGraph;
	private final TimelineGraphPanel timelineGraph;
	
	private final InformationItemPanel activeTimePanel;
	private final InformationItemPanel ioQueueLengthPanel;
	private final InformationItemPanel writeTransferPanel;
	private final InformationItemPanel readTransferPanel;
	
	private GraphTypeButton connectedButton;
	
	public DiskPanel(TimelineGroup timelineGroup, Disk disk) {
		this.disk = disk;
		
		JLabel labelHeader = new JLabel("Disk " + disk.index);
		labelHeader.setFont(labelHeader.getFont().deriveFont(24f));
		
		JLabel labelActiveTime = new JLabel("Active time");
		JLabel labelActiveTimeZero = new JLabel("0");
		JLabel labelActiveTimeMax = new JLabel("100%");
		JLabel labelMaxTime = new JLabel("Displaying 60 seconds");
		JLabel labelTransfer = new JLabel("Disk transfer rate");
		JLabel labelTransferZero = new JLabel("0");
		labelTransferMax = new JLabel("XX KB");
		
		activeTimeGraph = new GraphPanel(GraphType.Disk, ValueType.Percentage);
		transferGraph = new GraphPanel(GraphType.Disk, ValueType.BytesPerSecond);
		timelineGraph = new TimelineGraphPanel(activeTimeGraph, labelMaxTime);
		
		activeTimeGraph.addGraph(new DoubleToLong(disk.activeFraction));
		transferGraph.setIsLogarithmic(true);
		transferGraph.addGraph(disk.writeRate, true);
		transferGraph.addGraph(disk.readRate, false);
		timelineGraph.connectGraphs(transferGraph);
		timelineGraph.addGraph(new DoubleToLong(disk.activeFraction));
		timelineGroup.add(timelineGraph);

		activeTimeGraph.setMaxDatapointValue(Config.DOUBLE_TO_LONG);
		timelineGraph.setMaxDatapointValue(Config.DOUBLE_TO_LONG);

		// TODO Currently copy-n-paste strokes from GraphPanel! Improve this somehow!
		JPanel realTimePanel = new JPanel();
		activeTimePanel = new InformationItemPanel("Active time    ", ValueType.Percentage); // Wider text here to force the label panel further to the right
		ioQueueLengthPanel = new InformationItemPanel("I/O queue length", ValueType.Raw);
		writeTransferPanel = new InformationItemPanel("Write speed    ", ValueType.BytesPerSecond, new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[] {3f}, 0f), GraphType.Disk.color);
		readTransferPanel = new InformationItemPanel("Read speed    ", ValueType.BytesPerSecond, new BasicStroke(2), GraphType.Disk.color);
		
		SimpleGridBagLayout realTimeLayout = new SimpleGridBagLayout(realTimePanel);
		realTimeLayout.addToGrid(activeTimePanel, 0, 0, 1, 1, GridBagConstraints.HORIZONTAL, 1, 0);
		realTimeLayout.addToGrid(ioQueueLengthPanel, 1, 0, 1, 1, GridBagConstraints.HORIZONTAL, 1, 0);
		realTimeLayout.addToGrid(readTransferPanel, 0, 1, 1, 1, GridBagConstraints.HORIZONTAL, 1, 0);
		realTimeLayout.addToGrid(writeTransferPanel, 1, 1, 1, 1, GridBagConstraints.HORIZONTAL, 1, 0);

		JPanel constantsPanel = new JPanel();
		JLabel labelCapacityHeader = new JLabel("Capacity: ");
		JLabel capacityLabel = new JLabel(TextUtils.valueToString(disk.size, ValueType.Bytes));
		JLabel labelModelHeader = new JLabel("Model: ");
		JLabel modelLabel = new JLabel(disk.model);
		
		Font headerFont = labelCapacityHeader.getFont().deriveFont(Font.BOLD);
		labelCapacityHeader.setFont(headerFont);
		labelModelHeader.setFont(headerFont);
		
		SimpleGridBagLayout labelLayout = new SimpleGridBagLayout(constantsPanel);
		labelLayout.addToGrid(labelCapacityHeader, 1, 0, 1, 1, GridBagConstraints.WEST);
		labelLayout.addToGrid(labelModelHeader , 1, 1, 1, 1, GridBagConstraints.WEST);
		labelLayout.addToGrid(capacityLabel, 2, 0, 1, 1, GridBagConstraints.HORIZONTAL, 1, 0);
		labelLayout.addToGrid(modelLabel , 2, 1, 1, 1, GridBagConstraints.HORIZONTAL, 1, 0);
		
		SimpleGridBagLayout layout = new SimpleGridBagLayout(this);
		layout.addToGrid(labelHeader, 0, 0, 1, 1, GridBagConstraints.WEST);
		layout.setInsets(0, 5, 0, 5);
		layout.addToGrid(labelActiveTime, 0, 1, 1, 1, GridBagConstraints.WEST);
		layout.addToGrid(labelActiveTimeMax , 2, 1, 1, 1, GridBagConstraints.EAST);
		layout.setInsets(2, 5, 2, 5);
		layout.addToGrid(activeTimeGraph, 0, 2, 3, 1, GridBagConstraints.BOTH, 1, 1);
		layout.setInsets(0, 5, 5, 5);
		layout.addToGrid(labelMaxTime, 0, 3, 2, 1, GridBagConstraints.WEST);
		layout.addToGrid(labelActiveTimeZero, 2, 3, 1, 1, GridBagConstraints.EAST);

		layout.setInsets(0, 5, 0, 5);
		layout.addToGrid(labelTransfer, 0, 4, 1, 1, GridBagConstraints.WEST);
		layout.addToGrid(labelTransferMax, 2, 4, 1, 1, GridBagConstraints.EAST);
		layout.setInsets(2, 5, 2, 5);
		layout.addToGrid(transferGraph, 0, 5, 3, 1, GridBagConstraints.BOTH, 1, 1);
		layout.setInsets(0, 5, 0, 5);
//		layout.addToGrid(labelMaxTime, 0, 6, 2, 1, GridBagConstraints.WEST);
		layout.addToGrid(labelTransferZero, 2, 6, 1, 1, GridBagConstraints.EAST);
		layout.setInsets(5, 5, 5, 5);
		layout.addToGrid(timelineGraph, 0, 7, 3, 1, GridBagConstraints.HORIZONTAL, 1, 0);
		layout.addToGrid(realTimePanel, 0, 8, 1, 1, GridBagConstraints.NORTHWEST);
		layout.addToGrid(constantsPanel, 1, 8, 2, 2, GridBagConstraints.NORTHWEST);
	}

	
	public void update() {
		long max = Math.max(100 * 1024, Math.max(disk.writeRate.max(), disk.readRate.max()));
		labelTransferMax.setText(TextUtils.valueToString(max, ValueType.BytesPerSecond));
		transferGraph.setMaxDatapointValue(max);

		activeTimeGraph.newDatapoint();
		transferGraph.newDatapoint();
		timelineGraph.newDatapoint();
		connectedButton.newDatapoint((long)(disk.activeFraction.newest() * Config.DOUBLE_TO_LONG));
		
		activeTimePanel.updateValue((long)(disk.activeFraction.newest() * Config.DOUBLE_TO_LONG));
		ioQueueLengthPanel.updateValue(disk.ioQueueLength.newest());
		writeTransferPanel.updateValue(disk.writeRate.newest());
		readTransferPanel.updateValue(disk.readRate.newest());
	}


	public GraphTypeButton createGraphButton(int index) {
		connectedButton = new GraphTypeButton(GraphType.Disk, ValueType.Percentage, String.format("Disk %d (%s)", disk.index, disk.name), index);
		connectedButton.setIsLogarithmic(activeTimeGraph.isLogarithmic());
		connectedButton.addGraph(new DoubleToLong(disk.activeFraction));
		connectedButton.setMaxDatapointValue(Config.DOUBLE_TO_LONG);
		return connectedButton;
	}
}