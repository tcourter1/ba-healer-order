package com.bahealerorder.tilemarkers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class TileMarkerBuiltInStrategies
{
	static final String BEGINNER_TILES_STRATEGY_ID =
			"built-in:tile-marker-strategy:beginner-tiles";
	static final String W10_BEGINNER_STRATEGY_ID =
			"built-in:tile-marker-strategy:w10-beginner";
	static final String CENTER_FOOD_STRATEGY_ID =
			"built-in:tile-marker-strategy:center-food";
	static final String NW_FOOD_STRATEGY_ID =
			"built-in:tile-marker-strategy:nw-food";
	static final String NO_LOG_STRATEGY_ID =
			"built-in:tile-marker-strategy:no-log";
	static final String WALL_SPLIT_STRATEGY_ID =
			"built-in:tile-marker-strategy:wall-split";
	static final String HENDI_TRIANGLE_STRATEGY_ID =
			"built-in:tile-marker-strategy:hendi-triangle";
	static final String TWO_ONE_FIVE_STAR_TWO_STRATEGY_ID =
			"built-in:tile-marker-strategy:two-one-five-star-two";
	static final String AUK_W10_STRATEGY_ID =
			"built-in:tile-marker-strategy:auk-w10";

	private static final String WALL_SPLIT_NOTES = "44.4 - Move 2E of trap, soft crash\n"
			+ "48.0 - Move to 1E of trap, drop food\n"
			+ "54.0 - Step 1 south and multi";
	private static final String TWO_ONE_FIVE_NOTES = "38.4 (soft crash) - Move 4N of trap, drop 1. Pick up at 43.8. Return to trap.\n\n"
			+ "If 48s runner is W, drop 6 good\n"
			+ "54.0 - Multi, repair\n\n"
			+ "If 48s runner is S/E, drop 6 good + 1 bad\n"
			+ "60.0 - Multi, call, repair, drop 1 S";
	private static final String AUK_W10_NOTES = "12.0 - Delay, then drop 3 good NW\n"
			+ "24.0 - Delay at cave, then get logs. \n"
			+ "Drop 3 good W\n"
			+ "45.0 - Move N of trap, slow multi. Repair.\n"
			+ "Drop 1 more good W.\n"
			+ "Run to cannon and split reserves.";

	private TileMarkerBuiltInStrategies()
	{
	}

	static List<TileMarkerStrategyPreset> createStrategyPresets()
	{
		return Arrays.asList(
				strategy(BEGINNER_TILES_STRATEGY_ID, "Beginner Defender Tiles", "", TileMarkerWaveMap.WAVES_1_TO_9,
						TileMarkerBuiltInSets.BEGINNER_TILES_SET_ID),
				strategy(W10_BEGINNER_STRATEGY_ID, "W10 Beginner", "", TileMarkerWaveMap.WAVE_10,
						TileMarkerBuiltInSets.W10_BEGINNER_TILES_SET_ID),
				strategy(CENTER_FOOD_STRATEGY_ID, "Center Food", "", TileMarkerWaveMap.WAVES_1_TO_9,
						TileMarkerBuiltInSets.CENTER_TRAP_FOOD_SET_ID,
						TileMarkerBuiltInSets.SHIR_MAINSTACK_TRAIL_SET_ID),
				strategy(NW_FOOD_STRATEGY_ID, "NW Food", "", TileMarkerWaveMap.WAVES_1_TO_9,
						TileMarkerBuiltInSets.NW_TRAP_FOOD_SET_ID,
						TileMarkerBuiltInSets.HENKE_MAINSTACK_TRAIL_SET_ID),
				strategy(NO_LOG_STRATEGY_ID, "No Log", "", TileMarkerWaveMap.WAVES_1_TO_9,
						TileMarkerBuiltInSets.N_TRAP_FOOD_SET_ID,
						TileMarkerBuiltInSets.SHIR_MAINSTACK_TRAIL_SET_ID),
				strategy(WALL_SPLIT_STRATEGY_ID, "Wall Split", WALL_SPLIT_NOTES, TileMarkerWaveMap.WAVES_1_TO_9,
						TileMarkerBuiltInSets.N_TRAP_FOOD_SET_ID,
						TileMarkerBuiltInSets.SHIR_MAINSTACK_TRAIL_SET_ID,
						TileMarkerBuiltInSets.WALL_SPLIT_MULTI_SET_ID),
				strategy(HENDI_TRIANGLE_STRATEGY_ID, "Hendi Triangle", "", TileMarkerWaveMap.WAVES_1_TO_9,
						TileMarkerBuiltInSets.N_TRAP_FOOD_SET_ID,
						TileMarkerBuiltInSets.SHIR_MAINSTACK_TRAIL_SET_ID,
						TileMarkerBuiltInSets.HENDI_TRIANGLE_SET_ID),
				strategy(TWO_ONE_FIVE_STAR_TWO_STRATEGY_ID, "2-1-5*-2", TWO_ONE_FIVE_NOTES, TileMarkerWaveMap.WAVES_1_TO_9,
						TileMarkerBuiltInSets.N_TRAP_FOOD_SET_ID,
						TileMarkerBuiltInSets.SHIR_MAINSTACK_TRAIL_SET_ID,
						TileMarkerBuiltInSets.TWO_ONE_FIVE_SOFT_CRASH_SET_ID),
				strategy(AUK_W10_STRATEGY_ID, "Auk W10", AUK_W10_NOTES, TileMarkerWaveMap.WAVE_10,
						TileMarkerBuiltInSets.AUK_W10_TILES_SET_ID)
		);
	}

	static List<TileMarkerAssignmentPreset> createAssignmentPresets()
	{
		List<TileMarkerAssignmentPreset> presets = new ArrayList<>();
		presets.add(new TileMarkerAssignmentPreset(
				"built-in:tile-marker-assignment-preset:defender-beginner",
				"Beginner",
				TileMarkerRoleContext.DEFENDER,
				beginnerWaves(),
				true
		));
		presets.add(new TileMarkerAssignmentPreset(
				"built-in:tile-marker-assignment-preset:defender-intermediate",
				"Intermediate",
				TileMarkerRoleContext.DEFENDER,
				intermediateWaves(),
				true
		));
		return presets;
	}

	private static TileMarkerStrategyPreset strategy(
			String id,
			String name,
			String notes,
			TileMarkerWaveMap waveMap,
			String... markerSetIds)
	{
		return new TileMarkerStrategyPreset(id, name, notes, waveMap, Arrays.asList(markerSetIds), true);
	}

	private static Map<Integer, String> beginnerWaves()
	{
		Map<Integer, String> waves = new HashMap<>();
		for (int wave = 1; wave <= 9; wave++)
		{
			waves.put(wave, BEGINNER_TILES_STRATEGY_ID);
		}
		waves.put(10, W10_BEGINNER_STRATEGY_ID);
		return waves;
	}

	private static Map<Integer, String> intermediateWaves()
	{
		Map<Integer, String> waves = new HashMap<>();
		waves.put(1, CENTER_FOOD_STRATEGY_ID);
		waves.put(2, NW_FOOD_STRATEGY_ID);
		waves.put(3, NW_FOOD_STRATEGY_ID);
		waves.put(4, NO_LOG_STRATEGY_ID);
		waves.put(5, NO_LOG_STRATEGY_ID);
		waves.put(6, WALL_SPLIT_STRATEGY_ID);
		waves.put(7, HENDI_TRIANGLE_STRATEGY_ID);
		waves.put(8, HENDI_TRIANGLE_STRATEGY_ID);
		waves.put(9, TWO_ONE_FIVE_STAR_TWO_STRATEGY_ID);
		waves.put(10, AUK_W10_STRATEGY_ID);
		return waves;
	}
}
