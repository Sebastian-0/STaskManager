package config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class Config {
	private static final String CONFIG_FILE = "config.ini";

	public static final int DOUBLE_TO_LONG = 1000;

	public static final String KEY_LAST_WINDOW_WIDTH = "lastWindowWidth";
	public static final String KEY_LAST_WINDOW_HEIGHT = "lastWindowHeight";
	public static final String KEY_LAST_EXTENDED_STATE = "lastExtendedState";

	public static final String KEY_LAST_COLUMN_ORDER = "lastColumnOrder";
	public static final String KEY_LAST_COLUMN_WIDTHS = "lastColumnWidths";
	public static final String KEY_LAST_COLUMN_SELECTION = "lastColumnSelection";
	public static final String KEY_LAST_SELECTION_INVERTED = "lastSelectionInverted";

	public static final String KEY_LAST_DEAD_COLUMN_ORDER = "lastDeadColumnOrder";
	public static final String KEY_LAST_DEAD_COLUMN_WIDTHS = "lastDeadColumnWidths";
	public static final String KEY_LAST_DEAD_COLUMN_SELECTION = "lastDeadColumnSelection";
	public static final String KEY_LAST_DEAD_SELECTION_INVERTED = "lastDeadSelectionInverted";

	public static final String KEY_USE_DEBUG_MODE = "useDebugMode";

	public static final String KEY_DEAD_PROCESS_KEEP_TIME = "deadProcessKeepTime";
	public static final String KEY_SHOW_DEAD_PROCESSES = "showDeadProcesses";

	public static final String KEY_MINIMIZE_TO_TRAY = "minimizeToTray";

	public static final String KEY_UPDATE_RATE = "updateRate";
	public static final String KEY_MAXIMUM_MEASURMENT_BUFFER_SIZE = "maximumMeasurementBufferSize";
	public static final String KEY_GRAPH_MAX_PIXELS_PER_SEGMENT = "maxPixelsPerSegment";

	public static final String KEY_LINK_TIMELINES = "linkTimelines";

	private static Properties properties;

	static {
		properties = new Properties();
		loadConfig();
	}

	public static String get(String configKey) {
		return properties.getProperty(configKey, "");
	}

	public static String get(String configKey, String defaultValue) {
		return properties.getProperty(configKey, defaultValue);
	}

	public static void put(String configKey, String value) {
		properties.put(configKey, value);
		saveConfig();
	}

	private static void loadConfig() {
		generateDefaultValues();

		File config = new File(CONFIG_FILE);
		if (config.exists()) {
			try {
				FileInputStream in = new FileInputStream(config);
				properties.load(in);
				in.close();
			} catch (IOException e) {
				System.out.println("Config <static>: Failed to load config: " + e.getMessage());
			}
		} else {
			// Save default settings
			saveConfig();
		}
	}

	private static void generateDefaultValues() {
		properties.put(KEY_USE_DEBUG_MODE, "false");
		properties.put(KEY_DEAD_PROCESS_KEEP_TIME, "30");
		properties.put(KEY_SHOW_DEAD_PROCESSES, "true");
		properties.put(KEY_LAST_SELECTION_INVERTED, "false");
		properties.put(KEY_LAST_DEAD_SELECTION_INVERTED, "false");
		properties.put(KEY_UPDATE_RATE, "1.0");
		properties.put(KEY_MAXIMUM_MEASURMENT_BUFFER_SIZE, "3600");
		properties.put(KEY_GRAPH_MAX_PIXELS_PER_SEGMENT, "2");
		properties.put(KEY_LINK_TIMELINES, "true");
		properties.put(KEY_MINIMIZE_TO_TRAY, "false");
	}

	private static void saveConfig() {
		try {
			FileOutputStream out = new FileOutputStream(new File(CONFIG_FILE));
			properties.store(out, "Taskmanager ini-file. Do not change manually\n");
			out.close();
		} catch (IOException e) {
			System.out.println("Config <static>: Failed to save config: " + e.getMessage());
		}
	}
}
