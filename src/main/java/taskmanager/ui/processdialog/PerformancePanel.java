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

package taskmanager.ui.processdialog;

import config.Config;
import net.miginfocom.swing.MigLayout;
import taskmanager.data.Process;
import taskmanager.ui.TextUtils;
import taskmanager.ui.TextUtils.ValueType;
import taskmanager.ui.performance.GraphPanel;
import taskmanager.ui.performance.GraphPanel.Graph.GraphBuilder;
import taskmanager.ui.performance.GraphType;
import taskmanager.ui.performance.TimelineGraphPanel;
import taskmanager.ui.performance.TimelineGroup;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class PerformancePanel extends JPanel {
	private final Process process;

	private final JLabel labelCpuCurrent;
	private final JLabel labelMemoryCurrent;

	private final GraphPanel cpuGraph;
	private final GraphPanel memoryGraph;

	private final TimelineGraphPanel cpuTimeline;
	private final TimelineGraphPanel memoryTimeline;

	private final JPanel containerPanel;
	private JPanel currentTimeline;

	public PerformancePanel(Process process) {
		this.process = process;

		JLabel labelCpu = new JLabel("CPU utilization");
		JLabel labelMemory = new JLabel("Memory utilization");

		labelCpuCurrent = new JLabel("100%", SwingConstants.RIGHT);
		labelMemoryCurrent = new JLabel("999999 MB", SwingConstants.RIGHT);

		// Set preferred sizes to stop GBL from giving the graphs different widths
		labelCpu.setPreferredSize(labelMemory.getPreferredSize());
		labelCpuCurrent.setPreferredSize(labelMemoryCurrent.getPreferredSize());
		labelMemoryCurrent.setPreferredSize(labelMemoryCurrent.getPreferredSize());

		cpuGraph = new GraphPanel();
		memoryGraph = new GraphPanel();

		cpuGraph.addGraph(new GraphBuilder(process.cpuUsage, GraphType.Cpu).build());
		cpuGraph.setPreferredSize(new Dimension(cpuGraph.getPreferredSize().width * 2, cpuGraph.getPreferredSize().height * 2));
		cpuGraph.addMouseListener(mouseListener);
		memoryGraph.addGraph(new GraphBuilder(process.privateWorkingSet, GraphType.Memory).build());
		memoryGraph.setPreferredSize(new Dimension(memoryGraph.getPreferredSize().width * 2, memoryGraph.getPreferredSize().height * 2));
		memoryGraph.addMouseListener(mouseListener);

		JLabel labelZero = new JLabel(""); // TODO remove?
		JLabel labelMaxTime = new JLabel("Displaying 60 seconds");

		TimelineGroup group = new TimelineGroup();
		cpuTimeline = new TimelineGraphPanel(labelMaxTime);
		memoryTimeline = new TimelineGraphPanel(labelMaxTime);

		cpuTimeline.addGraph(new GraphBuilder(process.cpuUsage, GraphType.Cpu).build());
		cpuTimeline.connectGraphPanels(cpuGraph, memoryGraph);
		memoryTimeline.addGraph(new GraphBuilder(process.privateWorkingSet, GraphType.Memory).build());
		memoryTimeline.connectGraphPanels(memoryGraph, cpuGraph);

		group.add(cpuTimeline);
		group.add(memoryTimeline);
		group.setLinked(true);

		cpuGraph.setSelected(true);
		cpuGraph.setMaxDatapointValue(Config.DOUBLE_TO_LONG);
		cpuTimeline.setMaxDatapointValue(Config.DOUBLE_TO_LONG);

		containerPanel = new JPanel();
		containerPanel.setLayout(new GridLayout(1, 1));
		containerPanel.add(cpuTimeline);
		currentTimeline = cpuTimeline;

		setLayout(new MigLayout());
		add(labelCpu);
		add(labelCpuCurrent);
		add(labelMemory);
		add(labelMemoryCurrent, "wrap");

		add(cpuGraph, "push, grow, sg 1, span 2");
		add(memoryGraph, "push, grow, sg 1, span 2, wrap");

		add(labelMaxTime, "span 4, wrap");

		add(containerPanel, "growx, span 4");

		update(); // Read initial values
	}

	public void update() {
		// TODO Make Memory/CPU max computation more intelligent?
		long memoryMax = (long) (Math.max(128, process.privateWorkingSet.max()) * 1.1f);
		memoryGraph.setMaxDatapointValue(memoryMax);
		memoryTimeline.setMaxDatapointValue(memoryMax);

		long cpuMax = (long) Math.min(Config.DOUBLE_TO_LONG, Math.max(Config.DOUBLE_TO_LONG / 100f, process.cpuUsage.max()* 1.1f));
		cpuGraph.setMaxDatapointValue(cpuMax);
		cpuTimeline.setMaxDatapointValue(cpuMax);

		labelMemoryCurrent.setText(TextUtils.valueToString(process.privateWorkingSet.newest(), ValueType.Bytes));
		labelCpuCurrent.setText(TextUtils.valueToString(process.cpuUsage.newest(), ValueType.Percentage));

		cpuGraph.newDatapoint();
		memoryGraph.newDatapoint();

		cpuTimeline.newDatapoint();
		memoryTimeline.newDatapoint();
	}

	private final MouseListener mouseListener = new MouseAdapter() {
		@Override
		public void mousePressed(MouseEvent e) {
			if (e.getButton() == MouseEvent.BUTTON1) {
				JPanel newTimeline = null;
				if (e.getComponent() == cpuGraph) {
					newTimeline = cpuTimeline;
					memoryGraph.setSelected(false);
					cpuGraph.setSelected(true);
				} else if (e.getComponent() == memoryGraph) {
					newTimeline = memoryTimeline;
					memoryGraph.setSelected(true);
					cpuGraph.setSelected(false);
				}

				if (newTimeline != currentTimeline) {
					containerPanel.removeAll();
					containerPanel.add(newTimeline);
					containerPanel.revalidate();
					containerPanel.repaint();
					currentTimeline = newTimeline;
				}
			}
		}
	};
}
