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
import net.miginfocom.swing.MigLayout;
import taskmanager.data.Disk;
import taskmanager.ui.TextUtils;
import taskmanager.ui.TextUtils.ValueType;
import taskmanager.ui.performance.GraphPanel;
import taskmanager.ui.performance.GraphPanel.DoubleToLong;
import taskmanager.ui.performance.GraphPanel.Graph.GraphBuilder;
import taskmanager.ui.performance.GraphType;
import taskmanager.ui.performance.GraphTypeButton;
import taskmanager.ui.performance.TimelineGraphPanel;
import taskmanager.ui.performance.TimelineGroup;
import taskmanager.ui.performance.common.InformationItemPanel;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Font;

import static taskmanager.ui.performance.GraphPanel.Style;

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
		
		activeTimeGraph = new GraphPanel();
		transferGraph = new GraphPanel();
		timelineGraph = new TimelineGraphPanel(labelMaxTime);

		activeTimeGraph.addGraph(new GraphBuilder(new DoubleToLong(disk.activeFraction), GraphType.Disk).build());
		transferGraph.setIsLogarithmic(true);
		transferGraph.addGraph(new GraphBuilder(disk.writeRate, GraphType.Disk).valueType(ValueType.BytesPerSecond).style(new Style(true, "W: ")).build());
		transferGraph.addGraph(new GraphBuilder(disk.readRate, GraphType.Disk).valueType(ValueType.BytesPerSecond).style(new Style(false, "R: ")).build());
		timelineGraph.connectGraphPanels(this.activeTimeGraph, transferGraph);
		timelineGraph.addGraph(new GraphBuilder(new DoubleToLong(disk.activeFraction), GraphType.Disk).build());
		timelineGroup.add(timelineGraph);

		this.activeTimeGraph.setMaxDatapointValue(Config.DOUBLE_TO_LONG);
		timelineGraph.setMaxDatapointValue(Config.DOUBLE_TO_LONG);

		// TODO Currently copy-n-paste strokes from GraphPanel! Improve this somehow!
		JPanel informationPanel = new JPanel();
		activeTimePanel = new InformationItemPanel("Active time    ", ValueType.Percentage); // Wider text here to force the label panel further to the right
		ioQueueLengthPanel = new InformationItemPanel("I/O queue length", ValueType.Raw);
		writeTransferPanel = new InformationItemPanel("Write speed    ", ValueType.BytesPerSecond, new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[] {3f}, 0f), GraphType.Disk.color);
		readTransferPanel = new InformationItemPanel("Read speed    ", ValueType.BytesPerSecond, new BasicStroke(2), GraphType.Disk.color);

		informationPanel.setLayout(new MigLayout("wrap 2"));
		informationPanel.add(activeTimePanel);
		informationPanel.add(ioQueueLengthPanel);
		informationPanel.add(readTransferPanel);
		informationPanel.add(writeTransferPanel);

		JPanel constantsPanel = new JPanel();
		JLabel labelCapacityHeader = new JLabel("Capacity: ");
		JLabel capacityLabel = new JLabel(TextUtils.valueToString(disk.size, ValueType.Bytes));
		JLabel labelModelHeader = new JLabel("Model: ");
		JLabel modelLabel = new JLabel(disk.model);
		
		Font headerFont = labelCapacityHeader.getFont().deriveFont(Font.BOLD);
		labelCapacityHeader.setFont(headerFont);
		labelModelHeader.setFont(headerFont);

		constantsPanel.setLayout(new MigLayout("wrap 2, gapy 10"));
		constantsPanel.add(labelCapacityHeader);
		constantsPanel.add(capacityLabel);
		constantsPanel.add(labelModelHeader);
		constantsPanel.add(modelLabel);

		setLayout(new MigLayout("", "[][grow]"));
		add(labelHeader, "wrap");
		add(labelActiveTime);
		add(labelActiveTimeMax, "ax right, wrap");
		add(activeTimeGraph, "span 2, push, grow, wrap");
		add(labelMaxTime);
		add(labelActiveTimeZero, "ax right, wrap");

		add(labelTransfer);
		add(labelTransferMax, "gaptop 5, ax right, wrap");
		add(transferGraph, "span 2, push, grow, wrap");
		add(labelTransferZero, "skip 1, ax right, wrap");

		add(timelineGraph, "span 2, growx, wrap");
		add(informationPanel);
		add(constantsPanel, "ay top");
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
		connectedButton = new GraphTypeButton(String.format("Disk %d (%s)", disk.index, disk.name), index);
		connectedButton.setIsLogarithmic(activeTimeGraph.isLogarithmic());
		connectedButton.addGraph(new GraphBuilder(new DoubleToLong(disk.activeFraction), GraphType.Disk).build());
		connectedButton.setMaxDatapointValue(Config.DOUBLE_TO_LONG);
		return connectedButton;
	}
}