package taskmanager.ui.performance.memory;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.text.MessageFormat;

import javax.swing.JPanel;
import javax.swing.border.LineBorder;

import taskmanager.SystemInformation;
import taskmanager.ui.performance.GraphType;

public class MemoryCompositionPanel extends JPanel {
	private Section[] sections;

	private int lastMouseX;

	public MemoryCompositionPanel() {
		setBackground(Color.WHITE);
		setBorder(new LineBorder(Color.BLACK));
		setPreferredSize(new Dimension(80, 50));

		addMouseMotionListener(motionListener);

		sections = new Section[]{new Section(color(180), convertLinebreaks("Reserved ({0} MB)\nReserved memory for the BIOS and\n some drivers")),
				new Section(color(130), convertLinebreaks("In use ({0} MB)\nMemory used by processes, drivers\n and the operating system")),
				new Section(color(90), convertLinebreaks("Modified ({0} MB)\nMemory whose content must be written\n to disk before being used for another\n purpose")),
				new Section(color(50), convertLinebreaks("Standby ({0} MB)\nMemory that contains cached data\n and code that is not actively used")),
				new Section(color(0), convertLinebreaks("Free ({0} MB)\nMemory that is not currently in use\n and will be repurposed when\n processes, drivers or the operating\n system needs more memory"))};
	}

	private Color color(int alpha) {
		Color c = GraphType.Memory.color;
		return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
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

		for (int i = 0; i < sections.length; i++) {
			sections[i].fractionOfWidth = sections[i].value / (float) systemInformation.physicalMemoryTotalInstalled;
		}

		updateTooltipText();
		repaint();
	}

	private void updateTooltipText() {
		for (int i = 0; i < sections.length; i++) {
			if (lastMouseX <= sections[i].startPosition + sections[i].fractionOfWidth * getWidth()) {
				setToolTipText(MessageFormat.format(sections[i].tooltip, sections[i].value / 1024 / 1024));
				break;
			}
		}
	}


	private MouseMotionListener motionListener = new MouseAdapter() {
		@Override
		public void mouseMoved(MouseEvent e) {
			lastMouseX = e.getX();
			updateTooltipText();
		}
	};


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
