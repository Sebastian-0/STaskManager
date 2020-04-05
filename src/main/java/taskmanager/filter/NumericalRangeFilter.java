/*
 * Copyright (c) 2020. Sebastian Hjelm
 */

package taskmanager.filter;

import taskmanager.Process;

public abstract class NumericalRangeFilter implements Filter {
    private final long lowerBound;
    private final long upperBound;

    public NumericalRangeFilter(long lowerBound, long upperBound) {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    @Override
    public boolean apply(Process process) {
        long value = valueToFilter(process);
        return value >= lowerBound && value <= upperBound;
    }

    protected abstract long valueToFilter(Process process);
}
