package com.bahealerorder.healer.ttk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HealerTtkTrackerTest
{
	@Test
	public void waveOneSingleFoodDiesFromKnownMaxHp()
	{
		HealerTtkTracker tracker = new HealerTtkTracker();
		tracker.startWave(0, 1);
		tracker.onHealerSpawned(10, 1, 0);
		tracker.onFoodConsumedForHealer(10, 0);

		HealerTtkResult result = tracker.getTtk(10, 0).orElse(null);

		assertNotNull(result);
		assertEquals(27, tracker.getState(10).getMaxHp().intValue());
		assertEquals(30, result.getDeathTick());
	}

	@Test
	public void waveTenSingleFoodCannotKillHealer()
	{
		HealerTtkTracker tracker = new HealerTtkTracker();
		tracker.startWave(0, 10);
		tracker.onHealerSpawned(10, 1, 0);
		tracker.onFoodConsumedForHealer(10, 0);

		assertFalse(tracker.getTtk(10, 0).isPresent());
		assertTrue(tracker.hasPoisonedHealerWithUnknownTtk(10));
	}

	@Test
	public void repeatedFoodRefreshesDamageAndSubtractsFoodDamage()
	{
		HealerTtkTracker tracker = new HealerTtkTracker();
		tracker.startWave(0, 10);
		tracker.onHealerSpawned(10, 1, 0);
		tracker.onFoodConsumedForHealer(10, 0);
		tracker.onFoodConsumedForHealer(10, 25);

		HealerTtkResult result = tracker.getTtk(10, 25).orElse(null);

		assertNotNull(result);
		assertEquals(2, tracker.getState(10).getConfirmedFoodCount());
		assertEquals(70, result.getDeathTick());
	}

	@Test
	public void outOfOrderFoodEventsReplayChronologically()
	{
		HealerTtkTracker tracker = new HealerTtkTracker();
		tracker.startWave(0, 10);
		tracker.onHealerSpawned(10, 1, 0);
		tracker.onFoodConsumedForHealer(10, 25);
		tracker.onFoodConsumedForHealer(10, 0);

		assertEquals(70, tracker.getTtk(10, 25).get().getDeathTick());
	}

	@Test
	public void sameTickFoodEventsBothCount()
	{
		HealerTtkTracker tracker = new HealerTtkTracker();
		tracker.startWave(0, 1);
		tracker.onHealerSpawned(10, 1, 0);
		tracker.onFoodConsumedForHealer(10, 0);
		tracker.onFoodConsumedForHealer(10, 0);

		assertEquals(25, tracker.getTtk(10, 0).get().getDeathTick());
	}

	@Test
	public void sameTickRepoisonDoesNotEraseExistingPoisonHit()
	{
		HealerTtkTracker tracker = new HealerTtkTracker();
		tracker.startWave(0, 2);
		tracker.onHealerSpawned(10, 1, 0);
		tracker.onFoodConsumedForHealer(10, 0);
		tracker.onFoodConsumedForHealer(10, 25);

		assertEquals(30, tracker.getTtk(10, 25).get().getDeathTick());
	}

	@Test
	public void earlyFoodUsesGlobalWaveCadence()
	{
		HealerTtkTracker tracker = new HealerTtkTracker();
		tracker.startWave(0, 1);
		tracker.onHealerSpawned(10, 2, 20);
		tracker.onFoodConsumedForHealer(10, 24);

		assertEquals(25, tracker.getState(10).getFirstPoisonTick());
		assertEquals(50, tracker.getTtk(10, 24).get().getDeathTick());
	}

	@Test
	public void lateFoodUsesFoodRelativeCadence()
	{
		HealerTtkTracker tracker = new HealerTtkTracker();
		tracker.startWave(0, 1);
		tracker.onHealerSpawned(10, 2, 20);
		tracker.onFoodConsumedForHealer(10, 26);

		assertEquals(31, tracker.getState(10).getFirstPoisonTick());
		assertEquals(56, tracker.getTtk(10, 26).get().getDeathTick());
	}

	@Test
	public void invalidWaveDoesNotDisplayTtk()
	{
		HealerTtkTracker tracker = new HealerTtkTracker();
		tracker.startWave(0, 0);
		tracker.onHealerSpawned(10, 1, 0);
		tracker.onFoodConsumedForHealer(10, 0);

		assertFalse(tracker.getTtk(10, 0).isPresent());
		assertFalse(tracker.hasPoisonedHealerWithUnknownTtk(10));
	}
}
