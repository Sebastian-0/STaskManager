package taskmanager.ui.menubar;

import taskmanager.ui.callbacks.ApplicationCallback;

import javax.swing.JMenu;
import javax.swing.JMenuBar;

public class MenuBar extends JMenuBar {
    public MenuBar(ApplicationCallback callback) {
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic('F');
        fileMenu.add(new ExitMenuItem(callback));
        add(fileMenu);

        JMenu viewMenu = new JMenu("View");
        viewMenu.setMnemonic('V');
        viewMenu.add(new MinimizeToTrayMenuItem(callback));
        viewMenu.add(new AlwaysOnTopMenuItem(callback));
        viewMenu.addSeparator();
        viewMenu.add(new ShowDeadProcessesMenuItem(callback));
        viewMenu.addSeparator();
        viewMenu.add(new LinkTimelinesMenuItem(callback));
        add(viewMenu);

        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic('H');
        helpMenu.add(new AboutMenuItem(callback));
        add(helpMenu);
    }
}
