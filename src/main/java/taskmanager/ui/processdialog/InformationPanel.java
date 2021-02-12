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

import net.miginfocom.swing.MigLayout;
import taskmanager.data.Process;
import taskmanager.data.Status;
import taskmanager.ui.StatusUtils;
import taskmanager.ui.TextUtils;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

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

		// TODO Make all lines here take up equal vertical space!
		setLayout(new MigLayout("wrap 2", "", "sg 1"));
		add(new JLabel("PID: " + process.id));
		add(new JLabel("Parent ID: " + (process.parentId != -1 ? process.parentId : "---")), "gapleft 15");

		add(new JLabel("Name: " + process.fileName));
		add(new JLabel("User: " + process.userName), "gapleft 15");

		add(statusLabel, "wrap");

		if (process.startTimestamp > 0) {
			add(new JLabel("Started: " + TextUtils.valueToString(process.startTimestamp, TextUtils.ValueType.Date)));
			add(durationLabel, "gapleft 15");
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