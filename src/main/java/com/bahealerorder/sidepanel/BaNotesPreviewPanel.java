package com.bahealerorder.sidepanel;

import com.bahealerorder.tilemarkers.TimedStrategyNote;
import com.bahealerorder.tilemarkers.TimedStrategyNotes;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Insets;
import java.util.List;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

public final class BaNotesPreviewPanel
{
	private static final int CONTROL_HEIGHT = 24;

	private BaNotesPreviewPanel()
	{
	}

	public static JPanel create(String headingText, String notes, Color headingColor, int width, int currentWaveTick)
	{
		return create(headingText, notes, headingColor, width, currentWaveTick, 0, 0, CONTROL_HEIGHT);
	}

	public static JPanel create(String headingText, String notes, Color headingColor, int width, int currentWaveTick, int textTopPadding, int textBottomPadding)
	{
		return create(headingText, notes, headingColor, width, currentWaveTick, textTopPadding, textBottomPadding, 17);
	}

	private static JPanel create(String headingText, String notes, Color headingColor, int width, int currentWaveTick, int textTopPadding, int textBottomPadding, int lineHeight)
	{
		int notesHeight = getMessageHeight(notes, textTopPadding, textBottomPadding, lineHeight);
		int height = notesHeight + CONTROL_HEIGHT + 6;
		JPanel panel = BaPanelUi.verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
		BaPanelUi.fixedSize(panel, width, height);

		panel.add(notesHeadingRow(headingText, headingColor, width));
		panel.add(Box.createVerticalStrut(4));
		JTextPane notesText = createNotesText(notes, currentWaveTick, textTopPadding, textBottomPadding);
		BaPanelUi.fixedSize(notesText, width, notesHeight);
		panel.add(notesText);
		return panel;
	}

	private static JPanel notesHeadingRow(String headingText, Color headingColor, int width)
	{
		JLabel heading = new JLabel(headingText);
		heading.setForeground(headingColor);
		heading.setFont(FontManager.getRunescapeBoldFont());
		heading.setHorizontalAlignment(SwingConstants.CENTER);

		JPanel row = new JPanel(new BorderLayout());
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		BaPanelUi.fixedSize(row, width, CONTROL_HEIGHT);
		row.add(heading, BorderLayout.CENTER);
		return row;
	}

	private static JTextPane createNotesText(String notes, int currentWaveTick, int textTopPadding, int textBottomPadding)
	{
		JTextPane text = new JTextPane();
		text.setFont(FontManager.getRunescapeFont());
		text.setEditable(false);
		text.setFocusable(false);
		text.setOpaque(false);
		text.setAlignmentX(Component.LEFT_ALIGNMENT);
		text.setMargin(new Insets(textTopPadding, 0, textBottomPadding, 0));

		StyledDocument document = text.getStyledDocument();
		List<TimedStrategyNote> timedNotes = TimedStrategyNotes.parse(notes);
		int activeIndex = TimedStrategyNotes.getActiveTimedIndex(timedNotes, currentWaveTick);

		for (int i = 0; i < timedNotes.size(); i++)
		{
			TimedStrategyNote note = timedNotes.get(i);
			Color color = TimedStrategyNotes.colorFor(note, i, activeIndex, currentWaveTick, ColorScheme.TEXT_COLOR, Color.GRAY);
			appendStyledLine(document, note.getText().isEmpty() ? " " : note.getText(), color, i < timedNotes.size() - 1);
		}

		return text;
	}

	private static void appendStyledLine(StyledDocument document, String line, Color color, boolean addNewline)
	{
		SimpleAttributeSet attributes = new SimpleAttributeSet();
		StyleConstants.setForeground(attributes, color);

		try
		{
			document.insertString(document.getLength(), line + (addNewline ? "\n" : ""), attributes);
		}
		catch (BadLocationException ignored)
		{
			// JTextPane document positions are internal; failing here should not break the preview panel.
		}
	}

	private static int getMessageHeight(String text, int textTopPadding, int textBottomPadding, int lineHeight)
	{
		int lines = text == null || text.isEmpty() ? 1 : text.split("\\R", -1).length;
		return Math.max(CONTROL_HEIGHT * 2, textTopPadding + textBottomPadding + lineHeight * lines);
	}

}
