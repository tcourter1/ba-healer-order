package com.bahealerorder.healer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.bahealerorder.common.BaHealerSyncMessage;
import com.bahealerorder.common.BaOverviewNpcType;
import com.bahealerorder.common.BaWaveOverviewSnapshot;
import com.bahealerorder.common.BaWaveOverviewState;
import org.junit.Test;

public class HealerSharedStateTest
{
	@Test
	public void observedDeathOverwritesEarlierPresumedDeath()
	{
		HealerSharedState state = new HealerSharedState();

		assertTrue(state.recordPresumedDeath(1, 204));
		assertEquals(204, state.getActualDeathTick(1).intValue());
		assertFalse(state.isObservedDeath(1));

		assertTrue(state.recordDeath(1, 207));
		assertEquals(207, state.getActualDeathTick(1).intValue());
		assertTrue(state.isObservedDeath(1));

		assertFalse(state.recordDeath(1, 208));
		assertEquals(207, state.getActualDeathTick(1).intValue());
	}

	@Test
	public void visibleAliveHealerClearsPresumedDeath()
	{
		HealerSharedState state = new HealerSharedState();

		assertTrue(state.recordPresumedDeath(1, 204));
		assertTrue(state.clearPresumedDeath(1));
		assertNull(state.getActualDeathTick(1));

		assertTrue(state.recordDeath(1, 207));
		assertEquals(207, state.getActualDeathTick(1).intValue());
	}

	@Test
	public void partyPredictionIsOnlyAppliedWhenAccepted()
	{
		HealerSharedState state = new HealerSharedState();
		BaHealerSyncMessage message = message(30, false, false);

		state.updateFromParty(message, false);
		assertNull(state.getPredictedDeathTick(1));

		state.updateFromParty(message, true);
		assertEquals(30, state.getPredictedDeathTick(1).intValue());
	}

	@Test
	public void healthRatioModeIsMonotonic()
	{
		HealerSharedState state = new HealerSharedState();

		state.updateFromParty(message(30, false, true), true);
		assertTrue(state.isHealthRatioMode(1));

		state.updateFromParty(message(31, false, false), true);
		assertTrue(state.isHealthRatioMode(1));
	}

	@Test
	public void overviewSnapshotIncludesHealerPredictionAndDeath()
	{
		HealerSharedState state = new HealerSharedState();
		state.startWave(1);
		state.recordLocalSpawn(1, 10, 0);
		state.recordPrediction(1, 30, false);

		BaWaveOverviewSnapshot predicted = BaWaveOverviewSnapshot.fromStates(1, new BaWaveOverviewState(), state);
		assertEquals(30, predicted.getPredictedDeathTick(BaOverviewNpcType.HEALER, 1).intValue());

		state.recordDeath(1, 31);
		BaWaveOverviewSnapshot dead = BaWaveOverviewSnapshot.fromStates(1, new BaWaveOverviewState(), state);
		assertEquals(31, dead.getDeathTick(BaOverviewNpcType.HEALER, 1).intValue());
		assertNull(dead.getPredictedDeathTick(BaOverviewNpcType.HEALER, 1));
	}

	private static BaHealerSyncMessage message(int predictedDeathTick, boolean unknownTtk, boolean healthRatioMode)
	{
		return new BaHealerSyncMessage(
				1,
				1,
				10,
				1,
				0,
				0,
				predictedDeathTick,
				unknownTtk,
				-1,
				false,
				healthRatioMode,
				new int[0]
		);
	}
}
