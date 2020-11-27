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

import javax.swing.JDialog;
import javax.swing.JFrame;

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

		setLayout(new MigLayout());
		add(performancePanel, "grow, push, wrap");
		add(informationPanel, "growx, wrap");
		add(commandLinePanel, "growx");

		pack();
		setMinimumSize(getLayout().minimumLayoutSize(this));

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
