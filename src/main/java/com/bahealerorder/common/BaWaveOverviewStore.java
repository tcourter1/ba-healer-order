package com.bahealerorder.common;

import com.google.gson.Gson;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.RuneLite;

@Singleton
public class BaWaveOverviewStore
{
	private static final DateTimeFormatter RUN_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final String STORE_DIRECTORY = "ba-utilities";
	private static final String RUNS_DIRECTORY = "runs";
	private static final String RUN_FILE_EXTENSION = ".json";

	private final Map<String, MutableRun> runsById = new LinkedHashMap<>();
	private final Gson gson;
	private final File runsDirectory;

	private String currentRunId;
	private String selectedRunId;
	private int selectedWave = -1;
	private int activeWave = -1;

	@Inject
	public BaWaveOverviewStore(Gson gson)
	{
		this(gson.newBuilder().setPrettyPrinting().create(), getDefaultRunsDirectory());
	}

	BaWaveOverviewStore(Gson gson, File runsDirectory)
	{
		this.gson = gson;
		this.runsDirectory = runsDirectory;
		load();
	}

	public synchronized void startWave(int wave)
	{
		if (!BaWaveInfo.isValidWave(wave)) return;

		if (currentRunId == null || wave == 1 && activeWave != 1)
		{
			startRun();
		}

		activeWave = wave;
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
		boolean changed = previous == null || !previous.signature().equals(snapshot.signature());

		if (changed)
		{
			saveRun(run);
		}

		return changed;
	}

	public synchronized boolean updateCurrentRunTeamMembers(List<BaTeamMember> teamMembers)
	{
		if (currentRunId == null) return false;

		List<BaTeamMember> normalizedMembers = normalizeTeamMembers(teamMembers);
		if (normalizedMembers.isEmpty()) return false;

		MutableRun run = runsById.get(currentRunId);
		if (run.teamMembers.equals(normalizedMembers)) return false;

		run.teamMembers = normalizedMembers;
		saveRun(run);
		return true;
	}

	public synchronized boolean updateCurrentRunPlayerRole(BaRole role)
	{
		if (currentRunId == null || role == null) return false;

		MutableRun run = runsById.get(currentRunId);
		String playerRole = role.getDisplayName();
		if (playerRole.equals(run.playerRole)) return false;

		run.playerRole = playerRole;
		saveRun(run);
		return true;
	}

	public synchronized boolean updateLatestCompletedRunRoundDuration(String roundDuration)
	{
		if (roundDuration == null || roundDuration.isEmpty()) return false;

		List<MutableRun> runs = new ArrayList<>(runsById.values());
		Collections.reverse(runs);

		for (MutableRun run : runs)
		{
			if (run.completedSnapshotsByWave.isEmpty()) continue;
			if (roundDuration.equals(run.roundDuration)) return false;

			run.roundDuration = roundDuration;
			saveRun(run);
			return true;
		}

		return false;
	}

	public synchronized boolean saveCurrentRunWaveDuration(int wave, String duration)
	{
		if (!BaWaveInfo.isValidWave(wave) || duration == null || duration.isEmpty() || currentRunId == null) return false;

		MutableRun run = runsById.get(currentRunId);
		BaWaveOverviewSnapshot snapshot = run.completedSnapshotsByWave.get(wave);
		if (snapshot == null)
		{
			snapshot = run.liveSnapshotsByWave.remove(wave);
		}
		if (snapshot == null) return false;

		if (activeWave == wave)
		{
			activeWave = -1;
		}

		BaWaveOverviewSnapshot updated = snapshot.withDuration(duration);
		BaWaveOverviewSnapshot previous = run.completedSnapshotsByWave.put(wave, updated);
		boolean changed = previous == null || !previous.signature().equals(updated.signature());

		if (changed)
		{
			saveRun(run);
		}

		return changed;
	}

	public synchronized void leaveWave(int wave)
	{
		if (!BaWaveInfo.isValidWave(wave) || currentRunId == null) return;

		MutableRun run = runsById.get(currentRunId);
		run.liveSnapshotsByWave.remove(wave);

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
			runs.add(run.snapshot(isCurrentRunInProgress(run)));
		}

