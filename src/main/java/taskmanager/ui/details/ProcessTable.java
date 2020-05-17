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

package taskmanager.ui.details;

import config.Config;
import taskmanager.data.Process;
import taskmanager.data.Process.ProcessComparator;
import taskmanager.data.Status;
import taskmanager.data.SystemInformation;
import taskmanager.filter.AndFilter;
import taskmanager.filter.Filter;
import taskmanager.filter.concrete.UserNameFilter;
import taskmanager.ui.ColorUtils;
import taskmanager.ui.StatusUtils;
import taskmanager.ui.TextUtils;
import taskmanager.ui.TextUtils.ValueType;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Point;
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
		// TODO For some reason both these constants and visibleColumns need to be in the same order to avoid problems.
		//      this enum determines the default order and visibleColumns determine the default widths
		FileName("Process name", 180, new Process.FileNameComparator()),
		Pid("PID", 65, new Process.IdComparator()),
		Status("Status", 30, new Process.StatusComparator()),
		DeathTime("Death time", 100, new Process.DeadTimestampsComparator()),
		UserName("User name", 75, new Process.UserNameComparator()),
		Cpu("CPU", 75, new Process.CpuUsageComparator()),
		PrivateWorkingSet("Memory", 100, new Process.PrivateWorkingSetComparator()),
		CommandLine("Command line", 200, new Process.CommandLineComparator()),
		Description("Description", 100, new Process.DescriptionComparator());

		public final String name;
		public final int defaultWidth;
		public final ProcessComparator comparator;

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

		final Color color;

		Load(Color c) {
			color = c;
		}
	}

	public enum Time {
		None(new Color(255, 255, 255)), // 0%
		VeryLittle(new Color(255, 229, 229)), // 17%
		Little(new Color(255, 196, 196)), // 33%
		Halfway(new Color(247, 168, 168)), // 50%
		Much(new Color(255, 135, 135)),   // 67%
		VeryMuch(new Color(255, 100, 100)); // 83%

		final Color color;

		Time(Color c) {
			color = c;
		}
	}

	private final List<Columns> visibleColumns;

	private final SystemInformation systemInformation;
	private final ProcessDetailsCallback processCallback;

	private final boolean showDeadProcesses;
	private final CustomTableModel tableModel;
	private ColumnHeader[] headers;
	private int[] tableColumnToDataColumn;
	private Process[] tableRowToProcess;

	private final FontMetrics metrics;

	private Filter filter;
	private boolean showProcessesForAllUsers;

	private boolean isMovingColumn;
	private boolean isResizingColumn;

	private final ProcessContextMenu contextMenu;

	public ProcessTable(ProcessDetailsCallback processDetailsCallback, SystemInformation systemInformation,
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
		visibleColumns.add(Columns.Status);
		visibleColumns.add(Columns.UserName);
		visibleColumns.add(Columns.Cpu);
		visibleColumns.add(Columns.PrivateWorkingSet);
		visibleColumns.add(Columns.CommandLine);
		visibleColumns.add(Columns.Description);


		tableModel = new CustomTableModel();
		tableModel.columns = loadHeaders();
		tableModel.data = new Object[0][tableModel.columns.length];
		tableModel.color = new Color[0][tableModel.columns.length];
		setModel(tableModel);

		ProcessTableCellRenderer cellRenderer = new ProcessTableCellRenderer();
		cellRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
		getColumnModel().getColumn(headers[Columns.Cpu.ordinal()].index).setCellRenderer(cellRenderer);
		getColumnModel().getColumn(headers[Columns.PrivateWorkingSet.ordinal()].index).setCellRenderer(cellRenderer);
		if (showDeadProcesses) {
			getColumnModel().getColumn(headers[Columns.DeathTime.ordinal()].index).setCellRenderer(cellRenderer);
		}
		setIntercellSpacing(new Dimension(1, 0));

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
								if (header.isSelected) {
									header.comparator.invert();
								}
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

		filter = Filter.UNIVERSE;
		showProcessesForAllUsers = Config.getBoolean(Config.KEY_SHOW_PROCESSES_FOR_ALL_USERS);

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
		tableColumnToDataColumn = new int[values.length];
		for (int i = 0, idx = 0; i < headers.length; i++) {
			Columns value = values[i];
			if (visibleColumns.contains(value)) {
				ColumnHeader header = new ColumnHeader(value, columnOrder[idx]);
				headers[i] = header;
				tmp[idx] = header;
				idx++;
			}
			tableColumnToDataColumn[i] = i;
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
		int selectedColumn = Config.getInt(Config.KEY_LAST_COLUMN_SELECTION, Columns.Pid.ordinal());
		boolean selectionInverted = Config.getBoolean(Config.KEY_LAST_SELECTION_INVERTED);
		if (showDeadProcesses) {
			selectedColumn = Config.getInt(Config.KEY_LAST_DEAD_COLUMN_SELECTION, Columns.DeathTime.ordinal());
			selectionInverted = Config.getBoolean(Config.KEY_LAST_DEAD_SELECTION_INVERTED);
		}
		headers[selectedColumn].isSelected = true;
		if (selectionInverted) {
			headers[selectedColumn].comparator.invert();
		}
		processCallback.setComparator(headers[selectedColumn].comparator, showDeadProcesses);
	}


	public void update() {
		List<Process> processes = systemInformation.processes;
		if (showDeadProcesses) {
			processes = systemInformation.deadProcesses;
		}

		processes = filter(processes);

		if (!isMovingColumn && !isResizingColumn) {
			long selectedPid = getSelectedPid();

			tableModel.data = new Object[processes.size()][tableModel.columns.length];
			tableModel.color = new Color[processes.size()][tableModel.columns.length];
			tableRowToProcess = new Process[processes.size()];

			DecimalFormatSymbols symbols = new DecimalFormatSymbols();
			symbols.setGroupingSeparator(' ');
			DecimalFormat dfCpu = new DecimalFormat("##0.0");
			DecimalFormat dfMemory = new DecimalFormat("###,###.##");
			dfMemory.setDecimalFormatSymbols(symbols);

			for (int i = 0; i < processes.size(); i++) {
				Process process = processes.get(i);
				tableRowToProcess[i] = process;
				trySetData(Columns.FileName, i, process.fileName);
				trySetData(Columns.Pid, i, process.id);
				trySetData(Columns.Status, i, StatusUtils.letter(process.status));
				trySetData(Columns.UserName, i, process.userName);
				trySetData(Columns.DeathTime, i, TextUtils.valueToString(
						(long) ((System.currentTimeMillis() - process.deathTimestamp) / 1000f * Config.DOUBLE_TO_LONG),
						ValueType.Time));
				trySetColor(Columns.DeathTime, i, selectColorDeath((System.currentTimeMillis() - process.deathTimestamp) / 1000f));
				if (showDeadProcesses) {
					trySetData(Columns.Cpu, i, "--.- %");
					trySetColor(Columns.Cpu, i, selectColorCpu(0));
					trySetData(Columns.PrivateWorkingSet, i, "--- --- K");
					trySetColor(Columns.PrivateWorkingSet, i, selectColorMemory(0));
				} else {
					double cpuUsage = process.cpuUsage.newest() / (double) Config.DOUBLE_TO_LONG;
					trySetData(Columns.Cpu, i, dfCpu.format(cpuUsage * 100) + " %");
					if (process.id == 0) {
						trySetColor(Columns.Cpu, i, selectColorCpu(0));
					} else {
						trySetColor(Columns.Cpu, i, selectColorCpu(cpuUsage));
					}
					trySetData(Columns.PrivateWorkingSet, i, dfMemory.format(process.privateWorkingSet.newest() / 1024) + " K");
					trySetColor(Columns.PrivateWorkingSet, i, selectColorMemory(process.privateWorkingSet.newest() / (double) systemInformation.physicalMemoryTotal));
				}
				trySetData(Columns.CommandLine, i, process.commandLine);
				trySetData(Columns.Description, i, process.description);
			}

			trySelectPid(selectedPid);

			revalidate();
			repaint();
		}
	}

	private List<Process> filter(List<Process> processes) {
		Filter actualFilter = filter;
		if (!showProcessesForAllUsers) {
			actualFilter = new AndFilter(actualFilter, new UserNameFilter(systemInformation.userName));
		}
		List<Process> result = new ArrayList<>();
		for (Process process : processes) {
			if (actualFilter.apply(process)) {
				result.add(process);
			}
		}
		return result;
	}

	private long getSelectedPid() {
		long selectedPid = -1;
		int selectedRow = getSelectedRow();
		if (selectedRow >= 0) {
			selectedPid = (Long) tableModel.data[selectedRow][headers[Columns.Pid.ordinal()].index];
		}
		return selectedPid;
	}

	private void trySelectPid(long selectedPid) {
		for (int i = 0; i < tableModel.data.length; i++) {
			if (selectedPid == (Long) tableModel.data[i][headers[Columns.Pid.ordinal()].index]) {
				setRowSelectionInterval(i, i);
				return;
			}
		}
		clearSelection();
	}

	private void trySetData(Columns column, int row, Object data) {
		ColumnHeader header = headers[column.ordinal()];
		if (header != null) {
			tableModel.data[row][header.index] = data;
		}
	}

	private void trySetColor(Columns column, int row, Color color) {
		ColumnHeader header = headers[column.ordinal()];
		if (header != null) {
			tableModel.color[row][header.index] = color;
		}
	}

	private Color selectColorCpu(double fraction) {
		if (fraction > 0.8) {
			return Load.Extreme.color;
		} else if (fraction > 0.6) {
			return Load.VeryLarge.color;
		} else if (fraction > 0.4) {
			return Load.Large.color;
		} else if (fraction > 0.2) {
			return Load.Medium.color;
		} else if (fraction > 0.003) {
			return Load.Small.color;
		}
		return Load.None.color;
	}

	private Color selectColorMemory(double fraction) {
		if (fraction > 0.2) {
			return Load.Extreme.color;
		} else if (fraction > 0.1) {
			return Load.VeryLarge.color;
		} else if (fraction > 0.05) {
			return Load.Large.color;
		} else if (fraction > 0.03) {
			return Load.Medium.color;
		} else if (fraction > 0.01) {
			return Load.Small.color;
		}
		return Load.None.color;
	}

	private Color selectColorDeath(float seconds) {
		float fraction = seconds / Config.getInt(Config.KEY_DEAD_PROCESS_KEEP_TIME);
		if (fraction > 0.83) {
			return Time.VeryMuch.color;
		} else if (fraction > 0.67) {
			return Time.Much.color;
		} else if (fraction > 0.5) {
			return Time.Halfway.color;
		} else if (fraction > 0.33) {
			return Time.Little.color;
		} else if (fraction > 0.17) {
			return Time.VeryLittle.color;
		}
		return Time.None.color;
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
				for (ColumnHeader header : headers) {
					if (header != null) {
						int index = getColumnIndex(header.header);
						order.add(Integer.toString(index));
						tableColumnToDataColumn[index] = header.index;
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
				for (ColumnHeader header : headers) {
					if (header != null) {
						widths.add(Integer.toString(getColumnModel().getColumn(getColumnIndex(header.header)).getWidth()));
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

	public void setFilter(Filter filter) {
		this.filter = filter;
		update();
	}

	public void setShowProcessesForAllUsers(boolean newState) {
		showProcessesForAllUsers = newState;
		update();
	}


	public class ProcessTableCellRenderer extends DefaultTableCellRenderer {
		public static final int CELL_PADDING = 8;

		public final Color defaultForeground = new Color(51, 51, 51);

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

			Color background;
			if (tableModel.color[row][realColumn] != null) {
				background = tableModel.color[row][realColumn];
			} else {
				if (row % 2 == 0) {
					background = Color.WHITE;
				} else {
					background = new Color(243, 243, 243);
				}
			}

			Color statusColor = StatusUtils.color(tableRowToProcess[row].status);
			if (tableRowToProcess[row].status == Status.Running || tableRowToProcess[row].status == Status.Sleeping) {
				statusColor = Color.WHITE; // We don't want this table to be filled with colored text as the default
			}

			if (name.equals(Columns.Status.name)) {
				result.setBackground(ColorUtils.blend(statusColor, background, 50f/255));
			} else {
				result.setBackground(background);
			}
			if (statusColor != Color.WHITE) {
				result.setForeground(statusColor);
			} else {
				result.setForeground(defaultForeground);
			}

			if (isSelected) {
				Color selection = new Color(0, 160, 255);
				result.setBackground(ColorUtils.blend(selection, result.getBackground(), 50f/255));
			}

			ColumnHeader fileNameHeader = headers[Columns.FileName.ordinal()];
			if (showDeadProcesses && fileNameHeader != null && realColumn == fileNameHeader.index) {
				setStrikeThroughFontFor(result);
			}

			result.setBorder(new EmptyBorder(0, CELL_PADDING, 0, CELL_PADDING));
			return result;
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
		public Object[][] data;
		public Color[][] color;

		@Override
		public int getRowCount() {
			return data.length;
		}

		@Override
		public int getColumnCount() {
			return columns.length;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			return data[rowIndex][columnIndex];
		}

		@Override
		public String getColumnName(int column) {
			if (column >= 0) {
				return columns[column];
			}
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

		@Override
		public int hashCode() {
			return index;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof ColumnHeader) {
				return compareTo((ColumnHeader) obj) == 0;
			}
			return false;
		}
	}


	private final KeyAdapter keyListener = new KeyAdapter() {
		private String search;
		private long lastClick;

		@Override
		public void keyTyped(KeyEvent e) {
			ColumnHeader fileNameHeader = headers[Columns.FileName.ordinal()];
			if (fileNameHeader != null) {
				long time = System.currentTimeMillis();
				if (time - lastClick > 1000) {
					search = "";
				}

				boolean isRepeatedSingleChar = search.length() == 1 && search.charAt(0) == e.getKeyChar();
				if (!isRepeatedSingleChar) {
					search += e.getKeyChar();
				}

				int startIndex = getSelectedRow();
				for (int i = (search.length() == 1) ? 1 : 0; i < getRowCount(); i++) {
					int idx = (startIndex + i) % getRowCount();
					String currentProcessName = (String) tableModel.data[idx][fileNameHeader.index];
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
					long pid = (long) tableModel.data[getSelectedRow()][headers[Columns.Pid.ordinal()].index];
					DeleteProcessMenuItem menuItem = new DeleteProcessMenuItem((Component) processCallback);
					menuItem.setProcess(systemInformation.getProcessById(pid));
					menuItem.doAction();
				}
			}
		}
	};


	private final MouseAdapter mouseListener = new MouseAdapter() {
		private int lastRowHover = -1;
		private int lastColumnHover = -1;

		@Override
		public void mouseClicked(MouseEvent e) {
			if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
				int row = rowAtPoint(e.getPoint());
				long pid = (long) tableModel.data[row][headers[Columns.Pid.ordinal()].index];
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

			if (lastRowHover != row || lastColumnHover != column) {
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
				lastColumnHover = column;
				lastRowHover = row;
			}
		}
	};

	private final PopupMenuListener popupListener = new PopupMenuListener() {
		@Override
		public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
			SwingUtilities.invokeLater(() -> {
				int rowAtPoint = rowAtPoint(SwingUtilities.convertPoint(contextMenu, new Point(0, 0), ProcessTable.this));
				int columnAtPoint = columnAtPoint(SwingUtilities.convertPoint(contextMenu, new Point(0, 0), ProcessTable.this));
				if (rowAtPoint > -1 && columnAtPoint > -1) {
					setRowSelectionInterval(rowAtPoint, rowAtPoint);
					Long pid = (Long) tableModel.data[rowAtPoint][headers[Columns.Pid.ordinal()].index];
					if (showDeadProcesses) {
						contextMenu.setProcess(systemInformation.getDeadProcessById(pid));
					} else {
						contextMenu.setProcess(systemInformation.getProcessById(pid));
					}

					int index = tableColumnToDataColumn[columnAtPoint];
					contextMenu.setCellText(tableModel.columns[index], tableModel.data[rowAtPoint][index].toString());
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