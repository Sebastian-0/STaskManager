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
import taskmanager.SystemInformation;
import taskmanager.ui.ApplicationCallback;
import taskmanager.ui.ColorUtils;
import taskmanager.ui.TextUtils;
import taskmanager.ui.TextUtils.ValueType;
import taskmanager.ui.performance.GraphType;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.RenderingHints;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Tray extends TrayIcon {
	private Image applicationIcon;
	private BufferedImage iconImage;
	private Graphics2D imageGraphics;

	private SystemInformation latestInfo;
	private GraphType graphTypeToDisplay;

	public Tray(ApplicationCallback application, Image image) {
		super(image);
		applicationIcon = image;

		setImageAutoSize(true);

		PopupMenu popupMenu = new PopupMenu();

		Menu selectedGraphItem = new Menu("Selected graph");

		AwtRadioButtonMenuItem noneItem = new AwtRadioButtonMenuItem("None");
		AwtRadioButtonMenuItem cpuItem = new AwtRadioButtonMenuItem("CPU");
		AwtRadioButtonMenuItem memoryItem = new AwtRadioButtonMenuItem("Memory");
		noneItem.setActionListener(e -> setGraphType(null));
		cpuItem.setActionListener(e -> setGraphType(GraphType.Cpu));
		memoryItem.setActionListener(e -> setGraphType(GraphType.Memory));
		selectedGraphItem.add(noneItem);
		selectedGraphItem.add(cpuItem);
		selectedGraphItem.add(memoryItem);
		popupMenu.add(selectedGraphItem);

		AwtButtonGroup group = new AwtButtonGroup();
		group.addButtons(noneItem, cpuItem, memoryItem);

		popupMenu.addSeparator();

		MenuItem messageItem = new MenuItem("Restore");
		messageItem.addActionListener(e -> application.focus());
		popupMenu.add(messageItem);

		MenuItem closeItem = new MenuItem("Close");
		closeItem.addActionListener(e -> application.exit());
		popupMenu.add(closeItem);
		setPopupMenu(popupMenu);

		addActionListener(e -> application.focus());

		iconImage = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		imageGraphics = iconImage.createGraphics();
		imageGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//		imageGraphics.setStroke(new BasicStroke(4));

		String graphName = Config.get(Config.KEY_TRAY_GRAPH);
		if (graphName.isEmpty()) {
			setGraphType(null);
			noneItem.setState(true);
		} else if (graphName.equals(GraphType.Cpu.name())){
			setGraphType(GraphType.Cpu);
			cpuItem.setState(true);
		} else if (graphName.equals(GraphType.Memory.name())){
			setGraphType(GraphType.Memory);
			memoryItem.setState(true);
		}
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

		setToolTip(String.format(" CPU: %s\n Memory: %s / %s",
				TextUtils.valueToString(info.cpuUsageTotal.newest(), ValueType.Percentage),
				TextUtils.valueToString(info.physicalMemoryUsed.newest(), ValueType.Bytes),
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
			Iterator<Long> itr = info.physicalMemoryUsed.getRangeIterator(info.physicalMemoryUsed.size() - samples, info.physicalMemoryUsed.size() - 1);
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
