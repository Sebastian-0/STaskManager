package taskmanager.ui.performance.cpu;

import config.Config;
import taskmanager.Measurements;
import taskmanager.SystemInformation;
import taskmanager.ui.SimpleGridBagLayout;
import taskmanager.ui.TextUtils.ValueType;
import taskmanager.ui.performance.GraphPanel;
import taskmanager.ui.performance.GraphType;
import taskmanager.ui.performance.GraphTypeButton;
import taskmanager.ui.performance.InformationItemPanel;
import taskmanager.ui.performance.TimelineGraphPanel;
import taskmanager.ui.performance.TimelineGroup;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;

public class CpuPanel extends JPanel {
	private Measurements<Long> cpuUsage;

	private JPanel containerPanel;

	private GraphPanel singleCpuPanel;
	private MultiCpuPanel multiCpuPanel;
	private TimelineGraphPanel timelineGraph;

	private InformationItemPanel utilizationLabel;
	private InformationItemPanel processesLabel;
	private InformationItemPanel handlesLabel;
	private InformationItemPanel threadsLabel;
	private InformationItemPanel uptimeLabel;

	private GraphTypeButton connectedButton;


	public CpuPanel(TimelineGroup timelineGroup, SystemInformation systemInformation) {
		cpuUsage = systemInformation.cpuUsageTotal;

		JLabel labelHeader = new JLabel("CPU");
		labelHeader.setFont(labelHeader.getFont().deriveFont(24f));

		JLabel labelCpuUtilization = new JLabel("% utilization");
		JLabel labelZero = new JLabel("0");
		JLabel labelMaxTime = new JLabel("Displaying 60 seconds");
		JLabel labelMaxCpu = new JLabel("100%");

		singleCpuPanel = new GraphPanel(GraphType.Cpu, true);
		timelineGraph = new TimelineGraphPanel(singleCpuPanel, labelMaxTime);
		multiCpuPanel = new MultiCpuPanel(timelineGraph, systemInformation);

		singleCpuPanel.addGraph(cpuUsage, systemInformation.cpuTopList);
		timelineGraph.addGraph(cpuUsage);
		timelineGroup.add(timelineGraph);

		containerPanel = new JPanel();
		containerPanel.setLayout(new GridLayout(1, 1));
		containerPanel.add(singleCpuPanel);

		JPanel realTimePanel = new JPanel();
		utilizationLabel = new InformationItemPanel("Utilization", ValueType.Percentage);
		processesLabel = new InformationItemPanel("Processes", ValueType.Raw);
		threadsLabel = new InformationItemPanel("Threads", ValueType.Raw);
		handlesLabel = new InformationItemPanel("Handles", ValueType.Raw);
		uptimeLabel = new InformationItemPanel("Uptime", ValueType.TimeFull);

		SimpleGridBagLayout realTimeLayout = new SimpleGridBagLayout(realTimePanel);
		realTimeLayout.addToGrid(utilizationLabel, 0, 0, 1, 1, GridBagConstraints.HORIZONTAL, 1, 0);
		realTimeLayout.addToGrid(processesLabel, 1, 0, 1, 1, GridBagConstraints.HORIZONTAL, 1, 0);
		realTimeLayout.addToGrid(threadsLabel, 0, 1, 1, 1, GridBagConstraints.HORIZONTAL, 1, 0);
		realTimeLayout.addToGrid(handlesLabel, 1, 1, 1, 1, GridBagConstraints.HORIZONTAL, 1, 0);
		realTimeLayout.addToGrid(uptimeLabel, 0, 2, 1, 1, GridBagConstraints.HORIZONTAL, 1, 0);

		SimpleGridBagLayout layout = new SimpleGridBagLayout(this);
		layout.addToGrid(labelHeader, 0, 0, 1, 1, GridBagConstraints.WEST);
		layout.setInsets(0, 5, 0, 5);
		layout.addToGrid(labelCpuUtilization, 0, 1, 1, 1, GridBagConstraints.WEST);
		layout.addToGrid(labelMaxCpu, 1, 1, 1, 1, GridBagConstraints.EAST);
		layout.setInsets(2, 5, 2, 5);
		layout.addToGrid(containerPanel, 0, 2, 2, 1, GridBagConstraints.BOTH, 1, 1);
		layout.setInsets(0, 5, 0, 5);
		layout.addToGrid(labelMaxTime, 0, 3, 1, 1, GridBagConstraints.WEST);
		layout.addToGrid(labelZero, 1, 3, 1, 1, GridBagConstraints.EAST);
		layout.setInsets(5, 5, 5, 5);
		layout.addToGrid(timelineGraph, 0, 4, 2, 1, GridBagConstraints.HORIZONTAL, 1, 0);
		layout.addToGrid(realTimePanel, 0, 5, 2, 1, GridBagConstraints.WEST);

		setComponentPopupMenu(new CpuContextMenu(this));
	}


	public void update(SystemInformation systemInformation) {
		final int total = Config.DOUBLE_TO_LONG;

		singleCpuPanel.setMaxDatapointValue(total);
		singleCpuPanel.newDatapoint();

		multiCpuPanel.update(systemInformation);

		timelineGraph.setMaxDatapointValue(total);
		connectedButton.setMaxDatapointValue(total);
		timelineGraph.newDatapoint();
		connectedButton.newDatapoint(cpuUsage.newest());

		utilizationLabel.updateValue(cpuUsage.newest());
		processesLabel.updateValue(systemInformation.totalProcesses);
		threadsLabel.updateValue(systemInformation.totalThreads);
		handlesLabel.updateValue(systemInformation.totalHandles);
		uptimeLabel.updateValue(systemInformation.uptime);
	}


	public void showTotalCpu() {
		containerPanel.removeAll();
		containerPanel.add(singleCpuPanel);
		containerPanel.revalidate();
		containerPanel.repaint();
	}

	public void showCoresCpu() {
		containerPanel.removeAll();
		containerPanel.add(multiCpuPanel);
		containerPanel.revalidate();
		containerPanel.repaint();
	}


	public GraphTypeButton createCpuButton() {
		connectedButton = new GraphTypeButton(GraphType.Cpu, "CPU");
		connectedButton.setIsLogarithmic(singleCpuPanel.isLogarithmic());
		connectedButton.addGraph(cpuUsage);
		return connectedButton;
	}
}
