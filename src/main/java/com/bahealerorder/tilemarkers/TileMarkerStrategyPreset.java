package com.bahealerorder.tilemarkers;

import com.bahealerorder.defender.TileMarkerWaveMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TileMarkerStrategyPreset
{
	private String id;
	private String name;
	private String notes;
	private String waveMap;
	private boolean builtIn;
	private List<String> markerSetIds = new ArrayList<>();

	public TileMarkerStrategyPreset()
	{
	}

	public TileMarkerStrategyPreset(
			String id,
			String name,
			String notes,
			TileMarkerWaveMap waveMap,
			List<String> markerSetIds)
	{
		this(id, name, notes, waveMap, markerSetIds, false);
	}

	public TileMarkerStrategyPreset(
			String id,
			String name,
			String notes,
			TileMarkerWaveMap waveMap,
			List<String> markerSetIds,
			boolean builtIn)
	{
		this.id = id;
		this.name = name;
		this.notes = notes;
		this.waveMap = (waveMap == null ? TileMarkerWaveMap.WAVES_1_TO_9 : waveMap).name();
		this.builtIn = builtIn;
		setMarkerSetIds(markerSetIds);
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

	public String getNotes()
	{
		return notes;
	}

	public void setNotes(String notes)
	{
		this.notes = notes;
	}

	public TileMarkerWaveMap getWaveMap()
	{
		return TileMarkerWaveMap.fromName(waveMap);
	}

	public void setWaveMap(TileMarkerWaveMap waveMap)
	{
		this.waveMap = (waveMap == null ? TileMarkerWaveMap.WAVES_1_TO_9 : waveMap).name();
	}

	public boolean isBuiltIn()
	{
		return builtIn;
	}

	public void setBuiltIn(boolean builtIn)
	{
		this.builtIn = builtIn;
	}

	public List<String> getMarkerSetIds()
	{
		return markerSetIds == null ? Collections.emptyList() : markerSetIds;
	}

	public void setMarkerSetIds(List<String> markerSetIds)
	{
		this.markerSetIds = markerSetIds == null ? new ArrayList<>() : new ArrayList<>(markerSetIds);
	}

	@Override
	public String toString()
	{
		return name == null || name.trim().isEmpty() ? "Unnamed strategy" : name;
	}
}
