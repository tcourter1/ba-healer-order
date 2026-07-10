package com.bahealerorder.common;

import java.util.Collections;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Getter;

@Getter
public class BaWaveOverviewRun
{
	private final String id;
	private final String name;
	private final Map<Integer, BaWaveOverviewSnapshot> snapshotsByWave;
	private final String roundDuration;
	private final List<BaTeamMember> teamMembers;
	private final String playerRole;
	private final boolean current;

	BaWaveOverviewRun(
			String id,
			String name,
			Map<Integer, BaWaveOverviewSnapshot> snapshotsByWave,
			String roundDuration,
			List<BaTeamMember> teamMembers,
			String playerRole,
			boolean current)
	{
		this.id = id;
		this.name = name;
		this.snapshotsByWave = Collections.unmodifiableMap(new HashMap<>(snapshotsByWave));
		this.roundDuration = roundDuration;
		this.teamMembers = Collections.unmodifiableList(new ArrayList<>(teamMembers));
		this.playerRole = playerRole;
		this.current = current;
	}

	public BaWaveOverviewSnapshot getSnapshot(int wave)
	{
		return snapshotsByWave.get(wave);
	}

	public List<String> getTeamNames()
	{
		List<String> teamNames = new ArrayList<>();

		for (BaTeamMember member : teamMembers)
		{
			teamNames.add(member.getName());
		}

		return teamNames;
	}

	public boolean isComplete()
	{
		return roundDuration != null && !roundDuration.isEmpty();
	}

	public String metadataSignature()
	{
		return roundDuration + ":" + teamMembers + ":" + playerRole + ":" + current;
	}
}
