package taskmanager.ui;

import java.awt.*;

public class ColorUtils {
	public static Color blend(Color a, Color b, float alpha) {
		return new Color((int)(a.getRed() * alpha + b.getRed() * (1 - alpha)),
				(int)(a.getGreen() * alpha + b.getGreen() * (1 - alpha)),
				(int)(a.getBlue() * alpha + b.getBlue() * (1 - alpha)));
	}
}
