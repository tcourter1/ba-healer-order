package com.bahealerorder.tilemarkers;

import java.util.Arrays;
import java.util.List;

final class TileMarkerBuiltInSets
{
	private static final int WAVES_1_TO_9_REGION = 7509;
	private static final int WAVE_10_REGION = 7508;
	private static final String SET_ID_PREFIX = "built-in:tile-marker-set:";
	private static final String BEGINNER_COLOR = "#50aaff";
	private static final String BUILT_IN_MARKER_COLOR = "#77e2ff";
	private static final String HENDI_COLOR = "#59d4fd";
	private static final String ACTION_COLOR = "#81f777";
	private static final String AUK_FOOD_COLOR = "#78ff87";
	private static final int SOFT_OPACITY_PERCENT = 10;
	private static final float SOFT_BORDER_WIDTH = 0.5f;

	static final String BEGINNER_TILES_SET_ID = SET_ID_PREFIX + "beginner-tiles";
	static final String HENDI_TRIANGLE_SET_ID = SET_ID_PREFIX + "hendi-triangle";
	static final String SHIR_MAINSTACK_TRAIL_SET_ID = SET_ID_PREFIX + "shir-mainstack-trail";
	static final String HENKE_MAINSTACK_TRAIL_SET_ID = SET_ID_PREFIX + "henke-mainstack-trail";
	static final String N_TRAP_FOOD_SET_ID = SET_ID_PREFIX + "n-trap-food";
	static final String NW_TRAP_FOOD_SET_ID = SET_ID_PREFIX + "nw-trap-food";
	static final String CENTER_TRAP_FOOD_SET_ID = SET_ID_PREFIX + "center-trap-food";
	static final String E_TRAP_FOOD_SET_ID = SET_ID_PREFIX + "e-trap-food";
	static final String SAFETY_FOOD_SET_ID = SET_ID_PREFIX + "safety-food";
	static final String WEST_RELURE_FOOD_SET_ID = SET_ID_PREFIX + "west-relure-food";
	static final String WALL_SPLIT_MULTI_SET_ID = SET_ID_PREFIX + "wall-split-multi";
	static final String TWO_ONE_FIVE_SOFT_CRASH_SET_ID = SET_ID_PREFIX + "two-one-five-soft-crash";
	static final String FOUR_N_ONE_E_SET_ID = SET_ID_PREFIX + "four-n-one-e";
	static final String W10_BEGINNER_TILES_SET_ID = SET_ID_PREFIX + "w10-beginner-tiles";
	static final String AUK_W10_TILES_SET_ID = SET_ID_PREFIX + "auk-w10-tiles";

	private TileMarkerBuiltInSets()
	{
	}

