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

package taskmanager.ui.details.filter;

import config.Config;
import taskmanager.ui.details.ProcessTable.Columns;
import taskmanager.filter.FilterCompiler.Tag;

import javax.swing.JComboBox;
import java.util.List;

public class FilterAttributeComboBox extends JComboBox<String> {
	public FilterAttributeComboBox(List<Columns> visibleColumns, FilterPanel filterPanel) {
		super(visibleColumns.stream().map(c -> c.name).toArray(String[]::new));

		addActionListener(e -> {
			Config.put(Config.KEY_LAST_DEFAULT_FILTER_ATTRIBUTE, getSelectedItem().toString());
			for (Tag tag : Tag.values()) {
				if (tag.displayName.equals(getSelectedItem())) {
					filterPanel.setDefaultTag(tag);
					break;
				}
			}
		});

		boolean wasSet = false;
		String lastFilterAttribute = Config.get(Config.KEY_LAST_DEFAULT_FILTER_ATTRIBUTE);
		for (int i = 0; i < visibleColumns.size(); i++) {
			if (visibleColumns.get(i).name.equals(lastFilterAttribute)) {
				wasSet = true;
				setSelectedIndex(i);
				break;
			}
		}
		if (!wasSet) {
			setSelectedIndex(0);
		}
	}
}