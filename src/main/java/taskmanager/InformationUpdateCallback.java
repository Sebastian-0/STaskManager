package taskmanager;

public interface InformationUpdateCallback {
	void init(SystemInformation systemInformation);
	void update(SystemInformation systemInformation);
	boolean hasTerminated();
}
