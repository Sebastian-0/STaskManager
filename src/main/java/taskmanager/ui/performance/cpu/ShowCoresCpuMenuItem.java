package taskmanager.ui.performance.cpu;

import taskmanager.ui.AbstractRadioButtonMenuItem;

public class ShowCoresCpuMenuItem extends AbstractRadioButtonMenuItem
{
  private CpuPanel cpuPanel;
  
  public ShowCoresCpuMenuItem(CpuPanel cpuPanel)
  {
    super("Logical processors");
    this.cpuPanel = cpuPanel;
  }

  @Override
  protected void doAction() {
    cpuPanel.showCoresCpu();
  }
}
