package com.bahealerorder.tilemarkers;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@Getter
@NoArgsConstructor
@Setter
public class TileMarker
{
	public static final int DEFAULT_OPACITY_PERCENT = 22;
	public static final float DEFAULT_BORDER_WIDTH = 1.0f;

	private String id;
	private TileMarkerTile tile;
	private String name;
	private String label;
	private String color;
	private Integer opacityPercent;
	private Float borderWidth;

	public TileMarker(String id, TileMarkerTile tile, String name, String label, String color)
	{
		this(id, tile, name, label, color, DEFAULT_OPACITY_PERCENT, DEFAULT_BORDER_WIDTH);
	}

	public int getOpacityPercentOrDefault()
	{
		return opacityPercent == null ? DEFAULT_OPACITY_PERCENT : opacityPercent;
	}

	public float getBorderWidthOrDefault()
	{
		return borderWidth == null ? DEFAULT_BORDER_WIDTH : borderWidth;
	}
}
