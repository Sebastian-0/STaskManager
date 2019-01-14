package taskmanager.ui.performance.cpu;

import taskmanager.ui.AbstractRadioButtonMenuItem;

public class ShowTotalCpuMenuItem extends AbstractRadioButtonMenuItem
{
  private CpuPanel cpuPanel;
  
  public ShowTotalCpuMenuItem(CpuPanel cpuPanel)
  {
    super("Overall utilization");
    this.cpuPanel = cpuPanel;
  }

  @Override
  protected void doAction() {
    cpuPanel.showTotalCpu();
  }
}
