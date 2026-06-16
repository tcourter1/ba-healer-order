package com.bahealerorder.healer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.bahealerorder.healer.codes.HealerCodeParser;
import com.bahealerorder.healer.codes.RunPreset;
import com.bahealerorder.healer.codes.StrategyStore;
import com.bahealerorder.healer.codes.WaveCode;
import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class HealerCodeManagerTest
{
	@Test
	public void importsRunPresetByNameAndReplacesSameNameWaveCode()
	{
		StrategyStore store = new StrategyStore();
		WaveCode existing = HealerCodeParser.parseWaveCode("user:wave:fast-five", "Fast Five", 5, false, "1-1-1");
		store.getWaveCodes().add(existing);
		Map<Integer, String> existingWaveCodeIds = new HashMap<>();
		existingWaveCodeIds.put(5, existing.getId());
		store.getRunPresets().add(new RunPreset("user:preset:main-run", "Main Run", false, existingWaveCodeIds));

		HealerCodeManager manager = new HealerCodeManager(store, new Gson());
		String json = "{"
				+ "\"preset\":{\"name\":\"Main Run\",\"waveCodes\":{\"5\":\"Fast Five\"}},"
				+ "\"waveCodes\":[{\"name\":\"Fast Five\",\"wave\":5,\"sourceText\":\"2-2-2\"}]"
				+ "}";

		assertEquals(true, manager.importRunPresetJson(json));

		WaveCode imported = manager.findWaveCode(existing.getId());
		assertNotNull(imported);
		assertEquals("2-2-2", imported.getSourceText());
		assertEquals(existing.getId(), manager.getActiveWaveCodeId(5));
		assertEquals("user:preset:main-run", manager.getActiveRunPresetId());
		assertEquals(1, store.getWaveCodes().size());
		assertEquals(1, store.getRunPresets().size());
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
}
