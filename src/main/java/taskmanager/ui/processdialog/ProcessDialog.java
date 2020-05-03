package taskmanager.ui.processdialog;

import taskmanager.Process;
import taskmanager.ui.SimpleGridBagLayout;

import javax.swing.JDialog;
import javax.swing.JFrame;
import java.awt.GridBagConstraints;

public class ProcessDialog extends JDialog {
	private Process process;
	private PerformancePanel performancePanel;
	private final InformationPanel informationPanel;

	public ProcessDialog(JFrame parent, Process process) {
		super(parent);
		this.process = process;

		setTitle("Process: " + process.fileName + " (" + process.id + ")");

		setModalityType(ModalityType.MODELESS);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		performancePanel = new PerformancePanel(process);
		informationPanel = new InformationPanel(process);
		CommandLinePanel commandLinePanel = new CommandLinePanel(process);

		if (process.isDead) {
			processEnded();
		}

		SimpleGridBagLayout gbl = new SimpleGridBagLayout(this);
		gbl.addToGrid(performancePanel, 0, 0, 1, 1, GridBagConstraints.BOTH, 1, 1);
		gbl.addToGrid(informationPanel, 0, 1, 1, 1, GridBagConstraints.HORIZONTAL, 1, 0);
		gbl.addToGrid(commandLinePanel, 0, 2, 1, 1, GridBagConstraints.HORIZONTAL, 1, 0);
		pack();

		setMinimumSize(getSize());

		setLocationRelativeTo(parent);
	}

	public void update() {
		performancePanel.update();
	}

	public void processEnded() {
		setTitle(getTitle() + " - DEAD");
		informationPanel.processDied();
	}
}
