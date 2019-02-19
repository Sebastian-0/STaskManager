/*
 * Copyright (c) 2018 Sebastian Hjelm
 */

package taskmanager.ui.details;

import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.Arrays;

public class FilterTextField extends JTextField {
	private static final String FILTER_STRING = "Filter processes";
	private static final Color FILTER_COLOR = new Color(110, 110, 110);
	private boolean hasDefault;
	private Color defaultColor;

	private ProcessTable[] processTables;

	public FilterTextField(ProcessTable... processTables) {
		super(FILTER_STRING, 30);
		this.processTables = processTables;
		hasDefault = true;
		defaultColor = getForeground();
		setForeground(FILTER_COLOR);
		addFocusListener(focusListener);
		addActionListener(actionListener);
		getDocument().addDocumentListener(documentListener);
	}

	private FocusListener focusListener = new FocusListener() {
		@Override
		public void focusGained(FocusEvent e) {
			if (hasDefault) {
				setText("");
				setForeground(defaultColor);
			}
			hasDefault = false;
		}

		@Override
		public void focusLost(FocusEvent e) {
			if (getText().isEmpty()) {
				hasDefault = true;
				setText(FILTER_STRING);
				setForeground(FILTER_COLOR);
			}
		}
	};

	private DocumentListener documentListener = new DocumentListener() {
		@Override
		public void insertUpdate(DocumentEvent e) {
			update();
		}

		@Override
		public void removeUpdate(DocumentEvent e) {
			update();
		}

		@Override
		public void changedUpdate(DocumentEvent e) {
			update();
		}

		private void update() {
			if (!hasDefault) {
				Arrays.stream(processTables).forEach(table -> table.filterBy(getText()));
			}
		}
	};

	private ActionListener actionListener = e -> processTables[0].requestFocus();
}
