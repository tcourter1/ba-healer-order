package com.bahealerorder.tilemarkers;

import com.bahealerorder.common.BaIcons;
import com.bahealerorder.common.BaPanelUi;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;

class TileMarkerSetChecklistPanel extends JPanel
{
	private static final int CHECKBOX_ROW_HEIGHT = 30;
	private static final int COLUMN_GAP = 8;

	private final GeneralTileMarkerStrategyManager strategyManager;
	private final Runnable openMarkerEditor;
	private final Runnable selectionChanged;
	private final int contentWidth;
	private final int controlHeight;
	private final int listHeight;
	private final JPanel builtInMarkerSetPanel = BaPanelUi.verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
	private final JPanel customMarkerSetPanel = BaPanelUi.verticalPanel(ColorScheme.DARKER_GRAY_COLOR);
	private final Set<String> selectedMarkerSetIds = new LinkedHashSet<>();

	private TileMarkerWaveMap waveMap = TileMarkerWaveMap.WAVES_1_TO_9;

	TileMarkerSetChecklistPanel(
			GeneralTileMarkerStrategyManager strategyManager,
			Runnable openMarkerEditor,
			Runnable selectionChanged,
			int contentWidth,
			int controlHeight,
			int listHeight)
	{
		this.strategyManager = strategyManager;
		this.openMarkerEditor = openMarkerEditor;
		this.selectionChanged = selectionChanged;
		this.contentWidth = contentWidth;
		this.controlHeight = controlHeight;
		this.listHeight = listHeight;

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setAlignmentX(LEFT_ALIGNMENT);

		add(label("Tile Marker Sets", true));
		add(Box.createVerticalStrut(5));
		add(createMarkerSetColumns());
		refresh();
	}

	void setWaveMap(TileMarkerWaveMap waveMap)
	{
		this.waveMap = waveMap == null ? TileMarkerWaveMap.WAVES_1_TO_9 : waveMap;
		refresh();
	}

	void setSelectedMarkerSetIds(List<String> selectedIds)
	{
		selectedMarkerSetIds.clear();
		if (selectedIds != null)
		{
			selectedMarkerSetIds.addAll(selectedIds);
		}
		refresh();
	}

	List<String> getSelectedMarkerSetIds()
	{
		return new ArrayList<>(selectedMarkerSetIds);
	}

	void refresh()
	{
		builtInMarkerSetPanel.removeAll();
		customMarkerSetPanel.removeAll();
		int columnWidth = (contentWidth - COLUMN_GAP) / 2;

		for (TileMarkerSet set : strategyManager.getMarkerSets(waveMap))
		{
			JPanel targetPanel = set.isBuiltIn() ? builtInMarkerSetPanel : customMarkerSetPanel;
			targetPanel.add(createMarkerSetCheckBox(set, columnWidth));
		}

		if (builtInMarkerSetPanel.getComponentCount() == 0)
		{
			builtInMarkerSetPanel.add(emptyMessage("No commonly used tiles."));
		}

		if (customMarkerSetPanel.getComponentCount() == 0)
		{
			customMarkerSetPanel.add(emptyMessage("No custom tiles yet."));
		}

		builtInMarkerSetPanel.revalidate();
		builtInMarkerSetPanel.repaint();
		customMarkerSetPanel.revalidate();
		customMarkerSetPanel.repaint();
	}

	private JPanel createMarkerSetColumns()
	{
		JPanel columns = new JPanel(new GridLayout(1, 2, COLUMN_GAP, 0));
		columns.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		BaPanelUi.fixedSize(columns, contentWidth, listHeight);

		int columnWidth = (contentWidth - COLUMN_GAP) / 2;
		columns.add(createColumn("Commonly Used Tiles", builtInMarkerSetPanel, columnWidth, false));
		columns.add(createColumn("Custom Tiles", customMarkerSetPanel, columnWidth, true));
		return columns;
	}

	private JPanel createColumn(String title, JPanel listPanel, int columnWidth, boolean custom)
	{
		JPanel column = new JPanel(new BorderLayout(0, 4));
		column.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		BaPanelUi.fixedSize(column, columnWidth, listHeight);

		JLabel header = label(title, false);
		BaPanelUi.fixedSize(header, columnWidth, controlHeight);
		column.add(header, BorderLayout.NORTH);

		JScrollPane scrollPane = new JScrollPane(listPanel);
		scrollPane.setBorder(BorderFactory.createLineBorder(ColorScheme.DARK_GRAY_COLOR));
		scrollPane.getVerticalScrollBar().setUnitIncrement(CHECKBOX_ROW_HEIGHT);
		column.add(scrollPane, BorderLayout.CENTER);

		if (custom)
		{
			column.add(createMarkerSetLink(columnWidth), BorderLayout.SOUTH);
		}
		return column;
	}

	private JCheckBox createMarkerSetCheckBox(TileMarkerSet set, int columnWidth)
	{
		JCheckBox checkBox = new JCheckBox(set.toString(), selectedMarkerSetIds.contains(set.getId()));
		checkBox.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		checkBox.setForeground(ColorScheme.TEXT_COLOR);
		checkBox.setFocusable(false);
		checkBox.setIcon(BaIcons.checkboxIcon());
		checkBox.setSelectedIcon(BaIcons.checkboxSelectedIcon());
		checkBox.setBorder(new EmptyBorder(4, 3, 4, 3));
		BaPanelUi.fixedSize(checkBox, columnWidth - 20, CHECKBOX_ROW_HEIGHT);
		checkBox.addItemListener(event ->
		{
			if (checkBox.isSelected())
			{
				selectedMarkerSetIds.add(set.getId());
			}
			else
			{
				selectedMarkerSetIds.remove(set.getId());
			}

			if (selectionChanged != null)
			{
				selectionChanged.run();
			}
		});
		return checkBox;
	}

	private JButton createMarkerSetLink(int columnWidth)
	{
		JButton button = new JButton("<html><u>Create new custom tile markers...</u></html>");
		button.setHorizontalAlignment(JButton.LEFT);
		button.setForeground(ColorScheme.BRAND_ORANGE);
		button.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		button.setBorder(new EmptyBorder(2, 0, 0, 0));
		button.setContentAreaFilled(false);
		button.setFocusPainted(false);
		button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		button.addActionListener(event ->
		{
			if (openMarkerEditor != null)
			{
				openMarkerEditor.run();
			}
		});
		BaPanelUi.fixedSize(button, columnWidth, controlHeight + 4);
		return button;
	}

	private static JLabel emptyMessage(String text)
	{
		JLabel empty = label(text, false);
		empty.setBorder(new EmptyBorder(6, 6, 0, 0));
		return empty;
	}

	private static JLabel label(String text, boolean bold)
	{
		JLabel label = new JLabel(text);
		label.setForeground(ColorScheme.TEXT_COLOR);
		if (bold)
		{
			label.setFont(label.getFont().deriveFont(java.awt.Font.BOLD));
		}
		label.setAlignmentX(LEFT_ALIGNMENT);
		return label;
	}
}
