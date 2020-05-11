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

import javax.swing.JOptionPane;
import java.awt.Component;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

public class OpenFileLocationMenuItem extends AbstractMenuItem {
	private Component parent;
	private Process process;

	public OpenFileLocationMenuItem(Component parent) {
		super("Open file location");
		this.parent = parent;
	}

	@Override
	protected void doAction() {
		boolean succeeded = false;
		File file = new File(process.filePath);
		if (file.exists() && !file.isDirectory()) {
			File folder = file.getParentFile();
			succeeded = openPath(folder.getAbsolutePath());
		}

		if (!succeeded) {
			JOptionPane.showMessageDialog(parent, "Failed to open the process path!", "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	private boolean openPath(String path) {
		try {
			Desktop.getDesktop().open(new File(path));
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public void setProcess(Process process) {
		this.process = process;
		setEnabled(!process.filePath.isEmpty());
	}
}
