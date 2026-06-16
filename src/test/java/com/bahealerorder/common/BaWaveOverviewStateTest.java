package com.bahealerorder.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BaWaveOverviewStateTest
{
	@Test
	public void recordsDeathTickForNpcOrder()
	{
		BaWaveOverviewState state = new BaWaveOverviewState();
		state.startWave(1);
		state.recordSpawn(BaOverviewNpcType.RANGER, 100);

		assertNull(state.getDeathTick(BaOverviewNpcType.RANGER, 1));
		assertTrue(state.recordDeath(BaOverviewNpcType.RANGER, 100, 12));
		assertTrue(state.isDead(BaOverviewNpcType.RANGER, 1));
		assertEquals(12, state.getDeathTick(BaOverviewNpcType.RANGER, 1).intValue());

		assertFalse(state.recordDeath(BaOverviewNpcType.RANGER, 100, 15));
		assertEquals(12, state.getDeathTick(BaOverviewNpcType.RANGER, 1).intValue());
	}

	@Test
	public void syncsDeathTickByNpcOrder()
	{
		BaWaveOverviewState source = new BaWaveOverviewState();
		BaWaveOverviewState target = new BaWaveOverviewState();

		source.startWave(1);
		source.recordSpawn(BaOverviewNpcType.FIGHTER, 200);
		source.recordDeath(BaOverviewNpcType.FIGHTER, 200, 18);

		target.updateFromParty(source.toSyncMessage(302));

		assertTrue(target.isDead(BaOverviewNpcType.FIGHTER, 1));
		assertEquals(18, target.getDeathTick(BaOverviewNpcType.FIGHTER, 1).intValue());
	}
}
