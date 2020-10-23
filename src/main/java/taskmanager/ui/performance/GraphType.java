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

import taskmanager.ui.TextUtils.ValueType;

import java.awt.Color;

public enum GraphType {
	Memory(new Color(139, 18, 174), "Memory", ValueType.Bytes),
	Cpu(new Color(17, 125, 187), "Cpu", ValueType.Percentage),
	Network(new Color(167, 79, 1), "Network", ValueType.BitsPerSecond),
	Disk(new Color(77, 166, 12), "Disk", ValueType.Percentage),
	Gpu(new Color(167, 1, 7), "Gpu", ValueType.Percentage);
	
	public final Color color;
	public final String header;
	public final ValueType mainValueType;
	
	GraphType(Color color, String header, ValueType mainValueType) {
		this.color = color;
		this.header = header;
		this.mainValueType = mainValueType;
	}
}