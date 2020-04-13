/*
 * Copyright (c) 2020. Sebastian Hjelm
 */

package taskmanager.ui.performance.cpu;

import taskmanager.ui.AbstractRadioButtonMenuItem;

public class ShowTotalCpuMenuItem extends AbstractRadioButtonMenuItem {
	private final CpuPanel cpuPanel;
	
	public ShowTotalCpuMenuItem(CpuPanel cpuPanel) {
		super("Overall utilization");
		this.cpuPanel = cpuPanel;
	}

	@Override
	protected void doAction() {
		cpuPanel.showTotalCpu();
	}
}
