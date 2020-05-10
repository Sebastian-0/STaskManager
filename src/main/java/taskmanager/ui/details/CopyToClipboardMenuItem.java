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

import taskmanager.ui.AbstractMenuItem;

import javax.swing.JPopupMenu;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

public class CopyToClipboardMenuItem extends AbstractMenuItem {
	private String currentText;
	private JPopupMenu parent;

	public CopyToClipboardMenuItem(JPopupMenu parent) {
		super("");
		this.parent = parent;
	}

	@Override
	protected void doAction() {
		StringSelection selection = new StringSelection(currentText);
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(selection, selection);
	}

	public void setCellText(String column, String text) {
		setText("Copy " + column.toLowerCase());
		parent.pack();
		this.currentText = text;
	}
}
