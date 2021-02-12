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

import taskmanager.Measurements;
import taskmanager.data.TopList;
import taskmanager.ui.TextUtils.ValueType;
import taskmanager.ui.callbacks.ShowProcessCallback;
import taskmanager.ui.performance.common.TopListMenu;

import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;

public class CpuContextMenu extends JPopupMenu {
	public CpuContextMenu(CpuPanel panel, Measurements<TopList> cpuTopList, ShowProcessCallback callback) {
		ShowTotalCpuMenuItem totalCpu = new ShowTotalCpuMenuItem(panel);
		ShowCoresCpuMenuItem coresCpu = new ShowCoresCpuMenuItem(panel);
		
		ButtonGroup group = new ButtonGroup();
		group.add(totalCpu);
		group.add(coresCpu);
		totalCpu.setSelected(true);
		
		JMenu changeGraphMenu = new JMenu("Change graph to ");
		changeGraphMenu.add(totalCpu);
		changeGraphMenu.add(coresCpu);

		add(changeGraphMenu);
		add(new TopListMenu(this, cpuTopList, ValueType.Percentage, callback));
	}
}