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

package taskmanager.ui.details.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import taskmanager.filter.Filter;
import taskmanager.filter.FilterCompiler;
import taskmanager.filter.FilterCompiler.CompiledFilter;
import taskmanager.filter.FilterCompiler.Highlight;
import taskmanager.filter.FilterCompiler.Tag;
import taskmanager.ui.details.ProcessTable;

import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import java.awt.Color;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.Arrays;

public class FilterTextField extends JTextField {
	private static final Logger LOGGER = LoggerFactory.getLogger(FilterTextField.class);

	private static final String FILTER_STRING = "Filter processes";
	private static final Color FILTER_COLOR = new Color(110, 110, 110);

	private final Color defaultColor;
	private boolean hasDefault;

	private final FilterCompiler filterCompiler;
	private Tag defaultTag;

	private ProcessTable[] processTables;

	public FilterTextField(ProcessTable... processTables) {
		super(FILTER_STRING, 30);
		this.processTables = processTables;
		hasDefault = true;
		defaultColor = getForeground();
		setBackground(null);
		setForeground(FILTER_COLOR);
		addFocusListener(focusListener);
		addActionListener(e -> processTables[0].requestFocus());
		getDocument().addDocumentListener(documentListener);

		filterCompiler = new FilterCompiler();
	}

	public void clear() {
		if (hasFocus()) {
			setText("");
		} else {
			hasDefault = true;
			setText(FILTER_STRING);
			setForeground(FILTER_COLOR);
		}
		requestFocus();
	}

	public void setDefaultTag(Tag tag) {
		defaultTag = tag;
		compileFilter();
	}

	private void compileFilter() {
		if (!hasDefault) {
			CompiledFilter compiledFilter = filterCompiler.compile(getText(), defaultTag);

			try {
				Highlighter highlighter = getHighlighter();
				highlighter.removeAllHighlights();
				for (Highlight highlight : compiledFilter.highlight) {
					highlighter.addHighlight(highlight.start, highlight.end, new DefaultHighlighter.DefaultHighlightPainter(highlight.color));
				}
			} catch (BadLocationException e) {
				LOGGER.error("Failed to apply highlight", e);
			}

			Arrays.stream(processTables).forEach(table -> table.setFilter(compiledFilter.filter));
		} else {
			Arrays.stream(processTables).forEach(table -> table.setFilter(Filter.UNIVERSE));
		}
	}

	private final FocusListener focusListener = new FocusListener() {
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

	private final DocumentListener documentListener = new DocumentListener() {
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
			compileFilter();
		}
	};
}