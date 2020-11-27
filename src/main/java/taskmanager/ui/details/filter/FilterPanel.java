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

import net.miginfocom.swing.MigLayout;
import taskmanager.filter.FilterCompiler.Tag;
import taskmanager.ui.details.ProcessTable;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.Color;

public class FilterPanel extends JPanel {
	private final FilterTextField filterTextField;

	public FilterPanel(ProcessTable liveTable, ProcessTable deadTable) {
		filterTextField = new FilterTextField(liveTable, deadTable);
		JButton clearButton = new ClearFilterButton(filterTextField);

		setBorder(filterTextField.getBorder());
		filterTextField.setBorder(null);
		setBackground(Color.WHITE);

		setLayout(new MigLayout("ins 3 5 3 5"));
		add(filterTextField, "growx, pushx");
		add(clearButton);
	}

	public void setDefaultTag(Tag tag) {
		filterTextField.setDefaultTag(tag);
	}

	public void clearFilter() {
		filterTextField.clear();
	}
}