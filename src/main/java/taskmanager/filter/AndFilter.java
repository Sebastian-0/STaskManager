/*
 * Copyright (c) 2020. Sebastian Hjelm
 */

package taskmanager.filter;

import taskmanager.Process;

public class AndFilter implements Filter {
    private final Filter[] filters;

    public AndFilter(Filter... filters) {
        this.filters = filters;
    }

    @Override
    public boolean apply(Process process) {
        boolean result = true;
        for (Filter filter : filters) {
            result = result && filter.apply(process);
        }
        return result;
    }
}
