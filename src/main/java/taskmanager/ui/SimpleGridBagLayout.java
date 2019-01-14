
package taskmanager.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

public class SimpleGridBagLayout
{
  private Container component;
  
  private GridBagLayout gridBagLayout;  
  private GridBagConstraints constraints;
  
  public SimpleGridBagLayout(Container componentWithLayout)
  {
    this.component = componentWithLayout;
    
    gridBagLayout = new GridBagLayout();
    constraints = new GridBagConstraints();
    componentWithLayout.setLayout(gridBagLayout);
    
    constraints.insets.set(5, 5, 5, 5);
  }
  
  
  public void setInsets(int top, int left, int bottom, int right)
  {
    constraints.insets.set(top, left, bottom, right);
  }
  
  public void setInternalPadding(int padX, int padY)
  {
    constraints.ipadx = padX;
    constraints.ipady = padY;
  }
  
  public void addToGrid(Component comp, int x, int y, int width, int height)
  {
    addToGrid(comp, x, y, width, height, GridBagConstraints.NONE, 0, 0);
  }
  
  public void addToGrid(Component comp, int x, int y, int width, int height, int anchor)
  {
    addToGrid(comp, x, y, width, height, GridBagConstraints.NONE, 0, 0, anchor);
  }
  
  public void addToGrid(Component comp, int x, int y, int width, int height,
      int fill, double wx, double wy)
  {
    addToGrid(comp, x, y, width, height, fill, wx, wy, GridBagConstraints.CENTER);
  }

  public void addToGrid(Component comp, int x, int y, int width, int height,
      int fill, double wx, double wy, int anchor)
  {
    constraints.gridx = x;
    constraints.gridy = y;
    constraints.gridwidth  = width;
    constraints.gridheight = height;
    constraints.weightx = wx;
    constraints.weighty = wy;
    constraints.anchor  = anchor;
    constraints.fill    = fill;
    
    gridBagLayout.setConstraints(comp, constraints);
    component.add(comp);
  }
}
