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
import taskmanager.ui.AbstractMenuItem;
import taskmanager.ui.callbacks.ProcessDetailsCallback;

public class OpenProcessDialogMenuItem extends AbstractMenuItem {
	private final ProcessDetailsCallback callback;
	private Process process;

	public OpenProcessDialogMenuItem(ProcessDetailsCallback callback) {
		super("Open process details");
		this.callback = callback;
	}

	@Override
	protected void doAction() {
		callback.openDialog(process);
	}

	public void setProcess(Process process) {
		this.process = process;
	}
}
