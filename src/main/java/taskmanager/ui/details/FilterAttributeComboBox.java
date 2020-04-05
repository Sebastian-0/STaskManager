/*
 * Copyright (c) 2020 Sebastian Hjelm
 */

package taskmanager.ui.details;

import config.Config;
import taskmanager.ui.details.ProcessTable.Columns;
import taskmanager.ui.details.filter.FilterCompiler.Tag;
import taskmanager.ui.details.filter.FilterPanel;

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

		String lastFilterAttribute = Config.get(Config.KEY_LAST_DEFAULT_FILTER_ATTRIBUTE);
		for (int i = 0; i < visibleColumns.size(); i++) {
			if (visibleColumns.get(i).name.equals(lastFilterAttribute)) {
				setSelectedIndex(i);
				break;
			}
		}
	}
}
