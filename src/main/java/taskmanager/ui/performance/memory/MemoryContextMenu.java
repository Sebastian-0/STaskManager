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

import taskmanager.Measurements;
import taskmanager.data.TopList;
import taskmanager.ui.TextUtils.ValueType;
import taskmanager.ui.callbacks.ShowProcessCallback;
import taskmanager.ui.performance.common.TopListMenu;

import javax.swing.JPopupMenu;

public class MemoryContextMenu extends JPopupMenu {
	public MemoryContextMenu(Measurements<TopList> memoryTopList, ShowProcessCallback callback) {
		add(new TopListMenu(this, memoryTopList, ValueType.Bytes, callback));
	}
}