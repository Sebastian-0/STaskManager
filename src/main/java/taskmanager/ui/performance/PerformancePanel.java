package taskmanager.ui.performance;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import config.Config;
import taskmanager.SystemInformation;
import taskmanager.ui.performance.cpu.CpuPanel;
import taskmanager.ui.performance.disks.DiskPanel;
import taskmanager.ui.performance.memory.MemoryPanel;
import taskmanager.ui.performance.network.NetworkPanel;

public class PerformancePanel extends JSplitPane implements PerformanceButtonListener {
	private GraphSelectionPanel graphSelectionPanel;

	private JScrollPane selectedPanelContainer;

	private MemoryPanel memoryPanel;
	private CpuPanel cpuPanel;
	private DiskPanel[] diskPanels;
	private NetworkPanel[] networkPanels;

	public PerformancePanel(SystemInformation systemInformation) {
		super(JSplitPane.HORIZONTAL_SPLIT);

		selectedPanelContainer = new JScrollPane();
		selectedPanelContainer.setBorder(null);

		TimelineGroup timelineGroup = new TimelineGroup();
		timelineGroup.setLinked(Boolean.parseBoolean(Config.get(Config.KEY_LINK_TIMELINES)));
		memoryPanel = new MemoryPanel(timelineGroup, systemInformation);
		cpuPanel = new CpuPanel(timelineGroup, systemInformation);
		diskPanels = new DiskPanel[systemInformation.disks.length];
		networkPanels = new NetworkPanel[systemInformation.networks.length];

		for (int i = 0; i < diskPanels.length; i++) {
			diskPanels[i] = new DiskPanel(timelineGroup, systemInformation.disks[i]);
		}
		for (int i = 0; i < networkPanels.length; i++) {
			networkPanels[i] = new NetworkPanel(timelineGroup, systemInformation.networks[i]);
		}

		List<GraphTypeButton> buttons = new ArrayList<>();
		buttons.add(cpuPanel.createCpuButton());
		buttons.add(memoryPanel.createMemoryButton());
		for (int i = 0; i < diskPanels.length; i++) {
			GraphTypeButton button = diskPanels[i].createNetworkButton(i);
			buttons.add(button); // TODO make this more versatile so that you can continuously add/remove disks
		}
		for (int i = 0; i < networkPanels.length; i++) {
			GraphTypeButton button = networkPanels[i].createNetworkButton(i);
			if (systemInformation.networks[i].isEnabled) // TODO make this more versatile so that you can continuously add/remove interfaces
				buttons.add(button);
		}

		graphSelectionPanel = new GraphSelectionPanel(this, buttons.toArray(new GraphTypeButton[0]));
		JScrollPane selectionPanelContainer = new JScrollPane(graphSelectionPanel);

		add(selectionPanelContainer);
		add(selectedPanelContainer);

		selectedPanelContainer.setViewportView(cpuPanel);
	}

	public void update(SystemInformation systemInformation) {
		memoryPanel.update(systemInformation);
		cpuPanel.update(systemInformation);

		for (DiskPanel diskPanel : diskPanels) {
			diskPanel.update(systemInformation);
		}
		for (NetworkPanel networkPanel : networkPanels) {
			networkPanel.update(systemInformation);
		}
	}

	@Override
	public void swapTo(GraphType type, int index) {
		graphSelectionPanel.deselectAll();
		if (type == GraphType.Cpu)
			selectedPanelContainer.setViewportView(cpuPanel);
		else if (type == GraphType.Memory)
			selectedPanelContainer.setViewportView(memoryPanel);
		else if (type == GraphType.Disk)
			selectedPanelContainer.setViewportView(diskPanels[index]);
		else if (type == GraphType.Network)
			selectedPanelContainer.setViewportView(networkPanels[index]);

		revalidate();
		repaint();
	}
}
