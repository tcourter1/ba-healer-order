package com.bahealerorder.common;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;
import net.runelite.client.ui.ColorScheme;

public final class BaIcons
{
	private BaIcons()
	{
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
}