	static List<TileMarkerSet> create()
	{
		return Arrays.asList(
				set("beginner-tiles", "Beginner Tiles",
						marker("beginner:trap-food", 45, 35, "Trap Food", "4 good", BEGINNER_COLOR),
						marker("beginner:safety-food", 45, 29, "Safety Food", "1 bad", BEGINNER_COLOR),
						marker("beginner:trail", 38, 40, "Trail", "1 bad", BEGINNER_COLOR),
						marker("beginner:mainstack", 35, 43, "Mainstack", "4 good", BEGINNER_COLOR),
						marker("beginner:west-relure-food", 29, 46, "West Relure Food", "1 bad", BEGINNER_COLOR)),
				set("hendi-triangle", "Hendi Triangle Tiles",
						softMarker("hendi:1", 42, 39, "Hendi 1", "1", HENDI_COLOR),
						softMarker("hendi:2", 46, 39, "Hendi 2", "2", HENDI_COLOR),
						softMarker("hendi:3", 46, 36, "Hendi 3", "3", HENDI_COLOR)),
				set("shir-mainstack-trail", "Shir",
						softMarker("shir:trail", 39, 39, "Trail", "", BUILT_IN_MARKER_COLOR),
						softMarker("shir:mainstack", 35, 43, "Mainstack", "", BUILT_IN_MARKER_COLOR)),
				set("henke-mainstack-trail", "Henke",
						softMarker("henke:trail", 38, 40, "Trail", "", BUILT_IN_MARKER_COLOR),
						softMarker("henke:mainstack", 36, 42, "Mainstack", "", BUILT_IN_MARKER_COLOR)),
				set("n-trap-food", "N Trap",
						softMarker("food:n-trap", 45, 35, "N Trap", "", BUILT_IN_MARKER_COLOR)),
				set("nw-trap-food", "NW Trap",
						softMarker("food:nw-trap", 44, 35, "NW Trap", "", BUILT_IN_MARKER_COLOR)),
				set("center-trap-food", "C Trap",
						softMarker("food:center-trap", 45, 34, "C Trap", "", BUILT_IN_MARKER_COLOR)),
				set("e-trap-food", "E Trap",
						softMarker("food:e-trap", 46, 34, "E Trap", "", BUILT_IN_MARKER_COLOR)),
				set("safety-food", "Safety",
						marker("food:safety", 45, 29, "Safety", "1 bad", BEGINNER_COLOR)),
				set("west-relure-food", "West Relure",
						marker("food:west-relure", 29, 46, "West Relure", "1 bad", BEGINNER_COLOR)),
				set("wall-split-multi", "E Multi",
						softMarker("wall-split:multi", 46, 34, "E Multi", "", ACTION_COLOR)),
				set("two-one-five-soft-crash", "4N",
						softMarker("two-one-five:soft-crash", 45, 38, "4N", "", ACTION_COLOR)),
				set("four-n-one-e", "4N1E",
						softMarker("four-n-one-e:tile", 46, 38, "4N1E", "", ACTION_COLOR)),
				set(TileMarkerMapMode.EAST_SIDE_ONLY, TileMarkerWaveMap.WAVE_10, "w10-beginner-tiles", "W10 Beginner Tiles",
						marker("w10-beginner:safety-food", WAVE_10_REGION, 39, 32, "Safety Food", "1 bad", BEGINNER_COLOR),
						marker("w10-beginner:trap-food", WAVE_10_REGION, 44, 35, "Trap Food", "4 good", BEGINNER_COLOR),
						marker("w10-beginner:trail", WAVE_10_REGION, 39, 40, "Trail", "1 bad", BEGINNER_COLOR),
						marker("w10-beginner:mainstack", WAVE_10_REGION, 38, 46, "Mainstack", "4 good", BEGINNER_COLOR)),
				set(TileMarkerMapMode.EAST_SIDE_ONLY, TileMarkerWaveMap.WAVE_10, "auk-w10-tiles", "Auk W10 Tiles",
						softMarker("auk:safety", WAVE_10_REGION, 39, 30, "Safety", "", BUILT_IN_MARKER_COLOR),
						softMarker("auk:nw-trap-food", WAVE_10_REGION, 44, 35, "NW Trap Food", "3", BUILT_IN_MARKER_COLOR),
						softMarker("auk:mainstack", WAVE_10_REGION, 37, 43, "Mainstack", "", BUILT_IN_MARKER_COLOR),
						softMarker("auk:trail", WAVE_10_REGION, 39, 41, "Trail", "", BUILT_IN_MARKER_COLOR),
						softMarker("auk:w-trap-food", WAVE_10_REGION, 44, 34, "W Trap Food", "3+1", AUK_FOOD_COLOR))
		);
	}

	private static TileMarkerSet set(String id, String name, TileMarker... markers)
	{
		return set(TileMarkerMapMode.EAST_SIDE_ONLY, TileMarkerWaveMap.WAVES_1_TO_9, id, name, markers);
	}

	private static TileMarkerSet set(
			TileMarkerMapMode mapMode,
			TileMarkerWaveMap waveMap,
			String id,
			String name,
			TileMarker... markers)
	{
		return new TileMarkerSet(
				SET_ID_PREFIX + id,
				name,
				mapMode,
				waveMap,
				Arrays.asList(markers),
				true
		);
	}

	private static TileMarker marker(
			String id,
			int regionX,
			int regionY,
			String name,
			String label,
			String color)
	{
		return marker(id, regionX, regionY, name, label, color, null, null);
	}

	private static TileMarker marker(
			String id,
			int regionId,
			int regionX,
			int regionY,
			String name,
			String label,
			String color)
	{
		return marker(id, regionId, regionX, regionY, name, label, color, null, null);
	}

	private static TileMarker softMarker(
			String id,
			int regionX,
			int regionY,
			String name,
			String label,
			String color)
	{
		return marker(id, regionX, regionY, name, label, color, SOFT_OPACITY_PERCENT, SOFT_BORDER_WIDTH);
	}

	private static TileMarker softMarker(
			String id,
			int regionId,
			int regionX,
			int regionY,
			String name,
			String label,
			String color)
	{
		return marker(id, regionId, regionX, regionY, name, label, color, SOFT_OPACITY_PERCENT, SOFT_BORDER_WIDTH);
	}

	private static TileMarker marker(
			String id,
			int regionX,
			int regionY,
			String name,
			String label,
			String color,
			Integer opacityPercent,
			Float borderWidth)
	{
		return new TileMarker(
				"built-in:marker:" + id,
				new TileMarkerTile(WAVES_1_TO_9_REGION, regionX, regionY, 0),
				name,
				label,
				color,
				opacityPercent,
				borderWidth
		);
	}

	private static TileMarker marker(
			String id,
			int regionId,
			int regionX,
			int regionY,
			String name,
			String label,
			String color,
			Integer opacityPercent,
			Float borderWidth)
	{
		return new TileMarker(
				"built-in:marker:" + id,
				new TileMarkerTile(regionId, regionX, regionY, 0),
				name,
				label,
				color,
				opacityPercent,
				borderWidth
		);
	}
}
