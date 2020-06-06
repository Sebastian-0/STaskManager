package taskmanager.ui.menubar;

import config.Config;
import taskmanager.ui.ApplicationCallback;

public class LinkTimelinesMenuItem extends AbstractCheckboxMenuItem {
    public LinkTimelinesMenuItem(ApplicationCallback callback) {
        super("Link graph timelines", Config.KEY_LINK_TIMELINES, callback);
        setMnemonic('L');
    }
}
