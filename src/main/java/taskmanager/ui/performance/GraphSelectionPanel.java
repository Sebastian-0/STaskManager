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

import config.Config;
import net.miginfocom.swing.MigLayout;

import javax.swing.JPanel;

public class GraphSelectionPanel extends JPanel {
	private final GraphTypeButton[] buttons;
	
	public GraphSelectionPanel(PerformanceButtonListener parentListener, GraphTypeButton... buttons) {
		this.buttons = buttons;
		PerformanceButtonListener listener = (type, index) -> {
			savePreviousState(type, index);
			parentListener.swapTo(type, index);
		};

		setLayout(new MigLayout("wrap 1", "grow, fill"));
		int row = 0;
		for (; row < buttons.length; row++) {
			add(buttons[row]);
			buttons[row].setListener(listener);
		}
	}

	private void savePreviousState(GraphType type, int index) {
		Config.put(Config.KEY_LAST_PERFORMANCE_PANEL_GRAPH, type.name() + " " + index);
	}

	public void loadPreviousState() {
		String previous = Config.get(Config.KEY_LAST_PERFORMANCE_PANEL_GRAPH);
		GraphType type = GraphType.Cpu;
		int index = 0;
		if (!previous.isEmpty()) {
			String[] tokens = previous.split(" ");
			type = GraphType.valueOf(tokens[0]);
			index = Integer.parseInt(tokens[1]);
		}

		for (GraphTypeButton button : buttons) {
			button.loadPreviousState(type, index);
		}
	}

	public void deselectAll() {
		for (GraphTypeButton button : buttons) {
			button.deselect();
		}
	}
}