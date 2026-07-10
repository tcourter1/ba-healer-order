package com.bahealerorder.sidepanel.tilemarkers;

import com.bahealerorder.tilemarkers.TileMarkerMapLayout;


final class BaArenaMapTopology
{
	static final int WIDTH = 64;
	static final int HEIGHT = 48;

	// Rows are indexed directly by BA map y coordinate, from south (0) to north (47).
	private static final long[] USABLE_TILES_WAVES_1_TO_9 = {
			0x0000000000000000L, 0x0000000000000000L, 0x0000000000000000L, 0x0000000000000000L,
			0x0000000000000000L, 0x0000000000000000L, 0x00000001f2000000L, 0x0000003ff3000000L,
			0x000c3fffffc00000L, 0x001ffffffffc0000L, 0x000ffffffffe0000L, 0x001ffffffffe0000L,
			0x0019ffffffff0000L, 0x001a3fffffff8000L, 0x00047fffffff8000L, 0x0000ffffffff8000L,
			0x0001ffffffffc000L, 0x0001ffffffffe000L, 0x0001ffffffffe000L, 0x0001ffffffffe000L,
			0x0001ffffffffe000L, 0x0001ffffffffe000L, 0x0001ffffffffe000L, 0x0001ffffffffe000L,
			0x0001fbbfff77c000L, 0x0001ffffffffc000L, 0x0000fcffff9fc000L, 0x00007bbfff77e000L,
			0x00007ffffffff000L, 0x00007ffffffff000L, 0x00007fffffffe000L, 0x0000ffffffffc000L,
			0x0000ffffffffc000L, 0x00007fffffffc000L, 0x00003fffffffc000L, 0x00003fffffff8000L,
			0x00003fffffff0000L, 0x00001fffffff0000L, 0x00000ffffffe0000L, 0x0000003fff800000L,
			0x00000000e0000000L, 0x0000000000000000L, 0x0000000000000000L, 0x0000000000000000L,
			0x0000000000000000L, 0x0000000000000000L, 0x0000000000000000L, 0x0000000000000000L,
	};

	private static final long[] USABLE_TILES_WAVE_10 = {
			0x0000000000000000L, 0x0000000000000000L, 0x0000000000000000L, 0x0000000000000000L,
			0x0000000000000000L, 0x0000000000000000L, 0x00000001f2000000L, 0x0000003ff3000000L,
			0x000c3fffffc00000L, 0x001ffd2fff7a0000L, 0x000fff3fffff0000L, 0x001fffffffee8000L,
			0x0019feffffff8000L, 0x001a2ffffffdc000L, 0x00047fffffff2000L, 0x0000ffffffff2000L,
			0x0001efffffffe000L, 0x000337ffffff6000L, 0x00073ffffffff800L, 0x0005ffffffffe400L,
			0x0007bfffffffe400L, 0x000fffffffffdc00L, 0x0013fffffffff800L, 0x0013fffffffffe00L,
			0x000dffffff77f200L, 0x000ffffffffff200L, 0x000bffffff9fdc00L, 0x000fffffff77ec00L,
			0x0004fffffffffc00L, 0x0004ffffffffb800L, 0x0003dfffffffe000L, 0x00017fffffff3000L,
			0x00019fffffff2000L, 0x00009ffffffbc000L, 0x00007fffffffc000L, 0x00005ffffffec000L,
			0x00003fffffff8000L, 0x00001fffffff0000L, 0x00000ffffffe0000L, 0x0000003fff800000L,
			0x00000000e0000000L, 0x0000000000000000L, 0x0000000000000000L, 0x0000000000000000L,
			0x0000000000000000L, 0x0000000000000000L, 0x0000000000000000L, 0x0000000000000000L,
	};

	private BaArenaMapTopology()
	{
	}

	static boolean contains(int mapX, int mapY)
	{
		return mapX >= 0 && mapX < WIDTH && mapY >= 0 && mapY < HEIGHT;
	}

	static boolean isUsableTile(TileMarkerMapLayout layout, int mapX, int mapY)
	{
		if (!contains(mapX, mapY)) return false;

		long[] rows = layout == TileMarkerMapLayout.WAVE_10 ? USABLE_TILES_WAVE_10 : USABLE_TILES_WAVES_1_TO_9;
		return ((rows[mapY] >>> mapX) & 1L) != 0;
	}
}
