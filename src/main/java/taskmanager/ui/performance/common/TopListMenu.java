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

import taskmanager.Measurements;
import taskmanager.data.TopList;
import taskmanager.ui.TextUtils.ValueType;
import taskmanager.ui.performance.ShowProcessCallback;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

public class TopListMenu extends JMenu {
	private JMenuItem[] topListMenuItems;

	public TopListMenu(JPopupMenu parent, Measurements<TopList> topList, ValueType valueType,
					   ShowProcessCallback showProcessCallback) {
		super("Jump to top user ");
		topListMenuItems = new JMenuItem[0];
		
		parent.addPopupMenuListener(new PopupMenuListener() {
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent popupMenuEvent) {
				for (JMenuItem jMenuItem : topListMenuItems) {
					remove(jMenuItem);
				}
				TopList.Entry[] entries = topList.newest().entries;
				topListMenuItems = new JMenuItem[entries.length];
				for (int i = 0; i < entries.length; i++) {
					topListMenuItems[i] = new JumpToProcessMenuItem(entries[i], valueType, showProcessCallback);
					add(topListMenuItems[i]);
				}
			}

			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent) { }

			@Override
			public void popupMenuCanceled(PopupMenuEvent popupMenuEvent) { }
		});
	}
}