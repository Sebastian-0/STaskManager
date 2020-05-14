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

import taskmanager.data.Process;
import taskmanager.data.Status;
import taskmanager.ui.SimpleGridBagLayout;
import taskmanager.ui.StatusUtils;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import java.awt.GridBagConstraints;

public class InformationPanel extends JPanel {
	private final Process process;
	private final JLabel statusLabel;

	public InformationPanel(Process process) {
		this.process = process;
		setBorder(new TitledBorder("General"));

		// TODO Make the status colored!
		statusLabel = new JLabel();
		updateStatusLabelText();

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

	private void updateStatusLabelText() {
		statusLabel.setText("Status: " + StatusUtils.name(process.status));
	}

	public void processDied() {
		updateStatusLabelText();
	}

	public void update() {
		updateStatusLabelText();
	}
}