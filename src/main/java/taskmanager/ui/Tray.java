package taskmanager.ui;

import config.Config;
import taskmanager.SystemInformation;
import taskmanager.ui.TextUtils.ValueType;

import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.TrayIcon;

public class Tray extends TrayIcon {
	public Tray(ApplicationCallback application, Image image) {
		super(image);
		setImageAutoSize(true);

		PopupMenu menu = new PopupMenu();

		MenuItem messageItem = new MenuItem("Focus window");
		messageItem.addActionListener(e -> application.focus());
		menu.add(messageItem);

		MenuItem closeItem = new MenuItem("Close");
		closeItem.addActionListener(e -> application.exit());
		menu.add(closeItem);
		setPopupMenu(menu);

		addActionListener(e -> { application.focus(); });
	}

	public void update(SystemInformation info) {
		setToolTip(String.format("CPU: %s\n Memory: %s / %s",
				TextUtils.valueToString((long) (info.cpuUsageTotal.newest() * Config.DOUBLE_TO_LONG), ValueType.Percentage),
				TextUtils.valueToString(info.physicalMemoryUsed.newest(), ValueType.Bytes),
				TextUtils.valueToString(info.physicalMemoryTotal, ValueType.Bytes)));
	}
}
