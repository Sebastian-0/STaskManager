package taskmanager.ui.performance;

import java.awt.Dimension;
import java.awt.GridBagConstraints;

import javax.swing.Box;
import javax.swing.JPanel;

import taskmanager.ui.SimpleGridBagLayout;

public class GraphSelectionPanel extends JPanel
{
  private GraphTypeButton[] buttons;
  
  public GraphSelectionPanel(PerformanceButtonListener listener, GraphTypeButton... buttons)
  {
    this.buttons = buttons;
    
    SimpleGridBagLayout layout = new SimpleGridBagLayout(this);

    int row = 0;
    for (; row < buttons.length; row++) {
      layout.addToGrid(buttons[row], 0, row, 1, 1, GridBagConstraints.HORIZONTAL, 1, 0);
      buttons[row].setListener(listener);
    }
    layout.addToGrid(Box.createRigidArea(new Dimension(1, 1)), 0, row+1, 1, 1, GridBagConstraints.BOTH, 1, 1);
    
    buttons[0].select();
  }

  public void deselectAll() {
    for (GraphTypeButton button : buttons) {
      button.deselect();
    }
  }
}
