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

package taskmanager.ui.performance;

import net.miginfocom.swing.MigLayout;

import javax.swing.JPanel;

public class GraphSelectionPanel extends JPanel {
	private final GraphTypeButton[] buttons;
	
	public GraphSelectionPanel(PerformanceButtonListener listener, GraphTypeButton... buttons) {
		this.buttons = buttons;
		setLayout(new MigLayout("wrap 1", "grow, fill"));
		int row = 0;
		for (; row < buttons.length; row++) {
			add(buttons[row]);
			buttons[row].setListener(listener);
		}
		buttons[0].select();
	}

	public void deselectAll() {
		for (GraphTypeButton button : buttons) {
			button.deselect();
		}
	}
}