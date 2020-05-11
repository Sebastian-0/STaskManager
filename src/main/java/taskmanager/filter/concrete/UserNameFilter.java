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

import taskmanager.data.Process;
import taskmanager.filter.TextContainsFilter;

public class UserNameFilter extends TextContainsFilter {
	public UserNameFilter(String name) {
		super(name);
	}

	@Override
	protected String textToFilter(Process process) {
		return process.userName;
	}
}