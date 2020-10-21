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

package taskmanager.ui.performance.gpus;

import config.Config;
import taskmanager.data.Gpu;
import taskmanager.ui.SimpleGridBagLayout;
import taskmanager.ui.TextUtils;
import taskmanager.ui.TextUtils.ValueType;
import taskmanager.ui.performance.GraphPanel;
import taskmanager.ui.performance.GraphType;
import taskmanager.ui.performance.GraphTypeButton;
import taskmanager.ui.performance.common.InformationItemPanel;
import taskmanager.ui.performance.RatioItemPanel;
import taskmanager.ui.performance.TimelineGraphPanel;
import taskmanager.ui.performance.TimelineGroup;
import taskmanager.ui.performance.UnsupportedHardwareGraphPanel;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.GridBagConstraints;

public class GpuPanel extends JPanel {
	private final Gpu gpu;

	private final JLabel labelMemoryMax;

	private final GraphPanel utilizationGraph;
	private final GraphPanel encodeDecodeGraph;
	private final GraphPanel memoryGraph;
	private final GraphPanel temperatureGraph;
	private final TimelineGraphPanel timelineGraph;

	private final InformationItemPanel utilizationPanel;
	private final RatioItemPanel memoryPanel;
	private final InformationItemPanel encodePanel;
	private final InformationItemPanel decodePanel;
	private final InformationItemPanel temperaturePanel;

	private GraphTypeButton connectedButton;

	public GpuPanel(TimelineGroup timelineGroup, Gpu gpu) {
		this.gpu = gpu;

		JLabel labelHeader = new JLabel("GPU " + gpu.index);
		labelHeader.setFont(labelHeader.getFont().deriveFont(24f));

		JLabel labelUtilization = new JLabel("Utilization");
		JLabel labelUtilizationZero = new JLabel("0");
		JLabel labelUtilizationMax = new JLabel("100%");
		JLabel labelMaxTime = new JLabel("Displaying 60 seconds");

		JLabel labelEncodeDecode = new JLabel("Video encode/decode");
		JLabel labelEncodeDecodeZero = new JLabel("0");
		JLabel labelEncodeDecodeMax = new JLabel("100%");

		JLabel labelMemory = new JLabel("Memory");
		JLabel labelMemoryZero = new JLabel("0");
		labelMemoryMax = new JLabel("XX KB");

		JLabel labelTemperature = new JLabel("Temperature");
		JLabel labelTemperatureZero = new JLabel("0");
		JLabel labelTemperatureMax = new JLabel("120 C");

		// To make the graphs equally big (due to GBL)
		labelMemory.setPreferredSize(labelTemperature.getPreferredSize());

		if (gpu.utilizationSupported) {
			utilizationGraph = new GraphPanel(GraphType.Gpu, ValueType.Percentage);
		} else {
			utilizationGraph = new UnsupportedHardwareGraphPanel(GraphType.Gpu);
		}
		if (gpu.encoderSupported || gpu.decoderSupported) {
			encodeDecodeGraph = new GraphPanel(GraphType.Gpu, ValueType.Percentage);
		} else {
			encodeDecodeGraph = new UnsupportedHardwareGraphPanel(GraphType.Gpu);
		}
		if (gpu.memorySupported) {
			memoryGraph = new GraphPanel(GraphType.Gpu, ValueType.Bytes);
		} else {
			memoryGraph = new UnsupportedHardwareGraphPanel(GraphType.Gpu);
		}
		if (gpu.temperatureSupported) {
			temperatureGraph = new GraphPanel(GraphType.Gpu, ValueType.Temperature);
		} else {
			temperatureGraph = new UnsupportedHardwareGraphPanel(GraphType.Gpu);
		}
		timelineGraph = new TimelineGraphPanel(utilizationGraph, labelMaxTime);

		utilizationGraph.addGraph(gpu.utilization);
		encodeDecodeGraph.addGraph(gpu.encoderUtilization, false);
		encodeDecodeGraph.addGraph(gpu.decoderUtilization, true);
		memoryGraph.addGraph(gpu.usedMemory);
		temperatureGraph.addGraph(gpu.temperature);
		timelineGraph.connectGraphs(memoryGraph);
		timelineGraph.connectGraphs(temperatureGraph);
		timelineGraph.addGraph(gpu.utilization);
		timelineGroup.add(timelineGraph);

		utilizationGraph.setMaxDatapointValue(Config.DOUBLE_TO_LONG);
		encodeDecodeGraph.setMaxDatapointValue(Config.DOUBLE_TO_LONG);
		timelineGraph.setMaxDatapointValue(Config.DOUBLE_TO_LONG);
		temperatureGraph.setMaxDatapointValue(120);

		// TODO Currently copy-n-paste strokes from GraphPanel! Improve this somehow!
		utilizationPanel = new InformationItemPanel("Utilization    ", ValueType.Percentage); // Wider text here to force the label panel further to the right
		memoryPanel = new RatioItemPanel("Memory", ValueType.Bytes, gpu.totalMemory);
		encodePanel = new InformationItemPanel("Video encode    ", ValueType.Percentage, new BasicStroke(2), GraphType.Gpu.color);
		decodePanel = new InformationItemPanel("Video decode    ", ValueType.Percentage, new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[] {3f}, 0f), GraphType.Gpu.color);
		temperaturePanel = new InformationItemPanel("Temperature", ValueType.Temperature);

