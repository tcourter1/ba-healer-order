package com.bahealerorder.tilemarkers;

import com.bahealerorder.defender.TileMarkerMapMode;
import com.bahealerorder.defender.TileMarkerWaveMap;
import com.bahealerorder.defender.strategies.DefenderMarker;
import com.bahealerorder.defender.strategies.DefenderTile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class TileMarkerBuiltInSets
{
	private static final int WAVES_1_TO_9_REGION = 7509;
	private static final int WAVE_10_REGION = 7508;
	private static final String SET_ID_PREFIX = "built-in:tile-marker-set:";
	private static final String BEGINNER_COLOR = "#50aaff";
	private static final String DEFENDER_COLOR = "#77e2ff";
	private static final String HENDI_COLOR = "#59d4fd";
	private static final String ACTION_COLOR = "#81f777";
	private static final String AUK_FOOD_COLOR = "#78ff87";

	static final String BEGINNER_DEFENDER_TILES_SET_ID = SET_ID_PREFIX + "beginner-defender-tiles";
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
	static final String W10_BEGINNER_DEFENDER_TILES_SET_ID = SET_ID_PREFIX + "w10-beginner-defender-tiles";
	static final String AUK_W10_TILES_SET_ID = SET_ID_PREFIX + "auk-w10-tiles";

	private TileMarkerBuiltInSets()
	{
	}

	static List<TileMarkerSet> create()
	{
		return Arrays.asList(
				set("beginner-defender-tiles", "Beginner Defender Tiles",
						marker("beginner:trap-food", 45, 35, "Trap Food", "4 good", BEGINNER_COLOR, null, null),
						marker("beginner:safety-food", 45, 29, "Safety Food", "1 bad", BEGINNER_COLOR, null, null),
						marker("beginner:trail", 38, 40, "Trail", "1 bad", BEGINNER_COLOR, null, null),
						marker("beginner:mainstack", 35, 43, "Mainstack", "4 good", BEGINNER_COLOR, null, null),
						marker("beginner:west-relure-food", 29, 46, "West Relure Food", "1 bad", BEGINNER_COLOR, null, null)),
				set("hendi-triangle", "Hendi Triangle",
						marker("hendi:1", 42, 39, "Hendi 1", "1", HENDI_COLOR, 10, 0.5f),
						marker("hendi:2", 46, 39, "Hendi 2", "2", HENDI_COLOR, 10, 0.5f),
						marker("hendi:3", 46, 36, "Hendi 3", "3", HENDI_COLOR, 10, 0.5f)),
				set("shir-mainstack-trail", "Shir Mainstack/Trail",
						marker("shir:trail", 39, 39, "Trail", "", DEFENDER_COLOR, 10, 0.5f),
						marker("shir:mainstack", 35, 43, "Mainstack", "", DEFENDER_COLOR, 10, 0.5f)),
				set("henke-mainstack-trail", "Henke Mainstack/Trail",
						marker("henke:trail", 38, 40, "Trail", "", DEFENDER_COLOR, 10, 0.5f),
						marker("henke:mainstack", 36, 42, "Mainstack", "", DEFENDER_COLOR, 10, 0.5f)),
				set("n-trap-food", "N trap food",
						marker("food:n-trap", 45, 35, "N Trap Food", "", DEFENDER_COLOR, 10, 0.5f)),
				set("nw-trap-food", "NW trap food",
						marker("food:nw-trap", 44, 35, "NW Trap Food", "", DEFENDER_COLOR, 10, 0.5f)),
				set("center-trap-food", "Center Trap Food",
						marker("food:center-trap", 45, 34, "Center Trap Food", "", DEFENDER_COLOR, 10, 0.5f)),
				set("e-trap-food", "E trap food",
						marker("food:e-trap", 46, 34, "E Trap Food", "", DEFENDER_COLOR, 10, 0.5f)),
				set("safety-food", "Safety food",
						marker("food:safety", 45, 29, "Safety Food", "1 bad", BEGINNER_COLOR, null, null)),
				set("west-relure-food", "West relure food",
						marker("food:west-relure", 29, 46, "West Relure Food", "1 bad", BEGINNER_COLOR, null, null)),
				set("wall-split-multi", "54s Multi",
						marker("wall-split:multi", 46, 34, "54s Multi", "", ACTION_COLOR, 10, 0.5f)),
				set("two-one-five-soft-crash", "2-1-5*-2 Soft Crash",
						marker("two-one-five:soft-crash", 45, 38, "Soft crash", "", ACTION_COLOR, 10, 0.5f)),
				set(TileMarkerMapMode.EAST_SIDE_ONLY, TileMarkerWaveMap.WAVE_10, "w10-beginner-defender-tiles", "W10 Beginner Defender Tiles",
						marker("w10-beginner:safety-food", WAVE_10_REGION, 39, 32, "Safety Food", "1 bad", BEGINNER_COLOR, null, null),
						marker("w10-beginner:trap-food", WAVE_10_REGION, 44, 35, "Trap Food", "4 good", BEGINNER_COLOR, null, null),
						marker("w10-beginner:trail", WAVE_10_REGION, 39, 40, "Trail", "1 bad", BEGINNER_COLOR, null, null),
						marker("w10-beginner:mainstack", WAVE_10_REGION, 38, 46, "Mainstack", "4 good", BEGINNER_COLOR, null, null)),
				set(TileMarkerMapMode.EAST_SIDE_ONLY, TileMarkerWaveMap.WAVE_10, "auk-w10-tiles", "Auk W10 Tiles",
						marker("auk:safety", WAVE_10_REGION, 39, 30, "Safety", "", DEFENDER_COLOR, 10, 0.5f),
						marker("auk:nw-trap-food", WAVE_10_REGION, 44, 35, "NW Trap Food", "3", DEFENDER_COLOR, 10, 0.5f),
						marker("auk:mainstack", WAVE_10_REGION, 37, 43, "Mainstack", "", DEFENDER_COLOR, 10, 0.5f),
						marker("auk:trail", WAVE_10_REGION, 39, 41, "Trail", "", DEFENDER_COLOR, 10, 0.5f),
						marker("auk:w-trap-food", WAVE_10_REGION, 44, 34, "W Trap Food", "3+1", AUK_FOOD_COLOR, 10, 0.5f))
		);
	}

	private static TileMarkerSet set(String id, String name, DefenderMarker... markers)
	{
		return set(TileMarkerMapMode.EAST_SIDE_ONLY, TileMarkerWaveMap.WAVES_1_TO_9, id, name, markers);
	}

	private static TileMarkerSet set(
			TileMarkerMapMode mapMode,
			TileMarkerWaveMap waveMap,
			String id,
			String name,
			DefenderMarker... markers)
	{
		return new TileMarkerSet(
				SET_ID_PREFIX + id,
				name,
				mapMode,
				waveMap,
				new ArrayList<>(Arrays.asList(markers)),
				true
		);
	}

	private static DefenderMarker marker(
			String id,
			int regionX,
			int regionY,
			String name,
			String label,
			String color,
			Integer opacityPercent,
			Float borderWidth)
	{
		return new DefenderMarker(
				"built-in:marker:" + id,
				new DefenderTile(WAVES_1_TO_9_REGION, regionX, regionY, 0),
				name,
				label,
				color,
				opacityPercent,
				borderWidth
		);
	}

	private static DefenderMarker marker(
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
		return new DefenderMarker(
				"built-in:marker:" + id,
				new DefenderTile(regionId, regionX, regionY, 0),
				name,
				label,
				color,
				opacityPercent,
				borderWidth
		);
	}
}
