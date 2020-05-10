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
