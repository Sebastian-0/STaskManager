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

package taskmanager.ui;

import javax.swing.JMenuItem;

public abstract class AbstractMenuItem extends JMenuItem {
	public AbstractMenuItem(String text) {
		super (text);
		addActionListener(e -> doAction());
	}
	
	protected abstract void doAction();
}