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

import taskmanager.data.SystemInformation;
import taskmanager.ui.ColorUtils;
import taskmanager.ui.performance.GraphType;

import javax.swing.JPanel;
import javax.swing.border.LineBorder;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.MessageFormat;

public class MemoryCompositionPanel extends JPanel {
	private final Section[] sections;

	private int lastMouseX;

	public MemoryCompositionPanel() {
		setBackground(Color.WHITE);
		setBorder(new LineBorder(Color.BLACK));
		setPreferredSize(new Dimension(80, 50));

		addMouseMotionListener(new MouseAdapter() {
				@Override
				public void mouseMoved(MouseEvent e) {
					lastMouseX = e.getX();
					updateTooltipText();
				}
			});

		sections = new Section[]{new Section(color(180), convertLinebreaks("Reserved ({0} MB)\nReserved memory for the BIOS and\n some drivers")),
				new Section(color(130), convertLinebreaks("In use ({0} MB)\nMemory used by processes, drivers\n and the operating system")),
				new Section(color(90), convertLinebreaks("Modified ({0} MB)\nMemory whose content must be written\n to disk before being used for another\n purpose")),
				new Section(color(50), convertLinebreaks("Standby ({0} MB)\nMemory that contains cached data\n and code that is not actively used")),
				new Section(color(0), convertLinebreaks("Free ({0} MB)\nMemory that is not currently in use\n and will be repurposed when\n processes, drivers or the operating\n system needs more memory"))};
	}

	private Color color(int alpha) {
		Color c = GraphType.Memory.color;
		return ColorUtils.blend(c, Color.WHITE, alpha/255f);
	}

	private String convertLinebreaks(String input) {
		return "<html>" + input.replaceAll("\n", "<br>") + "</html>";
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		float pos = 0;
		int lastDrawEnd = 0;
		for (int i = 0; i < sections.length; i++) {
			float width = sections[i].fractionOfWidth * getWidth();

			if (i < sections.length - 1) {
				g.setColor(sections[i].color);
				g.fillRect(lastDrawEnd, 0, (int) (pos + width) - lastDrawEnd, getHeight());
				lastDrawEnd = (int) (pos + width);
			}

			sections[i].startPosition = pos;
			pos += width;
		}
	}

	public void update(SystemInformation systemInformation) {
		sections[0].value = systemInformation.reservedMemory;
		sections[1].value = systemInformation.physicalMemoryUsed.newest() - systemInformation.modifiedMemory;
		sections[2].value = systemInformation.modifiedMemory;
		sections[3].value = systemInformation.standbyMemory;
		sections[4].value = systemInformation.freeMemory;

		for (Section section : sections) {
			section.fractionOfWidth = section.value / (float) systemInformation.physicalMemoryTotalInstalled;
		}

		updateTooltipText();
		repaint();
	}

	private void updateTooltipText() {
		for (Section section : sections) {
			if (lastMouseX <= section.startPosition + section.fractionOfWidth * getWidth()) {
				setToolTipText(MessageFormat.format(section.tooltip, section.value / 1024 / 1024));
				break;
			}
		}
	}


	private static class Section {
		Color color;
		String tooltip;
		long value;

		float startPosition;
		float fractionOfWidth;

		public Section(Color color, String tooltip) {
			this.color = color;
			this.tooltip = tooltip;
		}
	}
}