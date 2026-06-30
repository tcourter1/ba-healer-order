package com.bahealerorder.tilemarkers;

public class TileMarkerWaveSelectionTarget
{
	private String type;
	private String id;

	public TileMarkerWaveSelectionTarget()
	{
	}

	public TileMarkerWaveSelectionTarget(TileMarkerWaveSelectionTarget source)
	{
		this(source == null ? null : source.getType(), source == null ? null : source.getId());
	}

	private TileMarkerWaveSelectionTarget(TileMarkerWaveSelectionType type, String id)
	{
		setType(type);
		this.id = id;
	}

	public static TileMarkerWaveSelectionTarget strategyPreset(String id)
	{
		return new TileMarkerWaveSelectionTarget(TileMarkerWaveSelectionType.STRATEGY_PRESET, id);
	}

	public static TileMarkerWaveSelectionTarget markerSet(String id)
	{
		return new TileMarkerWaveSelectionTarget(TileMarkerWaveSelectionType.MARKER_SET, id);
	}

	public TileMarkerWaveSelectionType getType()
	{
		return type == null ? null : TileMarkerWaveSelectionType.valueOf(type);
	}

	public void setType(TileMarkerWaveSelectionType type)
	{
		this.type = type == null ? null : type.name();
	}

	public String getId()
	{
		return id;
	}

	public void setId(String id)
	{
		this.id = id;
	}

	public boolean isEmpty()
	{
		return getType() == null || id == null || id.trim().isEmpty();
	}
}
