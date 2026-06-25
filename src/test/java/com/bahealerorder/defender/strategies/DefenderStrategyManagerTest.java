package com.bahealerorder.defender.strategies;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class DefenderStrategyManagerTest
{
	@Test
	public void builtInLibraryIncludesPromotedPresetsAndWaveStrategies()
	{
		DefenderStrategyManager manager = new DefenderStrategyManager(new DefenderStrategyStore(), new Gson());

		List<DefenderRunPreset> presets = manager.getRunPresets();
		assertEquals(2, presets.size());
		assertEquals("Beginner", presets.get(0).getName());
		assertEquals("Intermediate", presets.get(1).getName());
		assertTrue(presets.get(0).isBuiltIn());
		assertTrue(presets.get(1).isBuiltIn());

		DefenderRunPreset intermediate = manager.findRunPreset("user:defender:preset:intermediate");
		assertNotNull(intermediate);
		assertEquals("user:defender:wave:9-wall-split", intermediate.getWaveStrategyId(9));
		assertEquals("user:defender:wave:10-auk", intermediate.getWaveStrategyId(10));

		List<DefenderWaveStrategy> strategies = manager.getWaveStrategies();
		assertEquals(21, strategies.size());
		for (DefenderWaveStrategy strategy : strategies)
		{
			assertTrue(strategy.isBuiltIn());
		}

		DefenderWaveStrategy b9 = manager.findWaveStrategy("user:defender:wave:9-nw-food");
		assertNotNull(b9);
		assertEquals("B9", b9.getName());
		assertEquals(2, b9.getNumberOfLogs());
		assertEquals(6, b9.getMarkers().size());
		assertTrue(b9.getNotes().contains("28.2 - If broken"));

		DefenderWaveStrategy auk = manager.findWaveStrategy("user:defender:wave:10-auk");
		assertNotNull(auk);
		assertEquals("Auk", auk.getName());
		assertEquals(5, auk.getMarkers().size());
		assertTrue(auk.getNotes().contains("Run to cannon and split reserves."));
	}

	@Test
	public void builtInPresetsCanBeApplied()
	{
		DefenderStrategyManager manager = new DefenderStrategyManager(new DefenderStrategyStore(), new Gson());

		manager.applyRunPreset("user:defender:preset:intermediate");

		assertEquals("user:defender:preset:intermediate", manager.getActiveRunPresetId());
		assertEquals("user:defender:wave:9-wall-split", manager.getActiveWaveStrategyId(9));
		assertEquals("2-1-5*-2", manager.getActiveWaveStrategy(9).getName());
	}

	@Test
	public void builtInWaveStrategyEditsPreserveNameAndCanReset()
	{
		DefenderStrategyStore store = new DefenderStrategyStore();
		DefenderStrategyManager manager = new DefenderStrategyManager(store, new Gson());
		DefenderWaveStrategy edited = strategy("user:defender:wave:1-beginner", "Renamed", 2, "changed notes");
		edited.setNumberOfLogs(4);

		assertTrue(manager.updateBuiltInWaveStrategy("user:defender:wave:1-beginner", edited));

		DefenderWaveStrategy saved = manager.findWaveStrategy("user:defender:wave:1-beginner");
		assertNotNull(saved);
		assertTrue(saved.isBuiltIn());
		assertEquals("Beginner", saved.getName());
		assertEquals(1, saved.getWave());
		assertEquals("changed notes", saved.getNotes());
		assertEquals(4, saved.getNumberOfLogs());

		assertTrue(manager.resetBuiltInWaveStrategy("user:defender:wave:1-beginner"));
		DefenderWaveStrategy reset = manager.findWaveStrategy("user:defender:wave:1-beginner");
		assertNotNull(reset);
		assertEquals("Beginner", reset.getName());
		assertEquals(1, reset.getWave());
		assertEquals("test", reset.getNotes());
	}

	@Test
	public void storedCopiesWithPromotedBuiltInIdsDoNotShadowBuiltIns()
	{
		DefenderStrategyStore store = new DefenderStrategyStore();
		Map<Integer, String> stalePresetStrategies = new HashMap<>();
		stalePresetStrategies.put(1, "missing");
		store.getRunPresets().add(new DefenderRunPreset("user:defender:preset:beginner", "Stale Beginner", false, stalePresetStrategies));
		store.getWaveStrategies().add(strategy("user:defender:wave:1-beginner", "Stale Beginner", 1, "stale notes"));

		DefenderStrategyManager manager = new DefenderStrategyManager(store, new Gson());

		DefenderRunPreset preset = manager.findRunPreset("user:defender:preset:beginner");
		assertNotNull(preset);
		assertEquals("Beginner", preset.getName());
		assertTrue(preset.isBuiltIn());
		assertEquals("user:defender:wave:1-beginner", preset.getWaveStrategyId(1));

		DefenderWaveStrategy waveOne = manager.findWaveStrategy("user:defender:wave:1-beginner");
		assertNotNull(waveOne);
		assertEquals("Beginner", waveOne.getName());
		assertEquals("test", waveOne.getNotes());
		assertTrue(waveOne.isBuiltIn());
	}

	@Test
	public void importsRunPresetByNameAndReplacesSameNameWaveStrategy()
	{
		DefenderStrategyStore store = new DefenderStrategyStore();
		DefenderWaveStrategy existing = strategy("user:defender:wave:b9", "B9", 9, "old notes");
		store.getWaveStrategies().add(existing);
		Map<Integer, String> existingStrategyIds = new HashMap<>();
		existingStrategyIds.put(9, existing.getId());
		store.getRunPresets().add(new DefenderRunPreset("user:defender:preset:main-run", "Main Run", false, existingStrategyIds));

		DefenderStrategyManager manager = new DefenderStrategyManager(store, new Gson());
		String json = "{"
				+ "\"preset\":{\"name\":\"Main Run\",\"waveStrategies\":{\"9\":\"B9\"}},"
				+ "\"waveStrategies\":[{\"name\":\"B9\",\"wave\":9,\"notes\":\"new notes\"}]"
				+ "}";

		assertEquals(true, manager.importRunPresetJson(json));

		DefenderWaveStrategy imported = manager.findWaveStrategy(existing.getId());
		assertNotNull(imported);
		assertEquals("new notes", imported.getNotes());
		assertEquals(existing.getId(), manager.getActiveWaveStrategyId(9));
		assertEquals("user:defender:preset:main-run", manager.getActiveRunPresetId());
		assertEquals(1, store.getWaveStrategies().size());
		assertEquals(1, store.getRunPresets().size());
	}

	@Test
	public void clearingWaveStrategySelectionDoesNotFallBackToActivePreset()
	{
		DefenderStrategyStore store = new DefenderStrategyStore();
		DefenderWaveStrategy waveOne = strategy("user:defender:wave:one", "Wave One", 1, "one");
		DefenderWaveStrategy waveTwo = strategy("user:defender:wave:two", "Wave Two", 2, "two");
		store.getWaveStrategies().add(waveOne);
		store.getWaveStrategies().add(waveTwo);

		Map<Integer, String> strategyIds = new HashMap<>();
		strategyIds.put(1, waveOne.getId());
		strategyIds.put(2, waveTwo.getId());
		store.getRunPresets().add(new DefenderRunPreset("user:defender:preset:main-run", "Main Run", false, strategyIds));

		DefenderStrategyManager manager = new DefenderStrategyManager(store, new Gson());

		manager.applyRunPreset("user:defender:preset:main-run");
		manager.setActiveWaveStrategyId(1, null);

		assertNull(manager.getActiveWaveStrategy(1));
		assertEquals(waveTwo.getId(), manager.getActiveWaveStrategyId(2));
		assertEquals(waveTwo, manager.getActiveWaveStrategy(2));
		assertNull(manager.getActiveRunPresetId());
	}

	@Test
	public void exportRoundTripPreservesMarkers()
	{
		DefenderStrategyStore store = new DefenderStrategyStore();
		DefenderWaveStrategy waveNine = strategy("user:defender:wave:b9", "B9", 9, "44.4 - go 4N1E");
		waveNine.setNumberOfLogs(3);
		waveNine.setMarkers(Arrays.asList(new DefenderMarker(
				"m1",
				DefenderMapLayout.WAVES_1_TO_9.toTile(49, 30),
				"mainstack",
				"triangle",
				"#50aaff",
				70,
				2.5f
		)));
		store.getWaveStrategies().add(waveNine);
		Map<Integer, String> strategyIds = new HashMap<>();
		strategyIds.put(9, waveNine.getId());
		store.getRunPresets().add(new DefenderRunPreset("user:defender:preset:b9", "B9 Run", false, strategyIds));

		DefenderStrategyManager manager = new DefenderStrategyManager(store, new Gson());
		String json = manager.exportRunPresetJson("user:defender:preset:b9");

		DefenderStrategyStore importedStore = new DefenderStrategyStore();
		DefenderStrategyManager importedManager = new DefenderStrategyManager(importedStore, new Gson());
		assertEquals(true, importedManager.importRunPresetJson(json));

		DefenderWaveStrategy imported = importedManager.getActiveWaveStrategy(9);
		assertNotNull(imported);
		assertEquals("B9", imported.getName());
		assertEquals(3, imported.getNumberOfLogs());
		assertEquals(1, imported.getMarkers().size());
		assertEquals("mainstack", imported.getMarkers().get(0).getName());
		assertEquals("triangle", imported.getMarkers().get(0).getLabel());
		assertEquals("#50aaff", imported.getMarkers().get(0).getColor());
		assertEquals(Integer.valueOf(70), imported.getMarkers().get(0).getOpacityPercent());
		assertEquals(Float.valueOf(2.5f), imported.getMarkers().get(0).getBorderWidth());
	}

	@Test
	public void waveStrategyTemplateOmitsWaveAndImportsIntoTargetWave()
	{
		DefenderWaveStrategy source = strategy("user:defender:wave:b9", "Shared Layout", 9, "44.4 - go 4N1E");
		source.setNumberOfLogs(4);
		source.setMarkers(Arrays.asList(new DefenderMarker(
				"m1",
				DefenderMapLayout.WAVES_1_TO_9.toTile(49, 30),
				"mainstack",
				"4 good",
				"#50aaff",
				60,
				3.0f
		)));

		DefenderStrategyManager manager = new DefenderStrategyManager(new DefenderStrategyStore(), new Gson());
		String json = manager.exportWaveStrategyTemplateJson(source);

		assertFalse(json.contains("\"wave\""));
		assertFalse(json.contains("\"regionId\""));

		DefenderWaveStrategy imported = manager.importWaveStrategyTemplateJson(json, 10, "draft", false);

		assertNotNull(imported);
		assertEquals("Shared Layout", imported.getName());
		assertEquals(10, imported.getWave());
		assertEquals(4, imported.getNumberOfLogs());
		assertEquals("44.4 - go 4N1E", imported.getNotes());
		assertEquals(1, imported.getMarkers().size());
		assertEquals("mainstack", imported.getMarkers().get(0).getName());
		assertEquals("4 good", imported.getMarkers().get(0).getLabel());
		assertEquals(Integer.valueOf(60), imported.getMarkers().get(0).getOpacityPercent());
		assertEquals(Float.valueOf(3.0f), imported.getMarkers().get(0).getBorderWidth());
		assertEquals(DefenderMapLayout.WAVE_10.toTile(49, 30).getRegionId(), imported.getMarkers().get(0).getTile().getRegionId());
		assertEquals(49, DefenderMapLayout.WAVE_10.toMapX(imported.getMarkers().get(0).getTile()));
		assertEquals(30, DefenderMapLayout.WAVE_10.toMapY(imported.getMarkers().get(0).getTile()));
	}

	@Test
	public void persistedLastMarkerStyleSurvivesStoreRoundTrip()
	{
		Gson gson = new Gson();
		DefenderStrategyStore store = new DefenderStrategyStore();
		DefenderStrategyManager manager = new DefenderStrategyManager(store, gson);

		manager.setLastMarkerStyle("#123456", 74, 2.5f);

		DefenderStrategyStore restoredStore = gson.fromJson(gson.toJson(store), DefenderStrategyStore.class);
		DefenderStrategyManager restoredManager = new DefenderStrategyManager(restoredStore, gson);

		assertEquals("#123456", restoredManager.getLastMarkerColor());
		assertEquals(74, restoredManager.getLastMarkerOpacityPercent());
		assertEquals(2.5f, restoredManager.getLastMarkerBorderWidth(), 0.001f);
	}

	@Test
	public void markerClipboardRoundTripPreservesEditorLocationsAndStyles()
	{
		DefenderStrategyManager manager = new DefenderStrategyManager(new DefenderStrategyStore(), new Gson());
		String json = manager.exportMarkerClipboardJson(9, Arrays.asList(
				new DefenderMarker(
						"m1",
						DefenderMapLayout.WAVES_1_TO_9.toTile(49, 30),
						"mainstack",
						"4 good",
						"#50aaff",
						60,
						3.0f
				),
				new DefenderMarker(
						"m2",
						DefenderMapLayout.WAVES_1_TO_9.toTile(45, 26),
						"trap",
						"1 bad",
						"#ffee00",
						80,
						1.5f
				)
		));

		assertFalse(json.contains("\"wave\""));
		assertFalse(json.contains("\"regionId\""));

		java.util.List<DefenderMarker> imported = manager.importMarkerClipboardJson(10, json);

		assertNotNull(imported);
		assertEquals(2, imported.size());
		assertEquals("mainstack", imported.get(0).getName());
		assertEquals("4 good", imported.get(0).getLabel());
		assertEquals("#50aaff", imported.get(0).getColor());
		assertEquals(Integer.valueOf(60), imported.get(0).getOpacityPercent());
		assertEquals(Float.valueOf(3.0f), imported.get(0).getBorderWidth());
		assertEquals(DefenderMapLayout.WAVE_10.toTile(49, 30).getRegionId(), imported.get(0).getTile().getRegionId());
		assertEquals(49, DefenderMapLayout.WAVE_10.toMapX(imported.get(0).getTile()));
		assertEquals(30, DefenderMapLayout.WAVE_10.toMapY(imported.get(0).getTile()));
		assertEquals("trap", imported.get(1).getName());
		assertEquals("1 bad", imported.get(1).getLabel());
		assertEquals("#ffee00", imported.get(1).getColor());
		assertEquals(Integer.valueOf(80), imported.get(1).getOpacityPercent());
		assertEquals(Float.valueOf(1.5f), imported.get(1).getBorderWidth());
	}

	@Test
	public void markerClipboardRejectsEmptyInput()
	{
		DefenderStrategyManager manager = new DefenderStrategyManager(new DefenderStrategyStore(), new Gson());

		assertNull(manager.exportMarkerClipboardJson(9, null));
		assertNull(manager.importMarkerClipboardJson(9, ""));
	}

	private DefenderWaveStrategy strategy(String id, String name, int wave, String notes)
	{
		return new DefenderWaveStrategy(
				id,
				name,
				wave,
				false,
				notes,
				0,
				null
		);
	}
}
