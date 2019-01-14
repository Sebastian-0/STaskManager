package taskmanager.ui.performance.cpu;

import java.awt.GridLayout;

import javax.swing.JPanel;

import config.Config;
import taskmanager.Measurements;
import taskmanager.SystemInformation;
import taskmanager.ui.performance.GraphPanel;
import taskmanager.ui.performance.GraphPanel.DoubleToLong;
import taskmanager.ui.performance.GraphType;
import taskmanager.ui.performance.TimelineGraphPanel;

public class MultiCpuPanel extends JPanel
{
  private Measurements<Long>[] measurements;
  private GraphPanel[] graphs;
  
  @SuppressWarnings("unchecked")
  public MultiCpuPanel(TimelineGraphPanel timeline, SystemInformation systemInformation) {
    int numCores = systemInformation.logicalProcessorCount;
    measurements = new Measurements[numCores];
    graphs = new GraphPanel[numCores];
    
    for (int i = 0; i < numCores; i++) {
      measurements[i] = new DoubleToLong(systemInformation.cpuUsagePerCore[i]);
      graphs[i] = new GraphPanel(GraphType.Cpu, true);
      graphs[i].addGraph(measurements[i]);
    }
    
    int height = (int) (Math.log(numCores-1)/Math.log(2)) + 1; 
    int width = numCores / height + (numCores % height == 0 ? 0 : 1);
    
    setLayout(new GridLayout(height, width, 5, 5));
    
    for (int i = 0; i < numCores; i++) {
      add(graphs[i]);
    }
    
    timeline.connectGraphs(graphs);
  }
  
  public void update(SystemInformation systemInformation) {
    final int total = Config.DOUBLE_TO_LONG;
    for (int i = 0; i < graphs.length; i++) {      
      graphs[i].setMaxDatapointValue(total);
      graphs[i].newDatapoint();
    }
  }
}
