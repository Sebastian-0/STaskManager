/*
 * Copyright (c) 2020. Sebastian Hjelm
 */

package taskmanager.filter;

import taskmanager.Process;

public abstract class TextContainsFilter implements Filter {
    private final String textToContain;

    public TextContainsFilter(String textToContain) {
        this.textToContain = textToContain;
    }

    @Override
    public boolean apply(Process process) {
        return textToFilter(process).contains(textToContain);
    }

    protected abstract String textToFilter(Process process);
}
