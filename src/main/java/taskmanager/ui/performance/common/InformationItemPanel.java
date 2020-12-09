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

package taskmanager.ui.performance.common;

import net.miginfocom.swing.MigLayout;
import taskmanager.ui.TextUtils;
import taskmanager.ui.TextUtils.ValueType;
import taskmanager.ui.performance.GraphPanel.Graph;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;

public class InformationItemPanel extends JPanel {
	protected final ValueType type;
	private final Stroke stroke;
	private final Color color;

	protected JLabel valueLabel;

	public InformationItemPanel(String header, Graph sourceGraph) {
		this(header, sourceGraph.valueType, sourceGraph.style.createStroke(), sourceGraph.graphType.color);
	}

	public InformationItemPanel(String header, ValueType type) {
		this(header, type, null, null);
	}
	
	private InformationItemPanel(String header, ValueType type, Stroke stroke, Color color) {
		this.type = type;
		this.stroke = stroke;
		this.color = color;
		
		JLabel labelHeader = new JLabel(header); 
		valueLabel = new JLabel("0");
		valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD, valueLabel.getFont().getSize() + 3f));

		setLayout(new MigLayout("ins 2 15 2 5"));
		add(labelHeader, "wrap");
		add(valueLabel);
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		
		if (stroke != null) {
			Graphics2D g2d = (Graphics2D) g;
			g2d.setColor(color);
			Stroke old = g2d.getStroke();
			g2d.setStroke(stroke);
			g2d.drawLine(5, getHeight()/8, 5, getHeight()*7/8);
			g2d.setStroke(old);
		}
	}
	
	public void updateValue(long value) {
		valueLabel.setText(TextUtils.valueToString(value, type));
	}
}