package com.bahealerorder.ttk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class HealerTtkTrackerTest
{
	@Test
	public void foodDoesNotDisplayTtkBeforeCurrentHpIsKnown()
	{
		HealerTtkTracker tracker = new HealerTtkTracker(new HealerPoisonModel());
		tracker.startWave(0);
		tracker.onHealerSpawned(10, 1, 0);
		tracker.onFoodConsumedForHealer(10, 100);

		assertFalse(tracker.getTtk(10, 100).isPresent());
		assertEquals(1, tracker.getState(10).getConfirmedFoodCount());
	}

	@Test
	public void firstFoodUsesObservedCurrentHp()
	{
		HealerTtkTracker tracker = new HealerTtkTracker(new HealerPoisonModel());
		tracker.startWave(0);
		tracker.onHealerSpawned(10, 1, 0);
		tracker.onFoodConsumedForHealer(10, 100);
		tracker.observeHp(10, 100, new ObservedHealerHp(46, 50));

		HealerTtkResult result = tracker.getTtk(10, 100).orElse(null);

		assertNotNull(result);
		assertEquals(Integer.valueOf(46), tracker.getState(10).getCurrentHp());
		assertEquals(180, result.getDeathTick());
	}

	@Test
	public void confirmedFoodDoesNotDisplayWhenCurrentHpCannotBeKilledByRemainingPoison()
	{
		HealerTtkTracker tracker = new HealerTtkTracker(new HealerPoisonModel());
		tracker.startWave(0);
		tracker.onHealerSpawned(10, 1, 0);
		tracker.onFoodConsumedForHealer(10, 100);
		tracker.observeHp(10, 100, new ObservedHealerHp(51, 60));

		assertFalse(tracker.getTtk(10, 100).isPresent());
	}

	@Test
	public void repeatedFoodRefreshesDamageWithoutChangingCurrentHp()
	{
		HealerTtkTracker tracker = new HealerTtkTracker(new HealerPoisonModel());
		tracker.startWave(0);
		tracker.onHealerSpawned(10, 1, 0);
		tracker.onFoodConsumedForHealer(10, 100);
		tracker.onFoodConsumedForHealer(10, 119);
		tracker.observeHp(10, 119, new ObservedHealerHp(4, 24));

		HealerTtkResult result = tracker.getTtk(10, 119).orElse(null);

		assertNotNull(result);
		assertEquals(2, tracker.getState(10).getConfirmedFoodCount());
		assertEquals(Integer.valueOf(4), tracker.getState(10).getCurrentHp());
		assertEquals(120, result.getDeathTick());
	}

	@Test
	public void repeatedFoodImmediatelySubtractsFoodDamageFromObservedHp()
	{
		HealerTtkTracker tracker = new HealerTtkTracker(new HealerPoisonModel());
		tracker.startWave(0);
		tracker.onHealerSpawned(10, 1, 0);
		tracker.onFoodConsumedForHealer(10, 100);
		tracker.observeHp(10, 118, new ObservedHealerHp(8, 24));

		tracker.onFoodConsumedForHealer(10, 119);

		assertEquals(Integer.valueOf(4), tracker.getState(10).getCurrentHp());
		assertEquals(120, tracker.getTtk(10, 119).get().getDeathTick());
	}

	@Test
	public void staleHigherHpEstimateDoesNotUndoCountedFoodDamage()
	{
		HealerTtkTracker tracker = new HealerTtkTracker(new HealerPoisonModel());
		tracker.startWave(0);
		tracker.onHealerSpawned(10, 1, 0);
		tracker.onFoodConsumedForHealer(10, 100);
		tracker.observeHp(10, 118, new ObservedHealerHp(8, 24));
		tracker.onFoodConsumedForHealer(10, 119);

		tracker.observeHp(10, 119, new ObservedHealerHp(8, 24));

		assertEquals(Integer.valueOf(4), tracker.getState(10).getCurrentHp());
		assertEquals(120, tracker.getTtk(10, 119).get().getDeathTick());
	}

	@Test
	public void currentHpCanArriveAfterFoodHasBeenCounted()
	{
		HealerTtkTracker tracker = new HealerTtkTracker(new HealerPoisonModel());
		tracker.startWave(0);
		tracker.onHealerSpawned(10, 1, 0);
		tracker.onFoodConsumedForHealer(10, 100);
		tracker.observeHp(10, 100, new ObservedHealerHp(46, 50));

		assertEquals(180, tracker.getTtk(10, 100).get().getDeathTick());
	}

	@Test
	public void sameHpObservedLaterDoesNotMoveDeathTick()
	{
		HealerTtkTracker tracker = new HealerTtkTracker(new HealerPoisonModel());
		tracker.startWave(0);
		tracker.onHealerSpawned(10, 1, 0);
		tracker.onFoodConsumedForHealer(10, 100);
		tracker.observeHp(10, 119, new ObservedHealerHp(4, 50));

		assertEquals(120, tracker.getTtk(10, 119).get().getDeathTick());

		tracker.observeHp(10, 120, new ObservedHealerHp(4, 50));

		assertEquals(120, tracker.getTtk(10, 120).get().getDeathTick());
	}

	@Test
	public void changedHpMovesDeathTick()
	{
		HealerTtkTracker tracker = new HealerTtkTracker(new HealerPoisonModel());
		tracker.startWave(0);
		tracker.onHealerSpawned(10, 1, 0);
		tracker.onFoodConsumedForHealer(10, 100);
		tracker.observeHp(10, 119, new ObservedHealerHp(8, 50));

		assertEquals(125, tracker.getTtk(10, 119).get().getDeathTick());

		tracker.observeHp(10, 120, new ObservedHealerHp(4, 50));

		assertEquals(125, tracker.getTtk(10, 120).get().getDeathTick());
	}

	@Test
	public void earlyFoodUsesGlobalWaveCadence()
	{
		HealerTtkTracker tracker = new HealerTtkTracker(new HealerPoisonModel());
		tracker.startWave(0);
		tracker.onHealerSpawned(10, 2, 20);
		tracker.onFoodConsumedForHealer(10, 24);
		tracker.observeHp(10, 24, new ObservedHealerHp(4, 24));

		assertEquals(25, tracker.getState(10).getFirstPoisonTick());
		assertEquals(25, tracker.getTtk(10, 24).get().getDeathTick());
	}

	@Test
	public void lateFoodUsesFoodRelativeCadence()
	{
		HealerTtkTracker tracker = new HealerTtkTracker(new HealerPoisonModel());
		tracker.startWave(0);
		tracker.onHealerSpawned(10, 2, 20);
		tracker.onFoodConsumedForHealer(10, 26);
		tracker.observeHp(10, 26, new ObservedHealerHp(4, 24));

		assertEquals(31, tracker.getState(10).getFirstPoisonTick());
		assertEquals(31, tracker.getTtk(10, 26).get().getDeathTick());
	}
}
