/*
 * Copyright (c) 2020. Sebastian Hjelm
 */

package taskmanager.filter;

import taskmanager.Process;

public interface Filter {
    Filter UNIVERSE = p -> true;

    boolean apply(Process process);
}
