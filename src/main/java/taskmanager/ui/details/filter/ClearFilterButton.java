/*
 * Copyright (c) 2020. Sebastian Hjelm
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
