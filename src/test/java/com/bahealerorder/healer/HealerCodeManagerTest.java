package com.bahealerorder.healer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.bahealerorder.healer.codes.HealerCodeExport;
import com.bahealerorder.healer.codes.HealerCodeExportType;
import com.bahealerorder.healer.codes.HealerCodeParser;
import com.bahealerorder.healer.codes.HealerCodeStoreNormalizer;
import com.bahealerorder.healer.codes.RunPreset;
import com.bahealerorder.healer.codes.StrategyStore;
import com.bahealerorder.healer.codes.WaveCode;
import com.google.gson.Gson;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class HealerCodeManagerTest
{
	@Test
	public void importsRunPresetWithoutReplacingDifferentSameNameWaveCode()
	{
		StrategyStore store = new StrategyStore();
		WaveCode existing = HealerCodeParser.parseWaveCode("user:wave:fast-five", "Fast Five", 5, false, "1-1-1");
		store.getWaveCodes().add(existing);
		Map<Integer, String> existingWaveCodeIds = new HashMap<>();
		existingWaveCodeIds.put(5, existing.getId());
		store.getRunPresets().add(new RunPreset("user:preset:main-run", "Main Run", false, existingWaveCodeIds));

		HealerCodeManager manager = new HealerCodeManager(store, new Gson());
		WaveCode importedCode = HealerCodeParser.parseWaveCode(null, "Fast Five", 5, false, "2-2-2");
		Map<Integer, String> importedWaveCodeIds = new HashMap<>();
		importedWaveCodeIds.put(5, "Fast Five");
		RunPreset importedPreset = new RunPreset(null, "Main Run", false, importedWaveCodeIds);
		String json = new Gson().toJson(new HealerCodeExport(
				2,
				HealerCodeExportType.RUN_PRESET,
				importedPreset,
				null,
				Collections.singletonList(importedCode)
		));

		assertEquals(true, manager.importRunPresetJson(json));

		WaveCode imported = manager.getActiveWaveCode(5);
		assertNotNull(imported);
		assertEquals("Fast Five (1)", imported.getName());
		assertEquals(2, imported.getCall(0).getInstruction(1).getTargetFoodCount());
		assertEquals("1-1-1", existing.getSourceText());
		assertEquals(imported.getId(), manager.getActiveWaveCodeId(5));
		assertEquals(2, store.getWaveCodes().size());
		assertEquals(2, store.getRunPresets().size());
	}

	@Test
	public void clearingWaveCodeSelectionDoesNotFallBackToActivePreset()
	{
		StrategyStore store = new StrategyStore();
		WaveCode waveOne = HealerCodeParser.parseWaveCode("user:wave:one", "Wave One", 1, false, "1-1-1");
		WaveCode waveTwo = HealerCodeParser.parseWaveCode("user:wave:two", "Wave Two", 2, false, "2-2-2");
		store.getWaveCodes().add(waveOne);
		store.getWaveCodes().add(waveTwo);

		Map<Integer, String> waveCodeIds = new HashMap<>();
		waveCodeIds.put(1, waveOne.getId());
		waveCodeIds.put(2, waveTwo.getId());
		store.getRunPresets().add(new RunPreset("user:preset:main-run", "Main Run", false, waveCodeIds));

		HealerCodeManager manager = new HealerCodeManager(store, new Gson());

		manager.applyRunPreset("user:preset:main-run");
		manager.setActiveWaveCodeId(1, null);

		assertNull(manager.getActiveWaveCode(1));
		assertEquals(waveTwo.getId(), manager.getActiveWaveCodeId(2));
		assertEquals(waveTwo, manager.getActiveWaveCode(2));
		assertNull(manager.getActiveRunPresetId());
	}

	@Test
	public void exportsAndImportsUnnamedCurrentPresetAsSelections()
	{
		StrategyStore store = new StrategyStore();
		WaveCode waveOne = HealerCodeParser.parseWaveCode("user:wave:one", "Wave One", 1, false, "1-1-1");
		store.getWaveCodes().add(waveOne);
		store.getActiveWaveCodeIds().put(1, waveOne.getId());

		HealerCodeManager manager = new HealerCodeManager(store, new Gson());
		String json = manager.exportCurrentRunPreset(null).getJson();

		StrategyStore importedStore = new StrategyStore();
		HealerCodeManager importedManager = new HealerCodeManager(importedStore, new Gson());

		assertEquals(true, importedManager.importRunPresetJson(json));
		assertNull(importedManager.getActiveRunPresetId());
		assertNotNull(importedManager.getActiveWaveCode(1));
		assertEquals(0, importedStore.getRunPresets().size());
		assertEquals("Wave One", importedManager.getActiveWaveCode(1).getName());
	}

	@Test
	public void savesLegacyWaveCodeWithRawSourceText()
	{
		StrategyStore store = new StrategyStore();
		HealerCodeManager manager = new HealerCodeManager(store, new Gson());
		WaveCode legacy = HealerCodeParser.parseWaveCode(null, "Legacy", 5, false, "1-2-3");
		legacy.setLegacyMode(true);
		legacy.setLegacySourceText("1-2-3");

		WaveCode saved = manager.saveWaveCode(null, legacy);

		assertNotNull(saved);
		assertEquals(true, saved.isLegacyMode());
		assertEquals("1-2-3", saved.getSourceText());
	}

	@Test
	public void savingBuiltInWaveCodeCreatesResettableOverrideWithoutRenaming()
	{
		StrategyStore store = new StrategyStore();
		HealerCodeManager manager = new HealerCodeManager(store, new Gson());
		WaveCode builtIn = manager.findBuiltInWaveCode("builtin:w4:regular");
		WaveCode draft = HealerCodeParser.parseWaveCode(builtIn.getId(), builtIn.getName(), builtIn.getWave(), false, "9-9-9-9");

		WaveCode saved = manager.saveWaveCode(builtIn.getId(), draft);

		assertNotNull(saved);
		assertEquals(builtIn.getId(), saved.getId());
		assertEquals(builtIn.getName(), saved.getName());
		assertEquals(true, saved.isBuiltIn());
		assertEquals(true, manager.isModifiedBuiltInWaveCode(builtIn.getId()));
		assertEquals(1, store.getWaveCodes().size());
		assertEquals(9, manager.findWaveCode(builtIn.getId()).getCall(0).getInstruction(1).getTargetFoodCount());

		assertEquals(true, manager.resetBuiltInWaveCode(builtIn.getId()));
		assertEquals(false, manager.isModifiedBuiltInWaveCode(builtIn.getId()));
		assertEquals(0, store.getWaveCodes().size());
		assertEquals(2, manager.findWaveCode(builtIn.getId()).getCall(0).getInstruction(1).getTargetFoodCount());
	}

	@Test
	public void v1StoreNormalizeKeepsRunPresetsAndMergesStoredBuiltIns()
	{
		String json = "{"
				+ "\"activeRunPresetId\":\"user:preset:x69\","
				+ "\"activeWaveCodeIds\":{\"1\":\"user:wave:1-regular\",\"4\":\"builtin:w4:regular\"},"
				+ "\"runPresets\":[{\"id\":\"user:preset:x69\",\"name\":\"x69\",\"builtIn\":false,"
				+ "\"waveCodes\":{\"1\":\"user:wave:1-regular\",\"4\":\"builtin:w4:regular\"}}],"
				+ "\"waveCodes\":[{\"id\":\"user:wave:1-regular\",\"name\":\"Regular\",\"wave\":1,"
				+ "\"builtIn\":false,\"sourceText\":\"1-1\"},"
				+ "{\"id\":\"builtin:w4:regular\",\"name\":\"Regular\",\"wave\":4,"
				+ "\"builtIn\":true,\"sourceText\":\"2-4-2(27) //\\n0-0-0-8\"}]"
				+ "}";
		StrategyStore store = new Gson().fromJson(json, StrategyStore.class);

		assertEquals(true, HealerCodeStoreNormalizer.normalize(store, 2));

		assertEquals(2, store.getVersion());
		assertEquals("user:preset:x69", store.getActiveRunPresetId());
		assertEquals(1, store.getRunPresets().size());
		assertEquals("x69", store.getRunPresets().get(0).getName());
		assertEquals("user:wave:1-regular", store.getRunPresets().get(0).getWaveCodeId(1));
		assertEquals("builtin:w4:regular", store.getRunPresets().get(0).getWaveCodeId(4));
		assertEquals(2, store.getWaveCodes().size());
		assertEquals(true, store.getWaveCodes().get(0).isLegacyMode());
		assertEquals("1-1", store.getWaveCodes().get(0).getSourceText());

		HealerCodeManager manager = new HealerCodeManager(store, new Gson());
		int builtInCount = 0;
		for (WaveCode code : manager.getWaveCodesForWave(4))
		{
			if ("builtin:w4:regular".equals(code.getId()))
			{
				builtInCount++;
			}
		}
		assertEquals(1, builtInCount);
	}
}
