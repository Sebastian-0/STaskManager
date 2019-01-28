package taskmanager;

import java.util.Iterator;

public interface Measurements<T>
{
  public void copyFrom(Measurements<T> other);
  public void copyDelta(Measurements<T> other);

  public void addValue(T value);
  
  public T newest();
  public T oldest();
  public T max();
  public T min();
  
  public Iterator<T> getRangeIterator(int startIndex, int endIndex);
  public int size();
}
