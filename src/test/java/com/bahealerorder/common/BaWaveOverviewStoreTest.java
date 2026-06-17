package com.bahealerorder.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.bahealerorder.healer.HealerSharedState;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.util.Arrays;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class BaWaveOverviewStoreTest
{
	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

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
	public void sameLaterWaveAfterFailedWaveStaysInCurrentRun()
	{
		BaWaveOverviewStore store = new BaWaveOverviewStore();

		store.startWave(1);
		store.completeSnapshot(BaWaveOverviewSnapshot.blank(1));
		store.startWave(2);
		store.completeSnapshot(BaWaveOverviewSnapshot.blank(2));
		store.startWave(9);
		store.leaveWave(9);
		store.startWave(9);
		store.completeSnapshot(BaWaveOverviewSnapshot.blank(9));

		BaWaveOverviewRun run = store.getRuns().get(0);
		assertEquals(1, store.getRuns().size());
		assertEquals(3, run.getSnapshotsByWave().size());
		assertNotNull(run.getSnapshot(9));
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
	public void completedSnapshotPreservesNpcDeathsAndDurationForHistoricalSelection()
	{
		BaWaveOverviewStore store = new BaWaveOverviewStore();
		BaWaveOverviewState state = new BaWaveOverviewState();
		state.startWave(1);
		state.recordDeath(BaOverviewNpcType.RANGER, 100, 25);
		state.recordDeath(BaOverviewNpcType.FIGHTER, 101, 30);

		store.startWave(1);
		store.completeSnapshot(BaWaveOverviewSnapshot.fromStates(1, state, new HealerSharedState()).withDuration("0:42.6"));

		BaWaveOverviewRun run = store.getRuns().get(0);
		store.setSelectedRunId(run.getId());
		store.setSelectedWave(1);

		BaWaveOverviewSnapshot snapshot = store.getSelectedSnapshot();
		assertEquals("0:42.6", snapshot.getDuration());
		assertEquals(Integer.valueOf(25), snapshot.getDeathTick(BaOverviewNpcType.RANGER, 1));
		assertEquals(Integer.valueOf(30), snapshot.getDeathTick(BaOverviewNpcType.FIGHTER, 1));
	}

	@Test
	public void runMetadataStoresTeamNamesAndRoundDuration()
	{
		BaWaveOverviewStore store = new BaWaveOverviewStore();

		store.startWave(1);
		assertTrue(store.updateCurrentRunTeamMembers(Arrays.asList(
				new BaTeamMember("Leader", "Healer"),
				new BaTeamMember("Player2", "Attacker"),
				new BaTeamMember("Player3", "Defender"),
				new BaTeamMember("Player4", "Collector"),
				new BaTeamMember("Player5", "Attacker"))));
		store.completeSnapshot(BaWaveOverviewSnapshot.blank(1));
		assertTrue(store.updateLatestCompletedRunRoundDuration("14:04.2"));

		BaWaveOverviewRun run = store.getRuns().get(0);
		assertEquals("14:04.2", run.getRoundDuration());
		assertEquals(Arrays.asList("Leader", "Player2", "Player3", "Player4", "Player5"), run.getTeamNames());
		assertEquals("Healer", run.getTeamMembers().get(0).getRole());
		assertEquals("Attacker", run.getTeamMembers().get(1).getRole());
	}

	@Test
	public void roundDurationAppliesToMostRecentCompletedRun()
	{
		BaWaveOverviewStore store = new BaWaveOverviewStore();

		store.startWave(10);
		store.completeSnapshot(BaWaveOverviewSnapshot.blank(10));
		store.startWave(1);
		assertTrue(store.updateLatestCompletedRunRoundDuration("14:04.2"));

		BaWaveOverviewRun newestRun = store.getRuns().get(0);
		BaWaveOverviewRun previousRun = store.getRuns().get(1);
		assertNull(newestRun.getRoundDuration());
		assertEquals("14:04.2", previousRun.getRoundDuration());
	}

	@Test
	public void completedRunsReloadFromFile() throws Exception
	{
		File runsDirectory = temporaryFolder.newFolder("runs");
		BaWaveOverviewState state = new BaWaveOverviewState();
		state.startWave(1);
		state.recordDeath(BaOverviewNpcType.RANGER, 100, 25);

		BaWaveOverviewStore store = new BaWaveOverviewStore(new GsonBuilder().setPrettyPrinting().create(), runsDirectory);
		store.startWave(1);
		store.updateCurrentRunTeamMembers(Arrays.asList(
				new BaTeamMember("Leader", "Healer"),
				new BaTeamMember("Player2", "Attacker"),
				new BaTeamMember("Player3", "Defender"),
				new BaTeamMember("Player4", "Collector"),
				new BaTeamMember("Player5", "Attacker")));
		store.completeSnapshot(BaWaveOverviewSnapshot.fromStates(1, state, new HealerSharedState()).withDuration("0:42.6"));
		store.updateLatestCompletedRunRoundDuration("14:04.2");

		File[] files = runsDirectory.listFiles((directory, name) -> name.endsWith(".json"));
		assertNotNull(files);
		assertEquals(1, files.length);

		BaWaveOverviewStore reloaded = new BaWaveOverviewStore(new GsonBuilder().setPrettyPrinting().create(), runsDirectory);
		BaWaveOverviewRun run = reloaded.getRuns().get(0);
		reloaded.setSelectedRunId(run.getId());
		reloaded.setSelectedWave(1);

		assertEquals(run.getName().replace(':', '.') + ".json", files[0].getName());
		assertEquals("14:04.2", run.getRoundDuration());
		assertEquals(Arrays.asList("Leader", "Player2", "Player3", "Player4", "Player5"), run.getTeamNames());
		assertEquals("Healer", run.getTeamMembers().get(0).getRole());
		assertEquals("Collector", run.getTeamMembers().get(3).getRole());
		assertEquals("0:42.6", reloaded.getSelectedSnapshot().getDuration());
		assertEquals(Integer.valueOf(25), reloaded.getSelectedSnapshot().getDeathTick(BaOverviewNpcType.RANGER, 1));
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
