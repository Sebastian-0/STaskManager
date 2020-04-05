/*
 * Copyright (c) 2020. Sebastian Hjelm
 */

package taskmanager.ui.details;

import config.Config;

import javax.swing.JCheckBox;

public class ShowAllProcessesCheckbox extends JCheckBox {
    public ShowAllProcessesCheckbox(ProcessTable liveTable, ProcessTable deadTable) {
        super("Show processes for all users", Config.getBoolean(Config.KEY_SHOW_PROCESSES_FOR_ALL_USERS));
        addActionListener(e -> {
            Config.put(Config.KEY_SHOW_PROCESSES_FOR_ALL_USERS, Boolean.toString(isSelected()));
            liveTable.setShowProcessesForAllUsers(isSelected());
            deadTable.setShowProcessesForAllUsers(isSelected());
        });
    }
}
