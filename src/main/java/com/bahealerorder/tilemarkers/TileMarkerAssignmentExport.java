package com.bahealerorder.tilemarkers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TileMarkerAssignmentExport
{
	private String name;
	private Map<Integer, TileMarkerWaveSelectionTarget> waveSelections = new HashMap<>();
	private List<TileMarkerStrategyPreset> strategyPresets = new ArrayList<>();
	private List<TileMarkerSet> markerSets = new ArrayList<>();

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public Map<Integer, TileMarkerWaveSelectionTarget> getWaveSelections()
	{
		if (waveSelections == null)
		{
			waveSelections = new HashMap<>();
		}
		return waveSelections;
	}

	public void setWaveSelections(Map<Integer, TileMarkerWaveSelectionTarget> waveSelections)
	{
		this.waveSelections = waveSelections == null ? new HashMap<>() : new HashMap<>(waveSelections);
	}

	public List<TileMarkerStrategyPreset> getStrategyPresets()
	{
		if (strategyPresets == null)
		{
			strategyPresets = new ArrayList<>();
		}
		return strategyPresets;
	}

	public void setStrategyPresets(List<TileMarkerStrategyPreset> strategyPresets)
	{
		this.strategyPresets = strategyPresets == null ? new ArrayList<>() : new ArrayList<>(strategyPresets);
	}

	public List<TileMarkerSet> getMarkerSets()
	{
		if (markerSets == null)
		{
			markerSets = new ArrayList<>();
		}
		return markerSets;
	}

	public void setMarkerSets(List<TileMarkerSet> markerSets)
	{
		this.markerSets = markerSets == null ? new ArrayList<>() : new ArrayList<>(markerSets);
	}
}
