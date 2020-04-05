/*
 * Copyright (c) 2020. Sebastian Hjelm
 */

package taskmanager.filter.concrete;

import taskmanager.Process;
import taskmanager.filter.NumericalRangeFilter;

public class CpuFilter extends NumericalRangeFilter {
    public CpuFilter(long lowerBound, long upperBound) {
        super(lowerBound, upperBound);
    }

    @Override
    protected long valueToFilter(Process process) {
        return process.cpuUsage.newest();
    }
}
