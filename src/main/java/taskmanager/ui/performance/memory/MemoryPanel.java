package taskmanager.ui.performance.memory;

import taskmanager.Measurements;
import taskmanager.SystemInformation;
import taskmanager.ui.SimpleGridBagLayout;
import taskmanager.ui.TextUtils;
import taskmanager.ui.TextUtils.ValueType;
import taskmanager.ui.performance.GraphPanel;
import taskmanager.ui.performance.GraphType;
import taskmanager.ui.performance.GraphTypeButton;
import taskmanager.ui.performance.InformationItemPanel;
import taskmanager.ui.performance.TimelineGraphPanel;
import taskmanager.ui.performance.TimelineGroup;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Font;
import java.awt.GridBagConstraints;

public class MemoryPanel extends JPanel {
	private Measurements<Long> memoryAvailable;

	private JLabel labelMaxMemory;

	private GraphPanel memoryGraph;
	private TimelineGraphPanel timelineGraph;
	private MemoryCompositionPanel memoryComposition;

	private InformationItemPanel inUsePanel;
	private InformationItemPanel availablePanel;
	private InformationItemPanel committedPanel;
	private InformationItemPanel cachedPanel;
	private InformationItemPanel pagedPoolPanel;
	private InformationItemPanel nonpagedPoolPanel;

	private GraphTypeButton connectedButton;


	public MemoryPanel(TimelineGroup timelineGroup, SystemInformation systemInformation) {
		memoryAvailable = systemInformation.physicalMemoryUsed;

		JLabel labelHeader = new JLabel("Memory");
		labelHeader.setFont(labelHeader.getFont().deriveFont(24f));

		JLabel labelMemoryUsage = new JLabel("Memory usage");
		JLabel labelZero = new JLabel("0");
		JLabel labelMaxTime = new JLabel("Displaying 60 seconds");
		labelMaxMemory = new JLabel("XX GB");
		JLabel labelComposition = new JLabel("Memory composition");

		memoryGraph = new GraphPanel(GraphType.Memory, true);
		timelineGraph = new TimelineGraphPanel(memoryGraph, labelMaxTime);
		memoryComposition = new MemoryCompositionPanel();

		memoryGraph.addGraph(memoryAvailable, systemInformation.physicalMemoryTopList);
		timelineGraph.addGraph(memoryAvailable);
		timelineGroup.add(timelineGraph);


		JPanel realTimePanel = new JPanel();
		inUsePanel = new InformationItemPanel("In use", ValueType.Bytes);
		availablePanel = new InformationItemPanel("Available", ValueType.Bytes);
		committedPanel = new CommitItemPanel("Committed", systemInformation.commitLimit);
		cachedPanel = new InformationItemPanel("Cached", ValueType.Bytes);
		pagedPoolPanel = new InformationItemPanel("Paged pool", ValueType.Bytes);
		nonpagedPoolPanel = new InformationItemPanel("Non-paged pool", ValueType.Bytes);

		Font dataFont = inUsePanel.getFont().deriveFont(Font.BOLD, inUsePanel.getFont().getSize() + 3f);
		inUsePanel.setFont(dataFont);
		availablePanel.setFont(dataFont);
		committedPanel.setFont(dataFont);
		cachedPanel.setFont(dataFont);
		pagedPoolPanel.setFont(dataFont);
		nonpagedPoolPanel.setFont(dataFont);

		SimpleGridBagLayout realTimeLayout = new SimpleGridBagLayout(realTimePanel);
		realTimeLayout.addToGrid(inUsePanel, 0, 0, 1, 1, GridBagConstraints.HORIZONTAL, 1, 0);
		realTimeLayout.addToGrid(availablePanel, 1, 0, 1, 1, GridBagConstraints.HORIZONTAL, 1, 0);
		realTimeLayout.addToGrid(committedPanel, 0, 1, 1, 1, GridBagConstraints.HORIZONTAL, 1, 0);
		realTimeLayout.addToGrid(cachedPanel, 1, 1, 1, 1, GridBagConstraints.HORIZONTAL, 1, 0);
		realTimeLayout.addToGrid(pagedPoolPanel, 0, 2, 1, 1, GridBagConstraints.HORIZONTAL, 1, 0);
		realTimeLayout.addToGrid(nonpagedPoolPanel, 1, 2, 1, 1, GridBagConstraints.HORIZONTAL, 1, 0);

		SimpleGridBagLayout layout = new SimpleGridBagLayout(this);
		layout.addToGrid(labelHeader, 0, 0, 1, 1, GridBagConstraints.WEST);
		layout.setInsets(0, 5, 0, 5);
		layout.addToGrid(labelMemoryUsage, 0, 1, 1, 1, GridBagConstraints.WEST);
		layout.addToGrid(labelMaxMemory, 1, 1, 1, 1, GridBagConstraints.EAST);
		layout.setInsets(2, 5, 2, 5);
		layout.addToGrid(memoryGraph, 0, 2, 2, 1, GridBagConstraints.BOTH, 1, 1);
		layout.setInsets(0, 5, 0, 5);
		layout.addToGrid(labelMaxTime, 0, 3, 1, 1, GridBagConstraints.WEST);
		layout.addToGrid(labelZero, 1, 3, 1, 1, GridBagConstraints.EAST);
		layout.setInsets(5, 5, 5, 5);
		layout.addToGrid(timelineGraph, 0, 4, 2, 1, GridBagConstraints.HORIZONTAL, 1, 0);
		layout.setInsets(0, 5, 0, 5);
		layout.addToGrid(labelComposition, 0, 5, 1, 1, GridBagConstraints.WEST);
		layout.setInsets(5, 5, 5, 5);
		layout.addToGrid(memoryComposition, 0, 6, 2, 1, GridBagConstraints.HORIZONTAL, 1, 0);
		layout.addToGrid(realTimePanel, 0, 7, 2, 1, GridBagConstraints.WEST);
	}


	public void update(SystemInformation systemInformation) {
		labelMaxMemory.setText(TextUtils.valueToString(systemInformation.physicalMemoryTotal, ValueType.Bytes));

		long memoryUsed = systemInformation.physicalMemoryUsed.newest();

		memoryGraph.setMaxDatapointValue(systemInformation.physicalMemoryTotal);
		timelineGraph.setMaxDatapointValue(systemInformation.physicalMemoryTotal);
		connectedButton.setMaxDatapointValue(systemInformation.physicalMemoryTotal);
		memoryGraph.newDatapoint();
		timelineGraph.newDatapoint();
		connectedButton.newDatapoint(memoryUsed);

		memoryComposition.update(systemInformation);

		// Labels
		inUsePanel.updateValue(memoryUsed);
		availablePanel.updateValue(systemInformation.physicalMemoryTotal - memoryUsed);
		committedPanel.updateValue(systemInformation.commitUsed);
		cachedPanel.updateValue(systemInformation.standbyMemory + systemInformation.modifiedMemory);
		pagedPoolPanel.updateValue(systemInformation.kernelPaged);
		nonpagedPoolPanel.updateValue(systemInformation.kernelNonPaged);
	}


	public GraphTypeButton createMemoryButton() {
		connectedButton = new GraphTypeButton(GraphType.Memory, "Memory");
		connectedButton.setIsLogarithmic(memoryGraph.isLogarithmic());
		connectedButton.addGraph(memoryAvailable);
		return connectedButton;
	}
}
