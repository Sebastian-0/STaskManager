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

import taskmanager.ui.TextUtils;
import taskmanager.ui.TextUtils.ValueType;

public class RatioItemPanel extends InformationItemPanel {
	private long maximum;
	
	public RatioItemPanel(String header, long maximum) {
		super(header, ValueType.Bytes);
		this.maximum = maximum;
	}

	public void setMaximum(long maximum) {
		this.maximum = maximum;
    }
	
	@Override
	public void updateValue(long value) {
		valueLabel.setText(TextUtils.ratioToString(value, maximum, ValueType.Bytes));
	}
}