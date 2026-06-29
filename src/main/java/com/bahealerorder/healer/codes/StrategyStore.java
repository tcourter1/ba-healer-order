package com.bahealerorder.healer.codes;

import com.bahealerorder.common.strategies.WaveStrategyStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StrategyStore implements WaveStrategyStore<RunPreset, WaveCode>
{
	private String activeRunPresetId;
	private Map<Integer, String> activeWaveCodeIds = new HashMap<>();
	private List<RunPreset> runPresets = new ArrayList<>();
	private List<WaveCode> waveCodes = new ArrayList<>();

	public String getActiveRunPresetId()
	{
		return activeRunPresetId;
	}

	public void setActiveRunPresetId(String activeRunPresetId)
	{
		this.activeRunPresetId = activeRunPresetId;
	}

	public Map<Integer, String> getActiveWaveCodeIds()
	{
		if (activeWaveCodeIds == null)
		{
			activeWaveCodeIds = new HashMap<>();
		}

		return activeWaveCodeIds;
	}

	public void setActiveWaveCodeIds(Map<Integer, String> activeWaveCodeIds)
	{
		this.activeWaveCodeIds = activeWaveCodeIds == null ? new HashMap<>() : new HashMap<>(activeWaveCodeIds);
	}

	@Override
	public Map<Integer, String> getActiveWaveStrategyIds()
	{
		return getActiveWaveCodeIds();
	}

	@Override
	public void setActiveWaveStrategyIds(Map<Integer, String> activeWaveStrategyIds)
	{
		setActiveWaveCodeIds(activeWaveStrategyIds);
	}

	public List<RunPreset> getRunPresets()
	{
		return runPresets == null ? Collections.emptyList() : runPresets;
	}

	public void setRunPresets(List<RunPreset> runPresets)
	{
		this.runPresets = runPresets == null ? new ArrayList<>() : new ArrayList<>(runPresets);
	}

	public List<WaveCode> getWaveCodes()
	{
		return waveCodes == null ? Collections.emptyList() : waveCodes;
	}

	public void setWaveCodes(List<WaveCode> waveCodes)
	{
		this.waveCodes = waveCodes == null ? new ArrayList<>() : new ArrayList<>(waveCodes);
	}

	@Override
	public List<WaveCode> getWaveStrategies()
	{
		return getWaveCodes();
	}

	@Override
	public void setWaveStrategies(List<WaveCode> waveStrategies)
	{
		setWaveCodes(waveStrategies);
	}
}
