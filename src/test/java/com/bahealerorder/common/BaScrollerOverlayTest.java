package com.bahealerorder.common;

import static org.junit.Assert.assertEquals;

import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

public class BaScrollerOverlayTest
{
	@Test
	public void mapsWaitingRoomsToWaves()
	{
		assertEquals(1, BaScrollerOverlay.getWaitingRoomWave(new WorldPoint(2576, 5298, 0)));
		assertEquals(3, BaScrollerOverlay.getWaitingRoomWave(new WorldPoint(2595, 5298, 0)));
		assertEquals(10, BaScrollerOverlay.getWaitingRoomWave(new WorldPoint(2584, 5278, 0)));
		assertEquals(-1, BaScrollerOverlay.getWaitingRoomWave(new WorldPoint(2576, 5298, 1)));
		assertEquals(-1, BaScrollerOverlay.getWaitingRoomWave(null));
	}
}
