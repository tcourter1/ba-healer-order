package com.bahealerorder.common;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
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

	public static ImageIcon copyIcon()
	{
		int size = 14;
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setColor(ColorScheme.TEXT_COLOR);
		graphics.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		graphics.drawRoundRect(5, 2, 7, 8, 2, 2);
		graphics.drawRoundRect(2, 5, 7, 7, 2, 2);
		graphics.dispose();
		return new ImageIcon(image);
	}

	public static ImageIcon pasteIcon()
	{
		int size = 14;
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setColor(ColorScheme.TEXT_COLOR);
		graphics.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		graphics.drawRoundRect(3, 4, 8, 8, 2, 2);
		graphics.drawLine(5, 2, 9, 2);
		graphics.drawLine(5, 2, 5, 5);
		graphics.drawLine(9, 2, 9, 5);
		graphics.drawLine(5, 5, 9, 5);
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

	public static ImageIcon infoIcon()
	{
		int size = 14;
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setColor(ColorScheme.TEXT_COLOR);
		graphics.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		graphics.drawOval(2, 2, 10, 10);
		graphics.drawLine(7, 6, 7, 10);
		graphics.fillOval(6, 4, 2, 2);
		graphics.dispose();
		return new ImageIcon(image);
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
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setColor(ColorScheme.TEXT_COLOR);
		graphics.setStroke(new BasicStroke(Math.max(1.3f, size / 16f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

		int margin = Math.max(2, size / 8);
		int diameter = size - margin * 2;
		int x = margin;
		int y = margin;
		graphics.drawOval(x, y, diameter, diameter);
		graphics.drawOval(x + diameter / 4, y, diameter / 2, diameter);
		graphics.drawLine(x, y + diameter / 2, x + diameter, y + diameter / 2);
		graphics.drawArc(x + 2, y + diameter / 5, diameter - 4, diameter / 2, 0, 180);
		graphics.drawArc(x + 2, y + diameter - diameter / 5 - diameter / 2, diameter - 4, diameter / 2, 180, 180);
		graphics.dispose();
		return new ImageIcon(image);
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
