/*
 * Copyright (c) 2020. Sebastian Hjelm
 */

package taskmanager.filter.concrete;

import taskmanager.Process;
import taskmanager.filter.NumericalRangeFilter;

public class DeathTimeFilter extends NumericalRangeFilter {
    public DeathTimeFilter(long lowerBound, long upperBound) {
        super(lowerBound, upperBound);
    }

    @Override
    public boolean apply(Process process) {
        return super.apply(process) || process.deathTimestamp == 0;
    }

    @Override
    protected long valueToFilter(Process process) {
        return System.currentTimeMillis() - process.deathTimestamp;
    }
}
