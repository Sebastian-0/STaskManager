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

package taskmanager.ui.details;

import taskmanager.data.Process;
import taskmanager.ui.callbacks.ShowProcessCallback;

import javax.swing.JMenuItem;

public class JumpToParentMenuItem extends JMenuItem {
	private Process process;

	public JumpToParentMenuItem(ShowProcessCallback showProcessCallback) {
		super("Jump to parent");
		addActionListener(e -> showProcessCallback.showProcess(process.parentUniqueId));
	}

	public void setProcess(Process process) {
		this.process = process;
		setEnabled(process.parentUniqueId != -1);
	}
}
