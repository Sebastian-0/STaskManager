package taskmanager.ui.performance;

import config.Config;
import taskmanager.Measurements;
import taskmanager.SystemInformation.TopList;
import taskmanager.ui.ColorUtils;
import taskmanager.ui.TextUtils;
import taskmanager.ui.TextUtils.ValueType;

import javax.swing.JPanel;
import javax.swing.border.LineBorder;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GraphPanel extends JPanel {
	private List<Graph> graphs;
	private long measurementMaximumValue;

	protected GraphType graphType;

	protected int dataStartIndex;
	protected int dataEndIndex;
	private boolean moveDataWindow; // When false the displayed sequence stays the same until it is forced out of memory

	private int gridOffset;

	private boolean renderValueMarker;
	private boolean isLogarithmic;
	private int mouseX;
	private int mouseY;

	public GraphPanel(GraphType graphType, boolean renderValueMarker) {
		this.graphs = new ArrayList<>();
		this.graphType = graphType;
		this.renderValueMarker = renderValueMarker;
		isLogarithmic = false;

		setBackground(Color.WHITE);
		setSelected(false);
		setPreferredSize(new Dimension(80, 50));
		setMinimumSize(getPreferredSize());

		addMouseMotionListener(mouseListener);
		addMouseListener(mouseListener);

		moveDataWindow = true;
		mouseX = -1;
	}

	public void setIsLogarithmic(boolean isLogarithmic) {
		this.isLogarithmic = isLogarithmic;
	}

	public boolean isLogarithmic() {
		return isLogarithmic;
	}

	public void addGraph(Measurements<Long> measurements) {
		addGraph(measurements, null, false);
	}

	public void addGraph(Measurements<Long> measurements, boolean isDashed) {
		addGraph(measurements, null, isDashed);
	}

	public void addGraph(Measurements<Long> measurements, Measurements<TopList> topLists) {
		addGraph(measurements, topLists, false);
	}

	public void addGraph(Measurements<Long> measurements, Measurements<TopList> topLists, boolean isDashed) {
		this.graphs.add(new Graph(measurements, topLists, isDashed));
		setDataIndexInterval((int) (measurements.size() - 1 - 60 * Config.getFloat(Config.KEY_UPDATE_RATE)), measurements.size() - 1);
	}

	public void setSelected(boolean selected) {
		if (selected)
			setBorder(new LineBorder(Color.BLACK, 2));
		else
			setBorder(new LineBorder(Color.BLACK));
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		for (Graph graph : graphs) {
			int stepSize = computeIndicesPerPixel();
			graph.measurementAverager.setInterval(dataStartIndex, dataEndIndex, stepSize);
			if (graph.topListAverager != null) {
				graph.topListAverager.setInterval(dataStartIndex, dataEndIndex, stepSize);
			}
		}

		drawCurve(g2d);
		drawGrid(g);

		if (mouseX >= 0 && renderValueMarker) {
			drawSelection(g2d);
		}
	}

	protected int computeIndicesPerPixel() {
		return (int) Math.ceil((dataEndIndex - dataStartIndex) /
				(float) (getWidth() / Config.getInt(Config.KEY_GRAPH_MAX_PIXELS_PER_SEGMENT)));
	}

	private void drawGrid(Graphics g) {
		final int datapointsPerVerticalSection = computeDatapointsPerVerticalSection();

		final int numHorizontalSections = 10;
		final int numVerticalSections = (dataEndIndex - dataStartIndex) / datapointsPerVerticalSection;
		final int verticalOffset = 0;

//		g.setColor(new Color(227, 239, 247));
		g.setColor(new Color(127, 139, 147, 50));
		if (isLogarithmic) {
			double lower = 0;
			double upper = logarithm(numHorizontalSections);

			for (int i = 0; i < numHorizontalSections - 1; i++) {
				int y = (int) (logarithm((i + 1)) / (upper - lower) * getHeight());
				g.drawLine(0, getHeight() - y, getWidth(), getHeight() - y);
			}
		} else {
			for (int i = 0; i < numHorizontalSections - 1; i++) {
				int y = (i + 1) * getHeight() / numHorizontalSections;
				g.drawLine(0, y, getWidth(), y);
			}
		}

		final int indicesPerPixel = computeIndicesPerPixel();

		for (int i = 0; i < numVerticalSections; i++) {
			int alignedPoints = (gridOffset - gridOffset % indicesPerPixel) % datapointsPerVerticalSection;
			float offset = alignedPoints / (float) datapointsPerVerticalSection;
			int x = (int) (getWidth() * (i + 1 - offset) / numVerticalSections + verticalOffset);
			g.drawLine(x, 0, x, getHeight());
		}
	}

	private double logarithm(double value) {
		return Math.log10(value);
	}

	private int computeDatapointsPerVerticalSection() {
		int result = 0;
		int size = 0;
		do {
			size += 1;
			result = (dataEndIndex - dataStartIndex) / size;
		} while (result > 25);

		if (getWidth() / result < 30) {
			size *= 2;
		}

		return size;
	}

	private void drawCurve(Graphics2D g2d) {
		int alpha = 25;
		for (Graph graph : graphs) {
			drawCurvePart(g2d, graph, false, alpha);
			alpha += 25;
		}
		for (Graph graph : graphs) {
			drawCurvePart(g2d, graph, true, 255);
		}
	}

	private void drawCurvePart(Graphics2D g2d, Graph graph, boolean drawLine, int alpha) {
		Stroke oldStroke = g2d.getStroke();
		if (graph.isDashed) {
			g2d.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{3f}, 0f));
		}

		MeasurementAverager<Long> itr = graph.measurementAverager;
		itr.reset();
		long previous = itr.next();
		int idx = 0;
		while (itr.hasNext()) {
			long current = itr.next();

			int yPrev = (int) (getHeight() * Math.min(1, computeHeightFraction(previous, measurementMaximumValue)));
			int yCurr = (int) (getHeight() * Math.min(1, computeHeightFraction(current, measurementMaximumValue)));

			int x = getWidth() * (idx) / itr.numPoints();
			int xNext = getWidth() * (idx + 1) / itr.numPoints();

			Color color = graphType.color;
			if (drawLine) {
				g2d.setColor(color);
				g2d.drawLine(x, getHeight() - yPrev, xNext, getHeight() - yCurr);
			} else {
				g2d.setColor(ColorUtils.blend(color, Color.WHITE, alpha/255f));
				int[] xs = {x, x, xNext, xNext};
				int[] ys = {getHeight(), getHeight() - yPrev, getHeight() - yCurr, getHeight()};
				g2d.fillPolygon(xs, ys, xs.length);
			}

			idx += 1;
			previous = current;
		}
		g2d.setStroke(oldStroke);
	}

	private double computeHeightFraction(long value, long maximum) {
		if (isLogarithmic)
			return logarithm(value < 1 ? 1 : value) / logarithm(maximum);
		return value / (double) maximum;
	}

	protected void drawSelection(Graphics2D g2d) {
		int x = mouseX;
		List<Long> selectedValues = new ArrayList<>();
		List<Integer> selectedValuesY = new ArrayList<>();
		List<TopList> selectedTopLists = new ArrayList<>();
		for (Graph graph : graphs) {
			float indexAtMouse = x / (float) getWidth() * graph.measurementAverager.numPoints();
			float factor = indexAtMouse % 1;

			MeasurementAverager<Long> itr = graph.measurementAverager;
			itr.reset((int) indexAtMouse);

			long leftValue = itr.next();
			long rightValue = 0;
			if (itr.hasNext()) {
				rightValue = itr.next();
			}

			long selectedValue = (long) (rightValue * factor + leftValue * (1 - factor));
			selectedValues.add(selectedValue);

			// We don't just use selectedValue to compute y-coordinate since that won't "work" when using the logarithmic scale
			int leftY = (int) (getHeight() * (1 - computeHeightFraction(leftValue, measurementMaximumValue)));
			int rightY = (int) (getHeight() * (1 - computeHeightFraction(rightValue, measurementMaximumValue)));
			int y = (int) (rightY * factor + leftY * (1 - factor));
			selectedValuesY.add(y);

			MeasurementAverager<TopList> topListItr = graph.topListAverager;
			if (topListItr != null) {
				topListItr.reset((int) indexAtMouse);

				TopList leftList = topListItr.next();
				TopList rightList = TopList.EMPTY;
				if (topListItr.hasNext()) {
					rightList = topListItr.next();
				}

				selectedTopLists.add(MeasurementAveragerForTopList.weightedAverageOf(leftList, rightList, factor));
			}
		}

		drawSelectedValues(g2d, x, selectedValuesY);
		drawSelectedValuesLabel(g2d, x, selectedValues, selectedTopLists);
	}



	private void drawSelectedValues(Graphics2D g2d, int x, List<Integer> yCoordinates) {
		g2d.setColor(graphType.color);
		Stroke oldStroke = g2d.getStroke();
		g2d.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{5f}, 0f));
		g2d.drawLine(x, 0, x, getHeight());
		g2d.setStroke(oldStroke);

		final int size = 7;
		for (Integer y : yCoordinates) {
			g2d.fillOval(x - size / 2, y - size / 2, size, size);
		}
	}

	private void drawSelectedValuesLabel(Graphics2D g2d, int x, List<Long> selectedValues, List<TopList> selectedTopLists) {
		g2d.setFont(getFont());
		FontMetrics metrics = g2d.getFontMetrics();
		List<String> labelLines = new ArrayList<>();
		if (graphType == GraphType.Cpu) {
			labelLines.add(TextUtils.valueToString(selectedValues.get(0), ValueType.Percentage));
		} else if (graphType == GraphType.Memory) {
			labelLines.add(TextUtils.valueToString(selectedValues.get(0), ValueType.Bytes));
		} else if (graphType == GraphType.Network) {
			labelLines.add("S: " + TextUtils.valueToString(selectedValues.get(0), ValueType.BitsPerSecond));
			labelLines.add("R: " + TextUtils.valueToString(selectedValues.get(1), ValueType.BitsPerSecond));
		} else if (graphType == GraphType.Disk) {
			if (graphs.size() == 1) {
				labelLines.add(TextUtils.valueToString(selectedValues.get(0), ValueType.Percentage));
			} else {
				labelLines.add("W: " + TextUtils.valueToString(selectedValues.get(0), ValueType.BytesPerSecond));
				labelLines.add("R: " + TextUtils.valueToString(selectedValues.get(1), ValueType.BytesPerSecond));
			}
		}

		final int columnOffset = 10;
		final int padding = 2;
		final int insets = 8;

		int width = computeTextWidth(labelLines, metrics) + insets * 2;
		int height = computeTextHeight(labelLines.size(), metrics) + insets;

		TopList selectedTopList = null;
		List<String> usages = new ArrayList<>();
		List<String> names = new ArrayList<>();
		List<String> pids = new ArrayList<>();
		int[] columnPositions = new int[3];
		if (!selectedTopLists.isEmpty() && selectedTopLists.get(0) != TopList.EMPTY) {
			selectedTopList = selectedTopLists.get(0);
			int usageWidth = 40;
			int nameWidth = 80;
			int pidWidth = 40;

			TopList topList = selectedTopList;
			for (TopList.Entry entry : topList.entries) {
				String usage = "";
				String name = entry.process.fileName;
				String pid = Long.toString(entry.process.id);
				if (graphType == GraphType.Cpu) {
					usage = TextUtils.valueToString(entry.value, ValueType.Percentage);
				} else if (graphType == GraphType.Memory) {
					usage = TextUtils.valueToString(entry.value, ValueType.Bytes);
				}

				if (name.isEmpty()) {
					name = "<unnamed>";
				}

				name = TextUtils.limitWidth(name, nameWidth, metrics);

				usageWidth = Math.max(usageWidth, metrics.stringWidth(usage));
				pidWidth = Math.max(pidWidth, metrics.stringWidth(pid));

				usages.add(usage);
				names.add(name);
				pids.add(pid);
			}
			width = Math.max(width, usageWidth + nameWidth + pidWidth + columnOffset * 2 + insets * 2);
			height += computeTextHeight(topList.entries.length + 3/4f, metrics);

			columnPositions[1] = usageWidth + columnOffset;
			columnPositions[2] = columnPositions[1] + nameWidth + columnOffset;
		}

		int y = Math.max(padding, mouseY - height);
		if (x + width + padding * 2 > getWidth()) {
			x = Math.max(0, x - width - padding * 2) + padding;
		}
		g2d.setColor(Color.WHITE);
		g2d.fillRoundRect(x, y, width, height, 6, 6);
		g2d.setColor(new Color(150, 150, 150));
		g2d.drawRoundRect(x, y, width, height, 6, 6);

		// Render overall measurements
		g2d.setColor(Color.BLACK);
		for (int i = 0; i < labelLines.size(); i++) {
			g2d.drawString(labelLines.get(i), x + insets, y + insets / 2 + metrics.getHeight() * (i + 1) - metrics.getDescent());
		}

		y += labelLines.size() * metrics.getHeight() + insets / 2;

		// Render top list
		if (selectedTopList != null) {
			g2d.drawLine(x + insets/2, y + metrics.getHeight()/2, x + width - insets/2, y + metrics.getHeight()/2);
			y += metrics.getHeight() * 3 / 4;
			for (int i = 0; i < selectedTopList.entries.length; i++) {
				g2d.drawString(usages.get(i), x + insets + columnPositions[0], y + metrics.getHeight() * (i + 1) - metrics.getDescent());
				g2d.drawString(names.get(i), x + insets + columnPositions[1], y + metrics.getHeight() * (i + 1) - metrics.getDescent());
				g2d.drawString(pids.get(i), x + insets + columnPositions[2], y + metrics.getHeight() * (i + 1) - metrics.getDescent());
			}
		}
	}

	private int computeTextWidth(List<String> lines, FontMetrics metrics) {
		int max = 0;
		for (String line : lines) {
			max = Math.max(max, metrics.stringWidth(line));
		}
		return max;
	}

	private int computeTextHeight(double lines, FontMetrics metrics) {
		return (int) (metrics.getHeight() * lines);
	}

	protected void setDataIndexInterval(int start, int end) {
		dataStartIndex = start;
		dataEndIndex = end;
	}

	public void setMaxDatapointValue(long maximumValue) {
		measurementMaximumValue = maximumValue;
	}

	public void newDatapoint() {
		moveDataWindow = dataEndIndex == graphs.get(0).measurements.size() - 1;
		if (!moveDataWindow && dataStartIndex > 0) {
			dataStartIndex -= 1;
			dataEndIndex -= 1;
		} else {
			gridOffset += 1;
			for (Graph graph : graphs) {
				graph.measurementAverager.shift(gridOffset);
				if (graph.topListAverager != null) {
					graph.topListAverager.shift(gridOffset);
				}
			}
		}

		repaint();
	}


	private MouseAdapter mouseListener = new MouseAdapter() {
		@Override
		public void mouseMoved(MouseEvent e) {
			if (renderValueMarker) {
				mouseX = e.getX();
				mouseY = e.getY();
				repaint();
			}
		}

		@Override
		public void mouseExited(MouseEvent e) {
			mouseX = -1;
			repaint();
		}
	};


	private static class Graph {
		private Measurements<Long> measurements;
		private MeasurementAverager<Long> measurementAverager;
		private MeasurementAverager<TopList> topListAverager;
		private boolean isDashed;

		public Graph(Measurements<Long> measurements, Measurements<TopList> topLists, boolean isDashed) {
			this.measurements = measurements;
			this.measurementAverager = new MeasurementAveragerForLong(measurements);
			if (topLists != null) {
				this.topListAverager = new MeasurementAveragerForTopList(topLists);
			}
			this.isDashed = isDashed;
		}
	}


	public static class DoubleToLong implements Measurements<Long> {
		private Measurements<Double> iterable;

		public DoubleToLong(Measurements<Double> iterable) {
			this.iterable = iterable;
		}

		@Override
		public void copyFrom(Measurements<Long> other) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void copyDelta(Measurements<Long> other) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void addValue(Long value) {
			iterable.addValue(value / (double) Config.DOUBLE_TO_LONG);
		}

		@Override
		public int size() {
			return iterable.size();
		}

		@Override
		public int realSize() {
			return iterable.realSize();
		}

		@Override
		public Long newest() {
			return (long) (iterable.newest() * Config.DOUBLE_TO_LONG);
		}

		@Override
		public Long oldest() {
			return (long) (iterable.oldest() * Config.DOUBLE_TO_LONG);
		}

		@Override
		public Long min() {
			return (long) (iterable.min() * Config.DOUBLE_TO_LONG);
		}

		@Override
		public Long max() {
			return (long) (iterable.max() * Config.DOUBLE_TO_LONG);
		}

		@Override
		public Iterator<Long> getRangeIterator(int startIndex, int endIndex) {
			return new ConversionIterator(iterable.getRangeIterator(startIndex, endIndex));
		}

		private static class ConversionIterator implements Iterator<Long> {
			private Iterator<Double> sourceIterator;

			ConversionIterator(Iterator<Double> sourceIterator) {
				this.sourceIterator = sourceIterator;
			}

			@Override
			public Long next() {
				return (long) (sourceIterator.next() * Config.DOUBLE_TO_LONG);
			}

			@Override
			public boolean hasNext() {
				return sourceIterator.hasNext();
			}
		}
	}


	public static class ShortToLong implements Measurements<Long> {
		private Measurements<Short> iterable;

		public ShortToLong(Measurements<Short> iterable) {
			this.iterable = iterable;
		}

		@Override
		public void copyFrom(Measurements<Long> other) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void copyDelta(Measurements<Long> other) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void addValue(Long value) {
			iterable.addValue((short) value.longValue());
		}

		@Override
		public int size() {
			return iterable.size();
		}

		@Override
		public int realSize() {
			return iterable.realSize();
		}

		@Override
		public Long newest() {
			return (long) iterable.newest();
		}

		@Override
		public Long oldest() {
			return (long) (iterable.oldest() * Config.DOUBLE_TO_LONG);
		}

		@Override
		public Long min() {
			return (long) (iterable.min() * Config.DOUBLE_TO_LONG);
		}

		@Override
		public Long max() {
			return (long) (iterable.max() * Config.DOUBLE_TO_LONG);
		}

		@Override
		public Iterator<Long> getRangeIterator(int startIndex, int endIndex) {
			return new ConversionIterator(iterable.getRangeIterator(startIndex, endIndex));
		}

		private static class ConversionIterator implements Iterator<Long> {
			private Iterator<Short> sourceIterator;

			ConversionIterator(Iterator<Short> sourceIterator) {
				this.sourceIterator = sourceIterator;
			}

			@Override
			public Long next() {
				return (long) sourceIterator.next();
			}

			@Override
			public boolean hasNext() {
				return sourceIterator.hasNext();
			}
		}
	}
}
