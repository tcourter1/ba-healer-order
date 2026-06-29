package com.bahealerorder.tilemarkers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class GeneralTileMarkerStrategyManagerTest
{
	@Test
	public void savingBuiltInStrategyOverridesExistingBuiltInId()
	{
		TestStrategyManager manager = new TestStrategyManager();
		TileMarkerStrategyPreset builtIn = manager.findStrategyPreset(
				TileMarkerBuiltInStrategies.BEGINNER_DEFENDER_TILES_STRATEGY_ID);
		assertNotNull(builtIn);

		manager.setWaveStrategyPresetId(TileMarkerRoleContext.DEFENDER, 1, builtIn.getId());
		String notes = "12.0 - Test defender note";

		TileMarkerStrategyPreset saved = manager.saveStrategyPreset(
				builtIn.getId(),
				builtIn.getName(),
				notes,
				builtIn.getWaveMap(),
				builtIn.getMarkerSetIds());

		assertNotNull(saved);
		assertEquals(builtIn.getId(), saved.getId());
		assertTrue(saved.isBuiltIn());
		assertEquals(notes, manager.findStrategyPreset(builtIn.getId()).getNotes());
		assertEquals(notes, manager.getActiveNotes(1, TileMarkerRoleContext.DEFENDER));
		assertEquals(1, countPresetId(manager, builtIn.getId()));
	}

	private static int countPresetId(TestStrategyManager manager, String id)
	{
		int count = 0;
		for (TileMarkerStrategyPreset preset : manager.getStrategyPresets(null))
		{
			if (id.equals(preset.getId()))
			{
				count++;
			}
		}
		return count;
	}

	private static class TestStrategyManager extends GeneralTileMarkerStrategyManager
	{
		private TestStrategyManager()
		{
			super(null, null);
		}

		@Override
		public void save()
		{
		}
	}
}
