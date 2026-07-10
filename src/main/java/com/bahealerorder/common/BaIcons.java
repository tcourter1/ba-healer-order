package com.bahealerorder.common;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;
import javax.swing.ImageIcon;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.ImageUtil;

public final class BaIcons
{
	private static final ImageIcon EDIT_ICON = loadIcon("edit_icon.png");
	private static final ImageIcon VISIBLE_ICON = loadIcon("visible_icon.png");
	private static final ImageIcon INFO_ICON = loadIcon("info_icon.png");
	private static final ImageIcon GLOBE_ICON = loadIcon("globe_icon.png");
	private static final ImageIcon WAVE_OVERVIEW_ICON = loadIcon("wave_overview_icon.png");
	private static final ImageIcon HEALER_CODE_ICON = loadIcon("healer_code_icon.png");
	private static final ImageIcon TRASH_ICON = createTrashIcon();
	private static final ImageIcon BACK_ICON = createBackIcon();
	private static final ImageIcon CLOSE_ICON = createCloseIcon();
	private static final ImageIcon POPOUT_ICON = createPopoutIcon();
	private static final ImageIcon PLUS_ICON = createPlusIcon(Color.WHITE);
	private static final ImageIcon VERTICAL_ELLIPSIS_ICON = createVerticalEllipsisIcon();
	private static final ImageIcon IMPORT_ICON = loadIcon("import_icon.png");
	private static final ImageIcon EXPORT_ICON = loadIcon("export_icon.png");
	private static final ImageIcon CHECKBOX_ICON = createCheckboxIcon(false);
	private static final ImageIcon CHECKBOX_SELECTED_ICON = createCheckboxIcon(true);
	private static final ImageIcon CLOCK_ICON = createClockIcon();
	private static final ImageIcon RESET_ICON = scaledIcon(loadIcon("reset_icon.png"), 18);
	private static final ImageIcon SETTINGS_ICON = scaledIcon(loadIcon("settings_icon.png"), 22);
	private static final ImageIcon NOTES_ICON = createNotesIcon();
	private static final ImageIcon DISCORD_ICON = scaledIcon(loadIcon("discord_icon.png"), 16);
	private static final ImageIcon GITHUB_ICON = scaledIcon(loadIcon("github_icon.png"), 16);

	private BaIcons()
	{
	}

	private static ImageIcon loadIcon(String path)
	{
		return new ImageIcon(ImageUtil.loadImageResource(BaIcons.class, path));
	}

