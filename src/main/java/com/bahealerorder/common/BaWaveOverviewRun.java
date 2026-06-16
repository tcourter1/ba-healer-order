package com.bahealerorder.common;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class BaWaveOverviewRun
{
	private final String id;
	private final String name;
	private final Map<Integer, BaWaveOverviewSnapshot> snapshotsByWave;

	BaWaveOverviewRun(String id, String name, Map<Integer, BaWaveOverviewSnapshot> snapshotsByWave)
	{
		this.id = id;
		this.name = name;
		this.snapshotsByWave = Collections.unmodifiableMap(new HashMap<>(snapshotsByWave));
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
}
