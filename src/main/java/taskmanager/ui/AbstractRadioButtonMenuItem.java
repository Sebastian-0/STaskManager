
package taskmanager.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JRadioButtonMenuItem;

public abstract class AbstractRadioButtonMenuItem extends JRadioButtonMenuItem
{
  public AbstractRadioButtonMenuItem(String text)
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
