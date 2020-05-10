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