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