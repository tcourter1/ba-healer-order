package com.bahealerorder.common;

import java.awt.Color;

public final class TileMarkerStyle
{
	public static final Color DEFAULT_MARKER_COLOR = new Color(80, 170, 255);
	public static final int MIN_OPACITY_PERCENT = 0;
	public static final int MAX_OPACITY_PERCENT = 100;
	public static final float MIN_BORDER_WIDTH = 0f;
	public static final float MAX_BORDER_WIDTH = 8f;

	private TileMarkerStyle()
	{
	}

	public static Color parseColor(String color, Color fallback)
	{
		try
		{
			return color == null || color.trim().isEmpty() ? fallback : Color.decode(color);
		}
		catch (RuntimeException ex)
		{
			return fallback;
		}
	}

	public static Color withOpacity(Color color, int opacityPercent)
	{
		Color value = color == null ? DEFAULT_MARKER_COLOR : color;
		return new Color(value.getRed(), value.getGreen(), value.getBlue(), alphaFromOpacity(opacityPercent));
	}

	public static int alphaFromOpacity(int opacityPercent)
	{
		return Math.round(255 * clampOpacityPercent(opacityPercent) / 100f);
	}

	public static int clampOpacityPercent(int opacityPercent)
	{
		return Math.max(MIN_OPACITY_PERCENT, Math.min(MAX_OPACITY_PERCENT, opacityPercent));
	}

	public static float clampBorderWidth(float borderWidth)
	{
		return Math.max(MIN_BORDER_WIDTH, Math.min(MAX_BORDER_WIDTH, borderWidth));
	}

	public static Color readableTextColor(Color background)
	{
		Color value = background == null ? DEFAULT_MARKER_COLOR : background;
		return (value.getRed() * 299 + value.getGreen() * 587 + value.getBlue() * 114) / 1000 > 140
				? Color.BLACK
				: Color.WHITE;
	}

	public static String toHex(Color color)
	{
		Color value = color == null ? DEFAULT_MARKER_COLOR : color;
		return String.format("#%02x%02x%02x", value.getRed(), value.getGreen(), value.getBlue());
	}
}