		Collections.reverse(runs);
		return runs;
	}

	public synchronized BaWaveOverviewSnapshot getSelectedSnapshot()
	{
		if (!BaWaveInfo.isValidWave(selectedWave)) return null;

		if (selectedRunId == null) return BaWaveOverviewSnapshot.blank(selectedWave);

		MutableRun run = runsById.get(selectedRunId);
		if (run == null) return BaWaveOverviewSnapshot.blank(selectedWave);

		BaWaveOverviewSnapshot snapshot = run.liveSnapshotsByWave.get(selectedWave);
		if (snapshot != null) return snapshot;

		snapshot = run.completedSnapshotsByWave.get(selectedWave);
		return snapshot == null ? BaWaveOverviewSnapshot.blank(selectedWave) : snapshot;
	}

	public synchronized BaWaveOverviewRun getSelectedRun()
	{
		MutableRun run = selectedRunId == null ? null : runsById.get(selectedRunId);
		return run == null ? null : run.snapshot(isCurrentRunInProgress(run));
	}

	public synchronized String getSelectedRunId()
	{
		return selectedRunId;
	}

	public File getRunsDirectory()
	{
		return runsDirectory;
	}

	public synchronized void setSelectedRunId(String selectedRunId)
	{
		this.selectedRunId = selectedRunId == null || selectedRunId.isEmpty() ? null : selectedRunId;
	}

	public synchronized boolean isSelectedWaveInProgress()
	{
		return selectedRunId != null
				&& selectedRunId.equals(currentRunId)
				&& BaWaveInfo.isValidWave(selectedWave)
				&& selectedWave == activeWave;
	}

	public synchronized int getSelectedWave()
	{
		return selectedWave;
	}

	public synchronized void setSelectedWave(int selectedWave)
	{
		this.selectedWave = BaWaveInfo.isValidWave(selectedWave) ? selectedWave : -1;
	}

	public synchronized boolean deleteRun(String runId)
	{
		if (runId == null || runId.isEmpty()) return false;

		MutableRun run = runsById.remove(runId);
		if (run == null) return false;

		if (runId.equals(currentRunId))
		{
			currentRunId = null;
			activeWave = -1;
		}

		if (runId.equals(selectedRunId))
		{
			selectedRunId = null;
			selectedWave = -1;
		}

		deleteRunFile(run);
		return true;
	}

	private void startRun()
	{
		String name = LocalDateTime.now().format(RUN_TIME_FORMAT);
		String fileName = createRunFileName(name);
		String id = fileName.substring(0, fileName.length() - RUN_FILE_EXTENSION.length());
		runsById.put(id, new MutableRun(id, name, fileName));
		currentRunId = id;
		activeWave = -1;
	}

	private static File getDefaultRunsDirectory()
	{
		return new File(new File(RuneLite.RUNELITE_DIR, STORE_DIRECTORY), RUNS_DIRECTORY);
	}

	private void load()
	{
		if (runsDirectory == null || !runsDirectory.exists() || !runsDirectory.isDirectory()) return;

		File[] runFiles = runsDirectory.listFiles(file -> file.isFile() && file.getName().endsWith(RUN_FILE_EXTENSION));

		if (runFiles == null) return;

		List<MutableRun> loadedRuns = new ArrayList<>();

		for (File runFile : runFiles)
		{
			try (Reader reader = new InputStreamReader(new FileInputStream(runFile), StandardCharsets.UTF_8))
			{
				StoredRun storedRun = gson.fromJson(reader, StoredRun.class);

				if (storedRun == null || storedRun.id == null || storedRun.name == null) continue;

				loadedRuns.add(MutableRun.fromStoredRun(storedRun, runFile.getName()));
			}
			catch (RuntimeException | java.io.IOException ignored)
			{
			}
		}

		loadedRuns.sort(Comparator.comparing(run -> run.name));
		runsById.clear();

		for (MutableRun run : loadedRuns)
		{
			runsById.put(run.id, run);
		}
	}

	private void saveRun(MutableRun run)
	{
		if (runsDirectory == null || run.completedSnapshotsByWave.isEmpty()) return;

		try
		{
			if (!runsDirectory.exists() && !runsDirectory.mkdirs())
			{
				return;
			}

			File runFile = new File(runsDirectory, run.fileName);
			File temporaryFile = new File(runsDirectory, run.fileName + ".tmp");
			try (Writer writer = new OutputStreamWriter(new FileOutputStream(temporaryFile), StandardCharsets.UTF_8))
			{
				gson.toJson(run.toStoredRun(), writer);
			}

			try
			{
				Files.move(
						temporaryFile.toPath(),
						runFile.toPath(),
						StandardCopyOption.REPLACE_EXISTING,
						StandardCopyOption.ATOMIC_MOVE
				);
			}
			catch (AtomicMoveNotSupportedException ignored)
			{
				Files.move(temporaryFile.toPath(), runFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			}
		}
		catch (RuntimeException | java.io.IOException ignored)
		{
		}
	}

	private void deleteRunFile(MutableRun run)
	{
		if (runsDirectory == null) return;

		try
		{
			Files.deleteIfExists(new File(runsDirectory, run.fileName).toPath());
		}
		catch (RuntimeException | java.io.IOException ignored)
		{
		}
	}

	private boolean isCurrentRunInProgress(MutableRun run)
	{
		return run.id.equals(currentRunId)
				&& (run.roundDuration == null || run.roundDuration.isEmpty());
	}

	private List<BaTeamMember> normalizeTeamMembers(List<BaTeamMember> teamMembers)
	{
		List<BaTeamMember> normalizedMembers = new ArrayList<>();

		if (teamMembers == null) return normalizedMembers;

		for (BaTeamMember member : teamMembers)
		{
			if (member == null || member.getName() == null || member.getName().isBlank()) continue;

			String role = member.getRole() == null || member.getRole().isBlank()
					? null
					: member.getRole().trim();
			normalizedMembers.add(new BaTeamMember(member.getName().trim(), role));
		}

		return normalizedMembers;
	}

	private String createRunFileName(String runName)
	{
		String baseFileName = runName.replace(':', '.');
		String fileName = baseFileName + RUN_FILE_EXTENSION;
		int suffix = 2;

		while (runsById.containsKey(fileName.substring(0, fileName.length() - RUN_FILE_EXTENSION.length()))
				|| runsDirectory != null && new File(runsDirectory, fileName).exists())
		{
			fileName = baseFileName + "-" + suffix++ + RUN_FILE_EXTENSION;
		}

		return fileName;
	}

	private static class MutableRun
	{
		private final String id;
		private final String name;
		private final String fileName;
		private final Map<Integer, BaWaveOverviewSnapshot> completedSnapshotsByWave = new HashMap<>();
		private final Map<Integer, BaWaveOverviewSnapshot> liveSnapshotsByWave = new HashMap<>();
		private String roundDuration;
		private String playerRole;
		private List<BaTeamMember> teamMembers = new ArrayList<>();

		private MutableRun(String id, String name, String fileName)
		{
			this.id = id;
			this.name = name;
			this.fileName = fileName;
		}

		private BaWaveOverviewRun snapshot(boolean current)
		{
			return new BaWaveOverviewRun(id, name, completedSnapshotsByWave, roundDuration, teamMembers, playerRole, current);
		}

		private StoredRun toStoredRun()
		{
			StoredRun run = new StoredRun();
			run.id = id;
			run.name = name;
			run.roundDuration = roundDuration;
			run.playerRole = playerRole;
			run.teamMembers = new ArrayList<>(teamMembers);
			run.completedSnapshotsByWave = new HashMap<>(completedSnapshotsByWave);
			return run;
		}

		private static MutableRun fromStoredRun(StoredRun storedRun, String fileName)
		{
			MutableRun run = new MutableRun(storedRun.id, storedRun.name, fileName);
			run.roundDuration = storedRun.roundDuration;
			run.playerRole = storedRun.playerRole;
			run.teamMembers = storedRun.getTeamMembers();

			if (storedRun.completedSnapshotsByWave != null)
			{
				run.completedSnapshotsByWave.putAll(storedRun.completedSnapshotsByWave);
			}

			return run;
		}
	}

	private static class StoredRun
	{
		private String id;
		private String name;
		private String roundDuration;
		private String playerRole;
		private List<BaTeamMember> teamMembers = new ArrayList<>();
		private List<String> teamNames = new ArrayList<>();
		private Map<Integer, BaWaveOverviewSnapshot> completedSnapshotsByWave = new HashMap<>();

		private List<BaTeamMember> getTeamMembers()
		{
			if (teamMembers != null && !teamMembers.isEmpty()) return new ArrayList<>(teamMembers);

			List<BaTeamMember> members = new ArrayList<>();

			if (teamNames != null)
			{
				for (String name : teamNames)
				{
					members.add(new BaTeamMember(name, null));
				}
			}

			return members;
		}
	}
}