	private static ImageIcon scaledIcon(ImageIcon icon, int size)
	{
		return new ImageIcon(icon.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH));
	}

	private static ImageIcon drawIcon(int size, Consumer<Graphics2D> draw)
	{
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		draw.accept(graphics);
		graphics.dispose();
		return new ImageIcon(image);
	}

	private static ImageIcon createTrashIcon()
	{
		return drawIcon(14, graphics ->
		{
			graphics.setColor(ColorScheme.TEXT_COLOR);
			graphics.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			graphics.drawLine(5, 3, 9, 3);
			graphics.drawLine(6, 2, 8, 2);
			graphics.drawLine(3, 5, 11, 5);
			graphics.drawRoundRect(4, 6, 6, 6, 2, 2);
			graphics.drawLine(6, 8, 6, 10);
			graphics.drawLine(8, 8, 8, 10);
		});
	}

	public static ImageIcon trashIcon()
	{
		return TRASH_ICON;
	}

	public static ImageIcon pencilIcon()
	{
		return EDIT_ICON;
	}

	public static ImageIcon eyeIcon()
	{
		return VISIBLE_ICON;
	}

	public static ImageIcon plusIcon()
	{
		return PLUS_ICON;
	}

	public static ImageIcon verticalEllipsisIcon()
	{
		return VERTICAL_ELLIPSIS_ICON;
	}

	public static ImageIcon importIcon()
	{
		return IMPORT_ICON;
	}

	public static ImageIcon exportIcon()
	{
		return EXPORT_ICON;
	}

	public static ImageIcon checkboxIcon()
	{
		return CHECKBOX_ICON;
	}

	public static ImageIcon checkboxSelectedIcon()
	{
		return CHECKBOX_SELECTED_ICON;
	}

	public static ImageIcon clockIcon()
	{
		return CLOCK_ICON;
	}

	public static ImageIcon resetIcon()
	{
		return RESET_ICON;
	}

	public static ImageIcon backIcon()
	{
		return BACK_ICON;
	}

	private static ImageIcon createBackIcon()
	{
		return drawIcon(14, graphics ->
		{
			graphics.setColor(ColorScheme.TEXT_COLOR);
			graphics.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			graphics.drawLine(9, 3, 4, 7);
			graphics.drawLine(4, 7, 9, 11);
		});
	}

	public static ImageIcon settingsIcon()
	{
		return SETTINGS_ICON;
	}

	public static ImageIcon notesIcon()
	{
		return NOTES_ICON;
	}

	public static ImageIcon discordIcon()
	{
		return DISCORD_ICON;
	}

	public static ImageIcon githubIcon()
	{
		return GITHUB_ICON;
	}

	private static ImageIcon createPlusIcon(Color color)
	{
		return drawIcon(16, graphics ->
		{
			graphics.setColor(color);
			graphics.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			graphics.drawLine(8, 4, 8, 12);
			graphics.drawLine(4, 8, 12, 8);
		});
	}

	private static ImageIcon createCheckboxIcon(boolean selected)
	{
		return drawIcon(18, graphics ->
		{
			graphics.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			graphics.setColor(Color.WHITE);
			graphics.drawRoundRect(3, 3, 12, 12, 3, 3);
			if (selected)
			{
				graphics.setColor(ColorScheme.PROGRESS_COMPLETE_COLOR);
				graphics.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				graphics.drawLine(5, 9, 8, 12);
				graphics.drawLine(8, 12, 14, 5);
			}
		});
	}

	private static ImageIcon createVerticalEllipsisIcon()
	{
		return drawIcon(16, graphics ->
		{
			graphics.setColor(ColorScheme.TEXT_COLOR);
			graphics.fillOval(7, 3, 2, 2);
			graphics.fillOval(7, 7, 2, 2);
			graphics.fillOval(7, 11, 2, 2);
		});
	}

	private static ImageIcon createClockIcon()
	{
		return drawIcon(14, graphics ->
		{
			graphics.setColor(ColorScheme.TEXT_COLOR);
			graphics.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			graphics.drawOval(2, 2, 10, 10);
			graphics.drawLine(7, 7, 7, 4);
			graphics.drawLine(7, 7, 10, 8);
		});
	}

	private static ImageIcon createNotesIcon()
	{
		return drawIcon(16, graphics ->
		{
			graphics.setColor(ColorScheme.TEXT_COLOR);
			graphics.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			graphics.drawRoundRect(3, 2, 10, 12, 2, 2);
			graphics.drawLine(10, 2, 13, 5);
			graphics.drawLine(10, 2, 10, 5);
			graphics.drawLine(10, 5, 13, 5);
			graphics.drawLine(5, 7, 11, 7);
			graphics.drawLine(5, 10, 11, 10);
		});
	}

	public static ImageIcon closeIcon()
	{
		return CLOSE_ICON;
	}

	private static ImageIcon createCloseIcon()
	{
		return drawIcon(12, graphics ->
		{
			graphics.setColor(ColorScheme.TEXT_COLOR);
			graphics.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			graphics.drawLine(3, 3, 9, 9);
			graphics.drawLine(9, 3, 3, 9);
		});
	}

	public static ImageIcon infoIcon()
	{
		return INFO_ICON;
	}

	public static ImageIcon popoutIcon()
	{
		return POPOUT_ICON;
	}

	private static ImageIcon createPopoutIcon()
	{
		return drawIcon(14, graphics ->
		{
			graphics.setColor(ColorScheme.TEXT_COLOR);
			graphics.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			graphics.drawRoundRect(1, 5, 8, 8, 2, 2);
			graphics.drawLine(6, 1, 12, 1);
			graphics.drawLine(12, 1, 12, 7);
			graphics.drawLine(6, 7, 12, 1);
		});
	}

	public static ImageIcon globeIcon(int size)
	{
		return scaledIcon(GLOBE_ICON, size);
	}

	public static ImageIcon waveOverviewIcon(int size)
	{
		return scaledIcon(WAVE_OVERVIEW_ICON, size);
	}

	public static ImageIcon healerCodeIcon(int size)
	{
		return scaledIcon(HEALER_CODE_ICON, size);
	}

	public static ImageIcon tileMarkerIcon(int size)
	{
		return drawIcon(size, graphics ->
		{
			graphics.setColor(ColorScheme.TEXT_COLOR);
			graphics.setStroke(new BasicStroke(Math.max(1.4f, size / 14f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

			int margin = Math.max(3, size / 7);
			int cell = Math.max(4, (size - margin * 2) / 3);
			int gridSize = cell * 3;
			int start = (size - gridSize) / 2;

			for (int i = 0; i <= 3; i++)
			{
				int position = start + i * cell;
				graphics.drawLine(start, position, start + gridSize, position);
				graphics.drawLine(position, start, position, start + gridSize);
			}

			graphics.setColor(ColorScheme.PROGRESS_COMPLETE_COLOR);
			graphics.fillOval(start + cell + cell / 3, start + cell + cell / 3, Math.max(5, cell), Math.max(5, cell));
			graphics.setColor(ColorScheme.DARK_GRAY_COLOR);
			graphics.drawOval(start + cell + cell / 3, start + cell + cell / 3, Math.max(5, cell), Math.max(5, cell));
		});
	}
}
