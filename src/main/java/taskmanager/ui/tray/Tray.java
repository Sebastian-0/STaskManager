package taskmanager.ui.tray;

import config.Config;
import taskmanager.SystemInformation;
import taskmanager.ui.ApplicationCallback;
import taskmanager.ui.ColorUtils;
import taskmanager.ui.TextUtils;
import taskmanager.ui.TextUtils.ValueType;
import taskmanager.ui.performance.GraphType;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;

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

		iconImage = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
		imageGraphics = iconImage.createGraphics();
		imageGraphics.setStroke(new BasicStroke(4));

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
				TextUtils.valueToString((long) (info.cpuUsageTotal.newest() * Config.DOUBLE_TO_LONG), ValueType.Percentage),
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

		double ratio = 0;
		if (graphTypeToDisplay == GraphType.Cpu) {
			ratio = info.cpuUsageTotal.newest();
		} else if (graphTypeToDisplay == GraphType.Memory) {
			ratio = info.physicalMemoryUsed.newest() / (double) info.physicalMemoryTotal;
		}

		imageGraphics.setColor(ColorUtils.blend(graphTypeToDisplay.color, Color.WHITE, 75/255f));
		int height = (int) (ratio * iconImage.getHeight());
		imageGraphics.fillRect(0, iconImage.getHeight() - height, iconImage.getWidth(), height);

		imageGraphics.setColor(graphTypeToDisplay.color);
		imageGraphics.drawLine(0, iconImage.getHeight() - height, iconImage.getWidth(), iconImage.getHeight() - height);

		imageGraphics.setColor(Color.DARK_GRAY);
		imageGraphics.drawRect(1, 1, iconImage.getWidth() - 1, iconImage.getHeight() - 1);
	}
}
