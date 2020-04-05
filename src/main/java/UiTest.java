/*
 * Copyright (c) 2020. Sebastian Hjelm
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
