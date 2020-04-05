package taskmanager.filter.concrete;

import taskmanager.Process;
import taskmanager.filter.TextEqualsFilter;

public class PidFilter extends TextEqualsFilter {
    public PidFilter(String pid) {
        super(pid);
    }

    @Override
    protected String textToFilter(Process process) {
        return Long.toString(process.id);
    }
}
