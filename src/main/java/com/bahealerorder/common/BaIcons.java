package com.bahealerorder.common;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.ImageUtil;

public final class BaIcons
{
	private static final ImageIcon EDIT_ICON = loadIcon("edit_icon.png");
	private static final ImageIcon EDIT_HOVER_ICON = loadHoverIcon("edit_icon.png");
	private static final ImageIcon VISIBLE_ICON = loadIcon("visible_icon.png");
	private static final ImageIcon VISIBLE_HOVER_ICON = loadHoverIcon("visible_icon.png");
	private static final ImageIcon INFO_ICON = loadIcon("info_icon.png");
	private static final ImageIcon GLOBE_ICON = loadIcon("globe_icon.png");
	private static final ImageIcon WAVE_OVERVIEW_ICON = loadIcon("wave_overview_icon.png");
	private static final ImageIcon PLUS_ICON = createPlusIcon(ColorScheme.TEXT_COLOR);
	private static final ImageIcon PLUS_HOVER_ICON = createPlusIcon(Color.WHITE);
	private static final ImageIcon CHECKBOX_ICON = createCheckboxIcon(false);
	private static final ImageIcon CHECKBOX_SELECTED_ICON = createCheckboxIcon(true);

	private BaIcons()
	{
	}

	private static ImageIcon loadIcon(String path)
	{
		return new ImageIcon(ImageUtil.loadImageResource(BaIcons.class, path));
	}

	private static ImageIcon loadHoverIcon(String path)
	{
		return new ImageIcon(ImageUtil.alphaOffset(ImageUtil.loadImageResource(BaIcons.class, path), -100));
	}

	private static ImageIcon scaledIcon(ImageIcon icon, int size)
	{
		return new ImageIcon(icon.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH));
	}

	public static ImageIcon trashIcon()
	{
		int size = 14;
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setColor(ColorScheme.TEXT_COLOR);
		graphics.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		graphics.drawLine(5, 3, 9, 3);
		graphics.drawLine(6, 2, 8, 2);
		graphics.drawLine(3, 5, 11, 5);
		graphics.drawRoundRect(4, 6, 6, 6, 2, 2);
		graphics.drawLine(6, 8, 6, 10);
		graphics.drawLine(8, 8, 8, 10);
		graphics.dispose();
		return new ImageIcon(image);
	}

	public static ImageIcon pencilIcon()
	{
		return EDIT_ICON;
	}

	public static ImageIcon pencilHoverIcon()
	{
		return EDIT_HOVER_ICON;
	}

	public static ImageIcon eyeIcon()
	{
		return VISIBLE_ICON;
	}

	public static ImageIcon eyeHoverIcon()
	{
		return VISIBLE_HOVER_ICON;
	}

	public static ImageIcon plusIcon()
	{
		return PLUS_ICON;
	}

	public static ImageIcon plusHoverIcon()
	{
		return PLUS_HOVER_ICON;
	}

	public static ImageIcon checkboxIcon()
	{
		return CHECKBOX_ICON;
	}

	public static ImageIcon checkboxSelectedIcon()
	{
		return CHECKBOX_SELECTED_ICON;
	}

	private static ImageIcon createPlusIcon(Color color)
	{
		int size = 16;
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setColor(color);
		graphics.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		graphics.drawLine(8, 4, 8, 12);
		graphics.drawLine(4, 8, 12, 8);
		graphics.dispose();
		return new ImageIcon(image);
	}

	private static ImageIcon createCheckboxIcon(boolean selected)
	{
		int size = 18;
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
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
		graphics.dispose();
		return new ImageIcon(image);
	}

	public static ImageIcon closeIcon()
	{
		int size = 12;
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setColor(ColorScheme.TEXT_COLOR);
		graphics.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		graphics.drawLine(3, 3, 9, 9);
		graphics.drawLine(9, 3, 3, 9);
		graphics.dispose();
		return new ImageIcon(image);
	}

	public static ImageIcon infoIcon()
	{
		return INFO_ICON;
	}

	public static ImageIcon popoutIcon()
	{
		int size = 14;
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setColor(ColorScheme.TEXT_COLOR);
		graphics.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		graphics.drawRoundRect(1, 5, 8, 8, 2, 2);
		graphics.drawLine(6, 1, 12, 1);
		graphics.drawLine(12, 1, 12, 7);
		graphics.drawLine(6, 7, 12, 1);
		graphics.dispose();
		return new ImageIcon(image);
	}

	public static ImageIcon globeIcon(int size)
	{
		return scaledIcon(GLOBE_ICON, size);
	}

	public static ImageIcon waveOverviewIcon(int size)
	{
		return scaledIcon(WAVE_OVERVIEW_ICON, size);
	}

	public static ImageIcon tileMarkerIcon(int size)
	{
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
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
		graphics.dispose();
		return new ImageIcon(image);
	}
}
