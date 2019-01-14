/*
 * Copyright (c) 2019 Sebastian Hjelm
 */

package taskmanager.ui.details;

import taskmanager.ui.details.ProcessTable.Columns;

import javax.swing.JComboBox;
import java.awt.event.ActionListener;
import java.util.Arrays;

public class SearchAttributeComboBox extends JComboBox<String> {
	private ProcessTable[] processTables;

	public SearchAttributeComboBox(ProcessTable... processTables) {
		super(Arrays.stream(Columns.values()).map(c -> c.name).toArray(String[]::new));
		this.processTables = processTables;
		setSelectedIndex(Columns.FileName.ordinal());
		addActionListener(actionListener);
	}

	private ActionListener actionListener = e -> {
		Arrays.stream(processTables).forEach(table -> table.setFilterAttribute(Columns.values()[getSelectedIndex()]));
	};
}
