package com.bahealerorder.sidepanel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;

public final class BaPanelUi
{
	public static final Color ACTION_CONTROL_BORDER_COLOR = new Color(48, 48, 48);
	public static final Color ACTION_CONTROL_TEXT_COLOR = new Color(210, 210, 210);

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
		styleActionButton(button, width, height);
		return button;
	}

	public static void styleActionButton(AbstractButton button)
	{
		button.setForeground(ACTION_CONTROL_TEXT_COLOR);
		button.setBorder(BorderFactory.createLineBorder(ACTION_CONTROL_BORDER_COLOR));
		button.setFocusable(false);
	}

	public static void styleActionButton(AbstractButton button, int width, int height)
	{
		styleActionButton(button);
		fixedSize(button, width, height);
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
		comboBox.setForeground(ACTION_CONTROL_TEXT_COLOR);
		comboBox.setBorder(BorderFactory.createLineBorder(ACTION_CONTROL_BORDER_COLOR));
		comboBox.setFocusable(false);
		fixedSize(comboBox, width, height);
	}

	public static <T> JComboBox<T> fixedPopupWidthCombo(int popupWidth)
	{
		return new FixedPopupWidthComboBox<>(popupWidth);
	}

	public static DefaultListCellRenderer comboOptionRenderer(int controlHeight)
	{
		return new ComboOptionRenderer(controlHeight);
	}

	public static void styleTextInput(JTextComponent component, int width, int height)
	{
		component.setForeground(ACTION_CONTROL_TEXT_COLOR);
		component.setDisabledTextColor(ColorScheme.MEDIUM_GRAY_COLOR);
		component.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(ACTION_CONTROL_BORDER_COLOR),
				BorderFactory.createEmptyBorder(0, 6, 0, 6)
		));
		fixedSize(component, width, height);
	}

	public static void styleSpinner(JSpinner spinner, int width, int height)
	{
		spinner.setForeground(ACTION_CONTROL_TEXT_COLOR);
		spinner.setBorder(BorderFactory.createLineBorder(ACTION_CONTROL_BORDER_COLOR));
		if (spinner.getEditor() instanceof JSpinner.DefaultEditor)
		{
			JTextComponent textField = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
			textField.setForeground(ACTION_CONTROL_TEXT_COLOR);
			textField.setDisabledTextColor(ColorScheme.MEDIUM_GRAY_COLOR);
			textField.setBorder(BorderFactory.createEmptyBorder());
		}
		fixedSize(spinner, width, height);
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

	public static String selectedId(JComboBox<ComboOption> comboBox)
	{
		ComboOption item = (ComboOption) comboBox.getSelectedItem();
		return item == null ? null : item.getId();
	}

	@AllArgsConstructor
	@Getter
	public static class ComboOption
	{
		private final String id;
		private final String label;
		private final boolean builtIn;

		public ComboOption(String id, String label)
		{
			this(id, label, false);
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	private static class ComboOptionRenderer extends DefaultListCellRenderer
	{
		private final int controlHeight;

		@Override
		public Component getListCellRendererComponent(
				JList<?> list,
				Object value,
				int index,
				boolean isSelected,
				boolean cellHasFocus)
		{
			JLabel label = (JLabel) super.getListCellRendererComponent(
					list,
					value,
					index,
					isSelected,
					cellHasFocus
			);
			label.setPreferredSize(new Dimension(label.getPreferredSize().width, controlHeight));
			if (!isSelected)
			{
				label.setForeground(value instanceof ComboOption && ((ComboOption) value).isBuiltIn()
						? new Color(120, 120, 120)
						: ACTION_CONTROL_TEXT_COLOR);
			}
			return label;
		}
	}

	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	private static class FixedPopupWidthComboBox<T> extends JComboBox<T>
	{
		private final int popupWidth;
		private boolean layingOut;

		@Override
		public void doLayout()
		{
			try
			{
				layingOut = true;
				super.doLayout();
			}
			finally
			{
				layingOut = false;
			}
		}

		@Override
		public Dimension getSize()
		{
			Dimension size = super.getSize();
			if (!layingOut)
			{
				size.width = Math.max(size.width, popupWidth);
			}
			return size;
		}
	}
}
