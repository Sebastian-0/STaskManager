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
import net.miginfocom.swing.MigLayout;
import taskmanager.data.Gpu;
import taskmanager.ui.TextUtils;
import taskmanager.ui.TextUtils.ValueType;
import taskmanager.ui.performance.GraphPanel;
import taskmanager.ui.performance.GraphPanel.Graph;
import taskmanager.ui.performance.GraphPanel.Graph.GraphBuilder;
import taskmanager.ui.performance.GraphPanel.Style;
import taskmanager.ui.performance.GraphType;
import taskmanager.ui.performance.GraphTypeButton;
import taskmanager.ui.performance.RatioItemPanel;
import taskmanager.ui.performance.TimelineGraphPanel;
import taskmanager.ui.performance.TimelineGroup;
import taskmanager.ui.performance.UnsupportedHardwareGraphPanel;
import taskmanager.ui.performance.common.InformationItemPanel;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Font;

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
			utilizationGraph = new GraphPanel();
		} else {
			utilizationGraph = new UnsupportedHardwareGraphPanel(GraphType.Gpu);
		}
		if (gpu.encoderSupported || gpu.decoderSupported) {
			encodeDecodeGraph = new GraphPanel();
		} else {
			encodeDecodeGraph = new UnsupportedHardwareGraphPanel(GraphType.Gpu);
		}
		if (gpu.memorySupported) {
			memoryGraph = new GraphPanel();
		} else {
			memoryGraph = new UnsupportedHardwareGraphPanel(GraphType.Gpu);
		}
		if (gpu.temperatureSupported) {
			temperatureGraph = new GraphPanel();
		} else {
			temperatureGraph = new UnsupportedHardwareGraphPanel(GraphType.Gpu);
		}
		timelineGraph = new TimelineGraphPanel(labelMaxTime);

		Graph encodeGraph = new GraphBuilder(gpu.encoderUtilization, GraphType.Gpu).style(new Style(false, "E: ")).build();
		Graph decodeGraph = new GraphBuilder(gpu.decoderUtilization, GraphType.Gpu).style(new Style(false, "D: ")).build();
		utilizationGraph.addGraph(new GraphBuilder(gpu.utilization, GraphType.Gpu).build());
		encodeDecodeGraph.addGraph(encodeGraph);
		encodeDecodeGraph.addGraph(decodeGraph);
		memoryGraph.addGraph(new GraphBuilder(gpu.usedMemory, GraphType.Gpu).valueType(ValueType.Bytes).build());
		temperatureGraph.addGraph(new GraphBuilder(gpu.temperature, GraphType.Gpu).valueType(ValueType.Temperature).build());
		timelineGraph.connectGraphPanels(utilizationGraph, memoryGraph, temperatureGraph);
		timelineGraph.addGraph(new GraphBuilder(gpu.utilization, GraphType.Gpu).build());
		timelineGroup.add(timelineGraph);

		utilizationGraph.setMaxDatapointValue(Config.DOUBLE_TO_LONG);
		encodeDecodeGraph.setMaxDatapointValue(Config.DOUBLE_TO_LONG);
		timelineGraph.setMaxDatapointValue(Config.DOUBLE_TO_LONG);
		temperatureGraph.setMaxDatapointValue(120);

		utilizationPanel = new InformationItemPanel("Utilization    ", ValueType.Percentage); // Wider text here to force the label panel further to the right
		memoryPanel = new RatioItemPanel("Memory", ValueType.Bytes, gpu.totalMemory);
		encodePanel = new InformationItemPanel("Video encode    ", encodeGraph);
		decodePanel = new InformationItemPanel("Video decode    ", decodeGraph);
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
		
		JPanel utilizationGraphPanel = new JPanel(new MigLayout("ins 0 0 0 0"));
		utilizationGraphPanel.add(labelUtilization);
		utilizationGraphPanel.add(labelUtilizationMax, "wrap");
		utilizationGraphPanel.add(utilizationGraph, "span 2, push, grow, wrap");
		utilizationGraphPanel.add(labelMaxTime);
		utilizationGraphPanel.add(labelUtilizationZero, "ax right");

		JPanel encodeDecodeGraphPanel = new JPanel(new MigLayout("ins 0 0 0 0"));
		encodeDecodeGraphPanel.add(labelEncodeDecode);
		encodeDecodeGraphPanel.add(labelEncodeDecodeMax, "wrap");
		encodeDecodeGraphPanel.add(encodeDecodeGraph, "span 2, push, grow, wrap");
		encodeDecodeGraphPanel.add(labelEncodeDecodeZero, "skip 1, ax right");

		JPanel memoryAndTemperatureGraphPanel = new JPanel(new MigLayout("ins 0 0 0 0", "[][]10[][]"));
		memoryAndTemperatureGraphPanel.add(labelMemory);
		memoryAndTemperatureGraphPanel.add(labelMemoryMax);
		memoryAndTemperatureGraphPanel.add(labelTemperature);
		memoryAndTemperatureGraphPanel.add(labelTemperatureMax, "wrap");
		memoryAndTemperatureGraphPanel.add(memoryGraph, "span 2, sg 1, push, grow");
		memoryAndTemperatureGraphPanel.add(temperatureGraph, "span 2, sg 1, push, grow, wrap");
		memoryAndTemperatureGraphPanel.add(labelMemoryZero, "skip 1, ax right");
		memoryAndTemperatureGraphPanel.add(labelTemperatureZero, "skip 1, ax right");

		JPanel metricsPanel = new JPanel(new MigLayout("wrap 2"));
		metricsPanel.add(utilizationPanel);
		metricsPanel.add(memoryPanel);
		metricsPanel.add(decodePanel);
		metricsPanel.add(encodePanel);
		metricsPanel.add(temperaturePanel);

		JPanel constantsPanel = new JPanel(new MigLayout("wrap 2, gapy 10"));
		constantsPanel.add(labelCapacityHeader);
		constantsPanel.add(capacityLabel);
		constantsPanel.add(labelModelHeader);
		constantsPanel.add(modelLabel);
		constantsPanel.add(labelDriverHeader);
		constantsPanel.add(driverLabel);

		setLayout(new MigLayout("", "[][grow]"));
		add(labelHeader, "wrap");
		add(utilizationGraphPanel, "span 2, push, grow, wrap");
		add(encodeDecodeGraphPanel, "span 2, gaptop 5, pushy 25, grow, wrap");
		add(memoryAndTemperatureGraphPanel, "span 2, pushy 25, grow, wrap");
		add(timelineGraph, "span 2, growx, wrap");
		add(metricsPanel);
		add(constantsPanel, "ay top");
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
		connectedButton = new GraphTypeButton(String.format("GPU %d (%s)", gpu.index, gpu.name), index);
		connectedButton.setIsLogarithmic(utilizationGraph.isLogarithmic());
		connectedButton.addGraph(new GraphBuilder(gpu.utilization, GraphType.Gpu).build());
		connectedButton.setMaxDatapointValue(Config.DOUBLE_TO_LONG);
		return connectedButton;
	}
}