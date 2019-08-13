package taskmanager.ui.details;

import taskmanager.ui.SimpleGridBagLayout;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.GridBagConstraints;

public class FilterPanel extends JPanel {
	public FilterPanel(ProcessTable liveTable, ProcessTable deadTable) {
		FilterTextField search = new FilterTextField(liveTable, deadTable);
		JButton clearButton = new ClearFilterButton(search);

		setBorder(search.getBorder());
		search.setBorder(null);
		setBackground(Color.WHITE);


		SimpleGridBagLayout layout = new SimpleGridBagLayout(this);
		layout.setInsets(0, 0, 0, 0);
		layout.addToGrid(search, 0, 0, 1, 1, GridBagConstraints.BOTH, 1, 1);
		layout.setInsets(0, 0, 0, 5);
		layout.addToGrid(clearButton, 1, 0, 1, 1, GridBagConstraints.VERTICAL, 0, 0);
	}
}
