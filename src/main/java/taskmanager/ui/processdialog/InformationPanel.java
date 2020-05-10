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

package taskmanager.ui.processdialog;

import taskmanager.Process;
import taskmanager.ui.SimpleGridBagLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import java.awt.GridBagConstraints;

public class InformationPanel extends JPanel {
	private final JLabel statusLabel;

	public InformationPanel(Process process) {
		setBorder(new TitledBorder("General"));

		// TODO Extend this with more statuses, also make the status colored!
		statusLabel = new JLabel("Status: " + (process.isDead ? "DEAD" : "Running"));

		SimpleGridBagLayout gbl = new SimpleGridBagLayout(this);
		gbl.setInsets(5, 5, 5, 15);
		gbl.addToGrid(new JLabel("PID: " + process.id), 0, 0, 1, 1, GridBagConstraints.WEST);
		gbl.setInsets(5, 5, 5, 5);
		gbl.addToGrid(new JLabel("Name: " + process.fileName), 1, 0, 1, 1, GridBagConstraints.HORIZONTAL, 1, 0);
		gbl.setInsets(5, 5, 5, 15);
		gbl.addToGrid(new JLabel("User: " + process.userName), 0, 1, 1, 1, GridBagConstraints.WEST);
		gbl.setInsets(5, 5, 5, 5);
		gbl.addToGrid(statusLabel, 1, 1, 1, 1, GridBagConstraints.HORIZONTAL, 1, 0);
	}

	public void processDied() {
		statusLabel.setText("Status: DEAD");
	}
}