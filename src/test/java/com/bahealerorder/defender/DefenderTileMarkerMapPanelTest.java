package com.bahealerorder.defender;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.bahealerorder.defender.strategies.DefenderMapLayout;
import java.util.Collections;
import org.junit.Test;

public class DefenderTileMarkerMapPanelTest
{
	@Test
	public void eastSideModeUsesBaArenaTopologyWithinEastSlice()
	{
		DefenderTileMarkerMapPanel panel = panel(TileMarkerMapMode.EAST_SIDE_ONLY, TileMarkerWaveMap.WAVES_1_TO_9);

		assertTrue(panel.isSelectableMapTile(DefenderMapLayout.WAVES_1_TO_9, 45, 26));
		assertFalse(panel.isSelectableMapTile(DefenderMapLayout.WAVES_1_TO_9, 2, 2));
	}

	@Test
	public void fullArenaModeUsesBaArenaTopology()
	{
		DefenderTileMarkerMapPanel panel = panel(TileMarkerMapMode.FULL_MAP, TileMarkerWaveMap.WAVES_1_TO_9);

		assertTrue(panel.isSelectableMapTile(DefenderMapLayout.WAVES_1_TO_9, 33, 8));
		assertTrue(panel.isSelectableMapTile(DefenderMapLayout.WAVES_1_TO_9, 45, 26));
		assertFalse(panel.isSelectableMapTile(DefenderMapLayout.WAVES_1_TO_9, 0, 0));
		assertFalse(panel.isSelectableMapTile(DefenderMapLayout.WAVES_1_TO_9, 63, 47));
		assertFalse(panel.isSelectableMapTile(DefenderMapLayout.WAVES_1_TO_9, 64, 47));
	}

	@Test
	public void fullArenaModeUsesWaveTenTopology()
	{
		DefenderTileMarkerMapPanel panel = panel(TileMarkerMapMode.FULL_MAP, TileMarkerWaveMap.WAVE_10);

		assertTrue(panel.isSelectableMapTile(DefenderMapLayout.WAVE_10, 17, 9));
	}

	private DefenderTileMarkerMapPanel panel(TileMarkerMapMode mapMode, TileMarkerWaveMap waveMap)
	{
		return new DefenderTileMarkerMapPanel(
				waveMap::getLayout,
				() -> mapMode,
				Collections::emptyList,
				Collections::emptySet,
				() -> 10,
				(x, y) -> { }
		);
	}
}
