package taskmanager.ui.menubar;

import config.Config;

import javax.swing.ButtonGroup;
import javax.swing.JRadioButtonMenuItem;

public class UpdateRateRadioButtonMenuItem extends JRadioButtonMenuItem {
    public UpdateRateRadioButtonMenuItem(ButtonGroup group, double interval) {
        super (String.format("Every %.1f s", interval));
        if (Math.abs(Config.getFloat(Config.KEY_UPDATE_RATE) - 1 / interval) < 0.0001f) {
            setSelected(true);
        }
        addActionListener(e -> Config.put(Config.KEY_UPDATE_RATE, Double.toString(1 / interval)));
        group.add(this);
    }
}
