package taskmanager.ui.performance.network;

import taskmanager.SystemInformation;
import taskmanager.SystemInformation.Network;
import taskmanager.ui.SimpleGridBagLayout;
import taskmanager.ui.TextUtils;
import taskmanager.ui.TextUtils.ValueType;
import taskmanager.ui.performance.*;

import javax.swing.*;
import java.awt.*;

public class NetworkPanel extends JPanel {
	private Network network;

	private JLabel labelMaxTransfer;

	private GraphPanel transferGraph;
	private TimelineGraphPanel timelineGraph;

	private InformationItemPanel sendRatePanel;
	private InformationItemPanel receiveRatePanel;
	private JLabel nameLabel;
	private JLabel macLabel;
	private JLabel ipv4Label;
	private JLabel ipv6Label;

	private GraphTypeButton connectedButton;

	public NetworkPanel(TimelineGroup timelineGroup, Network network) {
		this.network = network;

		JLabel labelHeader = new JLabel("Network       "); // Wider text here to force the label panel further to the right
		labelHeader.setFont(labelHeader.getFont().deriveFont(24f));

		JLabel labelNetworkUsage = new JLabel("Throughput");
		JLabel labelZero = new JLabel("0");
		JLabel labelMaxTime = new JLabel("Displaying 60 seconds");
		labelMaxTransfer = new JLabel("XX Kbps");

		transferGraph = new GraphPanel(GraphType.Network, true);
		timelineGraph = new TimelineGraphPanel(transferGraph, labelMaxTime);

		transferGraph.setIsLogarithmic(true);
		transferGraph.addGraph(network.inRate, false);
		transferGraph.addGraph(network.outRate, true);
		timelineGraph.addGraph(network.inRate);
		timelineGraph.addGraph(network.outRate);
		timelineGroup.add(timelineGraph);

		// TODO Currently copy-n-paste strokes from GraphPanel! Improve this somehow!
		sendRatePanel = new InformationItemPanel("Send", ValueType.BitsPerSecond, new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{3f}, 0f), GraphType.Network.color);
		receiveRatePanel = new InformationItemPanel("Receive", ValueType.BitsPerSecond, new BasicStroke(2), GraphType.Network.color);

		JLabel labelNameHeader = new JLabel("Name: ");
		nameLabel = new JLabel();
		JLabel labelMacHeader = new JLabel("MAC address: ");
		macLabel = new JLabel();
		JLabel labelIpv4Header = new JLabel("IPv4 address: ");
		ipv4Label = new JLabel();
		JLabel labelIpv6Header = new JLabel("IPv6 address: ");
		ipv6Label = new JLabel();

		Font headerFont = labelNameHeader.getFont().deriveFont(Font.BOLD);
		labelNameHeader.setFont(headerFont);
		labelMacHeader.setFont(headerFont);
		labelIpv4Header.setFont(headerFont);
		labelIpv6Header.setFont(headerFont);

		JPanel ratePanel = new JPanel();
		ratePanel.setLayout(new GridLayout(2, 1));
		ratePanel.add(sendRatePanel);
		ratePanel.add(receiveRatePanel);

		JPanel labelPanel = new JPanel();
		SimpleGridBagLayout labelLayout = new SimpleGridBagLayout(labelPanel);
		labelLayout.addToGrid(labelNameHeader, 1, 0, 1, 1, GridBagConstraints.WEST);
		labelLayout.addToGrid(labelMacHeader, 1, 1, 1, 1, GridBagConstraints.WEST);
		labelLayout.addToGrid(labelIpv4Header, 1, 2, 1, 1, GridBagConstraints.WEST);
		labelLayout.addToGrid(labelIpv6Header, 1, 3, 1, 1, GridBagConstraints.WEST);
		labelLayout.addToGrid(nameLabel, 2, 0, 1, 1, GridBagConstraints.HORIZONTAL, 1, 0);
		labelLayout.addToGrid(macLabel, 2, 1, 1, 1, GridBagConstraints.HORIZONTAL, 1, 0);
		labelLayout.addToGrid(ipv4Label, 2, 2, 1, 1, GridBagConstraints.HORIZONTAL, 1, 0);
		labelLayout.addToGrid(ipv6Label, 2, 3, 1, 1, GridBagConstraints.HORIZONTAL, 1, 0);

		SimpleGridBagLayout layout = new SimpleGridBagLayout(this);
		layout.addToGrid(labelHeader, 0, 0, 1, 1, GridBagConstraints.WEST);
		layout.setInsets(0, 5, 0, 5);
		layout.addToGrid(labelNetworkUsage, 0, 1, 1, 1, GridBagConstraints.WEST);
		layout.addToGrid(labelMaxTransfer, 2, 1, 1, 1, GridBagConstraints.EAST);
		layout.setInsets(2, 5, 2, 5);
		layout.addToGrid(transferGraph, 0, 2, 3, 1, GridBagConstraints.BOTH, 1, 1);
		layout.setInsets(0, 5, 0, 5);
		layout.addToGrid(labelMaxTime, 0, 3, 2, 1, GridBagConstraints.WEST);
		layout.addToGrid(labelZero, 2, 3, 1, 1, GridBagConstraints.EAST);
		layout.setInsets(5, 5, 5, 5);
		layout.addToGrid(timelineGraph, 0, 4, 3, 1, GridBagConstraints.HORIZONTAL, 1, 0);
		layout.addToGrid(sendRatePanel, 0, 7, 1, 1, GridBagConstraints.WEST);
		layout.addToGrid(receiveRatePanel, 0, 8, 1, 1, GridBagConstraints.WEST);
		layout.addToGrid(labelPanel, 1, 7, 2, 2, GridBagConstraints.WEST);
	}


	public void update(SystemInformation systemInformation) {
		long max = Math.max(100 * 1024, 8 * Math.max(network.outRate.max(), network.inRate.max())) / 8;

		labelMaxTransfer.setText(TextUtils.bitsToString(max, 0) + "ps");

		transferGraph.setMaxDatapointValue(max);
		timelineGraph.setMaxDatapointValue(max);
		connectedButton.setMaxDatapointValue(max);

		transferGraph.newDatapoint();
		timelineGraph.newDatapoint();
		connectedButton.newDatapoint(network.outRate.newest(), network.inRate.newest());

		sendRatePanel.updateValue(network.outRate.newest());
		receiveRatePanel.updateValue(network.inRate.newest());
		nameLabel.setText(network.name);
		macLabel.setText(network.macAddress);
		ipv4Label.setText(String.join(", ", network.ipv4Addresses));
		ipv6Label.setText(String.join(", ", network.ipv6Addresses));
	}


	public GraphTypeButton createNetworkButton(int index) {
		connectedButton = new GraphTypeButton(GraphType.Network, "Network", index);
		connectedButton.setIsLogarithmic(transferGraph.isLogarithmic());
		connectedButton.addGraph(network.outRate, true);
		connectedButton.addGraph(network.inRate, false);
		return connectedButton;
	}
}
