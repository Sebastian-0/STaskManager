package taskmanager.ui.performance;

import java.util.ArrayList;
import java.util.List;

public class TimelineGroup
{
  private List<TimelineGraphPanel> timelines;
  private boolean areLinked;
  
  public TimelineGroup() {
    timelines = new ArrayList<>();
  }
  
  public void add(TimelineGraphPanel timeline) {
    timelines.add(timeline);
    timeline.addToGroup(this);
  }
  
  public void setLinked(boolean areLinked) {
    this.areLinked = areLinked;
  }
  
  protected void changed(TimelineGraphPanel source, int startIdx, int endIdx) {
    if (areLinked) {
      for (TimelineGraphPanel timeline : timelines)
      {
        if (timeline != source) {
          timeline.updateIndices(startIdx, endIdx);
        }
      }
    }
  }  
}
