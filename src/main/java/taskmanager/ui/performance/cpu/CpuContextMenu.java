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

import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;

public class CpuContextMenu extends JPopupMenu {
	public CpuContextMenu(CpuPanel panel) {
		ShowTotalCpuMenuItem totalCpu = new ShowTotalCpuMenuItem(panel);
		ShowCoresCpuMenuItem coresCpu = new ShowCoresCpuMenuItem(panel);
		
		ButtonGroup group = new ButtonGroup();
		group.add(totalCpu);
		group.add(coresCpu);
		totalCpu.setSelected(true);
		
		JMenu subMenu = new JMenu("Change graph to ");
		subMenu.add(totalCpu);
		subMenu.add(coresCpu);
		
		add(subMenu);
	}
}