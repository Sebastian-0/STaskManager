package taskmanager.ui.details;

import taskmanager.Process;

import javax.swing.JPopupMenu;
import java.awt.Component;

public class ProcessContextMenu extends JPopupMenu {
	private OpenProcessDialogMenuItem openDialogMenuItem;
	private DeleteProcessMenuItem deleteMenuItem;
	private OpenFileLocationMenuItem openLocationMenuItem;

	public ProcessContextMenu(Component parent) {
		openDialogMenuItem = new OpenProcessDialogMenuItem(parent);
		deleteMenuItem = new DeleteProcessMenuItem(parent);
		openLocationMenuItem = new OpenFileLocationMenuItem(parent);

		add(openDialogMenuItem);
		addSeparator();
		add(openLocationMenuItem);
		add(deleteMenuItem);
	}

	public void setProcess(Process process) {
		openDialogMenuItem.setProcess(process);
		deleteMenuItem.setProcess(process);
		openLocationMenuItem.setProcess(process);
	}
}
