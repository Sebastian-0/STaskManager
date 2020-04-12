/*
 * Copyright (c) 2020. Sebastian Hjelm
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
