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

package config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class Config {
	private static final Logger LOGGER = LoggerFactory.getLogger(Config.class);

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

	public static final String KEY_ALWAYS_ON_TOP = "alwaysOnTop";
	public static final String KEY_MINIMIZE_TO_TRAY = "minimizeToTray";
	public static final String KEY_TRAY_GRAPH = "trayGraph";

	public static final String KEY_UPDATE_RATE = "updateRate";
	public static final String KEY_MAXIMUM_MEASURMENT_BUFFER_SIZE = "maximumMeasurementBufferSize";
	public static final String KEY_GRAPH_MAX_PIXELS_PER_SEGMENT = "maxPixelsPerSegment";
	public static final String KEY_METRIC_TOP_LIST_SIZE = "metricTopListSize";

	public static final String KEY_LINK_TIMELINES = "linkTimelines";

	public static final String KEY_SHOW_PROCESSES_FOR_ALL_USERS = "showProcessesForAllUsers";
	public static final String KEY_LAST_DEFAULT_FILTER_ATTRIBUTE = "lastDefaultFilterAttribute";

	private static final Properties PROPERTIES;

	static {
		PROPERTIES = new Properties();
		loadConfig();
	}

	public static String get(String configKey) {
		return PROPERTIES.getProperty(configKey, "");
	}

	public static String get(String configKey, String defaultValue) {
		return PROPERTIES.getProperty(configKey, defaultValue);
	}

	public static int getInt(String configKey) {
		return Integer.parseInt(get(configKey));
	}

	public static int getInt(String configKey, int defaultValue) {
		return PROPERTIES.containsKey(configKey) ? getInt(configKey) : defaultValue;
	}

	public static float getFloat(String configKey) {
		return Float.parseFloat(get(configKey));
	}

	public static float getFloat(String configKey, float defaultValue) {
		return PROPERTIES.containsKey(configKey) ? getFloat(configKey) : defaultValue;
	}

	public static boolean getBoolean(String configKey) {
		return Boolean.parseBoolean(get(configKey));
	}

	public static boolean getBoolean(String configKey, boolean defaultValue) {
		return PROPERTIES.containsKey(configKey) ? getBoolean(configKey) : defaultValue;
	}

	public static void put(String configKey, String value) {
		PROPERTIES.put(configKey, value);
		saveConfig();
	}

	private static void loadConfig() {
		generateDefaultValues();

		File config = new File(CONFIG_FILE);
		if (config.exists()) {
			try (FileInputStream in = new FileInputStream(config)) {
				PROPERTIES.load(in);
			} catch (IOException e) {
				LOGGER.warn("Failed to load the config", e);
			}
		} else {
			// Save default settings
			saveConfig();
		}
	}

	private static void generateDefaultValues() {
		PROPERTIES.put(KEY_USE_DEBUG_MODE, "false");
		PROPERTIES.put(KEY_DEAD_PROCESS_KEEP_TIME, "1800");
		PROPERTIES.put(KEY_SHOW_DEAD_PROCESSES, "true");
		PROPERTIES.put(KEY_LAST_SELECTION_INVERTED, "false");
		PROPERTIES.put(KEY_LAST_DEAD_SELECTION_INVERTED, "true");
		PROPERTIES.put(KEY_UPDATE_RATE, "1.0");
		PROPERTIES.put(KEY_MAXIMUM_MEASURMENT_BUFFER_SIZE, "3600");
		PROPERTIES.put(KEY_GRAPH_MAX_PIXELS_PER_SEGMENT, "2");
		PROPERTIES.put(KEY_METRIC_TOP_LIST_SIZE, "3");
		PROPERTIES.put(KEY_LINK_TIMELINES, "true");
		PROPERTIES.put(KEY_ALWAYS_ON_TOP, "false");
		PROPERTIES.put(KEY_MINIMIZE_TO_TRAY, "false");
		PROPERTIES.put(KEY_TRAY_GRAPH, "");
		PROPERTIES.put(KEY_SHOW_PROCESSES_FOR_ALL_USERS, "true");
	}

	private static void saveConfig() {
		try (FileOutputStream out = new FileOutputStream(new File(CONFIG_FILE))) {
			PROPERTIES.store(out, "Taskmanager ini-file. Do not change manually\n");
		} catch (IOException e) {
			LOGGER.warn("Failed to save the config", e);
		}
	}
}