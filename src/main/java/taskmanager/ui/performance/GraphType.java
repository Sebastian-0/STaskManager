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

package taskmanager.ui.performance;

import java.awt.Color;

public enum GraphType {
	Memory(new Color(139, 18, 174), "Memory"),
	Cpu(new Color(17, 125, 187), "Cpu"),
	Network(new Color(167, 79, 1), "Network"),
	Disk(new Color(77, 166, 12), "Disk"),
	Gpu(new Color(167, 1, 7), "Gpu");
	
	public final Color color;
	public final String header;
	
	GraphType(Color color, String header) {
		this.color = color;
		this.header = header;
	}
}