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

public class StringTokenizer {
    private static final String DELIMITERS = " \t\n\r";

    private String text;
    private int idx;

    private int tokenStart = -1;
    private int tokenEnd = -1;
    private int lastTokenIdx = -1;

    public StringTokenizer(String text) {
        this.text = text;
    }

    public int lastTokenIdx() {
        return lastTokenIdx;
    }

    public boolean hasNext() {
        if (tokenStart == -1) {
            find();
        }
        return tokenStart < text.length();
    }

    public String next() {
        if (tokenStart == -1) {
            find();
        }
        if (tokenStart == text.length()) {
            throw new IllegalStateException("No more tokens!");
        }
        String token = text.substring(tokenStart, tokenEnd);
        lastTokenIdx = tokenStart;
        tokenStart = -1;
        tokenEnd = -1;
        return token;
    }

    private void find() {
        tokenStart = findTokenStart();
        tokenEnd = findTokenEnd();
    }

    private int findTokenStart() {
        while (idx < text.length()) {
            if (DELIMITERS.indexOf(text.charAt(idx)) == -1) {
                break;
            }
            idx += 1;
        }
        return idx;
    }

    private int findTokenEnd() {
        while (idx < text.length()) {
            if (DELIMITERS.indexOf(text.charAt(idx)) != -1) {
                break;
            }
            idx += 1;
        }
        return idx;
    }


}