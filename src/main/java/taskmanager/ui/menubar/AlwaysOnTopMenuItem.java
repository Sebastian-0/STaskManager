package taskmanager.ui.menubar;

import config.Config;
import taskmanager.ui.ApplicationCallback;

public class AlwaysOnTopMenuItem extends AbstractCheckboxMenuItem {
    public AlwaysOnTopMenuItem(ApplicationCallback callback) {
        super("Keep on top", Config.KEY_ALWAYS_ON_TOP, callback);
        setMnemonic('T');
    }
}
