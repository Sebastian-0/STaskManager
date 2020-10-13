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

package taskmanager.ui;

import config.Config;

import java.awt.FontMetrics;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TextUtils {
	private static final String[] PREFIXES = {"", "K", "M", "G", "T", "P"};

	private static final DecimalFormatSymbols FORMAT_SYMBOLS = new DecimalFormatSymbols();
	private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("###,###.#");

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd: HH:mm:ss");

	static {
		FORMAT_SYMBOLS.setGroupingSeparator(' ');
		DECIMAL_FORMAT.setDecimalFormatSymbols(FORMAT_SYMBOLS);
	}

	public enum ValueType {
		Percentage,
		Bytes,
		BytesPerSecond,
		Bits,
		BitsPerSecond,
		Millis,
		TimeFull,
		Time,
		Date,
		Temperature,
		Raw
	}

	public static String valueToString(long value, ValueType type) {
		if (type == ValueType.Percentage) {
			return String.format("%.1f%%", 100 * value / (double) Config.DOUBLE_TO_LONG);
		} else if (type == ValueType.Bytes) {
			return bytesToString(value);
		} else if (type == ValueType.BytesPerSecond) {
			return bytesToString(value) + "/s";
		} else if (type == ValueType.Bits) {
			return bitsToString(value);
		} else if (type == ValueType.BitsPerSecond) {
			return bitsToString(value) + "ps";
		} else if (type == ValueType.Millis) {
			return formattedNumber(value) + "ms";
		} else if (type == ValueType.TimeFull) {
			value /= 1000;
			long seconds = value % 60;
			long minutes = (value / 60) % 60;
			long hours = ((value / 60) / 60) % 24;
			long days = ((value / 60) / 60) / 24;
			if (days > 0) {
				return String.format("%02d:%02d:%02d:%02d", days, hours, minutes, seconds);
			}
			return String.format("%02d:%02d:%02d", hours, minutes, seconds);
		} else if (type == ValueType.Time) {
			double trueValue = value / (double) Config.DOUBLE_TO_LONG;
			if (trueValue > 60*60*24) {
				return formattedNumber(trueValue / (60 * 60 * 24)) + " d";
			} else if (trueValue > 60*60) {
				return formattedNumber(trueValue / (60 * 60)) + " h";
			} else if (trueValue > 60) {
				return formattedNumber(trueValue / 60) + " m";
			} else {
				return formattedNumber(trueValue) + " s";
			}
		} else if (type == ValueType.Date) {
			return DATE_FORMAT.format(new Date(value));
		} else if (type == ValueType.Temperature) {
			return formattedNumber(value) + " C";
		} else if (type == ValueType.Raw) {
			return formattedNumber(value);
		}
		throw new UnsupportedOperationException("Not implemented for: " + type);
	}

	public static String ratioToString(long value1, long value2, ValueType type) {
		if (type == ValueType.Bytes) {
			int factor = Math.max(getFactor(value1), getFactor(value2));
			return applyFactor(value1, factor, 1) + " / " + applyFactor(value2, factor, 1) + " " + PREFIXES[factor] + "B";
		} else if (type == ValueType.Raw) {
			return formattedNumber(value1) + " / " + formattedNumber(value2);
		}
		throw new UnsupportedOperationException("Not implemented for: " + type);
	}


	public static String bytesToString(long valueInBytes) {
		return bytesToString(valueInBytes, 1);
	}

	public static String bytesToString(long valueInBytes, int decimals) {
		int factor = getFactor(valueInBytes);
		return applyFactor(valueInBytes, factor, decimals) + " " + PREFIXES[factor] + "B";
	}

	public static String bitsToString(long valueInBytes) {
		return bitsToString(valueInBytes, 1);
	}

	public static String bitsToString(long valueInBytes, int decimals) {
		valueInBytes *= 8;
		int factor = getFactor(valueInBytes);
		return applyFactor(valueInBytes, factor, decimals) + " " + PREFIXES[factor] + "b";
	}

	private static int getFactor(long value) {
		int factor = 0;
		while (value > 1024) {
			value /= 1024;
			factor++;
		}

		return factor;
	}

	private static String applyFactor(long value, int factor, int decimals) {
		for (int i = 0; i < factor - 1; i++) {
			value /= 1024;
		}
		if (factor > 0) {
			return String.format("%." + decimals + "f", value / 1024f);
		}
		return formattedNumber(value);
	}

	private static String formattedNumber(long value) {
		return DECIMAL_FORMAT.format(value);
	}

	private static String formattedNumber(double value) {
		return DECIMAL_FORMAT.format(value);
	}


	public static String limitWidth(String text, int maxWidth, FontMetrics metrics) {
		if (metrics.stringWidth(text) < maxWidth) {
			return text;
		}

		for (int i = text.length()-1; i >= 0; i--) {
			if (metrics.stringWidth(text.substring(0, i) + "...") < maxWidth) {
				return text.substring(0, i) + "...";
			}
		}

		return "...";
	}
}