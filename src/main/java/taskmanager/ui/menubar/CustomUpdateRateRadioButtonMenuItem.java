package taskmanager.ui.menubar;

import config.Config;

import javax.swing.ButtonGroup;
import javax.swing.JRadioButtonMenuItem;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;

public class CustomUpdateRateRadioButtonMenuItem extends JRadioButtonMenuItem {
    public CustomUpdateRateRadioButtonMenuItem(ButtonGroup group) {
        super("Custom...");
        if (group.getSelection() == null) {
            setSelected(true);
            setText(String.format("Custom (%.1f)", Config.getFloat(Config.KEY_UPDATE_RATE)));
        }
        addActionListener(this::activated);
        addItemListener(this::itemStateChanged);
        group.add(this);
    }

    private void activated(ActionEvent e) {
//        Config.put(Config.KEY_UPDATE_RATE, Double.toString(1 / interval)
        // TODO Show dialog here!
    }

    private void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.DESELECTED) {
            setText("Custom...");
        }
    }
}
