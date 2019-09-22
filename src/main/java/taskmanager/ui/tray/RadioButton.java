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

import dorkbox.systemTray.Checkbox;

import java.awt.event.ActionListener;

public class RadioButton extends Checkbox {
	private ButtonGroup buttonGroup;

	public RadioButton(String text) {
		super(text);
		super.setCallback(makeCallback(null));
	}

	@Override
	public void setCallback(ActionListener callback) {
		super.setCallback(makeCallback(callback));
	}

	private ActionListener makeCallback(ActionListener userCallback) {
		return e -> {
			buttonGroup.deselectExcept(RadioButton.this);
			if (userCallback != null) {
				userCallback.actionPerformed(e);
			}
		};
	}

	public void setButtonGroup(ButtonGroup group) {
		buttonGroup = group;
	}
}
