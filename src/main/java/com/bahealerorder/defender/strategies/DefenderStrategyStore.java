package com.bahealerorder.defender.strategies;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefenderStrategyStore
{
	private String activeRunPresetId;
	private Map<Integer, String> activeWaveStrategyIds = new HashMap<>();
	private List<DefenderRunPreset> runPresets = new ArrayList<>();
	private List<DefenderWaveStrategy> waveStrategies = new ArrayList<>();
	private String lastMarkerColor;
	private Integer lastMarkerOpacityPercent;
	private Float lastMarkerBorderWidth;

	public String getActiveRunPresetId()
	{
		return activeRunPresetId;
	}

	public void setActiveRunPresetId(String activeRunPresetId)
	{
		this.activeRunPresetId = activeRunPresetId;
	}

	public Map<Integer, String> getActiveWaveStrategyIds()
	{
		if (activeWaveStrategyIds == null)
		{
			activeWaveStrategyIds = new HashMap<>();
		}

		return activeWaveStrategyIds;
	}

	public void setActiveWaveStrategyIds(Map<Integer, String> activeWaveStrategyIds)
	{
		this.activeWaveStrategyIds = activeWaveStrategyIds == null ? new HashMap<>() : new HashMap<>(activeWaveStrategyIds);
	}

	public List<DefenderRunPreset> getRunPresets()
	{
		return runPresets == null ? Collections.emptyList() : runPresets;
	}

	public void setRunPresets(List<DefenderRunPreset> runPresets)
	{
		this.runPresets = runPresets == null ? new ArrayList<>() : new ArrayList<>(runPresets);
	}

	public List<DefenderWaveStrategy> getWaveStrategies()
	{
		return waveStrategies == null ? Collections.emptyList() : waveStrategies;
	}

	public void setWaveStrategies(List<DefenderWaveStrategy> waveStrategies)
	{
		this.waveStrategies = waveStrategies == null ? new ArrayList<>() : new ArrayList<>(waveStrategies);
	}

	public String getLastMarkerColor()
	{
		return lastMarkerColor;
	}

	public void setLastMarkerColor(String lastMarkerColor)
	{
		this.lastMarkerColor = lastMarkerColor;
	}

	public int getLastMarkerOpacityPercent()
	{
		return lastMarkerOpacityPercent == null ? DefenderMarker.DEFAULT_OPACITY_PERCENT : lastMarkerOpacityPercent;
	}

	public void setLastMarkerOpacityPercent(Integer lastMarkerOpacityPercent)
	{
		this.lastMarkerOpacityPercent = lastMarkerOpacityPercent;
	}

	public float getLastMarkerBorderWidth()
	{
		return lastMarkerBorderWidth == null ? DefenderMarker.DEFAULT_BORDER_WIDTH : lastMarkerBorderWidth;
	}

	public void setLastMarkerBorderWidth(Float lastMarkerBorderWidth)
	{
		this.lastMarkerBorderWidth = lastMarkerBorderWidth;
	}
}
