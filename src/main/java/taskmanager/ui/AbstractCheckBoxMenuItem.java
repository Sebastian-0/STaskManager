/*
 * Copyright (c) 2020. Sebastian Hjelm
 */

package taskmanager.ui;

import javax.swing.JCheckBoxMenuItem;

public abstract class AbstractCheckBoxMenuItem extends JCheckBoxMenuItem {
	public AbstractCheckBoxMenuItem(String text) {
		super (text);
		addActionListener(e -> doAction());
	}
	
	protected abstract void doAction();
}
