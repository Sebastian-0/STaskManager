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

import javax.swing.JFrame;
import javax.swing.JTextPane;
import javax.swing.WindowConstants;

public class UiTest {
	public static void main(String[] args) {
		JTextPane area = new JTextPane();
		area.setContentType("text/html");
		area.setText("<html>Some <b>text</b></html>");

		JFrame frame = new JFrame();
		frame.add(area);

		frame.pack();
		frame.setVisible(true);
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
	}
}