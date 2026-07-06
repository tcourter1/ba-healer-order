package com.bahealerorder.tilemarkers;

public enum TileMarkerExportType
{
	ASSIGNMENT_PRESET("preset"),
	STRATEGY_PRESET("wave strategy"),
	MARKER_SET("tile set"),
	STRATEGY_COLLECTION("wave strategy collection"),
	MARKER_SET_COLLECTION("tile marker set collection");

	private final String displayName;

	TileMarkerExportType(String displayName)
	{
		this.displayName = displayName;
	}

	public String getDisplayName()
	{
		return displayName;
	}
}
