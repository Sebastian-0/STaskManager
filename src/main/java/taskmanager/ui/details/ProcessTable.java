package taskmanager.ui.details;

import config.Config;
import taskmanager.Process;
import taskmanager.Process.ProcessComparator;
import taskmanager.SystemInformation;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ProcessTable extends JTable {
	public enum Columns {
		FileName("Process name", 180, new Process.FileNameComparator()),
		Pid("PID", 65, new Process.IdComparator()),
		DeathTime("Death time", 100, new Process.DeadTimestampsComparator()),
		UserName("User name", 75, new Process.UserNameComparator()),
		Cpu("CPU", 75, new Process.CpuUsageComparator()),
		PrivateWorkingSet("Memory", 100, new Process.PrivateWorkingSetComparator()),
		CommandLine("Command line", 200, new Process.CommandLineComparator()),
		Description("Description", 100, new Process.DescriptionComparator());

		public String name;
		public int defaultWidth;
		public ProcessComparator comparator;

		Columns(String name, int defaultWidth, ProcessComparator comparator) {
			this.name = name;
			this.defaultWidth = defaultWidth;
			this.comparator = comparator;
		}
	}

	public enum Load {
		None(new Color(255, 244, 196)), // 0%
		Small(new Color(249, 236, 168)), // 0.3%
		Medium(new Color(255, 228, 135)), // 20%
		Large(new Color(255, 210, 100)),   // 40%
		VeryLarge(new Color(255, 184, 25)), // 60%?
		Extreme(new Color(255, 167, 29)); // 80%?

		private Color color;

		Load(Color c) {
			color = c;
		}
	}

	private List<Columns> visibleColumns;

	private SystemInformation systemInformation;
	private ProcessDetailsCallback processCallback;

	private boolean showDeadProcesses;
	private ColumnHeader[] headers;
	private CustomTableModel tableModel;

	private ProcessTableCellRenderer cellRenderer;
	private FontMetrics metrics;

	private Columns filterAttribute;
	private String filterPhrase;
	private boolean isMovingColumn;
	private boolean isResizingColumn;

	private ProcessContextMenu contextMenu;

	public ProcessTable(final ProcessDetailsCallback processDetailsCallback, SystemInformation systemInformation,
							  boolean showDeadProcesses) {
		this.processCallback = processDetailsCallback;
		this.systemInformation = systemInformation;
		this.showDeadProcesses = showDeadProcesses;

		setDefaultRenderer(Object.class, new ProcessTableCellRenderer());
		setShowHorizontalLines(false);
		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		setAutoResizeMode(AUTO_RESIZE_OFF);

		visibleColumns = new ArrayList<>(); // TODO The order of these must (probably?) be the same as in the enum, fix that?
		visibleColumns.add(Columns.FileName);
		visibleColumns.add(Columns.Pid); // TODO Can't disable PID
		if (showDeadProcesses) {
			visibleColumns.add(Columns.DeathTime); // TODO Can't disable Death Time
		}
		visibleColumns.add(Columns.UserName);
		visibleColumns.add(Columns.Cpu);
		visibleColumns.add(Columns.PrivateWorkingSet);
		visibleColumns.add(Columns.CommandLine);
		visibleColumns.add(Columns.Description);


		tableModel = new CustomTableModel();
		tableModel.columns = loadHeaders();
		tableModel.fullData = new Object[0][tableModel.columns.length];
		tableModel.filteredData = new Object[0][tableModel.columns.length];
		tableModel.filteredColor = new Color[0][tableModel.columns.length];
		tableModel.fullColor = new Color[0][tableModel.columns.length];
		setModel(tableModel);

		cellRenderer = new ProcessTableCellRenderer();
		cellRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
		getColumnModel().getColumn(headers[Columns.Cpu.ordinal()].index).setCellRenderer(cellRenderer);
		getColumnModel().getColumn(headers[Columns.PrivateWorkingSet.ordinal()].index).setCellRenderer(cellRenderer);
		if (showDeadProcesses) {
			getColumnModel().getColumn(headers[Columns.DeathTime.ordinal()].index).setCellRenderer(cellRenderer);
		}
		setIntercellSpacing(new Dimension(1, 0));

		getColumnModel().addColumnModelListener(columnListener);
		addKeyListener(keyListener);
		addMouseListener(mouseListener);
		addMouseMotionListener(mouseListener);

		loadPreviousColumnSizes();
		loadPreviousColumnSelection();

		setTableHeader(new CustomTableHeader(getColumnModel()));
		getTableHeader().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON1) {
					int col = columnAtPoint(e.getPoint());
					String name = getColumnName(col);
					for (int i = 0; i < headers.length; i++) {
						ColumnHeader header = headers[i];
						if (header != null) {
							if (header.header.equals(name)) {
								if (header.isSelected)
									header.comparator.invert();
								processDetailsCallback.setComparator(header.comparator, showDeadProcesses);
								header.isSelected = true;
								if (showDeadProcesses) {
									Config.put(Config.KEY_LAST_DEAD_COLUMN_SELECTION, Integer.toString(i));
									Config.put(Config.KEY_LAST_DEAD_SELECTION_INVERTED, Boolean.toString(header.comparator.isInverted()));
								} else {
									Config.put(Config.KEY_LAST_COLUMN_SELECTION, Integer.toString(i));
									Config.put(Config.KEY_LAST_SELECTION_INVERTED, Boolean.toString(header.comparator.isInverted()));
								}
							} else {
								header.isSelected = false;
							}
						}
					}
				}
			}
		});

		metrics = getTableHeader().getFontMetrics(getFont());

		filterAttribute = Columns.FileName;
		filterPhrase = "";

		contextMenu = new ProcessContextMenu((Component) processDetailsCallback);
		contextMenu.addPopupMenuListener(popupListener);
		setComponentPopupMenu(contextMenu);
	}

	private String[] loadHeaders() {
		String defaultValue = IntStream.range(0, visibleColumns.size()).mapToObj(Integer::toString).collect(Collectors.joining(";"));
		String orderAsString = Config.get(Config.KEY_LAST_COLUMN_ORDER, defaultValue);
		if (showDeadProcesses) {
			orderAsString = Config.get(Config.KEY_LAST_DEAD_COLUMN_ORDER, defaultValue);
		}
		int[] columnOrder = Arrays.stream(orderAsString.split(";"))
				.flatMapToInt(v -> IntStream.of(Integer.parseInt(v)))
				.toArray();

		ColumnHeader[] tmp = new ColumnHeader[visibleColumns.size()];
		Columns[] values = Columns.values();
		headers = new ColumnHeader[values.length];
		for (int i = 0, idx = 0; i < headers.length; i++) {
			Columns value = values[i];
			if (visibleColumns.contains(value)) {
				ColumnHeader header = new ColumnHeader(value, columnOrder[idx]);
				headers[i] = header;
				tmp[idx] = header;
				idx++;
			}
		}

		Arrays.sort(tmp);

		String[] columns = new String[tmp.length];
		for (int i = 0; i < tmp.length; i++) {
			columns[i] = tmp[i].header;
		}

		return columns;
	}

	private void loadPreviousColumnSizes() {
		String defaultValue = visibleColumns.stream().map(c -> Integer.toString(c.defaultWidth)).collect(Collectors.joining(";"));
		String widthsAsString = Config.get(Config.KEY_LAST_COLUMN_WIDTHS, defaultValue);
		if (showDeadProcesses) {
			widthsAsString = Config.get(Config.KEY_LAST_DEAD_COLUMN_WIDTHS, defaultValue);
		}
		int[] widths = Arrays.stream(widthsAsString.split(";"))
				.flatMapToInt(v -> IntStream.of(Integer.parseInt(v)))
				.toArray();
		for (int i = 0, idx = 0; i < headers.length; i++) {
			if (headers[i] != null) {
				getColumnModel().getColumn(headers[i].index).setPreferredWidth(widths[idx++]);
			}
		}
	}

	private void loadPreviousColumnSelection() {
		int selectedColumn = Integer.parseInt(Config.get(Config.KEY_LAST_COLUMN_SELECTION, "" + Columns.Pid.ordinal()));
		boolean selectionInverted = Boolean.parseBoolean(Config.get(Config.KEY_LAST_SELECTION_INVERTED));
		if (showDeadProcesses) {
			selectedColumn = Integer.parseInt(Config.get(Config.KEY_LAST_DEAD_COLUMN_SELECTION, "" + Columns.DeathTime.ordinal()));
			selectionInverted = Boolean.parseBoolean(Config.get(Config.KEY_LAST_DEAD_SELECTION_INVERTED));
		}
		headers[selectedColumn].isSelected = true;
		if (selectionInverted)
			headers[selectedColumn].comparator.invert();
		processCallback.setComparator(headers[selectedColumn].comparator, showDeadProcesses);
	}


	public void update() {
		List<Process> processes = systemInformation.processes;
		if (showDeadProcesses) {
			processes = systemInformation.deadProcesses;
		}

		if (!isMovingColumn && !isResizingColumn) {
			long selectedPid = getSelectedPid();

			tableModel.fullData = new Object[processes.size()][tableModel.columns.length];
			tableModel.fullColor = new Color[processes.size()][tableModel.columns.length];

			DecimalFormatSymbols symbols = new DecimalFormatSymbols();
			symbols.setGroupingSeparator(' ');
			DecimalFormat dfCpu = new DecimalFormat("##0.0");
			DecimalFormat dfMemory = new DecimalFormat("###,###.##");
			dfMemory.setDecimalFormatSymbols(symbols);

			for (int i = 0; i < processes.size(); i++) {
				Process process = processes.get(i);
				trySetData(Columns.FileName, i, process.fileName);
				trySetData(Columns.Pid, i, process.id);
				trySetData(Columns.UserName, i, process.userName);
				trySetData(Columns.DeathTime, i, String.format("%.1f s", (System.currentTimeMillis() - process.deathTimestamp) / 1000f));
				trySetColor(Columns.DeathTime, i, selectColorDeath((System.currentTimeMillis() - process.deathTimestamp) / 1000f));
				if (showDeadProcesses) {
					trySetData(Columns.Cpu, i, "--.- %");
					trySetColor(Columns.Cpu, i, selectColorCpu(0));
					trySetData(Columns.PrivateWorkingSet, i, "--- --- K");
					trySetColor(Columns.PrivateWorkingSet, i, selectColorMemory(0));
				} else {
					trySetData(Columns.Cpu, i, dfCpu.format(process.cpuUsage.newest() * 100) + " %");
					if (process.id == 0) {
						trySetColor(Columns.Cpu, i, selectColorCpu(0));
					} else {
						trySetColor(Columns.Cpu, i, selectColorCpu(process.cpuUsage.newest()));
					}
					trySetData(Columns.PrivateWorkingSet, i, dfMemory.format(process.privateWorkingSet.newest() / 1024) + " K");
					trySetColor(Columns.PrivateWorkingSet, i, selectColorMemory(process.privateWorkingSet.newest() / (double) systemInformation.physicalMemoryTotal));
				}
				trySetData(Columns.CommandLine, i, process.commandLine);
				trySetData(Columns.Description, i, process.description);
			}

			filter();
			trySelectPid(selectedPid);

			revalidate();
			repaint();
		} else {
			System.out.println("SKIP");
		}
	}

	private long getSelectedPid() {
		long selectedPid = -1;
		int selectedRow = getSelectedRow();
		if (selectedRow >= 0) {
			selectedPid = (Long) tableModel.filteredData[selectedRow][headers[Columns.Pid.ordinal()].index];
		}
		return selectedPid;
	}

	private void trySelectPid(long selectedPid) {
		for (int i = 0; i < tableModel.filteredData.length; i++) {
			if (selectedPid == (Long) tableModel.filteredData[i][headers[Columns.Pid.ordinal()].index]) {
				setRowSelectionInterval(i, i);
				return;
			}
		}
		clearSelection();
	}

	private void trySetData(Columns column, int row, Object data) {
		ColumnHeader header = headers[column.ordinal()];
		if (header != null) {
			tableModel.fullData[row][header.index] = data;
		}
	}

	private void trySetColor(Columns column, int row, Color color) {
		ColumnHeader header = headers[column.ordinal()];
		if (header != null) {
			tableModel.fullColor[row][header.index] = color;
		}
	}

	private Color selectColorCpu(double fraction) {
		if (fraction > 0.8)
			return Load.Extreme.color;
		if (fraction > 0.6)
			return Load.VeryLarge.color;
		if (fraction > 0.4)
			return Load.Large.color;
		if (fraction > 0.2)
			return Load.Medium.color;
		if (fraction > 0.003)
			return Load.Small.color;
		return Load.None.color;
	}

	private Color selectColorMemory(double fraction) {
		if (fraction > 0.2)
			return Load.Extreme.color;
		if (fraction > 0.1)
			return Load.VeryLarge.color;
		if (fraction > 0.05)
			return Load.Large.color;
		if (fraction > 0.03)
			return Load.Medium.color;
		if (fraction > 0.01)
			return Load.Small.color;
		return Load.None.color;
	}

	private Color selectColorDeath(float seconds) {
		float fraction = seconds / Integer.parseInt(Config.get(Config.KEY_DEAD_PROCESS_KEEP_TIME));
		if (fraction > 0.83)
			return Load.Extreme.color;
		if (fraction > 0.67)
			return Load.VeryLarge.color;
		if (fraction > 0.5)
			return Load.Large.color;
		if (fraction > 0.33)
			return Load.Medium.color;
		if (fraction > 0.17)
			return Load.Small.color;
		return Load.None.color;
	}


	public class CustomTableHeader extends JTableHeader {
		public CustomTableHeader(TableColumnModel model) {
			super(model);
		}

		@Override
		public void setDraggedColumn(TableColumn aColumn) {
			super.setDraggedColumn(aColumn);
			isMovingColumn = aColumn != null;
			if (!isMovingColumn) {
				List<String> order = new ArrayList<>();
				for (int i = 0; i < headers.length; i++) {
					if (headers[i] != null) {
						order.add(Integer.toString(getColumnIndex(headers[i].header)));
					}
				}
				if (showDeadProcesses) {
					Config.put(Config.KEY_LAST_DEAD_COLUMN_ORDER, String.join(";", order));
				} else {
					Config.put(Config.KEY_LAST_COLUMN_ORDER, String.join(";", order));
				}
			}
		}

		@Override
		public void setResizingColumn(TableColumn aColumn) {
			super.setResizingColumn(aColumn);
			isResizingColumn = aColumn != null;
			if (!isResizingColumn) {
				List<String> widths = new ArrayList<>();
				for (int i = 0; i < headers.length; i++) {
					if (headers[i] != null) {
						widths.add(Integer.toString(getColumnModel().getColumn(getColumnIndex(headers[i].header)).getWidth()));
					}
				}
				if (showDeadProcesses) {
					Config.put(Config.KEY_LAST_DEAD_COLUMN_WIDTHS, String.join(";", widths));
				} else {
					Config.put(Config.KEY_LAST_COLUMN_WIDTHS, String.join(";", widths));
				}
			}
		}
	}

	private int getColumnIndex(String header) {
		for (int i = 0; i < getColumnCount(); i++) {
			if (getColumnModel().getColumn(i).getHeaderValue().equals(header)) {
				return i;
			}
		}
		return -1;
	}

	public List<Columns> getVisibleColumns() {
		return visibleColumns;
	}

	public void setFilterAttribute(Columns column) {
		if (filterAttribute != column) {
			filterAttribute = column;

			long selectedPid = getSelectedPid();
			filter();
			trySelectPid(selectedPid);

			revalidate();
			repaint();
		}
	}

	public void filterBy(String text) {
		filterPhrase = text.toLowerCase();

		long selectedPid = getSelectedPid();
		filter();
		trySelectPid(selectedPid);

		revalidate();
		repaint();
	}

	private void filter() {
		if (filterPhrase.isEmpty()) {
			tableModel.filteredColor = tableModel.fullColor;
			tableModel.filteredData = tableModel.fullData;
		} else {
			int filterColumn = headers[filterAttribute.ordinal()].index;
			List<Object[]> dataRows = new ArrayList<>();
			List<Color[]> colorRows = new ArrayList<>();
			for (int i = 0; i < tableModel.fullData.length; i++) {
				if (tableModel.fullData[i][filterColumn].toString().toLowerCase().contains(filterPhrase)) {
					dataRows.add(tableModel.fullData[i]);
					colorRows.add(tableModel.fullColor[i]);
				}
			}

			tableModel.filteredData = dataRows.toArray(new Object[0][tableModel.columns.length]);
			tableModel.filteredColor = colorRows.toArray(new Color[0][tableModel.columns.length]);
		}
	}


	public class ProcessTableCellRenderer extends DefaultTableCellRenderer {
		public static final int CELL_PADDING = 8;

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
																	  int row, int column) {
			JComponent result = (JComponent) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

			int realColumn = 0;
			String name = table.getColumnName(column);
			for (ColumnHeader header : headers) {
				if (header != null && header.header.equals(name)) {
					realColumn = header.index;
					break;
				}
			}

			if (tableModel.filteredColor[row][realColumn] != null) {
				result.setBackground(tableModel.filteredColor[row][realColumn]);
			} else {
				result.setBackground(Color.WHITE);
			}

			if (isSelected) {
				Color selection = new Color(0, 160, 255, 50);
				result.setBackground(blend(result.getBackground(), selection));
			}

			ColumnHeader fileNameHeader = headers[Columns.FileName.ordinal()];
			if (showDeadProcesses && fileNameHeader != null && realColumn == fileNameHeader.index) {
				setStrikeThroughFontFor(result);
			}

			result.setBorder(new EmptyBorder(0, CELL_PADDING, 0, CELL_PADDING));
			return result;
		}

		private Color blend(Color c1, Color c2) {
			float a = c2.getAlpha() / 255f;
			return new Color((int) (c1.getRed() * (1 - a) + c2.getRed() * a),
					(int) (c1.getGreen() * (1 - a) + c2.getGreen() * a),
					(int) (c1.getBlue() * (1 - a) + c2.getBlue() * a));
		}

		private void setStrikeThroughFontFor(JComponent result) {
			Font font = result.getFont();
			Map attributes = font.getAttributes();
			attributes.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
			Font newFont = new Font(attributes);
			result.setFont(newFont);
		}
	}


	public static class CustomTableModel extends AbstractTableModel {
		public String[] columns;
		public Object[][] fullData;
		public Object[][] filteredData;
		public Color[][] fullColor;
		public Color[][] filteredColor;

		@Override
		public int getRowCount() {
			return filteredData.length;
		}

		@Override
		public int getColumnCount() {
			return columns.length;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			return filteredData[rowIndex][columnIndex];
		}

		@Override
		public String getColumnName(int column) {
			if (column >= 0)
				return columns[column];
			return "Undefined";
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return false;
		}
	}


	private static class ColumnHeader implements Comparable<ColumnHeader> {
		public String header;
		public int index;
		public ProcessComparator comparator;
		public boolean isSelected;

		public ColumnHeader(Columns column, int index) {
			this.header = column.name;
			this.index = index;
			this.comparator = column.comparator;
		}

		@Override
		public int compareTo(ColumnHeader o) {
			return index - o.index;
		}
	}


	private TableColumnModelListener columnListener = new TableColumnModelListener() {
		@Override
		public void columnSelectionChanged(ListSelectionEvent e) {
		}

		@Override
		public void columnRemoved(TableColumnModelEvent e) {
		}

		@Override
		public void columnMoved(TableColumnModelEvent e) {
		}

		@Override
		public void columnMarginChanged(ChangeEvent e) {
		}

		@Override
		public void columnAdded(TableColumnModelEvent e) {
		}
	};


	private KeyAdapter keyListener = new KeyAdapter() {
		private String search;
		private long lastClick;

		@Override
		public void keyTyped(KeyEvent e) {
			ColumnHeader fileNameHeader = headers[Columns.FileName.ordinal()];
			if (fileNameHeader != null) {
				long time = System.currentTimeMillis();
				if (time - lastClick > 1000)
					search = "";

				boolean isRepeatedSingleChar = search.length() == 1 && search.charAt(0) == e.getKeyChar();
				if (!isRepeatedSingleChar)
					search += e.getKeyChar();

				int startIndex = getSelectedRow();
				for (int i = (search.length() == 1) ? 1 : 0; i < getRowCount(); i++) {
					int idx = (startIndex + i) % getRowCount();
					String currentProcessName = (String) tableModel.filteredData[idx][fileNameHeader.index];
					if (currentProcessName.toLowerCase().startsWith(search.toLowerCase())) {
						setRowSelectionInterval(idx, idx);
						scrollRectToVisible(getCellRect(idx, getSelectedColumn(), true));
						break;
					}
				}

				lastClick = time;
			}
		}

		@Override
		public void keyPressed(KeyEvent e) {
			if (e.getKeyCode() == KeyEvent.VK_DELETE && !showDeadProcesses) {
				if (getSelectedRow() > -1) {
					long pid = (long) tableModel.filteredData[getSelectedRow()][headers[Columns.Pid.ordinal()].index];
					DeleteProcessMenuItem menuItem = new DeleteProcessMenuItem((Component) processCallback);
					menuItem.setProcess(systemInformation.getProcessById(pid));
					menuItem.doAction();
				}
			}
		}
	};


	private MouseAdapter mouseListener = new MouseAdapter() {
		@Override
		public void mouseClicked(MouseEvent e) {
			if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
				int row = rowAtPoint(e.getPoint());
				long pid = (long) tableModel.filteredData[row][headers[Columns.Pid.ordinal()].index];
				if (showDeadProcesses) {
					processCallback.openDialog(systemInformation.getDeadProcessById(pid));
				} else {
					processCallback.openDialog(systemInformation.getProcessById(pid));
				}
			}
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			int row = rowAtPoint(e.getPoint());
			int column = columnAtPoint(e.getPoint());

			String cellValue = getValueAt(row, column).toString();
			int cellWidth = getColumnModel().getColumn(column).getWidth();
			if (cellWidth <= metrics.stringWidth(cellValue) + ProcessTableCellRenderer.CELL_PADDING * 2) {
				final int maxTooltipWidth = 600;
				if (metrics.stringWidth(cellValue) > maxTooltipWidth) {
					StringBuilder sb = new StringBuilder();
					sb.append("<html>");
					int lastIndex = 0;
					for (int i = 0; i < cellValue.length(); i++) {
						if (metrics.stringWidth(cellValue.substring(lastIndex, i + 1)) > maxTooltipWidth) {
							sb.append(cellValue, lastIndex, i);
							sb.append("<br/>");
							lastIndex = i + 1;
						}
					}
					if (lastIndex < cellValue.length()) {
						sb.append(cellValue, lastIndex, cellValue.length());
					}
					sb.append("</html>");
					setToolTipText(sb.toString());
				} else {
					setToolTipText(cellValue);
				}
			} else {
				setToolTipText(null);
			}
		}
	};

	private PopupMenuListener popupListener = new PopupMenuListener() {
		@Override
		public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
			SwingUtilities.invokeLater(() -> {
				int rowAtPoint = rowAtPoint(SwingUtilities.convertPoint(contextMenu, new Point(0, 0), ProcessTable.this));
				if (rowAtPoint > -1) {
					setRowSelectionInterval(rowAtPoint, rowAtPoint);
					Long pid = (Long) tableModel.filteredData[rowAtPoint][headers[Columns.Pid.ordinal()].index];
					if (showDeadProcesses) {
						contextMenu.setProcess(systemInformation.getDeadProcessById(pid));
					} else {
						contextMenu.setProcess(systemInformation.getProcessById(pid));
					}
				} else {
					((Component) e.getSource()).setVisible(false);
				}
			});
		}

		@Override
		public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
		}

		@Override
		public void popupMenuCanceled(PopupMenuEvent e) {
		}
	};
}
