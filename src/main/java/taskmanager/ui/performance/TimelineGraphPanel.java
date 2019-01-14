package taskmanager.ui.performance;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JLabel;

import config.Config;
import taskmanager.Measurements;

public class TimelineGraphPanel extends GraphPanel
{
  private List<GraphPanel> connectedGraphs;
  private int startIndex;
  private int endIndex;
  
  private JLabel timeLabel;
  
  private boolean highlightLeftBorder;
  private boolean highlightRightBorder;
  
  private TimelineGroup group;
  
  public TimelineGraphPanel(GraphPanel mainGraph, JLabel timeLabel)
  {
    super(mainGraph.graphType, false);
    this.timeLabel = timeLabel;
    
    connectedGraphs = new ArrayList<>();
    connectedGraphs.add(mainGraph);
    
    addMouseListener(mouseListener);
    addMouseMotionListener(mouseListener);
    
    group = new TimelineGroup();
  }
  
  @Override
  public void addGraph(Measurements<Long> measurements, boolean isDashed) {
    super.addGraph(measurements, isDashed);
    setDataIndexInterval(0, measurements.size() - 1);
  }
  
  
  public void connectGraphs(GraphPanel... graphs) {
    connectedGraphs.addAll(Arrays.asList(graphs));
  }
  
  protected void addToGroup(TimelineGroup group) {
    this.group = group;
  }
  
  protected void updateIndices(int start, int end) {
    if (start != startIndex || end != endIndex) {
      startIndex = start;
      endIndex = end;
      
      updateConnectedGraphs();
      updateTimeLabel();
    }
  }
  
  private void updateConnectedGraphs() {
    for (GraphPanel graphPanel : connectedGraphs) {
      if (graphPanel.dataStartIndex != startIndex || graphPanel.dataEndIndex != endIndex) {
        graphPanel.setDataIndexInterval(startIndex, endIndex);
        graphPanel.repaint();
      }
    }
  }

  private void updateTimeLabel() {
    int diff = endIndex - startIndex;
    int seconds = (int) (diff / Float.parseFloat(Config.get(Config.KEY_UPDATE_RATE)));
    
    if (seconds > 60*3-1) {
      timeLabel.setText("Displaying " + Math.round(seconds/(float)60) + " minutes");
    } else {
      timeLabel.setText("Displaying " + seconds + " seconds");
    }
  }
  
  
  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    
    g.setColor(new Color(0, 0, 0, 20));
    int start = getLeftBorderPosition();
    int end = getRightBorderPosition();
    g.fillRect(start, 0, (end - start), getHeight());
    
