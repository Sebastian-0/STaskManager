/*
 * Copyright (c) 2019 Sebastian Hjelm
 */

package taskmanager.ui.details;

import taskmanager.ui.details.ProcessTable.Columns;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.List;

public class SearchAttributeComboBox extends JComboBox<String> {
	private ProcessTable[] processTables;

	public SearchAttributeComboBox(ProcessTable... processTables) {
		super(processTables[0].getVisibleColumns().stream().map(c -> c.name).toArray(String[]::new));
		this.processTables = processTables;
		setSelectedIndex(Columns.FileName.ordinal());
		addActionListener(actionListener);
	}

	private ActionListener actionListener = e -> {
		List<Columns> visibleColumns = processTables[0].getVisibleColumns();
		Arrays.stream(processTables).forEach(table -> table.setFilterAttribute(visibleColumns.get(getSelectedIndex())));
	};
}
