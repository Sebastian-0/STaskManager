package taskmanager.ui.performance.cpu;

import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;

public class CpuContextMenu extends JPopupMenu
{
  public CpuContextMenu(CpuPanel panel)
  {
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
