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

package taskmanager.ui.tray;

import config.Config;
import taskmanager.data.SystemInformation;
import taskmanager.ui.callbacks.ApplicationCallback;
import taskmanager.ui.ColorUtils;
import taskmanager.ui.TextUtils;
import taskmanager.ui.TextUtils.ValueType;
import taskmanager.ui.performance.GraphType;

import javax.swing.ButtonGroup;
import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.TrayIcon;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Tray extends TrayIcon {
	private final Image applicationIcon;
	private final BufferedImage iconImage;
	private final Graphics2D imageGraphics;

	private SystemInformation latestInfo;
	private GraphType graphTypeToDisplay;

	/** Only used to have a surface that can generate the popup menu */
	private final JDialog popupMenuDialog;
	private final JPopupMenu popupMenu;

	public Tray(ApplicationCallback application, Image image) {
		super(image);
		applicationIcon = image;

		popupMenuDialog = new JDialog((Frame) null);
		popupMenuDialog.setUndecorated(true);
		popupMenuDialog.setAlwaysOnTop(true);
		popupMenuDialog.addWindowFocusListener(new WindowAdapter() {
			@Override
			public void windowLostFocus(WindowEvent e) {
				popupMenuDialog.setVisible(false);
			}
		});

		setImageAutoSize(true);

		popupMenu = new JPopupMenu();
		JMenu selectedGraphItem = new JMenu("Selected graph");

		JRadioButtonMenuItem noneItem = new JRadioButtonMenuItem("None");
		JRadioButtonMenuItem cpuItem = new JRadioButtonMenuItem("CPU");
		JRadioButtonMenuItem memoryItem = new JRadioButtonMenuItem("Memory");
		noneItem.addActionListener(e -> setGraphType(null));
		cpuItem.addActionListener(e -> setGraphType(GraphType.Cpu));
		memoryItem.addActionListener(e -> setGraphType(GraphType.Memory));
		selectedGraphItem.add(noneItem);
		selectedGraphItem.add(cpuItem);
		selectedGraphItem.add(memoryItem);
		popupMenu.add(selectedGraphItem);

		ButtonGroup group = new ButtonGroup();
		group.add(noneItem);
		group.add(cpuItem);
		group.add(memoryItem);

		popupMenu.addSeparator();

		JMenuItem messageItem = new JMenuItem("Restore");
		messageItem.addActionListener(e -> application.focus());
		popupMenu.add(messageItem);

		JMenuItem closeItem = new JMenuItem("Close");
		closeItem.addActionListener(e -> application.exit());
		popupMenu.add(closeItem);

		addActionListener(e -> application.focus());

		iconImage = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		imageGraphics = iconImage.createGraphics();
		imageGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//		imageGraphics.setStroke(new BasicStroke(4));

		popupMenu.addPopupMenuListener(new PopupMenuListener() {
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
			}

			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
				popupMenuDialog.setVisible(false);
			}

			public void popupMenuCanceled(PopupMenuEvent e) {
				popupMenuDialog.setVisible(false);
			}
		});

		String graphName = Config.get(Config.KEY_TRAY_GRAPH);
		if (graphName.isEmpty()) {
			setGraphType(null);
			noneItem.setSelected(true);
		} else if (graphName.equals(GraphType.Cpu.name())){
			setGraphType(GraphType.Cpu);
			cpuItem.setSelected(true);
		} else if (graphName.equals(GraphType.Memory.name())){
			setGraphType(GraphType.Memory);
			memoryItem.setSelected(true);
		}

		addMouseListener(
				new MouseAdapter() {
					@Override
					public void mousePressed(MouseEvent e) {
						showJPopupMenu(e);
					}

					@Override
					public void mouseReleased(MouseEvent e) {
						showJPopupMenu(e);
					}

					private void showJPopupMenu(MouseEvent e) {
						if (e.isPopupTrigger()) {
							Dimension menuSize = popupMenu.getPreferredSize();
							popupMenuDialog.setLocation(e.getX(), e.getY() - menuSize.height);
							popupMenuDialog.setVisible(true);
							popupMenu.show(popupMenuDialog.getContentPane(), 0, 0);
						}
					}
				}
		);
	}

	private void setGraphType(GraphType type) {
		graphTypeToDisplay = type;
		if (graphTypeToDisplay == null) {
			setImage(applicationIcon);
		} else if (latestInfo != null) {
			updateIconImage(latestInfo);
			setImage(iconImage);
		}

		Config.put(Config.KEY_TRAY_GRAPH, type == null ? "" : type.name());
	}

	public void update(SystemInformation info) {
		latestInfo = info;

		setToolTip(String.format(" CPU: %s%n Memory: %s / %s",
				TextUtils.valueToString(info.cpuUsageTotal.newest(), ValueType.Percentage),
				TextUtils.valueToString(info.memoryUsed.newest(), ValueType.Bytes),
				TextUtils.valueToString(info.physicalMemoryTotal, ValueType.Bytes)));

		if (graphTypeToDisplay != null) {
			updateIconImage(info);
			setImage(iconImage);
		}
	}

	private void updateIconImage(SystemInformation info) {
		imageGraphics.setColor(Color.WHITE);
		imageGraphics.fillRect(0, 0, iconImage.getWidth(), iconImage.getHeight());

		imageGraphics.setColor(new Color(203, 208, 211));

		int lines = 3-1;
		for (int i = 0; i < lines; i++) {
			int y = (i + 1) * iconImage.getHeight() / (lines + 1);
			imageGraphics.drawLine(0, y, iconImage.getWidth(), y);
		}

		for (int i = 0; i < lines; i++) {
			int x = (i + 1) * iconImage.getWidth() / (lines + 1);
			imageGraphics.drawLine(x, 0, x, iconImage.getHeight());
		}

		final int samples = 4;
		List<Double> ratios = new ArrayList<>();
		if (graphTypeToDisplay == GraphType.Cpu) {
			Iterator<Short> itr = info.cpuUsageTotal.getRangeIterator(info.cpuUsageTotal.size() - samples, info.cpuUsageTotal.size() - 1);
			while (itr.hasNext()) {
				ratios.add(itr.next() / (double) Config.DOUBLE_TO_LONG);
			}
		} else if (graphTypeToDisplay == GraphType.Memory) {
			Iterator<Long> itr = info.memoryUsed.getRangeIterator(info.memoryUsed.size() - samples, info.memoryUsed.size() - 1);
			while (itr.hasNext()) {
				ratios.add(itr.next() / (double) info.physicalMemoryTotal);
			}
		}

		double previous = ratios.get(0);
		int idx = 0;
		for (int i = 1; i < ratios.size(); i++) {
			double current = ratios.get(i);

			int yPrev = (int) (iconImage.getHeight() * previous);
			int yCurr = (int) (iconImage.getHeight() * current);

			int x = iconImage.getWidth() * (idx) / (samples-1);
			int xNext = iconImage.getWidth() * (idx + 1) / (samples-1);

			imageGraphics.setColor(ColorUtils.blend(graphTypeToDisplay.color, Color.WHITE, 75/255f));
			int[] xs = {x, x, xNext, xNext};
			int[] ys = {iconImage.getHeight(), iconImage.getHeight() - yPrev, iconImage.getHeight() - yCurr, iconImage.getHeight()};
			imageGraphics.fillPolygon(xs, ys, xs.length);

			imageGraphics.setColor(graphTypeToDisplay.color);
			imageGraphics.drawLine(x, iconImage.getHeight() - yPrev, xNext, iconImage.getHeight() - yCurr);

			idx += 1;
			previous = current;
		}

		imageGraphics.setColor(Color.DARK_GRAY);
		imageGraphics.drawRect(0, 0, iconImage.getWidth() - 1, iconImage.getHeight() - 1);
	}
}
