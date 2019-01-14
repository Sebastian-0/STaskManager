package taskmanager.ui.processdialog;

import java.awt.BorderLayout;

import javax.swing.JDialog;
import javax.swing.JFrame;

import taskmanager.Process;

public class ProcessDialog extends JDialog {
	private Process process;
	private PerformancePanel performancePanel;

	public ProcessDialog(JFrame parent, Process process) {
		super(parent);
		this.process = process;

		setTitle("Process: " + process.fileName + " (" + process.id + ")");
		if (process.isDead) {
			processEnded();
		}

		setModalityType(ModalityType.MODELESS);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		performancePanel = new PerformancePanel(process);

		setLayout(new BorderLayout());
		add(performancePanel, BorderLayout.CENTER);
		pack();

		setLocationRelativeTo(parent);
	}

	public void update() {
		performancePanel.update();
	}

	public void processEnded() {
		setTitle(getTitle() + " - DEAD");
	}
}
