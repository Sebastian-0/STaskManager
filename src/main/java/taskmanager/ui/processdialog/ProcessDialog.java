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

import javax.swing.JDialog;
import javax.swing.JFrame;
import java.awt.GridBagConstraints;

public class ProcessDialog extends JDialog {
	private final PerformancePanel performancePanel;
	private final InformationPanel informationPanel;

	public ProcessDialog(JFrame parent, Process process) {
		super(parent);

		setTitle("Process: " + process.fileName + " (" + process.id + ")");

		setModalityType(ModalityType.MODELESS);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		performancePanel = new PerformancePanel(process);
		informationPanel = new InformationPanel(process);
		CommandLinePanel commandLinePanel = new CommandLinePanel(process);

		if (process.status == Status.Dead) {
			processEnded();
		}

		SimpleGridBagLayout gbl = new SimpleGridBagLayout(this);
		gbl.addToGrid(performancePanel, 0, 0, 1, 1, GridBagConstraints.BOTH, 1, 1);
		gbl.addToGrid(informationPanel, 0, 1, 1, 1, GridBagConstraints.HORIZONTAL, 1, 0);
		gbl.addToGrid(commandLinePanel, 0, 2, 1, 1, GridBagConstraints.HORIZONTAL, 1, 0);
		pack();

		setMinimumSize(getSize());

		setLocationRelativeTo(parent);
	}

	public void update() {
		performancePanel.update();
		informationPanel.update();
	}

	public void processEnded() {
		setTitle(getTitle() + " - DEAD");
		informationPanel.processDied();
	}
}
