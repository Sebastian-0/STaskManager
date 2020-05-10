/*
 * Copyright (c) 2020. Sebastian Hjelm
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * See LICENSE for further details.
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