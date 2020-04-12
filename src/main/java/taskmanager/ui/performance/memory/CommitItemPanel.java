/*
 * Copyright (c) 2020. Sebastian Hjelm
 */

package taskmanager.ui.performance.memory;

import taskmanager.ui.TextUtils;
import taskmanager.ui.TextUtils.ValueType;
import taskmanager.ui.performance.InformationItemPanel;

public class CommitItemPanel extends InformationItemPanel
{
  private long maximumCommit;
  
  public CommitItemPanel(String header, long maximum) {
    super(header, ValueType.Bytes);
    maximumCommit = maximum;
  }
  
  @Override
  public void updateValue(long value) {
    valueLabel.setText(TextUtils.ratioToString(value, maximumCommit, ValueType.Bytes));
  }
}
