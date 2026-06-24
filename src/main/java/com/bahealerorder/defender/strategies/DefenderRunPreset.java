package com.bahealerorder.defender.strategies;

import java.util.HashMap;
import java.util.Map;

public class DefenderRunPreset
{
	private String id;
	private String name;
	private boolean builtIn;
	private Map<Integer, String> waveStrategies = new HashMap<>();

	public DefenderRunPreset()
	{
	}

	public DefenderRunPreset(String id, String name, boolean builtIn, Map<Integer, String> waveStrategyIds)
	{
		this.id = id;
		this.name = name;
		this.builtIn = builtIn;
		this.waveStrategies = waveStrategyIds == null ? new HashMap<>() : new HashMap<>(waveStrategyIds);
	}

	public String getId()
	{
		return id;
	}

	public void setId(String id)
	{
		this.id = id;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public boolean isBuiltIn()
	{
		return builtIn;
	}

	public void setBuiltIn(boolean builtIn)
	{
		this.builtIn = builtIn;
	}

	public Map<Integer, String> getWaveStrategyIds()
	{
		if (waveStrategies == null)
		{
			waveStrategies = new HashMap<>();
		}

		return waveStrategies;
	}

	public void setWaveStrategyIds(Map<Integer, String> waveStrategyIds)
	{
		this.waveStrategies = waveStrategyIds == null ? new HashMap<>() : new HashMap<>(waveStrategyIds);
	}

	public String getWaveStrategyId(int wave)
	{
		return getWaveStrategyIds().get(wave);
	}

	@Override
	public String toString()
	{
		return name == null ? id : name;
	}
}
