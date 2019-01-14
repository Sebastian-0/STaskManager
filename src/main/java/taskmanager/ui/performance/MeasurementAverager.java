package taskmanager.ui.performance;

import java.util.Iterator;

import taskmanager.Measurements;

public class MeasurementAverager
{
  private Measurements<Long> measurements;
  private Iterator<Long> iterator;
  
  private int offset;
  private int numPoints;
  
  private int start;
  private int end;
  
  private int stepSize;
  
  public MeasurementAverager(Measurements<Long> measurements) {
    this.measurements = measurements;
    this.stepSize = 1;
  }
  
  public void setInterval(int start, int end, int stepSize) {
    this.stepSize = stepSize;
    this.start = Math.max(start, stepSize);
    this.end = end;
    
    this.end -= (this.end - this.start) % stepSize;
  }
  
  public void reset() {
    reset(0);
  }
  
  public void reset(int idx) {
    int start = this.start - offset + idx * stepSize;
    int end = this.end - offset;
    
    iterator = measurements.getRangeIterator(start, end - 1);
    numPoints = (end - start) / stepSize - 1;
  }
  
  public long next() {
    if (iterator != null && iterator.hasNext()) {
      long total = 0;
      for (int i = 0; i < stepSize; i++) {
        total += iterator.next();
      }
      return total / stepSize;
    }
    
    return 0;
  }
  
  public boolean hasNext() {
    return iterator != null && iterator.hasNext();
  }
  
  public int numPoints() {
    return numPoints;
  }
  
  public void shift(int off) {
//    offset = ++offset % stepSize;
    offset = off % stepSize;
  }
}
