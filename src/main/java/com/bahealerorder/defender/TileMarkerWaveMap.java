package com.bahealerorder.defender;

import com.bahealerorder.defender.strategies.DefenderMapLayout;

public enum TileMarkerWaveMap
{
	WAVES_1_TO_9("Wave 1-9", DefenderMapLayout.WAVES_1_TO_9),
	WAVE_10("Wave 10", DefenderMapLayout.WAVE_10);

	private final String displayName;
	private final DefenderMapLayout layout;

	TileMarkerWaveMap(String displayName, DefenderMapLayout layout)
	{
		this.displayName = displayName;
		this.layout = layout;
	}

	public String getDisplayName()
	{
		return displayName;
	}

	public DefenderMapLayout getLayout()
	{
		return layout;
	}

	public static TileMarkerWaveMap fromWave(int wave)
	{
		return wave == 10 ? WAVE_10 : WAVES_1_TO_9;
	}

	public static TileMarkerWaveMap fromName(String name)
	{
		if (name == null)
		{
			return WAVES_1_TO_9;
		}

		for (TileMarkerWaveMap map : values())
		{
			if (map.name().equals(name))
			{
				return map;
			}
		}

		throw new IllegalArgumentException("Unknown tile marker wave map: " + name);
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
