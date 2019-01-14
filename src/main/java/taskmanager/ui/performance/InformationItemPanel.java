package taskmanager.ui.performance;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.Stroke;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;

import taskmanager.ui.SimpleGridBagLayout;
import taskmanager.ui.TextUtils;
import taskmanager.ui.TextUtils.ValueType;

public class InformationItemPanel extends JPanel
{  
  private ValueType type;
  private Stroke stroke;
  private Color color; 

  protected JLabel valueLabel;

  public InformationItemPanel(String header, ValueType type) {
    this(header, type, null, null);
  }
  
  public InformationItemPanel(String header, ValueType type, Stroke stroke, Color color) {
    this.type = type;
    this.stroke = stroke;
    this.color = color;
    
    JLabel labelHeader = new JLabel(header); 
    valueLabel = new JLabel("0");
    valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD, valueLabel.getFont().getSize() + 3f));
    
    SimpleGridBagLayout layout = new SimpleGridBagLayout(this);
    layout.setInsets(0, 5, 5, 5);
    layout.addToGrid(Box.createRigidArea(new Dimension(1, 1)), 0, 0, 1, 2);
    layout.addToGrid(labelHeader, 1, 0, 1, 1, GridBagConstraints.HORIZONTAL, 1, 0, GridBagConstraints.WEST);
    layout.setInsets(0, 5, 0, 5);
    layout.addToGrid(valueLabel, 1, 1, 1, 1, GridBagConstraints.HORIZONTAL, 1, 0, GridBagConstraints.WEST);
  }
  
  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    
    if (stroke != null) {
      Graphics2D g2d = (Graphics2D) g;
      g2d.setColor(color);
      Stroke old = g2d.getStroke();
      g2d.setStroke(stroke);
      g2d.drawLine(5, getHeight()/8, 5, getHeight()*7/8);
      g2d.setStroke(old);
    }
  }
  
  public void updateValue(long value) {
    valueLabel.setText(TextUtils.valueToString(value, type));
  }
}
