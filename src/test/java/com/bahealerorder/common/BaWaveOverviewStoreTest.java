package com.bahealerorder.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.bahealerorder.healer.HealerSharedState;
import org.junit.Test;

public class BaWaveOverviewStoreTest
{
	@Test
	public void duplicateWaveStartDoesNotCreateNewRun()
	{
		BaWaveOverviewStore store = new BaWaveOverviewStore();

		store.startWave(1);
		store.startWave(1);

		assertEquals(1, store.getRuns().size());
	}

	@Test
	public void lowerWaveAfterCompletedWaveStartsNewRun()
	{
		BaWaveOverviewStore store = new BaWaveOverviewStore();

		store.startWave(1);
		store.completeSnapshot(BaWaveOverviewSnapshot.blank(1));
		store.startWave(2);
		store.startWave(1);

		assertEquals(2, store.getRuns().size());
	}

	@Test
	public void sameWaveAfterCompletionStartsNewRun()
	{
		BaWaveOverviewStore store = new BaWaveOverviewStore();

		store.startWave(1);
		store.completeSnapshot(BaWaveOverviewSnapshot.blank(1));
		store.startWave(1);

		assertEquals(2, store.getRuns().size());
	}

	@Test
	public void completedWavesAccumulateInCurrentRun()
	{
		BaWaveOverviewStore store = new BaWaveOverviewStore();

		store.startWave(1);
		store.completeSnapshot(BaWaveOverviewSnapshot.blank(1));
		store.startWave(2);
		store.completeSnapshot(BaWaveOverviewSnapshot.blank(2));

		BaWaveOverviewRun run = store.getRuns().get(0);
		assertEquals(1, store.getRuns().size());
		assertEquals(2, run.getSnapshotsByWave().size());
	}

	@Test
	public void completedSnapshotStoresDuration()
	{
		BaWaveOverviewStore store = new BaWaveOverviewStore();

		store.startWave(1);
		store.completeSnapshot(BaWaveOverviewSnapshot.blank(1).withDuration("0:42.6"));

		BaWaveOverviewRun run = store.getRuns().get(0);
		assertEquals("0:42.6", run.getSnapshot(1).getDuration());
	}

	@Test
	public void runNameUsesDateTime()
	{
		BaWaveOverviewStore store = new BaWaveOverviewStore();

		store.startWave(1);

		BaWaveOverviewRun run = store.getRuns().get(0);
		assertTrue(run.getName().matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"));
	}

	@Test
	public void noSelectedWaveReturnsNoSnapshot()
	{
		BaWaveOverviewStore store = new BaWaveOverviewStore();

		assertNull(store.getSelectedSnapshot());
	}

	@Test
	public void blankRunSelectionReturnsBlankWave()
	{
		BaWaveOverviewStore store = new BaWaveOverviewStore();
		store.setSelectedRunId(null);
		store.setSelectedWave(9);

		BaWaveOverviewSnapshot snapshot = store.getSelectedSnapshot();

		assertNotNull(snapshot);
		assertEquals(9, snapshot.getWave());
		assertFalse(snapshot.hasSpawned(BaOverviewNpcType.RANGER, 1));
		assertNull(snapshot.getDeathTick(BaOverviewNpcType.HEALER, 1));
	}

	@Test
	public void selectedRunReturnsSavedWaveSnapshot()
	{
		BaWaveOverviewStore store = new BaWaveOverviewStore();
		BaWaveOverviewState state = new BaWaveOverviewState();
		state.startWave(1);
		state.recordSpawn(BaOverviewNpcType.RUNNER, 100);

		store.startWave(1);
		assertTrue(store.saveSnapshot(BaWaveOverviewSnapshot.fromStates(1, state, new HealerSharedState())));

		BaWaveOverviewRun run = store.getRuns().get(0);
		store.setSelectedRunId(run.getId());
		store.setSelectedWave(1);

		assertTrue(store.getSelectedSnapshot().hasSpawned(BaOverviewNpcType.RUNNER, 1));
	}
}
