
package taskmanager.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;

public abstract class AbstractMenuItem extends JMenuItem
{
  public AbstractMenuItem(String text)
  {
    super (text);
    
    addActionListener(actionListener);
  }
  
  protected abstract void doAction();
  
  
  private ActionListener actionListener = new ActionListener()
  {
    @Override
    public void actionPerformed(ActionEvent e)
    {
      doAction();
    }
  };
}
