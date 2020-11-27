/*
 * Copyright (c) 2020. Sebastian Hjelm
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * See LICENSE for further details.
 */

package taskmanager.ui.performance;

import config.Config;
import taskmanager.Measurements;
import taskmanager.data.TopList;
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
	private final List<Graph> graphs;
	private long measurementMaximumValue;

	protected int dataStartIndex;
	protected int dataEndIndex;
	private boolean moveDataWindow; // When false the displayed sequence stays the same until it is forced out of memory

	private int gridOffset;

	private final boolean renderValueMarker;
	private boolean isLogarithmic;
	private int mouseX;
	private int mouseY;

	public GraphPanel() {
		this (true);
	}

	public GraphPanel(boolean renderValueMarker) {
		this.graphs = new ArrayList<>();
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

	public void addGraph(Graph graph) {
		graphs.add(graph);
		setDataIndexInterval((int) (graph.measurements.size() - 1 - 60 * Config.getFloat(Config.KEY_UPDATE_RATE)), graph.measurements.size() - 1);
	}

	public void setSelected(boolean selected) {
		if (selected) {
			setBorder(new LineBorder(Color.BLACK, 2));
		} else {
			setBorder(new LineBorder(Color.BLACK));
		}
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
		g2d.setStroke(graph.style.createStroke());

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

			Color color = graph.graphType.color;
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
		if (isLogarithmic) {
			return logarithm(value < 1 ? 1 : value) / logarithm(maximum);
		}
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
			} else {
				selectedTopLists.add(null);
			}
		}

		drawSelectedValues(g2d, x, selectedValuesY);
		drawSelectedValuesLabel(g2d, x, selectedValues, selectedTopLists);
	}



	private void drawSelectedValues(Graphics2D g2d, int x, List<Integer> yCoordinates) {
		g2d.setColor(getSelectionLineColor());
		Stroke oldStroke = g2d.getStroke();
		g2d.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{5f}, 0f));
		g2d.drawLine(x, 0, x, getHeight());
		g2d.setStroke(oldStroke);

		final int size = 7;
		for (int i = 0; i < yCoordinates.size(); i++) {
			Integer y = yCoordinates.get(i);
			g2d.setColor(graphs.get(i).graphType.color);
			g2d.fillOval(x - size / 2, y - size / 2, size, size);
		}
	}

	private Color getSelectionLineColor() {
		if (graphs.stream().map(g -> g.graphType.color).distinct().count() > 1) {
			return Color.GRAY;
		}
		return graphs.get(0).graphType.color;
	}

	private void drawSelectedValuesLabel(Graphics2D g2d, int x, List<Long> selectedValues, List<TopList> selectedTopLists) {
		g2d.setFont(getFont());
		FontMetrics metrics = g2d.getFontMetrics();
		List<String> labelLines = new ArrayList<>();

		for (int i = 0; i < graphs.size(); i++) {
			labelLines.add(graphs.get(i).style.tooltipPrefix + TextUtils.valueToString(selectedValues.get(i), graphs.get(i).valueType));
		}

		final int columnOffset = 10;
		final int padding = 2;
		final int insets = 8;

		int width = computeTextWidth(labelLines, metrics) + insets * 2;
		int height = computeTextHeight(labelLines.size(), metrics) + insets;

		List<String> usages = new ArrayList<>();
		List<String> names = new ArrayList<>();
		List<String> pids = new ArrayList<>();
		int[] columnPositions = new int[3];

		// Only render the first top list we find
		int topListIdx = -1;
		for (int i = 0; i < selectedTopLists.size(); i++) {
			if (selectedTopLists.get(i) != null && selectedTopLists.get(i) != TopList.EMPTY) {
				topListIdx = i;
				break;
			}
		}

		if (topListIdx != -1) {
			int usageWidth = 40;
			int nameWidth = 80;
			int pidWidth = 40;

			TopList topList = selectedTopLists.get(topListIdx);
			for (TopList.Entry entry : topList.entries) {
				String usage = "";
				String name = entry.process.fileName;
				String pid = Long.toString(entry.process.id);
				usage = TextUtils.valueToString(entry.value, graphs.get(topListIdx).valueType);

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
		for (int i = 0; i < labelLines.size(); i++) {
			g2d.setColor(graphs.get(i).graphType.color);
			Stroke old = g2d.getStroke();
			if (graphs.get(i).style.dashedLine) {
				g2d.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[] {3f}, 0f));
			} else {
				g2d.setStroke(new BasicStroke(2f));
			}
			int lineY = (int) (y + insets / 2 + metrics.getHeight() * (i + 0.5));
			g2d.drawLine(x + insets, lineY, x + insets + 10, lineY);
			g2d.setStroke(old);

			g2d.setColor(Color.BLACK);
			g2d.drawString(labelLines.get(i), x + insets * 2 + 10, y + insets / 2 + metrics.getHeight() * (i + 1) - metrics.getDescent());
		}

		y += labelLines.size() * metrics.getHeight() + insets / 2;

		// Render top list
		if (topListIdx != -1) {
			g2d.setColor(graphs.get(topListIdx).graphType.color.darker());
			g2d.drawLine(x + insets/2, y + metrics.getHeight()/2, x + width - insets/2, y + metrics.getHeight()/2);
			y += metrics.getHeight() * 3 / 4;
			for (int i = 0; i < selectedTopLists.get(topListIdx).entries.length; i++) {
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


	private final MouseAdapter mouseListener = new MouseAdapter() {
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


	public static class Style {
		public final boolean dashedLine;
		public final String tooltipPrefix;

		public Style() {
			this(false, "");
		}

		public Style(boolean dashedLine, String tooltipPrefix) {
			this.dashedLine = dashedLine;
			this.tooltipPrefix = tooltipPrefix;
		}

		public Stroke createStroke() {
			if (dashedLine) {
				return new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[] {3f}, 0f);
			} else {
				return new BasicStroke(1.5f);
			}
		}
	}


	public static class Graph {
		public final Measurements<Long> measurements;
		public final MeasurementAverager<Long> measurementAverager;
		public final MeasurementAverager<TopList> topListAverager;

		public final GraphType graphType;
		public final ValueType valueType;
		public final Style style;

		private Graph(Measurements<Long> measurements, Measurements<TopList> topLists, GraphType graphType, ValueType valueType, Style style) {
			this.measurements = measurements;
			this.measurementAverager = new MeasurementAveragerForLong(measurements);
			if (topLists != null) {
				this.topListAverager = new MeasurementAveragerForTopList(topLists);
			} else {
				this.topListAverager = null;
			}
			this.graphType = graphType;
			this.valueType = valueType != null ? valueType : graphType.mainValueType;
			this.style = style;
		}
		
		
		public static class GraphBuilder {
			private final Measurements<Long> measurements;
			private Measurements<TopList> topList;

			private final GraphType graphType;

			private ValueType valueType;
			private Style style;

			public GraphBuilder(Measurements<Long> measurements, GraphType graphType) {
				this.measurements = measurements;
				this.graphType = graphType;
				this.style = new Style();
			}

			public GraphBuilder topList(Measurements<TopList> topList) {
				this.topList = topList;
				return this;
			}

			public GraphBuilder valueType(ValueType valueType) {
				this.valueType = valueType;
				return this;
			}

			public GraphBuilder style(Style style) {
				this.style = style;
				return this;
			}

			public Graph build() {
				return new Graph(measurements, topList, graphType, valueType, style);
			}
		}
	}


	public static class DoubleToLong implements Measurements<Long> {
		private final Measurements<Double> iterable;

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
			private final Iterator<Double> sourceIterator;

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
		private final Measurements<Short> iterable;

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
			private final Iterator<Short> sourceIterator;

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