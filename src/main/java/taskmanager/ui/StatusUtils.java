package taskmanager.ui;

import taskmanager.data.Status;

import java.awt.Color;

public class StatusUtils { // TODO Create an enum that mirrors the one in the data collector?
	public static String name(Status status) {
		switch (status) {
			case Running:
			case Sleeping:
				return "Running";
			case Waiting:
				return "Disk sleep";
			case Zombie:
				return "Zombie";
			case Suspended:
				return "Suspended";
			case Dead:
				return "Dead";
		}
		return "Unknown";
	}

	public static String letter(Status status) {
		switch (status) {
			case Running:
			case Sleeping:
				return "R";
			case Waiting:
				return "W";
			case Zombie:
				return "Z";
			case Suspended:
				return "S";
			case Dead:
				return "D";
		}
		return "U";
	}

	public static Color color(Status status) {
		switch (status) {
			case Running:
			case Sleeping:
				return new Color(25, 160, 35);
			case Waiting:
				return new Color(0, 70, 227);
			case Zombie:
				return new Color(140, 0, 204);
			case Suspended:
				return new Color(134, 140, 112);
			case Dead:
				return new Color(180, 0, 0);
		}
		return Color.WHITE;
	}
}
