/*
 * Copyright (c) 2020. Sebastian Hjelm
 */

package taskmanager.filter.concrete;

import taskmanager.Process;
import taskmanager.filter.TextContainsFilter;

public class CommandLineFilter extends TextContainsFilter {
    public CommandLineFilter(String cmd) {
        super(cmd);
    }

    @Override
    protected String textToFilter(Process process) {
        return process.commandLine;
    }
}
