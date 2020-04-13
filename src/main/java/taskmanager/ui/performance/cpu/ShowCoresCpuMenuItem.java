/*
 * Copyright (c) 2020. Sebastian Hjelm
 */

package taskmanager.ui.performance.cpu;

import taskmanager.ui.AbstractRadioButtonMenuItem;

public class ShowCoresCpuMenuItem extends AbstractRadioButtonMenuItem {
	private final CpuPanel cpuPanel;
	
	public ShowCoresCpuMenuItem(CpuPanel cpuPanel) {
		super("Logical processors");
		this.cpuPanel = cpuPanel;
	}

	@Override
	protected void doAction() {
		cpuPanel.showCoresCpu();
	}
}
