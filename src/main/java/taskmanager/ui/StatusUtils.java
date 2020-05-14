package taskmanager.ui;

import taskmanager.data.Status;

public class StatusUtils { // TODO We will need colors too, create an enum that mirrors the one in the data collector?
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
				return "DEAD";
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
}
