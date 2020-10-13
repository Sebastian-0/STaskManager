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
import taskmanager.ui.TextUtils;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import java.awt.GridBagConstraints;
import java.time.Duration;

public class InformationPanel extends JPanel {
	private final Process process;
	private final JLabel statusLabel;
	private final JLabel durationLabel;

	public InformationPanel(Process process) {
		this.process = process;
		setBorder(new TitledBorder("General"));

		statusLabel = new JLabel();
		durationLabel = new JLabel();
		updateStatusLabelText();
		updateDurationLabel();

		SimpleGridBagLayout gbl = new SimpleGridBagLayout(this);
		gbl.setInsets(5, 5, 5, 15);
		gbl.addToGrid(new JLabel("PID: " + process.id), 0, 0, 1, 1, GridBagConstraints.WEST);
		gbl.setInsets(5, 5, 5, 5);
		gbl.addToGrid(new JLabel("Name: " + process.fileName), 1, 0, 1, 1, GridBagConstraints.HORIZONTAL, 1, 0);

		gbl.setInsets(5, 5, 5, 15);
		gbl.addToGrid(new JLabel("User: " + process.userName), 0, 1, 1, 1, GridBagConstraints.WEST);
		gbl.setInsets(5, 5, 5, 5);
		gbl.addToGrid(statusLabel, 1, 1, 1, 1, GridBagConstraints.HORIZONTAL, 1, 0);

		if (process.startTimestamp > 0) {
			gbl.setInsets(5, 5, 5, 15);
			gbl.addToGrid(new JLabel("Started: " + TextUtils.valueToString(process.startTimestamp, TextUtils.ValueType.Date)), 0, 2, 1, 1, GridBagConstraints.WEST);
			gbl.setInsets(5, 5, 5, 5);
			gbl.addToGrid(durationLabel, 1, 2, 1, 1, GridBagConstraints.HORIZONTAL, 1, 0);
		}
	}

	public void processDied() {
		updateStatusLabelText();
	}

	public void update() {
		updateStatusLabelText();
		updateDurationLabel();
	}

	private void updateStatusLabelText() {
		String colorInHex = Integer.toHexString(StatusUtils.color(process.status).getRGB() & 0xFFFFFF);
		statusLabel.setText("<html>Status: <font color=#" + colorInHex + ">" + StatusUtils.name(process.status) + "</font></html>");
	}

	private void updateDurationLabel() {
		if (process.status == Status.Dead) {
			durationLabel.setText("Uptime: " + TextUtils.valueToString(process.deathTimestamp - process.startTimestamp, TextUtils.ValueType.TimeFull));
		} else {
			durationLabel.setText("Uptime: " + TextUtils.valueToString(System.currentTimeMillis() - process.startTimestamp, TextUtils.ValueType.TimeFull));
		}
	}
}