package taskmanager.ui.menubar;

import taskmanager.ui.ApplicationCallback;

import javax.swing.JMenuItem;

public class ExitMenuItem extends JMenuItem {
    public ExitMenuItem(ApplicationCallback callback) {
        super("Exit");
        setMnemonic('E');
        addActionListener(e -> callback.exit());
    }
}
