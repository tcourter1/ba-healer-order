package com.bahealerorder.defender.strategies;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DefenderMapLayoutTest
{
	@Test
	public void convertsWaveOneToNineMapCoordinatesToRegionTiles()
	{
		DefenderTile tile = DefenderMapLayout.WAVES_1_TO_9.toTile(45, 26);

		assertEquals(7509, tile.getRegionId());
		assertEquals(45, tile.getRegionX());
		assertEquals(34, tile.getRegionY());
		assertEquals(45, DefenderMapLayout.WAVES_1_TO_9.toMapX(tile));
		assertEquals(26, DefenderMapLayout.WAVES_1_TO_9.toMapY(tile));
		assertTrue(DefenderMapLayout.WAVES_1_TO_9.contains(tile));
	}

	@Test
	public void convertsWaveTenMapCoordinatesToRegionTiles()
	{
		DefenderTile tile = DefenderMapLayout.WAVE_10.toTile(15, 25);

		assertEquals(7508, tile.getRegionId());
		assertEquals(15, tile.getRegionX());
		assertEquals(33, tile.getRegionY());
		assertEquals(15, DefenderMapLayout.WAVE_10.toMapX(tile));
		assertEquals(25, DefenderMapLayout.WAVE_10.toMapY(tile));
		assertTrue(DefenderMapLayout.WAVE_10.contains(tile));
	}
}
