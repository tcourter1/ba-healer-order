package com.bahealerorder.common;

import java.util.Collections;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BaWaveOverviewRun
{
	private final String id;
	private final String name;
	private final Map<Integer, BaWaveOverviewSnapshot> snapshotsByWave;
	private final String roundDuration;
	private final List<BaTeamMember> teamMembers;

	BaWaveOverviewRun(
			String id,
			String name,
			Map<Integer, BaWaveOverviewSnapshot> snapshotsByWave,
			String roundDuration,
			List<BaTeamMember> teamMembers)
	{
		this.id = id;
		this.name = name;
		this.snapshotsByWave = Collections.unmodifiableMap(new HashMap<>(snapshotsByWave));
		this.roundDuration = roundDuration;
		this.teamMembers = Collections.unmodifiableList(new ArrayList<>(teamMembers));
	}

	public String getId()
	{
		return id;
	}

	public String getName()
	{
		return name;
	}

	public BaWaveOverviewSnapshot getSnapshot(int wave)
	{
		return snapshotsByWave.get(wave);
	}

	public Map<Integer, BaWaveOverviewSnapshot> getSnapshotsByWave()
	{
		return snapshotsByWave;
	}

	public String getRoundDuration()
	{
		return roundDuration;
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

	public List<BaTeamMember> getTeamMembers()
	{
		return teamMembers;
	}

	public String metadataSignature()
	{
		return roundDuration + ":" + teamMembers;
	}
}
