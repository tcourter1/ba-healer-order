package com.bahealerorder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.bahealerorder.codes.HealerCodeParser;
import com.bahealerorder.codes.RunPreset;
import com.bahealerorder.codes.StrategyStore;
import com.bahealerorder.codes.WaveCode;
import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class BaHealerCodeManagerTest
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

		BaHealerCodeManager manager = new BaHealerCodeManager(store, new Gson());
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
}
