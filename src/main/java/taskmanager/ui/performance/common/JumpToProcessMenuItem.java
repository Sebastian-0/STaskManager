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

import taskmanager.data.TopList;
import taskmanager.ui.TextUtils;
import taskmanager.ui.TextUtils.ValueType;
import taskmanager.ui.performance.ShowProcessCallback;

import javax.swing.JMenuItem;

public class JumpToProcessMenuItem extends JMenuItem {
	public JumpToProcessMenuItem(TopList.Entry entry, ValueType valueType, ShowProcessCallback showProcessCallback) {
		super(String.format("(%s) %s (%d)", TextUtils.valueToString(entry.value, valueType), entry.process.fileName, entry.process.id));
		addActionListener(e -> showProcessCallback.showProcess(entry.process.uniqueId));
	}
}
