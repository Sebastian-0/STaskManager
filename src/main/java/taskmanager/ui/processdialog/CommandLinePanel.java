/*
 * Copyright (c) 2020. Sebastian Hjelm
 */

package taskmanager.ui.processdialog;

import taskmanager.Process;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;

public class CommandLinePanel extends JPanel {
    public CommandLinePanel(Process process) {
        setBorder(new TitledBorder("Command line"));
        setLayout(new BorderLayout());

        JTextArea text = new JTextArea(process.commandLine, 10, 30);
        text.setEditable(false);
        text.setLineWrap(true);

        JScrollPane scrollPane = new JScrollPane(text,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);
    }
}
