/*
 * Copyright (c) 2020. Sebastian Hjelm
 */

package taskmanager.filter.concrete;

import taskmanager.Process;
import taskmanager.filter.TextContainsFilter;

public class ProcessNameFilter extends TextContainsFilter {
    public ProcessNameFilter(String name) {
        super(name);
    }

    @Override
    protected String textToFilter(Process process) {
        return process.fileName;
    }
}
