/*
 * Copyright (c) 2020. Sebastian Hjelm
 */

package taskmanager.filter.concrete;

import taskmanager.Process;
import taskmanager.filter.TextContainsFilter;

public class DescriptionFilter extends TextContainsFilter {
    public DescriptionFilter(String description) {
        super(description);
    }

    @Override
    protected String textToFilter(Process process) {
        return process.description;
    }
}
