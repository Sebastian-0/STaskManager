/*
 * Copyright (c) 2020. Sebastian Hjelm
 */

package taskmanager.filter;

import taskmanager.Process;

public abstract class TextEqualsFilter implements Filter {
    private final String expectedText;

    public TextEqualsFilter(String expectedText) {
        this.expectedText = expectedText;
    }

    @Override
    public boolean apply(Process process) {
        return textToFilter(process).equals(expectedText);
    }

    protected abstract String textToFilter(Process process);
}
