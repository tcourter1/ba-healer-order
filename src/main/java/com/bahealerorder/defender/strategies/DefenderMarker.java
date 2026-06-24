package com.bahealerorder.defender.strategies;

public class DefenderMarker
{
	public static final int DEFAULT_OPACITY_PERCENT = 22;
	public static final float DEFAULT_BORDER_WIDTH = 1.0f;

	private String id;
	private DefenderTile tile;
	private String name;
	private String label;
	private String color;
	private Integer opacityPercent;
	private Float borderWidth;

	public DefenderMarker()
	{
	}

	public DefenderMarker(String id, DefenderTile tile, String name, String label, String color)
	{
		this(id, tile, name, label, color, DEFAULT_OPACITY_PERCENT, DEFAULT_BORDER_WIDTH);
	}

	public DefenderMarker(String id, DefenderTile tile, String name, String label, String color, Integer opacityPercent, Float borderWidth)
	{
		this.id = id;
		this.tile = tile;
		this.name = name;
		this.label = label;
		this.color = color;
		this.opacityPercent = opacityPercent;
		this.borderWidth = borderWidth;
	}

	public String getId()
	{
		return id;
	}

	public void setId(String id)
	{
		this.id = id;
	}

	public DefenderTile getTile()
	{
		return tile;
	}

	public void setTile(DefenderTile tile)
	{
		this.tile = tile;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getLabel()
	{
		return label;
	}

	public void setLabel(String label)
	{
		this.label = label;
	}

	public String getColor()
	{
		return color;
	}

	public void setColor(String color)
	{
		this.color = color;
	}

	public Integer getOpacityPercent()
	{
		return opacityPercent;
	}

	public void setOpacityPercent(Integer opacityPercent)
	{
		this.opacityPercent = opacityPercent;
	}

	public Float getBorderWidth()
	{
		return borderWidth;
	}

	public void setBorderWidth(Float borderWidth)
	{
		this.borderWidth = borderWidth;
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
