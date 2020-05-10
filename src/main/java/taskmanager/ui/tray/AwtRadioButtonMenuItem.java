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

package taskmanager.ui.tray;

import java.awt.CheckboxMenuItem;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;

public class AwtRadioButtonMenuItem extends CheckboxMenuItem {

	private ActionListener manualListener;
	private AwtButtonGroup buttonGroup;

	public AwtRadioButtonMenuItem(String label) {
		super(label);
		super.addItemListener(e -> {
			if (manualListener != null) {
				if (e.getStateChange() == ItemEvent.DESELECTED) {
					setState(true);
				}
				else {
					manualListener.actionPerformed(new ActionEvent(e.getSource(), e.getID(), ""));
					buttonGroup.deselectExcept(this);
				}
			}
		});
	}

	public void setButtonGroup(AwtButtonGroup group) {
		buttonGroup = group;
	}

	public void setActionListener(ActionListener l) {
		manualListener = l;
	}
}
