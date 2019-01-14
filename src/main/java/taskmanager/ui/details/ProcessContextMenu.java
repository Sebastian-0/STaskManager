package taskmanager.ui.details;

import java.awt.Component;

import javax.swing.JPopupMenu;

import taskmanager.Process;

public class ProcessContextMenu extends JPopupMenu
{
  private DeleteProcessMenuItem deleteMenuItem;
  private OpenFileLocationMenuItem openLocationMenuItem;

  public ProcessContextMenu(Component parent) {
    deleteMenuItem = new DeleteProcessMenuItem(parent);
    openLocationMenuItem = new OpenFileLocationMenuItem(parent);
    
    add(openLocationMenuItem);
    add(deleteMenuItem);
  }

  public void setProcess(Process process) {
    deleteMenuItem.setProcess(process);
    openLocationMenuItem.setProcess(process);
  }
}
