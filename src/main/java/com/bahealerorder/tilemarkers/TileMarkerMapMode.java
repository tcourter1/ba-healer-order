package com.bahealerorder.tilemarkers;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TileMarkerMapMode
{
	FULL_MAP("Full Map", true),
	EAST_SIDE_ONLY("East Side Only", false);

	private final String displayName;
	private final boolean fullArena;

	public static TileMarkerMapMode fromName(String name)
	{
		if (name == null) return FULL_MAP;

		for (TileMarkerMapMode mode : values())
		{
			if (mode.name().equals(name)) return mode;
		}

		throw new IllegalArgumentException("Unknown tile marker map mode: " + name);
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
