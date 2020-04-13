/*
 * Copyright (c) 2020. Sebastian Hjelm
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
