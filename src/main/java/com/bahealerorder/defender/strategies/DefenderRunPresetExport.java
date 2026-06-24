package com.bahealerorder.defender.strategies;

import java.util.ArrayList;
import java.util.List;

public class DefenderRunPresetExport
{
	private DefenderRunPreset preset;
	private List<DefenderWaveStrategy> waveStrategies = new ArrayList<>();

	public DefenderRunPresetExport()
	{
	}

	public DefenderRunPresetExport(DefenderRunPreset preset, List<DefenderWaveStrategy> waveStrategies)
	{
		this.preset = preset;
		this.waveStrategies = waveStrategies == null ? new ArrayList<>() : new ArrayList<>(waveStrategies);
	}

	public DefenderRunPreset getPreset()
	{
		return preset;
	}

	public void setPreset(DefenderRunPreset preset)
	{
		this.preset = preset;
	}

	public List<DefenderWaveStrategy> getWaveStrategies()
	{
		if (waveStrategies == null)
		{
			waveStrategies = new ArrayList<>();
		}

		return waveStrategies;
	}

	public void setWaveStrategies(List<DefenderWaveStrategy> waveStrategies)
	{
		this.waveStrategies = waveStrategies == null ? new ArrayList<>() : new ArrayList<>(waveStrategies);
	}
}