		JLabel labelCapacityHeader = new JLabel("Model: ");
		JLabel capacityLabel = new JLabel(gpu.name);
		JLabel labelModelHeader = new JLabel("Vendor: ");
		JLabel modelLabel = new JLabel(gpu.vendor);
		JLabel labelDriverHeader = new JLabel("Driver version: ");
		JLabel driverLabel = new JLabel(gpu.driverVersion);

		Font headerFont = labelCapacityHeader.getFont().deriveFont(Font.BOLD);
		labelCapacityHeader.setFont(headerFont);
		labelModelHeader.setFont(headerFont);
		
		JPanel utilizationGraphPanel = new JPanel();
		SimpleGridBagLayout layout = new SimpleGridBagLayout(utilizationGraphPanel);
		layout.setInsets(0, 5, 0, 5);
		layout.addToGrid(labelUtilization, 0, 1, 1, 1, GridBagConstraints.WEST);
		layout.addToGrid(labelUtilizationMax, 2, 1, 1, 1, GridBagConstraints.EAST);
		layout.setInsets(2, 5, 2, 5);
		layout.addToGrid(utilizationGraph, 0, 2, 3, 1, GridBagConstraints.BOTH, 1, 1);
		layout.setInsets(0, 5, 5, 5);
		layout.addToGrid(labelMaxTime, 0, 3, 2, 1, GridBagConstraints.WEST);
		layout.addToGrid(labelUtilizationZero, 2, 3, 1, 1, GridBagConstraints.EAST);

		JPanel encodeDecodeGraphPanel = new JPanel();
		layout = new SimpleGridBagLayout(encodeDecodeGraphPanel);
		layout.setInsets(0, 5, 0, 5);
		layout.addToGrid(labelEncodeDecode, 0, 1, 1, 1, GridBagConstraints.WEST);
		layout.addToGrid(labelEncodeDecodeMax, 2, 1, 1, 1, GridBagConstraints.EAST);
		layout.setInsets(2, 5, 2, 5);
		layout.addToGrid(encodeDecodeGraph, 0, 2, 3, 1, GridBagConstraints.BOTH, 1, 1);
		layout.setInsets(0, 5, 5, 5);
		layout.addToGrid(labelEncodeDecodeZero, 2, 3, 1, 1, GridBagConstraints.EAST);

		JPanel memoryAndTemperatureGraphPanel = new JPanel();
		layout = new SimpleGridBagLayout(memoryAndTemperatureGraphPanel);
		layout.setInsets(0, 5, 0, 5);
		layout.addToGrid(labelMemory, 0, 0, 1, 1, GridBagConstraints.WEST);
		layout.addToGrid(labelMemoryMax, 2, 0, 1, 1, GridBagConstraints.EAST);
		layout.setInsets(2, 5, 2, 5);
		layout.addToGrid(memoryGraph, 0, 1, 3, 1, GridBagConstraints.BOTH, 1, 1);
		layout.setInsets(0, 5, 0, 5);
		layout.addToGrid(labelMemoryZero, 2, 2, 1, 1, GridBagConstraints.EAST);

		layout.addToGrid(labelTemperature, 3, 0, 1, 1, GridBagConstraints.WEST);
		layout.addToGrid(labelTemperatureMax, 5, 0, 1, 1, GridBagConstraints.EAST);
		layout.setInsets(2, 5, 2, 5);
		layout.addToGrid(temperatureGraph, 3, 1, 3, 1, GridBagConstraints.BOTH, 1, 1);
		layout.setInsets(0, 5, 0, 5);
		layout.addToGrid(labelTemperatureZero, 5, 2, 1, 1, GridBagConstraints.EAST);

