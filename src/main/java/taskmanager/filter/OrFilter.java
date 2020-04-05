/*
 * Copyright (c) 2020. Sebastian Hjelm
 */

package taskmanager.filter;

import taskmanager.Process;

public class OrFilter implements Filter {
    private final Filter[] filters;

    public OrFilter(Filter... filters) {
        this.filters = filters;
    }

    @Override
    public boolean apply(Process process) {
        boolean result = false;
        for (Filter filter : filters) {
            result = result || filter.apply(process);
        }
        return result;
    }
}
