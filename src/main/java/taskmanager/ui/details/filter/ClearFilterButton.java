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

package taskmanager.ui.details.filter;

import config.TextureStorage;

import javax.swing.ImageIcon;
import javax.swing.JButton;

public class ClearFilterButton extends JButton {
	public ClearFilterButton(FilterTextField textField) {
		super(new ImageIcon(TextureStorage.instance().getTexture("cross")));
		setFocusable(false);
		addActionListener(e -> textField.clear());
		setBackground(null);
		setBorder(null);
	}
}