    g.setColor(Color.GRAY);
    if (highlightLeftBorder) {
      g.fillRect(Math.max(getLeftBorderPosition() - 2, 0), 0, 4, getHeight());
    }
    if (highlightRightBorder) {
      g.fillRect(Math.min(getRightBorderPosition() - 2, getWidth() - 4), 0, 4, getHeight());
    }
  }
  
  
  private int getLeftBorderPosition() {
    return indexToPosition(startIndex);
  }
  
  
  private int getRightBorderPosition() {
    return indexToPosition(endIndex);
  }

  private int indexToPosition(int idx) {
    int width = dataEndIndex - dataStartIndex;
    float fraction = clamp(idx / (float)width, 0, 1);
    return (int) (getWidth() * fraction);
  }

  private int positionToIndex(int pos) {
    int width = dataEndIndex - dataStartIndex;
    float fraction = clamp(pos / (float) getWidth(), 0, 1);
    return (int) (width * fraction);
  }
  
  private float clamp(float value, float min, float max) {
    return Math.min(Math.max(min, value), max);
  }
  
  @Override
  public void newDatapoint() {
    super.newDatapoint();

    startIndex = connectedGraphs.get(0).dataStartIndex;
    endIndex = connectedGraphs.get(0).dataEndIndex;
  }
  
  
  private MouseAdapter mouseListener = new MouseAdapter() {
    static final int BORDER_WIDTH = 10;

    private boolean isMovingLeftBorder;
    private boolean isMovingRightBorder;
    private boolean isMovingWholeInterval;
    
    private int grabDistanceLeft;
    private int grabDistanceRight;
    
    private boolean isMouseOver;
    
    @Override
    public void mousePressed(MouseEvent e) {
      
      if (e.getButton() == MouseEvent.BUTTON1) {
        int x = e.getX();
        
        int left = getLeftBorderPosition();
        int right = getRightBorderPosition();
        
        if (Math.abs(x - left) <= BORDER_WIDTH/2) {
          isMovingLeftBorder = true;
        } else if (Math.abs(x - right) <= BORDER_WIDTH/2) {
          isMovingRightBorder = true;
        } else if (x >= left && x <= right) {
          isMovingWholeInterval = true;
          int xIdx = positionToIndex(x);
          grabDistanceLeft = xIdx - startIndex;
          grabDistanceRight = endIndex - xIdx;
        }
      }
    }
    
    @Override
    public void mouseReleased(MouseEvent e) {
      if (e.getButton() == MouseEvent.BUTTON1) {
        isMovingLeftBorder = false;
        isMovingRightBorder = false;
        isMovingWholeInterval = false;
        
        if (!isMouseOver) {
          highlightLeftBorder = false;
          highlightRightBorder = false;
        }
      }
    }
    
    @Override
    public void mouseDragged(MouseEvent e) {
      int x = e.getX();
      
      final int smallestIntervalPoints = (int)(60 * Float.parseFloat(Config.get(Config.KEY_UPDATE_RATE)));
      
      if (isMovingLeftBorder) {
        startIndex = positionToIndex(x);
        startIndex = Math.min(startIndex, endIndex - smallestIntervalPoints);

        updateConnectedGraphs();
        updateTimeLabel();
        group.changed(TimelineGraphPanel.this, startIndex, endIndex);
        
        repaint();
      } else if (isMovingRightBorder) {
        endIndex = positionToIndex(x);
        endIndex = Math.max(endIndex, startIndex + smallestIntervalPoints);

        updateConnectedGraphs();
        updateTimeLabel();
        group.changed(TimelineGraphPanel.this, startIndex, endIndex);
        
        repaint();
      } else if (isMovingWholeInterval) {
        int intervalWidth = endIndex - startIndex;
        startIndex = Math.min(dataEndIndex - intervalWidth, Math.max(0, positionToIndex(x) - grabDistanceLeft));
        endIndex = Math.max(intervalWidth, Math.min(dataEndIndex, positionToIndex(x) + grabDistanceRight));

        updateConnectedGraphs();
        group.changed(TimelineGraphPanel.this, startIndex, endIndex);
        
        repaint();
      }
      
      updateHighlight(x);
    }
    
    
    @Override
    public void mouseMoved(MouseEvent e) {
      int x = e.getX();
      updateHighlight(x);
    }

    private void updateHighlight(int x) {
      int left = getLeftBorderPosition();
      int right = getRightBorderPosition();
      
      if (Math.abs(x - left) <= BORDER_WIDTH/2) {
        if (!highlightLeftBorder)
          repaint();
        highlightLeftBorder = true;
      } else {
        if (highlightLeftBorder)
          repaint();
        highlightLeftBorder = false;
      }
        
      if (Math.abs(x - right) <= BORDER_WIDTH/2) {
        if (!highlightRightBorder)
          repaint();
        highlightRightBorder = true;
      } else {
        if (highlightRightBorder)
          repaint();
        highlightRightBorder = false;
      }
    }
    
    @Override
    public void mouseEntered(MouseEvent e) {
      isMouseOver = true;
    }
    
    @Override
    public void mouseExited(MouseEvent e) {
      if (highlightLeftBorder)
        repaint();
      if (highlightRightBorder)
        repaint();
      highlightLeftBorder = false;
      highlightRightBorder = false;
      
      isMouseOver = false;
    }
  };
}
