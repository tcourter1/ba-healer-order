package com.bahealerorder.common;

import java.awt.Color;

public final class BaRoleColors
{
	public static final Color ATTACKER = new Color(0xff5a5a);
	public static final Color HEALER = new Color(0x00ff00);
	public static final Color DEFENDER = new Color(0x65a7ff);
	public static final Color COLLECTOR = new Color(0xffd84d);

	private BaRoleColors()
	{
	}

	public static Color color(BaRole role)
	{
		if (role == null) return null;

		switch (role)
		{
			case ATTACKER:
				return ATTACKER;
			case HEALER:
				return HEALER;
			case DEFENDER:
				return DEFENDER;
			case COLLECTOR:
				return COLLECTOR;
			default:
				return null;
		}
	}

	public static String htmlColor(BaRole role)
	{
		Color color = color(role);
		if (color == null) return null;

		return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
	}
}
