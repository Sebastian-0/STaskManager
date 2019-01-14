package taskmanager;

public interface UI
{
  void init(SystemInformation systemInformation);
  void update(SystemInformation systemInformation);
  boolean hasTerminated();
}
