package com.bahealerorder.tilemarkers;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum TileMarkerMapLayout
{
	WAVES_1_TO_9("W1-9", 7509),
	WAVE_10("W10", 7508);

	private static final int WIDTH = 64;
	private static final int HEIGHT = 48;
	private static final int REGION_Y_OFFSET = 8;

	private final String name;
	private final int regionId;

	public TileMarkerTile toTile(int mapX, int mapY)
	{
		return new TileMarkerTile(regionId, mapX, mapY + REGION_Y_OFFSET, 0);
	}

	public int toMapX(TileMarkerTile tile)
	{
		return tile == null ? -1 : tile.getRegionX();
	}

	public int toMapY(TileMarkerTile tile)
	{
		return tile == null ? -1 : tile.getRegionY() - REGION_Y_OFFSET;
	}

	public boolean contains(TileMarkerTile tile)
	{
		if (tile == null || tile.getRegionId() != regionId) return false;

		int x = toMapX(tile);
		int y = toMapY(tile);
		return x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT;
	}

	@Override
	public String toString()
	{
		return name;
	}
}
