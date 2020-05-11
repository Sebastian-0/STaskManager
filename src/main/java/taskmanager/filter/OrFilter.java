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

import taskmanager.data.Process;

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