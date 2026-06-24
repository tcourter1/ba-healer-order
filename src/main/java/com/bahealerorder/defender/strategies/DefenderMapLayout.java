package com.bahealerorder.defender.strategies;

public enum DefenderMapLayout
{
	WAVES_1_TO_9("W1-9", 7509),
	WAVE_10("W10", 7508);

	private static final int WIDTH = 64;
	private static final int HEIGHT = 48;
	private static final int REGION_Y_OFFSET = 8;

	private final String name;
	private final int regionId;

	DefenderMapLayout(String name, int regionId)
	{
		this.name = name;
		this.regionId = regionId;
	}

	public DefenderTile toTile(int mapX, int mapY)
	{
		return new DefenderTile(regionId, mapX, mapY + REGION_Y_OFFSET, 0);
	}

	public int toMapX(DefenderTile tile)
	{
		return tile == null ? -1 : tile.getRegionX();
	}

	public int toMapY(DefenderTile tile)
	{
		return tile == null ? -1 : tile.getRegionY() - REGION_Y_OFFSET;
	}

	public boolean contains(DefenderTile tile)
	{
		if (tile == null || tile.getRegionId() != regionId)
		{
			return false;
		}

		int x = toMapX(tile);
		int y = toMapY(tile);
		return x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT;
	}

	public static DefenderMapLayout forWave(int wave)
	{
		return wave == 10 ? WAVE_10 : WAVES_1_TO_9;
	}

	@Override
	public String toString()
	{
		return name;
	}
}
