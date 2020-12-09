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

import net.miginfocom.swing.MigLayout;
import taskmanager.ui.ColorUtils;
import taskmanager.ui.TextUtils;
import taskmanager.ui.TextUtils.ValueType;
import taskmanager.ui.performance.GraphPanel.Graph;

import javax.swing.JButton;
import javax.swing.JLabel;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class GraphTypeButton extends JButton {
	private final List<Graph> graphs;
	private final GraphPanel graphPanel;
	private final JLabel valueLabel;

	private final int index;
	private long measurementMaximumValue;

	private PerformanceButtonListener listener;

	public GraphTypeButton(String header) {
		this(header, 0);
	}

	public GraphTypeButton(String header, int index) {
		graphs = new ArrayList<>();
		this.index = index;

		setBackground(Color.WHITE);
		addActionListener(actionListener);

		graphPanel = new GraphPanel(false);
		valueLabel = new JLabel();
		JLabel headerLabel = new JLabel(header);

		setLayout(new MigLayout());
		add(graphPanel, "spany 2");
		add(headerLabel, "wrap");
		add(valueLabel, "skip 1");
	}

	public void setIsLogarithmic(boolean isLogarithmic) {
		graphPanel.setIsLogarithmic(isLogarithmic);
	}

	public void addGraph(Graph graph) {
		if (!graphs.isEmpty() && graph.graphType != getGraphType()) {
			throw new IllegalArgumentException("All graphs must have the same type, " + graph.graphType + " != " + getGraphType());
		}
		graphs.add(graph);
		graphPanel.addGraph(graph);
	}

	private GraphType getGraphType() {
		return graphs.get(0).graphType;
	}

	public void setListener(PerformanceButtonListener listener) {
		this.listener = listener;
	}

	public void setMaxDatapointValue(long maxValue) {
		measurementMaximumValue = maxValue;
		graphPanel.setMaxDatapointValue(maxValue);
	}

	public void newDatapoint(long... currentValues) {
		GraphType type = getGraphType();
		ValueType valueType = graphs.get(0).valueType;
		if (type == GraphType.Memory) {
			valueLabel.setText(String.format("%s (%.1f%%)",
					TextUtils.ratioToString(currentValues[0], measurementMaximumValue, ValueType.Bytes),
					100 * currentValues[0] / (double) measurementMaximumValue));
		} else if (type == GraphType.Network) {
			valueLabel.setText(String.format("S: %s, R: %s",
					TextUtils.valueToString(currentValues[0], ValueType.BitsPerSecond),
					TextUtils.valueToString(currentValues[1], ValueType.BitsPerSecond)));
		} else {
			valueLabel.setText(TextUtils.valueToString(currentValues[0], valueType));
		}
		graphPanel.newDatapoint();
	}

	public void select() {
		setBackground(ColorUtils.blend(getGraphType().color, Color.WHITE, 0.1f));
	}

	public void deselect() {
		setBackground(Color.WHITE);
	}


	private ActionListener actionListener = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			listener.swapTo(getGraphType(), index);
			select();
		}
	};
}