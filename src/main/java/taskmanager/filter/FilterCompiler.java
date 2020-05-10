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

import config.Config;
import taskmanager.filter.concrete.CommandLineFilter;
import taskmanager.filter.concrete.CpuFilter;
import taskmanager.filter.concrete.DeathTimeFilter;
import taskmanager.filter.concrete.DescriptionFilter;
import taskmanager.filter.concrete.MemoryFilter;
import taskmanager.filter.concrete.PidFilter;
import taskmanager.filter.concrete.ProcessNameFilter;
import taskmanager.filter.concrete.UserNameFilter;
import taskmanager.ui.details.ProcessTable.Columns;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class FilterCompiler { // TODO Get rid of dependency on ProcessTable.Columns to make filter package independent
    private static final Color ERROR_COLOR = new Color(255, 113, 113);

    public enum Tag {
        Pid("pid", Columns.Pid.name, new Color(230, 230, 230)),
        ProcessName("name", Columns.FileName.name, new Color(230, 230, 230)),
        UserName("user", Columns.UserName.name, new Color(230, 230, 230)),
        Cpu("cpu", Columns.Cpu.name, new Color(188, 231, 255)),
        Memory("mem", Columns.PrivateWorkingSet.name, new Color(244, 205, 255)),
        CommandLine("cmd", Columns.CommandLine.name, new Color(230, 230, 230)),
        Description("desc", Columns.Description.name, new Color(230, 230, 230)),
        DeathTime("death", Columns.DeathTime.name, new Color(230, 230, 230));

        public final String text;
        public final String displayName;
        public final Color color;

        Tag(String text, String displayName, Color color) {
            this.text = text + ":";
            this.displayName = displayName;
            this.color = color;
        }
    }

    public CompiledFilter compile(String text, Tag defaultTag) {
        CompiledFilter result = new CompiledFilter();

        Tag currentTag = defaultTag;
        int currentTextStart = 0;
        List<String> currentText = new ArrayList<>();

        List<Filter> filters = new ArrayList<>();
        StringTokenizer tokenizer = new StringTokenizer(text);
        while (tokenizer.hasNext()) {
            String token = tokenizer.next();
            Tag tag = tokenToTag(token);
            if (tag != null) {
                if (!currentText.isEmpty()) {
                    tryFinishTag(currentTag, currentText, currentTextStart, tokenizer.lastTokenIdx() - 1, result.highlight, filters);
                }
                currentTag = tag;
                currentTextStart = tokenizer.lastTokenIdx() + token.length();
                currentText.clear();
                result.highlight.add(new Highlight(
                        tokenizer.lastTokenIdx(),
                        currentTextStart,
                        tag.color));
            } else {
                currentText.add(token);
            }
        }
        if (!currentText.isEmpty()) {
            tryFinishTag(currentTag, currentText, currentTextStart, text.length(), result.highlight, filters);
        }
        if (filters.isEmpty()) {
            result.filter = Filter.UNIVERSE;
        } else {
            result.filter = new AndFilter(filters.toArray(new Filter[0]));
        }
        return result;
    }

    private void tryFinishTag(Tag currentTag, List<String> currentText, int currentTextStart, int currentTextEnd,
                              List<Highlight> highlights, List<Filter> filters) {
        if (currentTextStart < currentTextEnd) {
            try {
                filters.add(finishTag(currentTag, String.join(" ", currentText)));
            } catch (IllegalArgumentException e) {
                highlights.add(new Highlight(currentTextStart, currentTextEnd, ERROR_COLOR));
            }
        }
    }

    private Tag tokenToTag(String token) {
        for (Tag tag : Tag.values()) {
            if (tag.text.equals(token)) {
                return tag;
            }
        }
        return null;
    }

    private Filter finishTag(Tag tag, String text) {
        switch (tag) {
            case Pid:
                return new PidFilter(text);
            case ProcessName:
                return new ProcessNameFilter(text);
            case UserName:
                return new UserNameFilter(text);
            case Cpu:
                return parseCpu(text);
            case Memory:
                return parseMemory(text);
            case CommandLine:
                return new CommandLineFilter(text);
            case Description:
                return new DescriptionFilter(text);
            case DeathTime:
                return parseDeathTime(text);
            default:
                throw new UnsupportedOperationException("Unsupported tag (programmer error): " + tag);
        }
    }

    private Filter parseCpu(String text) {
        text = text.replaceAll("%", "");
        try {
            double lower = Double.parseDouble(lowerBound(text, "0"));
            double upper = Double.parseDouble(upperBound(text, "100000"));
            return new CpuFilter((long) (lower / 100 * Config.DOUBLE_TO_LONG),
                    (long) (upper / 100 * Config.DOUBLE_TO_LONG));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid CPU filter text: " + text, e);
        }
    }

    private Filter parseMemory(String text) {
        text = text.toLowerCase();
        try {
            long lower = parseMemoryNumber(lowerBound(text, "0"));
            long upper = parseMemoryNumber(upperBound(text, Long.toString(Long.MAX_VALUE / 1024)));
            return new MemoryFilter(lower, upper);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid memory filter text: " + text, e);
        }
    }

    private long parseMemoryNumber(String number) {
        if (number.isEmpty()) {
            throw new NumberFormatException("The empty string is not a number!");
        }
        long factor = 1;
        char modifier = number.charAt(number.length()-1);
        if (modifier == 'k') {
            number = number.substring(0, number.length()-1);
        } else if (modifier == 'm') {
            number = number.substring(0, number.length()-1);
            factor = 1024L;
        } else if (modifier == 'g') {
            number = number.substring(0, number.length()-1);
            factor = 1024L * 1024;
        } else if (modifier == 't') {
            number = number.substring(0, number.length()-1);
            factor = 1024L * 1024 * 1024;
        } else if (modifier == 'p') {
            number = number.substring(0, number.length()-1);
            factor = 1024L * 1024 * 1024 * 1024;
        }

        return Long.parseLong(number) * factor * 1024;
    }

    private Filter parseDeathTime(String text) {
        text = text.toLowerCase();
        try {
            long lower = parseTime(lowerBound(text, "0"));
            long upper = parseTime(upperBound(text, Long.toString(Long.MAX_VALUE / 1000)));
            return new DeathTimeFilter(lower, upper);
        } catch (NumberFormatException e) {throw new IllegalArgumentException("Invalid death time filter text: " + text, e);
        }
    }

    private long parseTime(String time) {
        if (time.isEmpty()) {
            throw new NumberFormatException("The empty string is not a timestamp!");
        }
        long seconds;
        char modifier = time.charAt(time.length()-1);
        if (Character.isDigit(modifier)) {
            seconds = (long) (Double.parseDouble(time) * 1000);
        } else {
            seconds = (long) (Double.parseDouble(time.substring(0, time.length() - 1)) * 1000);
        }
        if (modifier == 'm') {
            seconds *= 60;
        } else if (modifier == 'h') {
            seconds *= 60 * 60;
        } else if (modifier == 'd') {
            seconds *= 60 * 60 * 24;
        }
        return seconds;
    }

    private String lowerBound(String text, String defaultValue) {
        text = text.replaceAll("\\s+", "");
        if (text.contains("-")) {
            return text.split("-", -1)[0];
        }
        if (text.startsWith("<")) {
            return defaultValue;
        }
        if (text.startsWith(">")) {
            return text.substring(1);
        }
        return text;
    }

    private String upperBound(String text, String defaultValue) {
        text = text.replaceAll("\\s+", "");
        if (text.contains("-")) {
            return text.split("-", -1)[1];
        }
        if (text.startsWith("<")) {
            return text.substring(1);
        }
        if (text.startsWith(">")) {
            return defaultValue;
        }
        return text;
    }


    public static class CompiledFilter {
        public Filter filter;
        public List<Highlight> highlight;

        public CompiledFilter() {
            highlight = new ArrayList<>();
        }
    }

    public static class Highlight {
        public final int start;
        public final int end;
        public final Color color;

        public Highlight(int start, int end, Color color) {
            this.start = start;
            this.end = end;
            this.color = color;
        }
    }
}