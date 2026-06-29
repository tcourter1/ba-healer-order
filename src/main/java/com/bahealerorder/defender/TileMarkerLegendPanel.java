package com.bahealerorder.defender;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;

final class TileMarkerLegendPanel
{
	private TileMarkerLegendPanel()
	{
	}

	static JPanel create(int width)
	{
		JPanel panel = verticalPanel();
		panel.add(label("Legend", true));
		panel.add(Box.createVerticalStrut(5));

		JPanel grid = new JPanel(new GridLayout(5, 2, 8, 2));
		grid.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		grid.setAlignmentX(JPanel.LEFT_ALIGNMENT);
		fixedSize(grid, width, 96);
		grid.add(row(width, DefenderTileMarkerMapPanel.TRAP_COLOR, "Trap"));
		grid.add(row(width, DefenderTileMarkerMapPanel.RANGER_CAVE_COLOR, "Ranger cave"));
		grid.add(row(width, DefenderTileMarkerMapPanel.HAMMER_COLOR, "Hammer"));
		grid.add(row(width, DefenderTileMarkerMapPanel.FIGHTER_CAVE_COLOR, "Fighter cave"));
		grid.add(row(width, DefenderTileMarkerMapPanel.LOGS_COLOR, "Logs"));
		grid.add(row(width, DefenderTileMarkerMapPanel.RUNNER_CAVE_COLOR, "Runner cave"));
		grid.add(row(width, DefenderTileMarkerMapPanel.START_TILE_COLOR, "Start"));
		grid.add(row(width, DefenderTileMarkerMapPanel.HEALER_CAVE_COLOR, "Healer Cave"));
		grid.add(row(width, DefenderTileMarkerMapPanel.DISABLED_TILE_COLOR, "Unavailable"));
		grid.add(row(width, DefenderTileMarkerMapPanel.DISPENSER_COLOR, "Dispensers"));
		panel.add(grid);
		return panel;
	}

	private static JPanel row(int width, Color color, String text)
	{
		JPanel row = new JPanel();
		row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
		row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		row.setBorder(new EmptyBorder(1, 0, 1, 0));
		row.setPreferredSize(new Dimension((width - 8) / 2, 18));
		row.setMaximumSize(new Dimension((width - 8) / 2, 18));
		row.setAlignmentX(JPanel.LEFT_ALIGNMENT);

		JPanel swatch = new JPanel();
		swatch.setBackground(color);
		fixedSize(swatch, 12, 12);
		row.add(swatch);
		row.add(Box.createHorizontalStrut(6));
		row.add(label(text, false));
		return row;
	}

	private static JLabel label(String text, boolean bold)
	{
		JLabel label = new JLabel(text);
		label.setForeground(ColorScheme.TEXT_COLOR);
		if (bold)
		{
			label.setFont(label.getFont().deriveFont(java.awt.Font.BOLD));
		}
		label.setAlignmentX(JLabel.LEFT_ALIGNMENT);
		return label;
	}

	private static JPanel verticalPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		return panel;
	}

	private static void fixedSize(java.awt.Component component, int width, int height)
	{
		Dimension size = new Dimension(width, height);
		component.setPreferredSize(size);
		component.setMinimumSize(size);
		component.setMaximumSize(size);
	}
}
