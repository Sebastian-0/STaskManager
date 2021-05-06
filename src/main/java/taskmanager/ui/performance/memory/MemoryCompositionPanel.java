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
import taskmanager.platform.linux.LinuxExtraInformation;
import taskmanager.platform.osx.OsXExtraInformation;
import taskmanager.platform.win32.WindowsExtraInformation;
import taskmanager.ui.ColorUtils;
import taskmanager.ui.TextUtils;
import taskmanager.ui.performance.GraphType;

import javax.swing.JPanel;
import javax.swing.border.LineBorder;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.MessageFormat;

public class MemoryCompositionPanel extends JPanel {
	private final Section[] sections;

	private int lastMouseX;

	public MemoryCompositionPanel(SystemInformation systemInformation) {
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

		if (systemInformation.extraInformation instanceof WindowsExtraInformation) {
			sections = new Section[] {
					new Section(color(180), TextUtils.convertLineBreaksToHtml("Reserved ({0} MB)\nReserved memory for the BIOS and\n some drivers")),
					new Section(color(130), TextUtils.convertLineBreaksToHtml("In use ({0} MB)\nMemory used by processes, drivers\n and the operating system")),
					new Section(color(90), TextUtils.convertLineBreaksToHtml("Modified ({0} MB)\nMemory whose content must be written\n to disk before being used for another\n purpose")),
					new Section(color(50), TextUtils.convertLineBreaksToHtml("Standby ({0} MB)\nMemory that contains cached data\n and code that is not actively used")),
					new Section(color(0), TextUtils.convertLineBreaksToHtml("Free ({0} MB)\nMemory that is not currently in use\n and will be repurposed when\n processes, drivers or the operating\n system needs more memory"))
			};
		} else if (systemInformation.extraInformation instanceof LinuxExtraInformation) {
			sections = new Section[] {
					new Section(color(180), TextUtils.convertLineBreaksToHtml("Reserved ({0} MB)\nReserved memory for the BIOS and\n kernel")),
					new Section(color(130), TextUtils.convertLineBreaksToHtml("In use ({0} MB)\nMemory used by privately by processes,\n drivers and the operating system")),
					new Section(color(90), TextUtils.convertLineBreaksToHtml("Buffers ({0} MB)\nMemory used by kernel file buffers,\n can be partially reclaimed when needed")),
					new Section(color(50), TextUtils.convertLineBreaksToHtml("Cached ({0} MB)\nMemory used by the page cache and\n slabs, can be partially reclaimed when \nneeded")),
					new Section(color(0), TextUtils.convertLineBreaksToHtml("Free ({0} MB)\nMemory that is not currently in use\n and will be repurposed when\n processes, drivers or the operating\n system needs more memory"))
			};
		} else if (systemInformation.extraInformation instanceof OsXExtraInformation) {
			sections = new Section[] {
					new Section(color(180), TextUtils.convertLineBreaksToHtml("Reserved ({0} MB)\nReserved memory for the BIOS and\n kernel")),
					new Section(color(130), TextUtils.convertLineBreaksToHtml("Wired ({0} MB)\nMemory used by the kernel and the\n operating system, cannot be paged out")),
					new Section(color(90), TextUtils.convertLineBreaksToHtml("Active ({0} MB)\nMemory used by currently running\n applications")),
					new Section(color(50), TextUtils.convertLineBreaksToHtml("Inactive ({0} MB)\nMemory that is no longer used or has\n not been accessed in a long time")),
					new Section(color(0), TextUtils.convertLineBreaksToHtml("Free ({0} MB)\nMemory that is not currently in use\n and will be repurposed when\n processes, drivers or the operating\n system needs more memory"))
			};
		} else {
			throw new UnsupportedOperationException("You are running an unsupported operating system!");
		}
	}

	private Color color(int alpha) {
		Color c = GraphType.Memory.color;
		return ColorUtils.blend(c, Color.WHITE, alpha/255f);
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		// Enable anti-aliasing to combat LineBorder bug in OSX (right border is missing...) Maybe the bug is related to
		// a specific version/distribution of java?
		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

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
		sections[4].value = systemInformation.freeMemory;
		if (systemInformation.extraInformation instanceof WindowsExtraInformation) {
			WindowsExtraInformation extraInformation = (WindowsExtraInformation) systemInformation.extraInformation;
			sections[1].value = systemInformation.memoryUsed.newest() - extraInformation.modifiedMemory;
			sections[2].value = extraInformation.modifiedMemory;
			sections[3].value = extraInformation.standbyMemory;
		} else if (systemInformation.extraInformation instanceof LinuxExtraInformation) {
			LinuxExtraInformation extraInformation = (LinuxExtraInformation) systemInformation.extraInformation;
			sections[1].value = systemInformation.physicalMemoryTotal - systemInformation.freeMemory -
					extraInformation.bufferMemory - extraInformation.cacheMemory;
			sections[2].value = extraInformation.bufferMemory;
			sections[3].value = extraInformation.cacheMemory;
		} else if (systemInformation.extraInformation instanceof OsXExtraInformation) {
			OsXExtraInformation extraInformation = (OsXExtraInformation) systemInformation.extraInformation;
			sections[1].value = extraInformation.wiredMemory;
			sections[2].value = extraInformation.activeMemory;
			sections[3].value = extraInformation.inactiveMemory;
		}

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