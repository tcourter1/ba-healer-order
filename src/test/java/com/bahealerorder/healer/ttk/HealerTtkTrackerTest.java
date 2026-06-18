package com.bahealerorder.healer.ttk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HealerTtkTrackerTest
{
	@Test
	public void waveOneSingleFoodDiesFromKnownMaxHp()
	{
		HealerTtkTracker tracker = tracker(1);
		tracker.onHealerSpawned(10, 0);
		tracker.onFoodConsumedForHealer(10, 0);

		assertEquals(27, tracker.getState(10).getMaxHp().intValue());
		assertEquals(30, deathTick(tracker, 10));
	}

	@Test
	public void waveTenSingleFoodCannotKillHealer()
	{
		HealerTtkTracker tracker = tracker(10);
		tracker.onHealerSpawned(10, 0);
		tracker.onFoodConsumedForHealer(10, 0);

		HealerTtkPrediction prediction = tracker.getPrediction(10);
		assertFalse(prediction.hasDeathTick());
		assertTrue(prediction.isUnknown());
	}

	@Test
	public void repeatedFoodRefreshesDamageAndSubtractsFoodDamage()
	{
		HealerTtkTracker tracker = tracker(10);
		tracker.onHealerSpawned(10, 0);
		tracker.onFoodConsumedForHealer(10, 0);
		tracker.onFoodConsumedForHealer(10, 25);

		assertEquals(2, tracker.getState(10).getConfirmedFoodCount());
		assertEquals(70, deathTick(tracker, 10));
	}

	@Test
	public void outOfOrderFoodEventsReplayChronologically()
	{
		HealerTtkTracker tracker = tracker(10);
		tracker.onHealerSpawned(10, 0);
		tracker.onFoodConsumedForHealer(10, 25);
		tracker.onFoodConsumedForHealer(10, 0);

		assertEquals(70, deathTick(tracker, 10));
	}

	@Test
	public void sameTickFoodEventsBothCount()
	{
		HealerTtkTracker tracker = tracker(1);
		tracker.onHealerSpawned(10, 0);
		tracker.onFoodConsumedForHealer(10, 0);
		tracker.onFoodConsumedForHealer(10, 0);

		assertEquals(25, deathTick(tracker, 10));
	}

	@Test
	public void sameTickRepoisonDoesNotEraseExistingPoisonHit()
	{
		HealerTtkTracker tracker = tracker(2);
		tracker.onHealerSpawned(10, 0);
		tracker.onFoodConsumedForHealer(10, 0);
		tracker.onFoodConsumedForHealer(10, 25);

		assertEquals(30, deathTick(tracker, 10));
	}

	@Test
	public void earlyFoodUsesGlobalWaveCadence()
	{
		HealerTtkTracker tracker = tracker(1);
		tracker.onHealerSpawned(10, 20);
		tracker.onFoodConsumedForHealer(10, 24);

		assertEquals(25, tracker.getState(10).getFirstPoisonTick());
		assertEquals(50, deathTick(tracker, 10));
	}

	@Test
	public void lateFoodUsesFoodRelativeCadence()
	{
		HealerTtkTracker tracker = tracker(1);
		tracker.onHealerSpawned(10, 20);
		tracker.onFoodConsumedForHealer(10, 26);

		assertEquals(31, tracker.getState(10).getFirstPoisonTick());
		assertEquals(56, deathTick(tracker, 10));
	}

	@Test
	public void invalidWaveDoesNotDisplayTtk()
	{
		HealerTtkTracker tracker = tracker(0);
		tracker.onHealerSpawned(10, 0);
		tracker.onFoodConsumedForHealer(10, 0);

		assertFalse(tracker.getPrediction(10).hasValue());
	}

	@Test
	public void observedHpDoesNotChangeDeterministicTtk()
	{
		HealerTtkTracker tracker = tracker(1);
		tracker.onHealerSpawned(10, 0);
		tracker.onFoodConsumedForHealer(10, 0);

		assertFalse(tracker.observeHp(10, 5, 17, 30));
		assertEquals(30, deathTick(tracker, 10));
	}

	@Test
	public void localFoodPredictionCanBePublished()
	{
		HealerTtkTracker tracker = tracker(1);
		tracker.onHealerSpawned(10, 0);
		tracker.onFoodConsumedForHealer(10, 0);

		assertTrue(tracker.getPrediction(10).isPublishable());
	}

	@Test
	public void partyFoodPredictionIsNotPublishedWithoutLocalObservation()
	{
		HealerTtkTracker tracker = tracker(1);
		tracker.onHealerSpawned(10, 0);
		tracker.onFoodConsumedForHealer(10, 0, false);

		assertEquals(30, deathTick(tracker, 10));
		assertFalse(tracker.getPrediction(10).isPublishable());
	}

	@Test
	public void explicitSelectionSwitchesToHealthRatioEstimate()
	{
		HealerTtkTracker tracker = tracker(1);
		tracker.onHealerSpawned(10, 0);
		tracker.onFoodConsumedForHealer(10, 0);

		assertTrue(tracker.switchToHealthRatioTtk(10, 5, 17, 30));
		assertEquals(25, deathTick(tracker, 10));
	}

	@Test
	public void healthRatioSelectionBeforeFoodDoesNotSelectAMode()
	{
		HealerTtkTracker tracker = tracker(1);
		tracker.onHealerSpawned(10, 0);

		assertFalse(tracker.switchToHealthRatioTtk(10, 5, 27, 30));
		assertFalse(tracker.getPrediction(10).hasValue());

		tracker.onFoodConsumedForHealer(10, 10);
		assertEquals(40, deathTick(tracker, 10));
	}

	@Test
	public void healthRatioSelectionAfterPartyFoodMakesPredictionPublishable()
	{
		HealerTtkTracker tracker = tracker(1);
		tracker.onHealerSpawned(10, 0);
		tracker.onFoodConsumedForHealer(10, 0, false);
		tracker.switchToHealthRatioTtk(10, 5, 17, 30);

		assertTrue(tracker.getPrediction(10).isPublishable());
	}

	@Test
	public void healthRatioModeUpdatesFromLaterLowerHpObservation()
	{
		HealerTtkTracker tracker = tracker(1);
		tracker.onHealerSpawned(10, 0);
		tracker.onFoodConsumedForHealer(10, 0);
		tracker.switchToHealthRatioTtk(10, 5, 17, 30);

		assertEquals(25, deathTick(tracker, 10));
		assertTrue(tracker.observeHp(10, 10, 8, 30));
		assertEquals(20, deathTick(tracker, 10));
	}

	private static HealerTtkTracker tracker(int wave)
	{
		HealerTtkTracker tracker = new HealerTtkTracker();
		tracker.startWave(0, wave);
		return tracker;
	}

	private static int deathTick(HealerTtkTracker tracker, int npcIndex)
	{
		HealerTtkPrediction prediction = tracker.getPrediction(npcIndex);
		assertTrue(prediction.hasDeathTick());
		return prediction.getDeathTick();
	}
}
