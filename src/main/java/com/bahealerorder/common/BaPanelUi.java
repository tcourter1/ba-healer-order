package com.bahealerorder.common;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;

public final class BaPanelUi
{
	private BaPanelUi()
	{
	}

	public static JPanel section(String title, int contentWidth, int controlHeight)
	{
		JPanel panel = verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
		panel.setBorder(new EmptyBorder(8, 8, 8, 8));
		panel.setMaximumSize(new Dimension(contentWidth, Integer.MAX_VALUE));
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.add(centeredLabelRow(title, true, ColorScheme.DARKER_GRAY_COLOR, contentWidth, controlHeight));
		panel.add(Box.createVerticalStrut(6));
		return panel;
	}

	public static JPanel centeredLabelRow(String text, boolean bold, Color background, int contentWidth, int controlHeight)
	{
		JLabel label = label(text, bold);
		label.setHorizontalAlignment(SwingConstants.CENTER);

		JPanel row = new JPanel(new BorderLayout());
		row.setBackground(background);
		fixedSize(row, contentWidth - 16, controlHeight);
		row.add(label, BorderLayout.CENTER);
		return row;
	}

	public static JPanel comboRow(String text, JComboBox<?> comboBox, int contentWidth, int controlHeight, int labelWidth)
	{
		JLabel rowLabel = label(text);
		fixedSize(rowLabel, labelWidth, controlHeight);

		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		fixedSize(row, contentWidth - 16, controlHeight);
		row.add(rowLabel, BorderLayout.WEST);
		row.add(comboBox, BorderLayout.CENTER);
		return row;
	}

	public static JLabel label(String text)
	{
		return label(text, false);
	}

	public static JLabel label(String text, boolean bold)
	{
		JLabel label = new JLabel(text);
		label.setForeground(ColorScheme.TEXT_COLOR);
		label.setFont(bold ? FontManager.getRunescapeBoldFont() : FontManager.getRunescapeSmallFont());
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		return label;
	}

	public static JButton action(String text, Runnable runnable, int width, int height)
	{
		JButton button = new JButton(text);
		button.addActionListener(event -> runnable.run());
		fixedSize(button, width, height);
		return button;
	}

	public static JPanel horizontalActionRow(int width, int height)
	{
		JPanel panel = new JPanel(new DynamicGridLayout(1, 2, 6, 0));
		panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		fixedSize(panel, width, height);
		return panel;
	}

	public static JPanel verticalPanel(Color background)
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBackground(background);
		return panel;
	}

	public static JScrollPane wrapTextArea(JTextArea area, int width, int height)
	{
		JScrollPane scrollPane = new JScrollPane(area);
		scrollPane.setBorder(javax.swing.BorderFactory.createLineBorder(ColorScheme.DARK_GRAY_COLOR));
		fixedSize(scrollPane, width, height);
		return scrollPane;
	}

	public static void styleCombo(JComboBox<?> comboBox, int width, int height)
	{
		comboBox.setFocusable(false);
		fixedSize(comboBox, width, height);
	}

	public static void styleTextArea(JTextArea area, int rows)
	{
		area.setRows(rows);
		area.setLineWrap(true);
		area.setWrapStyleWord(true);
	}

	public static void addTextChangeListener(JTextComponent component, Runnable runnable)
	{
		component.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent event)
			{
				runnable.run();
			}

			@Override
			public void removeUpdate(DocumentEvent event)
			{
				runnable.run();
			}

			@Override
			public void changedUpdate(DocumentEvent event)
			{
				runnable.run();
			}
		});
	}

	public static void fixedSize(JComponent component, int width, int height)
	{
		Dimension size = new Dimension(width, height);
		component.setPreferredSize(size);
		component.setMinimumSize(size);
		component.setMaximumSize(size);
		component.setAlignmentX(Component.LEFT_ALIGNMENT);
	}

	public static void selectComboValue(JComboBox<ComboOption> comboBox, String id)
	{
		for (int i = 0; i < comboBox.getItemCount(); i++)
		{
			ComboOption item = comboBox.getItemAt(i);

			if ((id == null && item.id == null) || (id != null && id.equals(item.id)))
			{
				comboBox.setSelectedIndex(i);
				return;
			}
		}

		if (comboBox.getItemCount() > 0)
		{
			comboBox.setSelectedIndex(0);
		}
	}

	public static void setComboItems(JComboBox<ComboOption> comboBox, List<ComboOption> items, String selectedId)
	{
		comboBox.setModel(new DefaultComboBoxModel<>(items.toArray(new ComboOption[0])));
		selectComboValue(comboBox, selectedId);
	}

	public static String selectedId(JComboBox<ComboOption> comboBox)
	{
		ComboOption item = (ComboOption) comboBox.getSelectedItem();
		return item == null ? null : item.getId();
	}

	public static class ComboOption
	{
		private final String id;
		private final String label;

		public ComboOption(String id, String label)
		{
			this.id = id;
			this.label = label;
		}

		public String getId()
		{
			return id;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}
}
