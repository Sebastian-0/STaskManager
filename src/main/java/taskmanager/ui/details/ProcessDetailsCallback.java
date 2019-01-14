package taskmanager.ui.details;

import taskmanager.Process;

import java.util.Comparator;

public interface ProcessDetailsCallback {
	void openDialog(Process process);
	void setComparator(Comparator<Process> comparator, boolean isDeadList);
}
