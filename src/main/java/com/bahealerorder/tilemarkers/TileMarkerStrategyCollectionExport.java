package com.bahealerorder.tilemarkers;

import java.util.ArrayList;
import java.util.List;

public class TileMarkerStrategyCollectionExport
{
	private List<TileMarkerStrategyPreset> strategyPresets = new ArrayList<>();
	private List<TileMarkerSet> markerSets = new ArrayList<>();

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
