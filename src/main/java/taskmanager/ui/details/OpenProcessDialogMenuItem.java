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

import taskmanager.Process;
import taskmanager.ui.AbstractMenuItem;

import java.awt.Component;

public class OpenProcessDialogMenuItem extends AbstractMenuItem {
	private Component parent;
	private Process process;

	public OpenProcessDialogMenuItem(Component parent) {
		super("Open process details");
		this.parent = parent;
	}

	@Override
	protected void doAction() {
		((ProcessDetailsCallback) parent).openDialog(process);
	}

	public void setProcess(Process process) {
		this.process = process;
	}
}
