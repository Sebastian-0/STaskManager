package taskmanager.ui.details;

import java.awt.Component;

import javax.swing.JOptionPane;

import com.sun.jna.Platform;

import taskmanager.Process;
import taskmanager.ui.AbstractMenuItem;
import taskmanager.win32.WindowsProcess;

public class DeleteProcessMenuItem extends AbstractMenuItem
{
  private Component parent;
  private Process process;
  
  public DeleteProcessMenuItem(Component parent) {
    super("End process");
    this.parent = parent;
  }

  @Override
  protected void doAction() {
    int result = JOptionPane.showConfirmDialog(parent, "<html>Do you want to end \"" + process.fileName + "\"?<br> All unsaved data in the process will be lost.</html>", "Killing process", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
    if (result == JOptionPane.YES_OPTION) {
      if (Platform.isWindows()) {
        boolean succeeded = WindowsProcess.kill(process.id);
        if (!succeeded) {
          JOptionPane.showMessageDialog(parent, "Failed to terminate the process!", "Termination failed", JOptionPane.ERROR_MESSAGE);
        }
      } else {
        throw new UnsupportedOperationException("You are running an unsupported operating system!");
      }
    }
  }
  
  public void setProcess(Process process) {
    this.process = process;
    setEnabled(!process.isDead);
  }
}
