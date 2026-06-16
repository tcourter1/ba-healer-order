package com.bahealerorder.common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Singleton;

@Singleton
public class BaWaveOverviewStore
{
	private static final DateTimeFormatter RUN_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private final Map<String, MutableRun> runsById = new LinkedHashMap<>();

	private String currentRunId;
	private String selectedRunId;
	private int selectedWave = -1;
	private int nextRunNumber = 1;
	private int activeWave = -1;
	private int lastWave = -1;

	public synchronized void startWave(int wave)
	{
		if (!BaWaveInfo.isValidWave(wave)) return;

		if (currentRunId == null
				|| hasCompletedCurrentRun() && activeWave < 0 && lastWave > 0 && wave <= lastWave
				|| hasCompletedCurrentRun() && activeWave > 0 && wave < activeWave)
		{
			startRun();
		}

		activeWave = wave;
		lastWave = wave;
		selectedRunId = currentRunId;
		selectedWave = wave;
	}

	public synchronized boolean saveSnapshot(BaWaveOverviewSnapshot snapshot)
	{
		if (snapshot == null || !BaWaveInfo.isValidWave(snapshot.getWave())) return false;

		if (currentRunId == null)
		{
			startRun();
		}

		MutableRun run = runsById.get(currentRunId);
		BaWaveOverviewSnapshot previous = run.liveSnapshotsByWave.put(snapshot.getWave(), snapshot);
		return previous == null || !previous.signature().equals(snapshot.signature());
	}

	public synchronized boolean completeSnapshot(BaWaveOverviewSnapshot snapshot)
	{
		if (snapshot == null || !BaWaveInfo.isValidWave(snapshot.getWave())) return false;

		if (currentRunId == null)
		{
			startRun();
		}

		MutableRun run = runsById.get(currentRunId);
		run.liveSnapshotsByWave.remove(snapshot.getWave());

		if (activeWave == snapshot.getWave())
		{
			activeWave = -1;
		}

		BaWaveOverviewSnapshot previous = run.completedSnapshotsByWave.put(snapshot.getWave(), snapshot);
		return previous == null || !previous.signature().equals(snapshot.signature());
	}

	public synchronized void leaveWave(int wave)
	{
		if (!BaWaveInfo.isValidWave(wave) || currentRunId == null) return;

		MutableRun run = runsById.get(currentRunId);
		if (run != null)
		{
			run.liveSnapshotsByWave.remove(wave);
		}

		if (activeWave == wave)
		{
			activeWave = -1;
		}
	}

	public synchronized List<BaWaveOverviewRun> getRuns()
	{
		List<BaWaveOverviewRun> runs = new ArrayList<>();

		for (MutableRun run : runsById.values())
		{
			runs.add(run.snapshot());
		}

		Collections.reverse(runs);
		return runs;
	}

	public synchronized BaWaveOverviewSnapshot getSelectedSnapshot()
	{
		if (!BaWaveInfo.isValidWave(selectedWave))
		{
			return null;
		}

		if (selectedRunId == null)
		{
			return BaWaveOverviewSnapshot.blank(selectedWave);
		}

		MutableRun run = runsById.get(selectedRunId);
		if (run == null)
		{
			return BaWaveOverviewSnapshot.blank(selectedWave);
		}

		BaWaveOverviewSnapshot snapshot = run.completedSnapshotsByWave.get(selectedWave);
		if (snapshot != null)
		{
			return snapshot;
		}

		snapshot = run.liveSnapshotsByWave.get(selectedWave);
		return snapshot == null ? BaWaveOverviewSnapshot.blank(selectedWave) : snapshot;
	}

	public synchronized String getSelectedRunId()
	{
		return selectedRunId;
	}

	public synchronized void setSelectedRunId(String selectedRunId)
	{
		this.selectedRunId = selectedRunId == null || selectedRunId.isEmpty() ? null : selectedRunId;
	}

	public synchronized int getSelectedWave()
	{
		return selectedWave;
	}

	public synchronized void setSelectedWave(int selectedWave)
	{
		this.selectedWave = BaWaveInfo.isValidWave(selectedWave) ? selectedWave : -1;
	}

	private void startRun()
	{
		String id = "run-" + nextRunNumber++;
		String name = LocalDateTime.now().format(RUN_TIME_FORMAT);
		runsById.put(id, new MutableRun(id, name));
		currentRunId = id;
		activeWave = -1;
		lastWave = -1;
	}

	private boolean hasCompletedCurrentRun()
	{
		MutableRun run = currentRunId == null ? null : runsById.get(currentRunId);
		return run != null && !run.completedSnapshotsByWave.isEmpty();
	}

	private static class MutableRun
	{
		private final String id;
		private final String name;
		private final Map<Integer, BaWaveOverviewSnapshot> completedSnapshotsByWave = new HashMap<>();
		private final Map<Integer, BaWaveOverviewSnapshot> liveSnapshotsByWave = new HashMap<>();

		private MutableRun(String id, String name)
		{
			this.id = id;
			this.name = name;
		}

		private BaWaveOverviewRun snapshot()
		{
			return new BaWaveOverviewRun(id, name, completedSnapshotsByWave);
		}
	}
}
