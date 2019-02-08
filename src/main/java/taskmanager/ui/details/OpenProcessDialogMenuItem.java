package taskmanager.ui.details;

import taskmanager.Process;
import taskmanager.ui.AbstractMenuItem;

import java.awt.Component;

public class OpenProcessDialogMenuItem extends AbstractMenuItem {
	private Component parent;
	private Process process;

	public OpenProcessDialogMenuItem(Component parent) {
		super("Open process details");
		this.parent = parent;
	}

	@Override
	protected void doAction() {
		((ProcessDetailsCallback) parent).openDialog(process);
	}

	public void setProcess(Process process) {
		this.process = process;
	}
}
