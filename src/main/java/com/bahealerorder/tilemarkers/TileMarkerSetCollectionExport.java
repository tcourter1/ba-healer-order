package com.bahealerorder.tilemarkers;

import java.util.ArrayList;
import java.util.List;

public class TileMarkerSetCollectionExport
{
	private List<TileMarkerSet> markerSets = new ArrayList<>();

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
