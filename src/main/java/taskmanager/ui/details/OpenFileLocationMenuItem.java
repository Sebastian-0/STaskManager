package taskmanager.ui.details;

import java.awt.Component;
import java.io.File;

import javax.swing.JOptionPane;

import com.sun.jna.Platform;

import taskmanager.Process;
import taskmanager.ui.AbstractMenuItem;
import taskmanager.win32.WindowsProcess;

public class OpenFileLocationMenuItem extends AbstractMenuItem
{
  private Component parent;
  private Process process;
  
  public OpenFileLocationMenuItem(Component parent) {
    super("Open file location");
    this.parent = parent;
  }

  @Override
  protected void doAction() {
    boolean succeeded = false;
    File file = new File(process.filePath);
    if (file.exists() && !file.isDirectory()) {
      File folder = file.getParentFile();
      if (Platform.isWindows()) {
        succeeded = WindowsProcess.openPath(folder.getAbsolutePath());
      } else {
        throw new UnsupportedOperationException("You are running an unsupported operating system!");
      }      
    }
    
    if (!succeeded) {
      JOptionPane.showMessageDialog(parent, "Failed to open the process path!", "Error", JOptionPane.ERROR_MESSAGE);
    }
  }
  
  public void setProcess(Process process) {
    this.process = process;
  }
}
