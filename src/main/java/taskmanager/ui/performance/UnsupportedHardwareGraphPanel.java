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

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public class UnsupportedHardwareGraphPanel extends GraphPanel {
	public UnsupportedHardwareGraphPanel(GraphType graphType) {
		for (MouseListener listener : getMouseListeners()) {
			removeMouseListener(listener);
		}

		for (MouseMotionListener listener : getMouseMotionListeners()) {
			removeMouseMotionListener(listener);
		}
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		g.setColor(new Color(220, 220, 220, 180));
		g.fillRect(0, 0, getWidth(), getHeight());

		g.setColor(Color.DARK_GRAY);
		g.setFont(getFont().deriveFont(24f));
		FontMetrics metrics = g.getFontMetrics();
		final String label = "Unsupported Hardware";
		g.drawString(label, getWidth() / 2 - metrics.stringWidth(label) / 2, getHeight() / 2 + metrics.getHeight() / 2 - metrics.getDescent());
	}
}