package taskmanager.ui.menubar;

import config.Config;
import taskmanager.ui.ApplicationCallback;

public class MinimizeToTrayMenuItem extends AbstractCheckboxMenuItem {
    public MinimizeToTrayMenuItem(ApplicationCallback callback) {
        super("Minimize to tray", Config.KEY_MINIMIZE_TO_TRAY, callback);
        setMnemonic('M');
    }
}
