package com.bahealerorder.tilemarkers;

import java.util.ArrayList;
import java.util.List;

public class TileMarkerStrategyPresetExport
{
	private TileMarkerStrategyPreset strategyPreset;
	private List<TileMarkerSet> markerSets = new ArrayList<>();

	public TileMarkerStrategyPreset getStrategyPreset()
	{
		return strategyPreset;
	}

	public void setStrategyPreset(TileMarkerStrategyPreset strategyPreset)
	{
		this.strategyPreset = strategyPreset;
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
