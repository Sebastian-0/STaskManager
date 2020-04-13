/*
 * Copyright (c) 2020. Sebastian Hjelm
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
