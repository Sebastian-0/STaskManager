/*
 * Copyright (c) 2020. Sebastian Hjelm
 */

package taskmanager.ui.performance;

import taskmanager.Measurements;
import taskmanager.ui.ColorUtils;
import taskmanager.ui.SimpleGridBagLayout;
import taskmanager.ui.TextUtils;
import taskmanager.ui.TextUtils.ValueType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class GraphTypeButton extends JButton {
	private final GraphPanel graph;
	private final JLabel valueLabel;

	private final GraphType type;
	private final int index;
	private long measurementMaximumValue;

	private PerformanceButtonListener listener;

	public GraphTypeButton(GraphType type, ValueType valueType, String header) {
		this(type, valueType, header, 0);
	}

	public GraphTypeButton(GraphType type, ValueType valueType, String header, int index) {
		this.type = type;
		this.index = index;

		setBackground(Color.WHITE);
		addActionListener(actionListener);

		graph = new GraphPanel(type, valueType, false);
		valueLabel = new JLabel();
		JLabel headerLabel = new JLabel(header);

		SimpleGridBagLayout layout = new SimpleGridBagLayout(this);

		layout.addToGrid(graph, 0, 0, 1, 2);
		layout.addToGrid(headerLabel, 1, 0, 1, 1, GridBagConstraints.HORIZONTAL, 1, 0, GridBagConstraints.WEST);
		layout.addToGrid(valueLabel, 1, 1, 1, 1, GridBagConstraints.HORIZONTAL, 1, 0, GridBagConstraints.WEST);
	}

	public void setIsLogarithmic(boolean isLogarithmic) {
		graph.setIsLogarithmic(isLogarithmic);
	}

	public void addGraph(Measurements<Long> measurements) {
		addGraph(measurements, false);
	}

	public void addGraph(Measurements<Long> measurements, boolean isDashed) {
		graph.addGraph(measurements, isDashed);
	}

	public void setListener(PerformanceButtonListener listener) {
		this.listener = listener;
	}

	public void setMaxDatapointValue(long maxValue) {
		measurementMaximumValue = maxValue;
		graph.setMaxDatapointValue(maxValue);
	}

	public void newDatapoint(long... currentValues) {
		if (type == GraphType.Cpu) {
			valueLabel.setText(TextUtils.valueToString(currentValues[0], ValueType.Percentage));
		} else if (type == GraphType.Memory) {
			valueLabel.setText(String.format("%s (%.1f%%)",
					TextUtils.ratioToString(currentValues[0], measurementMaximumValue, ValueType.Bytes),
					100 * currentValues[0] / (double) measurementMaximumValue));
		} else if (type == GraphType.Network) {
			valueLabel.setText(String.format("S: %s, R: %s",
					TextUtils.valueToString(currentValues[0], ValueType.BitsPerSecond),
					TextUtils.valueToString(currentValues[1], ValueType.BitsPerSecond)));
		} else if (type == GraphType.Disk) {
			valueLabel.setText(TextUtils.valueToString(currentValues[0], ValueType.Percentage));
		} else if (type == GraphType.Gpu) {
			valueLabel.setText(TextUtils.valueToString(currentValues[0], ValueType.Percentage));
		}
		graph.newDatapoint();
	}

	public void select() {
		setBackground(ColorUtils.blend(type.color, Color.WHITE, 0.1f));
	}

	public void deselect() {
		setBackground(Color.WHITE);
	}


	private ActionListener actionListener = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			listener.swapTo(type, index);
			select();
		}
	};
}
