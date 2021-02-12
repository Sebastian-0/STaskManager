package taskmanager.ui.menubar;

import config.Config;
import taskmanager.ui.callbacks.ApplicationCallback;

import javax.swing.JCheckBoxMenuItem;

public class AbstractCheckboxMenuItem extends JCheckBoxMenuItem {
    public AbstractCheckboxMenuItem(String text, String configKey, ApplicationCallback callback) {
        super(text, Config.getBoolean(configKey));
        addActionListener(e -> {
            Config.put(configKey, Boolean.toString(isSelected()));
            callback.configChanged();
        });
    }
}
