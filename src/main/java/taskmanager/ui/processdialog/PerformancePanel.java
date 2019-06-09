package taskmanager.ui.processdialog;

import config.Config;
import taskmanager.Process;
import taskmanager.ui.SimpleGridBagLayout;
import taskmanager.ui.TextUtils;
import taskmanager.ui.TextUtils.ValueType;
import taskmanager.ui.performance.GraphPanel;
import taskmanager.ui.performance.GraphType;
import taskmanager.ui.performance.TimelineGraphPanel;
import taskmanager.ui.performance.TimelineGroup;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class PerformancePanel extends JPanel {
	private Process process;

	private JLabel labelCpuCurrent;
	private JLabel labelMemoryCurrent;

	private GraphPanel cpuGraph;
	private GraphPanel memoryGraph;

	private TimelineGraphPanel cpuTimeline;
	private TimelineGraphPanel memoryTimeline;

	private JPanel containerPanel;
	private JPanel currentTimeline;

	public PerformancePanel(Process process) {
		this.process = process;

		JLabel labelCpu = new JLabel("CPU utilization");
		JLabel labelMemory = new JLabel("Memory utilization");

		labelCpuCurrent = new JLabel("100%");
		labelMemoryCurrent = new JLabel("10 MB");

		cpuGraph = new GraphPanel(GraphType.Cpu, true);
		memoryGraph = new GraphPanel(GraphType.Memory, true);

		cpuGraph.addGraph(process.cpuUsage);
		cpuGraph.setPreferredSize(new Dimension(cpuGraph.getPreferredSize().width * 2, cpuGraph.getPreferredSize().height * 2));
		cpuGraph.addMouseListener(mouseListener);
		memoryGraph.addGraph(process.privateWorkingSet);
		memoryGraph.setPreferredSize(new Dimension(memoryGraph.getPreferredSize().width * 2, memoryGraph.getPreferredSize().height * 2));
		memoryGraph.addMouseListener(mouseListener);

		JLabel labelZero = new JLabel(""); // TODO remove?
		JLabel labelMaxTime = new JLabel("Displaying 60 seconds");

		TimelineGroup group = new TimelineGroup();
		cpuTimeline = new TimelineGraphPanel(cpuGraph, labelMaxTime);
		memoryTimeline = new TimelineGraphPanel(memoryGraph, labelMaxTime);

		cpuTimeline.addGraph(process.cpuUsage);
		cpuTimeline.connectGraphs(memoryGraph);
		memoryTimeline.addGraph(process.privateWorkingSet);
		memoryTimeline.connectGraphs(cpuGraph);

		group.add(cpuTimeline);
		group.add(memoryTimeline);
		group.setLinked(true);

		cpuGraph.setSelected(true);
		cpuGraph.setMaxDatapointValue(Config.DOUBLE_TO_LONG);
		cpuTimeline.setMaxDatapointValue(Config.DOUBLE_TO_LONG);

		containerPanel = new JPanel();
		containerPanel.setLayout(new GridLayout(1, 1));
		containerPanel.add(cpuTimeline);
		currentTimeline = cpuTimeline;

		SimpleGridBagLayout layout = new SimpleGridBagLayout(this);
		layout.setInsets(0, 5, 0, 5);
		layout.addToGrid(labelCpu, 0, 0, 1, 1, GridBagConstraints.WEST);
		layout.addToGrid(labelCpuCurrent, 1, 0, 1, 1, GridBagConstraints.EAST);
		layout.addToGrid(labelMemory, 2, 0, 1, 1, GridBagConstraints.WEST);
		layout.addToGrid(labelMemoryCurrent, 3, 0, 1, 1, GridBagConstraints.EAST);
		layout.setInsets(2, 5, 2, 5);
		layout.addToGrid(cpuGraph, 0, 1, 2, 1, GridBagConstraints.BOTH, 1, 1);
		layout.addToGrid(memoryGraph, 2, 1, 2, 1, GridBagConstraints.BOTH, 1, 1);
		layout.setInsets(0, 5, 0, 5);
		layout.addToGrid(labelMaxTime, 0, 2, 2, 1, GridBagConstraints.WEST);
		layout.addToGrid(labelZero, 3, 2, 1, 1, GridBagConstraints.EAST);
		layout.setInsets(5, 5, 5, 5);
		layout.addToGrid(containerPanel, 0, 3, 4, 1, GridBagConstraints.HORIZONTAL, 1, 0);

		update(); // Read initial values
	}

	public void update() {
		long memoryMax = (long) (Math.max(128, process.privateWorkingSet.max()) * 1.1f);
		memoryGraph.setMaxDatapointValue(memoryMax); // TODO Make this more intelligent
		memoryTimeline.setMaxDatapointValue(memoryMax);

		labelMemoryCurrent.setText(TextUtils.valueToString(process.privateWorkingSet.newest(), ValueType.Bytes));
		labelCpuCurrent.setText(String.format("%.1f%%", 100 * process.cpuUsage.newest() / (double) Config.DOUBLE_TO_LONG));

		cpuGraph.newDatapoint();
		memoryGraph.newDatapoint();

		cpuTimeline.newDatapoint();
		memoryTimeline.newDatapoint();
	}

	private MouseListener mouseListener = new MouseAdapter() {
		@Override
		public void mousePressed(MouseEvent e) {
			if (e.getButton() == MouseEvent.BUTTON1) {
				JPanel newTimeline = null;
				if (e.getComponent() == cpuGraph) {
					newTimeline = cpuTimeline;
					memoryGraph.setSelected(false);
					cpuGraph.setSelected(true);
				} else if (e.getComponent() == memoryGraph) {
					newTimeline = memoryTimeline;
					memoryGraph.setSelected(true);
					cpuGraph.setSelected(false);
				}

				if (newTimeline != currentTimeline) {
					containerPanel.removeAll();
					containerPanel.add(newTimeline);
					containerPanel.revalidate();
					containerPanel.repaint();
					currentTimeline = newTimeline;
				}
			}
		}
	};
}
