package taskmanager.ui.performance;

import config.Config;
import taskmanager.Measurements;
import taskmanager.ui.ColorUtils;
import taskmanager.ui.TextUtils;
import taskmanager.ui.TextUtils.ValueType;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
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
		addGraph(measurements, false);
	}

	public void addGraph(Measurements<Long> measurements, boolean isDashed) {
		this.graphs.add(new Graph(measurements, isDashed));
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
			graph.measurementAverager.setInterval(dataStartIndex, dataEndIndex, computeIndicesPerPixel());
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

		MeasurementAverager itr = graph.measurementAverager;
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
		List<Long> selectedValues = drawSelectedValues(g2d, x);
		drawSelectedValuesLabel(g2d, x, selectedValues);
	}

	private List<Long> drawSelectedValues(Graphics2D g2d, int x) {
		g2d.setColor(graphType.color);
		Stroke oldStroke = g2d.getStroke();
		g2d.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{5f}, 0f));
		g2d.drawLine(x, 0, x, getHeight());
		g2d.setStroke(oldStroke);

		List<Long> selectedValues = new ArrayList<>();
		for (Graph graph : graphs) {
			float indexAtMouse = x / (float) getWidth() * graph.measurementAverager.numPoints();
			float factor = indexAtMouse % 1;

			MeasurementAverager itr = graph.measurementAverager;
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

			final int size = 7;
			g2d.fillOval(x - size / 2, y - size / 2, size, size);
		}
		return selectedValues;
	}

	private void drawSelectedValuesLabel(Graphics2D g2d, int x, List<Long> selectedValues) {
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

		final int padding = 2;
		final int insets = 8;

		int width = computeTextWidth(labelLines, metrics) + insets * 2;
		int height = computeTextHeight(labelLines, metrics) + insets;
		int y = Math.max(padding, mouseY - height);
		if (x + width + padding * 2 > getWidth())
			x = Math.max(0, x - width - padding * 2);
		g2d.setColor(Color.WHITE);
		g2d.fillRoundRect(x + padding, y, width, height, 6, 6);
		g2d.setColor(new Color(150, 150, 150));
		g2d.drawRoundRect(x + padding, y, width, height, 6, 6);

		g2d.setColor(Color.BLACK);
		for (int i = 0; i < labelLines.size(); i++) {
			g2d.drawString(labelLines.get(i), x + insets + padding, y + insets / 2 + metrics.getHeight() * (i + 1) - metrics.getDescent());
		}
	}

	private int computeTextWidth(List<String> lines, FontMetrics metrics) {
		int max = 0;
		for (String line : lines) {
			max = Math.max(max, metrics.stringWidth(line));
		}
		return max;
	}

	private int computeTextHeight(List<String> lines, FontMetrics metrics) {
		return metrics.getHeight() * lines.size();
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
		private MeasurementAverager measurementAverager;
		private boolean isDashed;

		public Graph(Measurements<Long> measurements, boolean isDashed) {
			this.measurements = measurements;
			this.measurementAverager = new MeasurementAverager(measurements);
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

		private class ConversionIterator implements Iterator<Long> {
			private Iterator<Double> sourceIterator;

			public ConversionIterator(Iterator<Double> sourceIterator) {
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
}
