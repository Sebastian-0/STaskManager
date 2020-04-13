/*
 * Copyright (c) 2020. Sebastian Hjelm
 */

package taskmanager.ui;

import javax.swing.JRadioButtonMenuItem;

public abstract class AbstractRadioButtonMenuItem extends JRadioButtonMenuItem {
	public AbstractRadioButtonMenuItem(String text) {
		super (text);
		addActionListener(e -> doAction());
	}
	
	protected abstract void doAction();
}
