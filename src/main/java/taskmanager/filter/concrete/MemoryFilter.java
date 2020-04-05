/*
 * Copyright (c) 2020. Sebastian Hjelm
 */

package taskmanager.filter.concrete;

import taskmanager.Process;
import taskmanager.filter.NumericalRangeFilter;

public class MemoryFilter extends NumericalRangeFilter {
    public MemoryFilter(long lowerBound, long upperBound) {
        super(lowerBound, upperBound);
    }

    @Override
    protected long valueToFilter(Process process) {
        return process.privateWorkingSet.newest();
    }
}
