/*
 * Copyright (c) 2021. Sebastian Hjelm
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * See LICENSE for further details.
 */

package taskmanager.ui.callbacks;

import javax.swing.JFrame;

public interface ApplicationCallback { // TODO shj: Inheritance between all the callbacks to avoid casting? (e.g. ProcessContextMenu, ProcessTable, ...)
	void exit();
	void focus();
	void configChanged();
	JFrame frame();
}