		JPanel metricsPanel = new JPanel();
		layout = new SimpleGridBagLayout(metricsPanel);
		layout.addToGrid(utilizationPanel, 0, 0, 1, 1, GridBagConstraints.HORIZONTAL, 1, 0);
		layout.addToGrid(memoryPanel, 1, 0, 1, 1, GridBagConstraints.HORIZONTAL, 1, 0);
		layout.addToGrid(decodePanel, 0, 1, 1, 1, GridBagConstraints.HORIZONTAL, 1, 0);
		layout.addToGrid(encodePanel, 1, 1, 1, 1, GridBagConstraints.HORIZONTAL, 1, 0);
		layout.addToGrid(temperaturePanel, 0, 2, 1, 1, GridBagConstraints.HORIZONTAL, 1, 0);

		JPanel constantsPanel = new JPanel();
		layout = new SimpleGridBagLayout(constantsPanel);
		layout.addToGrid(labelCapacityHeader, 1, 0, 1, 1, GridBagConstraints.WEST);
		layout.addToGrid(labelModelHeader, 1, 1, 1, 1, GridBagConstraints.WEST);
		layout.addToGrid(labelDriverHeader, 1, 2, 1, 1, GridBagConstraints.WEST);
		layout.addToGrid(capacityLabel, 2, 0, 1, 1, GridBagConstraints.HORIZONTAL, 1, 0);
		layout.addToGrid(modelLabel, 2, 1, 1, 1, GridBagConstraints.HORIZONTAL, 1, 0);
		layout.addToGrid(driverLabel, 2, 2, 1, 1, GridBagConstraints.HORIZONTAL, 1, 0);

		layout = new SimpleGridBagLayout(this);
		layout.addToGrid(labelHeader, 0, 0, 1, 1, GridBagConstraints.WEST);

		layout.setInsets(0, 0, 0, 0);
		layout.addToGrid(utilizationGraphPanel, 0, 1, 3, 1, GridBagConstraints.BOTH, 1, 1);
		layout.addToGrid(encodeDecodeGraphPanel, 0, 2, 3, 1, GridBagConstraints.BOTH, 1, 0.25);
		layout.addToGrid(memoryAndTemperatureGraphPanel, 0, 3, 3, 1, GridBagConstraints.BOTH, 1, 0.25);

		layout.setInsets(5, 5, 5, 5);
		layout.addToGrid(timelineGraph, 0, 4, 3, 1, GridBagConstraints.HORIZONTAL, 1, 0);
		layout.addToGrid(metricsPanel, 0, 5, 1, 1, GridBagConstraints.NORTHWEST);
		layout.addToGrid(constantsPanel, 1, 5, 2, 1, GridBagConstraints.NORTHWEST);
	}


	public void update() {
		labelMemoryMax.setText(TextUtils.valueToString(gpu.totalMemory, ValueType.Bytes));
		memoryPanel.setMaximum(gpu.totalMemory);
		memoryGraph.setMaxDatapointValue(gpu.totalMemory);

		utilizationGraph.newDatapoint();
		encodeDecodeGraph.newDatapoint();
		memoryGraph.newDatapoint();
		temperatureGraph.newDatapoint();
		timelineGraph.newDatapoint();
		connectedButton.newDatapoint(gpu.utilization.newest());

		utilizationPanel.updateValue(gpu.utilization.newest());
		memoryPanel.updateValue(gpu.usedMemory.newest());
		encodePanel.updateValue(gpu.encoderUtilization.newest());
		decodePanel.updateValue(gpu.decoderUtilization.newest());
		temperaturePanel.updateValue(gpu.temperature.newest());
	}


	public GraphTypeButton createGraphButton(int index) {
		connectedButton = new GraphTypeButton(GraphType.Gpu, ValueType.Percentage, String.format("GPU %d (%s)", gpu.index, gpu.name), index);
		connectedButton.setIsLogarithmic(utilizationGraph.isLogarithmic());
		connectedButton.addGraph(gpu.utilization);
		connectedButton.setMaxDatapointValue(Config.DOUBLE_TO_LONG);
		return connectedButton;
	}
}