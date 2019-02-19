package taskmanager.ui.details;

import taskmanager.SystemInformation;
import taskmanager.ui.SimpleGridBagLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;

public class ProcessPanel extends JPanel {
	private ProcessTable liveTable;
	private ProcessTable deadTable;

	private JPanel container;
	private JScrollPane liveTableScrollPane;
	private JSplitPane splitPane;

	public ProcessPanel(ProcessDetailsCallback processCallback, SystemInformation systemInformation) {
		liveTable = new ProcessTable(processCallback, systemInformation, false);
		deadTable = new ProcessTable(processCallback, systemInformation, true);
		FilterTextField search = new FilterTextField(liveTable, deadTable);
		JLabel attributeLabel = new JLabel("By:");
		SearchAttributeComboBox attribute = new SearchAttributeComboBox(liveTable, deadTable);

		liveTableScrollPane = new JScrollPane(liveTable);
		splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, liveTableScrollPane, new JScrollPane(deadTable));
		splitPane.setResizeWeight(0.9);

		container = new JPanel();
		container.setLayout(new GridLayout(1, 1));

		int borderWidth = 10;
		SimpleGridBagLayout gbl = new SimpleGridBagLayout(this);
		gbl.setInsets(borderWidth, borderWidth, borderWidth / 2, borderWidth);
		gbl.addToGrid(container, 0, 0, 3, 1, GridBagConstraints.BOTH, 1, 1);
		gbl.setInsets(0, borderWidth, borderWidth / 2, borderWidth);
		gbl.addToGrid(search, 0, 1, 1, 1, GridBagConstraints.BOTH, 1, 0);
		gbl.setInsets(0, 0, borderWidth / 2, borderWidth/2);
		gbl.addToGrid(attributeLabel, 1, 1, 1, 1);
		gbl.setInsets(0, 0, borderWidth / 2, borderWidth);
		gbl.addToGrid(attribute, 2, 1, 1, 1);

		setShowDeadProcesses(true);//Boolean.parseBoolean(Config.get(Config.KEY_SHOW_DEAD_PROCESSES)));
	}

	public void setShowDeadProcesses(boolean shouldShow) {
		container.remove(splitPane);
		container.remove(liveTableScrollPane);
		if (shouldShow) {
			container.add(splitPane);
		} else {
			container.add(liveTableScrollPane);
		}
		revalidate();
		repaint();
	}

	public void update() {
		liveTable.update();
		deadTable.update();
	}
}
