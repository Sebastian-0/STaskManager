package taskmanager.ui.menubar;

import config.Config;
import taskmanager.ui.ApplicationCallback;

public class ShowDeadProcessesMenuItem extends AbstractCheckboxMenuItem {
    public ShowDeadProcessesMenuItem(ApplicationCallback callback) {
        super("Show dead processes", Config.KEY_SHOW_DEAD_PROCESSES, callback);
        setMnemonic('D');
    }
}
