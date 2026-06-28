package com.bahealerorder.defender.strategies;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DefenderStrategyLibrary
{
	private DefenderStrategyLibrary()
	{
	}

	public static DefenderStrategyStore create()
	{
		DefenderStrategyStore store = new DefenderStrategyStore();
		List<DefenderRunPreset> presets = new ArrayList<>();
		List<DefenderWaveStrategy> waveStrategies = new ArrayList<>();

		addPreset(presets, "user:defender:preset:beginner", "Beginner",
				1, "user:defender:wave:1-beginner",
				2, "user:defender:wave:2-beginner",
				3, "user:defender:wave:3-beginner",
				4, "user:defender:wave:4-beginner",
				5, "user:defender:wave:5-beginner",
				6, "user:defender:wave:6-beginner",
				7, "user:defender:wave:7-beginner",
				8, "user:defender:wave:8-beginner",
				9, "user:defender:wave:9-beginner",
				10, "user:defender:wave:10-beginner"
		);
		addPreset(presets, "user:defender:preset:intermediate", "Intermediate",
				1, "user:defender:wave:1-center-food",
				2, "user:defender:wave:2-nw-food",
				3, "user:defender:wave:3-nw-food",
				4, "user:defender:wave:4-center-food",
				5, "user:defender:wave:5-no-log",
				6, "user:defender:wave:6-no-log",
				7, "user:defender:wave:7-hendi-triangle",
				8, "user:defender:wave:8-hendi-triangle",
				9, "user:defender:wave:9-wall-split",
				10, "user:defender:wave:10-auk"
		);

		addStrategy(waveStrategies, "user:defender:wave:1-beginner", "Beginner", 1, "test", 0,
				marker("marker:1:45:27:2497334928354700", 7509, 45, 35, 0, "Trap Food", "4 good", "#50aaff", null, null),
				marker("marker:1:45:21:2501332514209200", 7509, 45, 29, 0, "Safety Food", "1 bad", "#50aaff", null, null),
				marker("marker:1:38:32:2501378528357700", 7509, 38, 40, 0, "Trail", "1 bad", "#50aaff", null, null),
				marker("marker:1:35:35:2501401789260000", 7509, 35, 43, 0, "Mainstack", "4 good", "#50aaff", null, null),
				marker("marker:1:29:38:2501421064095500", 7509, 29, 46, 0, "West Relure Food", "1 bad", "#50aaff", null, null)
		);
		addStrategy(waveStrategies, "user:defender:wave:1-center-food", "Center Food", 1, "", 0,
				marker("marker:1:45:26:2503668700048200", 7509, 45, 34, 0, "Trap", "", "#77e2ff", 10, 0.5f),
				marker("marker:1:39:31:2503736377353100", 7509, 39, 39, 0, "Trail", "", "#77e2ff", 10, 0.5f),
				marker("marker:1:35:35:2503827394489700", 7509, 35, 43, 0, "Mainstack", "", "#77e2ff", 10, 0.5f)
		);
		addStrategy(waveStrategies, "user:defender:wave:2-beginner", "Beginner", 2, "", 1,
				marker("marker:2:45:27:0", 7509, 45, 35, 0, "Trap Food", "4 good", "#50aaff", null, null),
				marker("marker:2:45:21:1", 7509, 45, 29, 0, "Safety Food", "1 bad", "#50aaff", null, null),
				marker("marker:2:38:32:2", 7509, 38, 40, 0, "Trail", "1 bad", "#50aaff", null, null),
				marker("marker:2:35:35:3", 7509, 35, 43, 0, "Mainstack", "4 good", "#50aaff", null, null),
				marker("marker:2:29:38:4", 7509, 29, 46, 0, "West Relure Food", "1 bad", "#50aaff", null, null)
		);
		addStrategy(waveStrategies, "user:defender:wave:2-nw-food", "NW Food", 2, "", 1,
				marker("marker:2:44:27:2513360936814700", 7509, 44, 35, 0, "Trap Food", "", "#77e2ff", 10, 0.5f),
				marker("marker:2:38:32:2513389142746100", 7509, 38, 40, 0, "Trail", "", "#77e2ff", 10, 0.5f),
				marker("marker:2:36:34:2513412531439300", 7509, 36, 42, 0, "Mainstack", "", "#77e2ff", 10, 0.5f)
		);
		addStrategy(waveStrategies, "user:defender:wave:3-beginner", "Beginner", 3, "", 1,
				marker("marker:3:45:27:0", 7509, 45, 35, 0, "Trap Food", "4 good", "#50aaff", null, null),
				marker("marker:3:45:21:1", 7509, 45, 29, 0, "Safety Food", "1 bad", "#50aaff", null, null),
				marker("marker:3:38:32:2", 7509, 38, 40, 0, "Trail", "1 bad", "#50aaff", null, null),
				marker("marker:3:35:35:3", 7509, 35, 43, 0, "Mainstack", "4 good", "#50aaff", null, null),
				marker("marker:3:29:38:4", 7509, 29, 46, 0, "West Relure Food", "1 bad", "#50aaff", null, null)
		);
		addStrategy(waveStrategies, "user:defender:wave:3-nw-food", "NW Food", 3, "", 1,
				marker("marker:3:44:27:0", 7509, 44, 35, 0, "Trap Food", "", "#77e2ff", 10, 0.5f),
				marker("marker:3:38:32:1", 7509, 38, 40, 0, "Trail", "", "#77e2ff", 10, 0.5f),
				marker("marker:3:36:34:2", 7509, 36, 42, 0, "Mainstack", "", "#77e2ff", 10, 0.5f)
		);
		addStrategy(waveStrategies, "user:defender:wave:4-beginner", "Beginner", 4, "", 1,
				marker("marker:4:45:27:0", 7509, 45, 35, 0, "Trap Food", "4 good", "#50aaff", null, null),
				marker("marker:4:45:21:1", 7509, 45, 29, 0, "Safety Food", "1 bad", "#50aaff", null, null),
				marker("marker:4:38:32:2", 7509, 38, 40, 0, "Trail", "1 bad", "#50aaff", null, null),
				marker("marker:4:35:35:3", 7509, 35, 43, 0, "Mainstack", "4 good", "#50aaff", null, null),
				marker("marker:4:29:38:4", 7509, 29, 46, 0, "West Relure Food", "1 bad", "#50aaff", null, null)
		);
		addStrategy(waveStrategies, "user:defender:wave:4-center-food", "No Log", 4, "", 0,
				marker("marker:4:39:31:1", 7509, 39, 39, 0, "Trail", "", "#77e2ff", 10, 0.5f),
				marker("marker:4:35:35:2", 7509, 35, 43, 0, "Mainstack", "", "#77e2ff", 10, 0.5f),
				marker("marker:4:45:27:2515220942277400", 7509, 45, 35, 0, "Trap", "", "#77e2ff", 10, 0.5f)
		);
		addStrategy(waveStrategies, "user:defender:wave:5-beginner", "Beginner", 5, "", 2,
				marker("marker:5:45:27:0", 7509, 45, 35, 0, "Trap Food", "4 good", "#50aaff", null, null),
				marker("marker:5:45:21:1", 7509, 45, 29, 0, "Safety Food", "1 bad", "#50aaff", null, null),
				marker("marker:5:38:32:2", 7509, 38, 40, 0, "Trail", "1 bad", "#50aaff", null, null),
				marker("marker:5:35:35:3", 7509, 35, 43, 0, "Mainstack", "4 good", "#50aaff", null, null),
				marker("marker:5:29:38:4", 7509, 29, 46, 0, "West Relure Food", "1 bad", "#50aaff", null, null)
		);
		addStrategy(waveStrategies, "user:defender:wave:5-no-log", "No Log", 5, "", 0,
				marker("marker:5:39:31:0", 7509, 39, 39, 0, "Trail", "", "#77e2ff", 10, 0.5f),
				marker("marker:5:35:35:1", 7509, 35, 43, 0, "Mainstack", "", "#77e2ff", 10, 0.5f),
				marker("marker:5:45:27:2", 7509, 45, 35, 0, "Trap", "", "#77e2ff", 10, 0.5f)
		);
		addStrategy(waveStrategies, "user:defender:wave:6-beginner", "Beginner", 6, "", 2,
				marker("marker:6:45:27:0", 7509, 45, 35, 0, "Trap Food", "4 good", "#50aaff", null, null),
				marker("marker:6:45:21:1", 7509, 45, 29, 0, "Safety Food", "1 bad", "#50aaff", null, null),
				marker("marker:6:38:32:2", 7509, 38, 40, 0, "Trail", "1 bad", "#50aaff", null, null),
				marker("marker:6:35:35:3", 7509, 35, 43, 0, "Mainstack", "4 good", "#50aaff", null, null),
				marker("marker:6:29:38:4", 7509, 29, 46, 0, "West Relure Food", "1 bad", "#50aaff", null, null)
		);
		addStrategy(waveStrategies, "user:defender:wave:6-no-log", "Wall Split", 6, "44.4 - Move 2E of trap, soft crash\n48.0 - Move to 1E of trap, drop food\n54.0 - Step 1 south and multi", 1,
				marker("marker:6:39:31:0", 7509, 39, 39, 0, "Trail", "", "#77e2ff", 10, 0.5f),
				marker("marker:6:35:35:1", 7509, 35, 43, 0, "Mainstack", "", "#77e2ff", 10, 0.5f),
				marker("marker:6:45:27:2", 7509, 45, 35, 0, "Trap", "", "#77e2ff", 10, 0.5f),
				marker("marker:6:46:26:2518036556484600", 7509, 46, 34, 0, "54s Multi", "", "#81f777", 10, 0.5f)
		);
		addStrategy(waveStrategies, "user:defender:wave:7-beginner", "Beginner", 7, "", 2,
				marker("marker:7:45:27:0", 7509, 45, 35, 0, "Trap Food", "4 good", "#50aaff", null, null),
				marker("marker:7:45:21:1", 7509, 45, 29, 0, "Safety Food", "1 bad", "#50aaff", null, null),
				marker("marker:7:38:32:2", 7509, 38, 40, 0, "Trail", "1 bad", "#50aaff", null, null),
				marker("marker:7:35:35:3", 7509, 35, 43, 0, "Mainstack", "4 good", "#50aaff", null, null),
				marker("marker:7:29:38:4", 7509, 29, 46, 0, "West Relure Food", "1 bad", "#50aaff", null, null)
		);
		addStrategy(waveStrategies, "user:defender:wave:7-hendi-triangle", "Hendi Triangle", 7, "", 1,
				marker("marker:7:42:31:2513044854450500", 7509, 42, 39, 0, "Hendi 1", "1", "#59d4fd", 10, 0.5f),
				marker("marker:7:46:31:2513104898659300", 7509, 46, 39, 0, "Hendi 2", "2", "#59d4fd", 10, 0.5f),
				marker("marker:7:46:28:2513123712706400", 7509, 46, 36, 0, "Hendi 3", "3", "#59d4fd", 10, 0.5f),
				marker("marker:7:39:31:2536945451056600", 7509, 39, 39, 0, "Trail", "", "#77e2ff", 10, 0.5f),
				marker("marker:7:35:35:2536945452721100", 7509, 35, 43, 0, "Mainstack", "", "#77e2ff", 10, 0.5f),
				marker("marker:7:45:27:2536945452725500", 7509, 45, 35, 0, "Trap", "", "#77e2ff", 10, 0.5f)
		);
		addStrategy(waveStrategies, "user:defender:wave:8-beginner", "Beginner", 8, "", 2,
				marker("marker:8:45:27:0", 7509, 45, 35, 0, "Trap Food", "4 good", "#50aaff", null, null),
				marker("marker:8:45:21:1", 7509, 45, 29, 0, "Safety Food", "1 bad", "#50aaff", null, null),
				marker("marker:8:38:32:2", 7509, 38, 40, 0, "Trail", "1 bad", "#50aaff", null, null),
				marker("marker:8:35:35:3", 7509, 35, 43, 0, "Mainstack", "4 good", "#50aaff", null, null),
				marker("marker:8:29:38:4", 7509, 29, 46, 0, "West Relure Food", "1 bad", "#50aaff", null, null)
		);
		addStrategy(waveStrategies, "user:defender:wave:8-hendi-triangle", "Hendi Triangle", 8, "", 1,
				marker("marker:8:42:31:0", 7509, 42, 39, 0, "Hendi 1", "1", "#59d4fd", 10, 0.5f),
				marker("marker:8:46:31:1", 7509, 46, 39, 0, "Hendi 2", "2", "#59d4fd", 10, 0.5f),
				marker("marker:8:46:28:2", 7509, 46, 36, 0, "Hendi 3", "3", "#59d4fd", 10, 0.5f),
				marker("marker:8:39:31:3", 7509, 39, 39, 0, "Trail", "", "#77e2ff", 10, 0.5f),
				marker("marker:8:35:35:4", 7509, 35, 43, 0, "Mainstack", "", "#77e2ff", 10, 0.5f),
				marker("marker:8:45:27:5", 7509, 45, 35, 0, "Trap", "", "#77e2ff", 10, 0.5f)
		);
		addStrategy(waveStrategies, "user:defender:wave:9-beginner", "Beginner", 9, "", 3,
				marker("marker:9:45:27:0", 7509, 45, 35, 0, "Trap Food", "4 good", "#50aaff", null, null),
				marker("marker:9:45:21:1", 7509, 45, 29, 0, "Safety Food", "1 bad", "#50aaff", null, null),
				marker("marker:9:38:32:2", 7509, 38, 40, 0, "Trail", "1 bad", "#50aaff", null, null),
				marker("marker:9:35:35:3", 7509, 35, 43, 0, "Mainstack", "4 good", "#50aaff", null, null),
				marker("marker:9:29:38:4", 7509, 29, 46, 0, "West Relure Food", "1 bad", "#50aaff", null, null)
		);
		addStrategy(waveStrategies, "user:defender:wave:9-wall-split", "2-1-5*-2", 9, "38.4 (soft crash) - Move 4N of trap, drop 1. Pick up at 43.8. Return to trap.\n\nIf 48s runner is W, drop 6 good\n54.0 - Multi, repair\n\nIf 48s runner is S/E, drop 6 good + 1 bad\n60.0 - Multi, call, repair, drop 1 S", 2,
				marker("marker:9:39:31:0", 7509, 39, 39, 0, "Trail", "", "#77e2ff", 10, 0.5f),
				marker("marker:9:35:35:1", 7509, 35, 43, 0, "Mainstack", "", "#77e2ff", 10, 0.5f),
				marker("marker:9:45:27:2", 7509, 45, 35, 0, "Trap", "", "#77e2ff", 10, 0.5f),
				marker("marker:9:45:30:2519060258517800", 7509, 45, 38, 0, "Soft crash", "", "#81f777", 10, 0.5f)
		);
		addStrategy(waveStrategies, "user:defender:wave:9-nw-food", "B9", 9, "28.2 - If broken, drop 1 food 4N1E\n32.4 - Pick up\n33.0 - Repair, drop 6 good NW\n44.4 - Move to 4N1E, drop 2 good\nRepair, then drop 1 good 2E", 2,
				marker("marker:9:38:32:1", 7509, 38, 40, 0, "Trail", "1g", "#77e2ff", 10, 0.5f),
				marker("marker:9:36:34:2", 7509, 36, 42, 0, "Mainstack", "", "#77e2ff", 10, 0.5f),
				marker("marker:9:46:26:2519224928604700", 7509, 46, 34, 0, "E Trap Food", "", "#77e2ff", 10, 0.5f),
				marker("marker:9:46:30:2519294035213700", 7509, 46, 38, 0, "4N1E", "", "#78ff9b", 10, 0.5f),
				marker("marker:9:44:27:2519355239657800", 7509, 44, 35, 0, "NW Trap Food", "", "#78ff9b", 10, 0.5f),
				marker("marker:9:47:26:2519453434990900", 7509, 47, 34, 0, "2E", "", "#e8ff78", 10, 0.5f)
		);
		addStrategy(waveStrategies, "user:defender:wave:10-beginner", "Beginner", 10, "", 2,
				marker("marker:10:39:24:0", 7508, 39, 32, 0, "Safety Food", "1 bad", "#50aaff", null, null),
				marker("marker:10:44:27:1", 7508, 44, 35, 0, "Trap Food", "4 good", "#50aaff", null, null),
				marker("marker:10:39:32:2", 7508, 39, 40, 0, "Trail", "1 bad", "#50aaff", null, null),
				marker("marker:10:38:38:3", 7508, 38, 46, 0, "Mainstack", "4 good", "#50aaff", null, null)
		);
		addStrategy(waveStrategies, "user:defender:wave:10-auk", "Auk", 10, "12.0 - Delay, then drop 3 good NW\n24.0 - Delay at cave, then get logs. \nDrop 3 good W\n45.0 - Move N of trap, slow multi. Repair.\nDrop 1 more good W.\nRun to cannon and split reserves.", 2,
				marker("marker:10:39:22:2519637285711800", 7508, 39, 30, 0, "Safety", "", "#77e2ff", 10, 0.5f),
				marker("marker:10:44:27:2519675417807500", 7508, 44, 35, 0, "NW Trap Food", "3", "#77e2ff", 10, 0.5f),
				marker("marker:10:37:35:2519818416792400", 7508, 37, 43, 0, "Mainstack", "", "#77e2ff", 10, 0.5f),
				marker("marker:10:39:33:2519838931998700", 7508, 39, 41, 0, "Trail", "", "#77e2ff", 10, 0.5f),
				marker("marker:10:44:26:2519871349312600", 7508, 44, 34, 0, "W Trap Food", "3+1", "#78ff87", 10, 0.5f)
		);

		store.setRunPresets(presets);
		store.setWaveStrategies(waveStrategies);
		return store;
	}

	private static void addPreset(List<DefenderRunPreset> presets, String id, String name, Object... waveStrategies)
	{
		Map<Integer, String> waveStrategyIds = new HashMap<>();

		for (int i = 0; i < waveStrategies.length; i += 2)
		{
			waveStrategyIds.put((Integer) waveStrategies[i], (String) waveStrategies[i + 1]);
		}

		presets.add(new DefenderRunPreset(id, name, true, waveStrategyIds));
	}

	private static void addStrategy(
			List<DefenderWaveStrategy> waveStrategies,
			String id,
			String name,
			int wave,
			String notes,
			int numberOfLogs,
			DefenderMarker... markers)
	{
		waveStrategies.add(new DefenderWaveStrategy(id, name, wave, true, notes, numberOfLogs, Arrays.asList(markers)));
	}

	private static DefenderMarker marker(
			String id,
			int regionId,
			int regionX,
			int regionY,
			int z,
			String name,
			String label,
			String color,
			Integer opacityPercent,
			Float borderWidth)
	{
		return new DefenderMarker(id, new DefenderTile(regionId, regionX, regionY, z), name, label, color, opacityPercent, borderWidth);
	}
}
