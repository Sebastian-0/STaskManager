/*
 * Copyright (c) 2020. Sebastian Hjelm
 */

package taskmanager.filter.concrete;

import taskmanager.Process;
import taskmanager.filter.TextContainsFilter;

public class UserNameFilter extends TextContainsFilter {
    public UserNameFilter(String name) {
        super(name);
    }

    @Override
    protected String textToFilter(Process process) {
        return process.userName;
    }
}
