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

import taskmanager.ui.SimpleGridBagLayout;
import taskmanager.ui.details.ProcessTable;
import taskmanager.filter.FilterCompiler.Tag;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.GridBagConstraints;

public class FilterPanel extends JPanel {

	private final FilterTextField filterTextField;

	public FilterPanel(ProcessTable liveTable, ProcessTable deadTable) {
		filterTextField = new FilterTextField(liveTable, deadTable);
		JButton clearButton = new ClearFilterButton(filterTextField);

		setBorder(filterTextField.getBorder());
		filterTextField.setBorder(null);
		setBackground(Color.WHITE);


		SimpleGridBagLayout layout = new SimpleGridBagLayout(this);
		layout.setInsets(0, 0, 0, 0);
		layout.addToGrid(filterTextField, 0, 0, 1, 1, GridBagConstraints.BOTH, 1, 1);
		layout.setInsets(0, 0, 0, 5);
		layout.addToGrid(clearButton, 1, 0, 1, 1, GridBagConstraints.VERTICAL, 0, 0);
	}

	public void setDefaultTag(Tag tag) {
		filterTextField.setDefaultTag(tag);
	}
}