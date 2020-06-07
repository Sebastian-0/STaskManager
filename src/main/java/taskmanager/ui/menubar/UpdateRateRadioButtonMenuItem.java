package taskmanager.ui.menubar;

import config.Config;

import javax.swing.ButtonGroup;
import javax.swing.JRadioButtonMenuItem;

public class UpdateRateRadioButtonMenuItem extends JRadioButtonMenuItem {
    public UpdateRateRadioButtonMenuItem(ButtonGroup group, double interval) {
        super (String.format("Every %.1f s", interval));
        addActionListener(e -> Config.put(Config.KEY_UPDATE_RATE, Double.toString(1 / interval)));
        group.add(this);
    }
}
